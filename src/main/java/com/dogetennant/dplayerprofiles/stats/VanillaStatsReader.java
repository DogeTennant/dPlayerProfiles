package com.dogetennant.dplayerprofiles.stats;

import com.dogetennant.dplayerprofiles.model.StatsSnapshot;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reads Minecraft's own per-player statistics, which the server tracks for everyone who has
 * ever joined: live through the Bukkit API for online players, and straight from the
 * world/stats/<uuid>.json files for offline players (the API would re-parse that file on
 * every single call, which is far too slow for summing blocks mined over all block types).
 */
public final class VanillaStatsReader {

    /** Every block type MINE_BLOCK can be asked about; computed once. */
    private static final List<Material> BLOCK_MATERIALS = new ArrayList<>();

    static {
        for (Material m : Material.values()) {
            if (!m.isLegacy() && m.isBlock()) BLOCK_MATERIALS.add(m);
        }
    }

    private VanillaStatsReader() {}

    /** Main thread only. Balance is left null; the caller fills it in. */
    public static StatsSnapshot fromOnline(Player player) {
        long blocksMined = 0;
        for (Material m : BLOCK_MATERIALS) {
            try {
                blocksMined += player.getStatistic(Statistic.MINE_BLOCK, m);
            } catch (IllegalArgumentException ignored) { /* no statistic for this block */ }
        }
        return new StatsSnapshot(player.getUniqueId(), player.getName(),
                player.getStatistic(Statistic.MOB_KILLS),
                player.getStatistic(Statistic.PLAYER_KILLS),
                player.getStatistic(Statistic.DEATHS),
                player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L, // despite the name: play time in ticks
                blocksMined,
                null);
    }

    /**
     * Parses a vanilla stats file. Safe to call off the main thread - it touches no Bukkit API.
     *
     * @param username may be null when unknown
     */
    public static StatsSnapshot fromStatsFile(File file, UUID uuid, String username) throws IOException {
        JsonObject stats;
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            stats = root.isJsonObject() ? root.getAsJsonObject().getAsJsonObject("stats") : null;
        } catch (RuntimeException e) {
            throw new IOException("Malformed stats file: " + file.getName(), e);
        }

        JsonObject custom = stats == null ? null : stats.getAsJsonObject("minecraft:custom");
        // Renamed from play_one_minute to play_time in 1.17; old files may still carry the old key
        long playTicks = getLong(custom, "minecraft:play_time");
        if (playTicks == 0) playTicks = getLong(custom, "minecraft:play_one_minute");

        long blocksMined = 0;
        JsonObject mined = stats == null ? null : stats.getAsJsonObject("minecraft:mined");
        if (mined != null) {
            for (Map.Entry<String, JsonElement> e : mined.entrySet()) {
                if (e.getValue().isJsonPrimitive()) blocksMined += e.getValue().getAsLong();
            }
        }

        return new StatsSnapshot(uuid, username,
                getLong(custom, "minecraft:mob_kills"),
                getLong(custom, "minecraft:player_kills"),
                getLong(custom, "minecraft:deaths"),
                playTicks / 20L,
                blocksMined,
                null);
    }

    /** The UUID a stats file belongs to, or null if the file name is not "<uuid>.json". */
    public static UUID uuidFromFileName(String fileName) {
        if (!fileName.endsWith(".json")) return null;
        try {
            return UUID.fromString(fileName.substring(0, fileName.length() - 5));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static long getLong(JsonObject obj, String key) {
        if (obj == null) return 0;
        JsonElement el = obj.get(key);
        return el != null && el.isJsonPrimitive() ? el.getAsLong() : 0;
    }
}
