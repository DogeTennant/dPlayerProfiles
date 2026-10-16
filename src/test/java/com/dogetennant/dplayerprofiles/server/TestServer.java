package com.dogetennant.dplayerprofiles.server;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.model.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.plugin.ServicePriority;
import org.jetbrains.annotations.NotNull;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.scheduler.BukkitSchedulerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * The whole plugin on a MockBukkit server: its own config, translations, layouts and SQLite
 * database in a temporary folder, every listener registered, a world, and Vault with an in-memory
 * economy ({@link TestEconomy}). dTournaments, OneInTheChamberReborn and FluxShops are present by
 * name, so the plugin hooks their events; reward commands can run the test command {@code record}.
 * Stop it with {@link #stop()}.
 *
 * <p>Achievements are written into the plugin folder by the test ({@link #achievement}) and read
 * with {@code /dp reload}; the shipped example file is ignored by the plugin.</p>
 */
final class TestServer {

    final ServerMock server;
    final WorldMock world;
    final TestEconomy economy;
    final DPlayerProfiles plugin;

    /** Every run of the test command {@code /record <words>} (reward commands), as its words. */
    final List<String> recorded = new CopyOnWriteArrayList<>();

    /** Every warning or error the plugin logged since the test server started, its start-up included. */
    final List<String> warnings;

    /** MockBukkit's pool for asynchronous tasks, made one thread, first in first out. */
    private final ExecutorService backgroundPool;

    private TestServer(ServerMock server, WorldMock world, TestEconomy economy, DPlayerProfiles plugin,
                       ExecutorService backgroundPool) {
        this.server = server;
        this.world = world;
        this.economy = economy;
        this.plugin = plugin;
        this.warnings = WARNINGS.messages;
        this.backgroundPool = backgroundPool;
    }

    private static final Warnings WARNINGS = new Warnings();

    static TestServer start() {
        removeMockitoServer();
        WARNINGS.messages.clear();
        WorldsServer server = MockBukkit.mock(new WorldsServer());
        try {
            ExecutorService backgroundPool = oneBackgroundThread(server);
            Logger.getLogger("").addHandler(WARNINGS);
            WorldMock world = new FolderWorld(server, "world");
            server.addWorld(world);
            TestEconomy economy = new TestEconomy();
            server.getServicesManager().register(Economy.class, economy.vault, MockBukkit.createMockPlugin("Vault"),
                    ServicePriority.Normal);
            // our plugins it hooks into: present by name, the tests fire their events
            for (String name : List.of("dTournaments", "OneInTheChamberReborn", "FluxShops")) {
                MockBukkit.createMockPlugin(name);
            }
            DPlayerProfiles plugin = MockBukkit.load(DPlayerProfiles.class);

            TestServer mc = new TestServer(server, world, economy, plugin, backgroundPool);
            server.getCommandMap().register("test", new Command("record") {
                @Override
                public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
                    mc.recorded.add(String.join(" ", args));
                    return true;
                }
            });
            mc.settle();
            return mc;
        } catch (RuntimeException | Error e) {
            stopServer(server);
            throw e;
        }
    }

    /** Stops the server (the plugin's onDisable runs) and deletes its world folder. */
    void stop() {
        stopServer((WorldsServer) server);
    }

    private static void stopServer(WorldsServer server) {
        MockBukkit.unmock();
        Logger.getLogger("").removeHandler(WARNINGS);
        try (Stream<Path> files = Files.walk(server.worldContainer.toPath())) {
            files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** MockBukkit has no folder for worlds; the web-stats export reads the players' stats files there. */
    static final class WorldsServer extends ServerMock {

        final File worldContainer;

        WorldsServer() {
            try {
                worldContainer = Files.createTempDirectory("dplayerprofiles-worlds").toFile();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public File getWorldContainer() {
            return worldContainer;
        }
    }

    /** A grass world (ground at y 4, as MockBukkit's default) with a folder in the server's world container. */
    static final class FolderWorld extends WorldMock {

        private final File folder;

        FolderWorld(WorldsServer server, String name) {
            super(Material.GRASS_BLOCK, 4);
            setName(name);
            folder = new File(server.worldContainer, name);
        }

        @Override
        public File getWorldFolder() {
            return folder;
        }
    }

    /**
     * Keeps every record of the plugin's logger at WARNING or above. Paper gives the plugin a logger
     * of its own that is not registered by name, so the handler sits on the root logger (removed
     * again by {@link #stop()}) and picks the plugin's records by their logger name.
     */
    private static final class Warnings extends Handler {

        final List<String> messages = new CopyOnWriteArrayList<>();

        @Override
        public void publish(LogRecord record) {
            if ("dPlayerProfiles".equals(record.getLoggerName()) && record.getLevel().intValue() >= Level.WARNING.intValue()) {
                messages.add(record.getMessage());
            }
        }

        @Override public void flush() {}
        @Override public void close() {}
    }

    /**
     * MockBukkit runs each asynchronous task on a thread of its own, and a task that has just been
     * handed over shows neither as queued nor as active - no check can tell "idle" from "about to
     * run". With one thread taking tasks in order, a marker queued behind them finishes last.
     */
    private static ExecutorService oneBackgroundThread(ServerMock server) {
        ThreadPoolExecutor ordered = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        try {
            Field field = BukkitSchedulerMock.class.getDeclaredField("pool");
            field.setAccessible(true);
            ((ExecutorService) field.get(server.getScheduler())).shutdown();
            field.set(server.getScheduler(), ordered);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot replace MockBukkit's pool for asynchronous tasks", e);
        }
        return ordered;
    }

    /** The other tests leave a Mockito server in {@code Bukkit}; MockBukkit refuses to start next to it. */
    private static void removeMockitoServer() {
        if (Bukkit.getServer() == null || Bukkit.getServer() instanceof ServerMock) return;
        try {
            Field field = Bukkit.class.getDeclaredField("server");
            field.setAccessible(true);
            field.set(null, null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot remove the other tests' server", e);
        }
    }

    /** Stops and starts the plugin again with the same data folder, as a server restart does. */
    void restart() {
        server.getPluginManager().disablePlugin(plugin);
        server.getPluginManager().enablePlugin(plugin);
        settle();
    }

    Path file(String name) {
        return plugin.getDataFolder().toPath().resolve(name);
    }

    /** Writes a file into the plugin folder, as an admin editing it by hand. */
    void write(String name, String content) {
        try {
            Path target = file(name);
            Files.createDirectories(target.getParent());
            Files.writeString(target, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    String read(String name) {
        try {
            return Files.readString(file(name));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * An achievement file: {@code id}, a trigger and how many, and points. Read with the next
     * {@code /dp reload}.
     */
    void achievement(String id, String trigger, String target, long count, int points) {
        write("achievements/" + id + ".yml", """
                id: %s
                display-name: "%s"
                description: "test"
                icon: STONE
                points: %d
                trigger:
                  type: %s
                  target: '%s'
                  count: %d
                """.formatted(id, id, points, trigger, target, count));
    }

    /** A player joins, and the login (its database read and what it hands back) has finished. */
    PlayerMock join(String name) {
        PlayerMock player = server.addPlayer(name);
        settle();
        return player;
    }

    /** An admin (op) is online. */
    PlayerMock admin(String name) {
        PlayerMock admin = join(name);
        admin.setOp(true);
        return admin;
    }

    PlayerProfile profile(PlayerMock player) {
        return plugin.getProfileManager().get(player.getUniqueId());
    }

    /** Runs a command as the console, then lets what it started finish. */
    void console(String command) {
        server.dispatchCommand(server.getConsoleSender(), command);
        settle();
    }

    /** The player runs a command, then what it started finishes. */
    void command(PlayerMock player, String command) {
        player.performCommand(command);
        settle();
    }

    /**
     * Runs the next tick, then waits for the plugin's database thread and MockBukkit's background
     * pool, a few rounds for results handed back to the main thread and work they start.
     */
    void settle() {
        for (int i = 0; i < 3; i++) {
            server.getScheduler().performOneTick();
            awaitBackgroundTasks();
            awaitDatabase();
        }
    }

    /** Waits for a marker queued behind every database task so far (a stuck one fails). */
    private void awaitDatabase() {
        if (plugin == null || plugin.getDatabaseQueue() == null) return;
        CountDownLatch done = new CountDownLatch(1);
        plugin.getDatabaseQueue().submit(done::countDown);
        try {
            if (!done.await(30, TimeUnit.SECONDS)) throw new IllegalStateException("the database thread is stuck");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /** Waits for a marker queued behind every asynchronous task handed over so far (a stuck one fails). */
    private void awaitBackgroundTasks() {
        try {
            backgroundPool.submit(() -> { }).get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new IllegalStateException("a background task is stuck", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The player clicks a raw slot of the screen they have open (the screen first, then their inventory). */
    InventoryClickEvent click(PlayerMock player, int rawSlot, ClickType type, InventoryAction action) {
        ClickableView view = new ClickableView(player, player.getOpenInventory().getTopInventory());
        return fire(new InventoryClickEvent(view, InventoryType.SlotType.CONTAINER, rawSlot, type, action));
    }

    InventoryClickEvent click(PlayerMock player, int rawSlot) {
        return click(player, rawSlot, ClickType.LEFT, InventoryAction.PICKUP_ALL);
    }

    <E extends Event> E fire(E event) {
        server.getPluginManager().callEvent(event);
        return event;
    }

    /** Every message the player got since the last call, as plain text. */
    static List<String> messages(PlayerMock player) {
        List<String> messages = new ArrayList<>();
        for (Component message; (message = player.nextComponentMessage()) != null; ) {
            messages.add(PlainTextComponentSerializer.plainText().serialize(message));
        }
        return messages;
    }
}
