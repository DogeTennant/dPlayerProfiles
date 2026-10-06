package com.dogetennant.dplayerprofiles.achievement;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.config.MainConfig;
import com.dogetennant.dplayerprofiles.util.LogUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Remembers which block positions have been placed by a player, so that breaking
 * them (or re-placing at the same spot) does not count towards achievements.
 *
 * <p>Positions are held per chunk in memory and persisted in the chunk's own
 * {@link PersistentDataContainer}, so they travel with the world save and only
 * loaded chunks cost memory. A position is added when placed and never removed:
 * that caps a place/break loop at a single credit per position, which is what
 * makes the farm unprofitable without needing to store who placed what.
 *
 * <p>All methods are main-thread only - they touch Bukkit chunk state.
 */
public class PlacedBlockTracker implements Listener {

    /** How often cached chunks are written back, as a safety net against an unclean shutdown. */
    private static final long FLUSH_INTERVAL_TICKS = 20L * 60L * 5L; // 5 minutes

    private final DPlayerProfiles plugin;
    private final NamespacedKey key;
    /** world UID -> chunk key -> tracked positions. Only loaded chunks are cached. */
    private final Map<UUID, Long2ObjectMap<TrackedChunk>> cache = new HashMap<>();

    private boolean enabled;
    private int maxPerChunk;
    private int flushTaskId = -1;

    public PlacedBlockTracker(DPlayerProfiles plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "placed_blocks");
        applyConfig();
    }

    /** Re-reads the anti-farm settings. Safe to call at runtime (e.g. from /dp reload). */
    public void applyConfig() {
        MainConfig config = plugin.getConfigManager().get();
        this.enabled = config.antiFarmEnabled;
        this.maxPerChunk = config.antiFarmMaxPerChunk; // <= 0 means no cap
        if (!enabled) {
            flushAll();
            cache.clear();
        }
    }

    public void start() {
        if (flushTaskId != -1) return;
        flushTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(
                plugin, this::flushAll, FLUSH_INTERVAL_TICKS, FLUSH_INTERVAL_TICKS);
    }

    public void shutdown() {
        if (flushTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(flushTaskId);
            flushTaskId = -1;
        }
        flushAll();
        cache.clear();
    }

    /** True if a player has already placed a block at this position. */
    public boolean isPlayerPlaced(Block block) {
        if (!enabled) return false;
        return lookup(block.getChunk()).positions.contains(packLocal(block));
    }

    /**
     * Records that a player placed {@code material} at this position, provided some
     * block achievement could be affected by it. Positions for materials no achievement
     * watches are not worth the memory or the region file space.
     */
    public void markPlayerPlaced(Block block, String material) {
        if (!enabled) return;
        if (!plugin.getAchievementConfigLoader().isBlockMaterialWatched(material)) return;

        TrackedChunk tracked = lookup(block.getChunk());
        if (maxPerChunk > 0 && tracked.positions.size() >= maxPerChunk) {
            if (!tracked.capWarned) {
                tracked.capWarned = true;
                LogUtil.warn("Anti-farm tracking cap (" + maxPerChunk + ") reached in chunk "
                        + block.getChunk().getX() + "," + block.getChunk().getZ()
                        + " of " + block.getWorld().getName()
                        + " - further placements there are not tracked. Raise anti-farm.max-tracked-per-chunk"
                        + " if this chunk should stay protected.");
            }
            return;
        }
        if (tracked.positions.add(packLocal(block))) {
            tracked.dirty = true;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        Long2ObjectMap<TrackedChunk> world = cache.get(event.getWorld().getUID());
        if (world == null) return;
        TrackedChunk tracked = world.remove(chunkKey(event.getChunk().getX(), event.getChunk().getZ()));
        if (tracked != null) write(event.getChunk(), tracked);
    }

    /** Writes every dirty cached chunk back to its PersistentDataContainer. */
    public void flushAll() {
        for (Map.Entry<UUID, Long2ObjectMap<TrackedChunk>> worldEntry : cache.entrySet()) {
            World world = plugin.getServer().getWorld(worldEntry.getKey());
            if (world == null) continue;

            Iterator<Long2ObjectMap.Entry<TrackedChunk>> it =
                    worldEntry.getValue().long2ObjectEntrySet().iterator();
            while (it.hasNext()) {
                Long2ObjectMap.Entry<TrackedChunk> entry = it.next();
                int chunkX = (int) (entry.getLongKey() >> 32);
                int chunkZ = (int) entry.getLongKey();
                if (!world.isChunkLoaded(chunkX, chunkZ)) {
                    // Unloaded without firing our handler - drop it rather than leak the entry.
                    it.remove();
                    continue;
                }
                write(world.getChunkAt(chunkX, chunkZ), entry.getValue());
            }
        }
    }

    /** Returns the cached positions for this chunk, hydrating from the chunk's PDC on first touch. */
    private TrackedChunk lookup(Chunk chunk) {
        Long2ObjectMap<TrackedChunk> world = cache.computeIfAbsent(
                chunk.getWorld().getUID(), uid -> new Long2ObjectOpenHashMap<>());
        long chunkKey = chunkKey(chunk.getX(), chunk.getZ());

        TrackedChunk tracked = world.get(chunkKey);
        if (tracked == null) {
            long[] stored = chunk.getPersistentDataContainer().get(key, PersistentDataType.LONG_ARRAY);
            tracked = new TrackedChunk(stored == null ? new LongOpenHashSet() : new LongOpenHashSet(stored));
            world.put(chunkKey, tracked);
        }
        return tracked;
    }

    private void write(Chunk chunk, TrackedChunk tracked) {
        if (!tracked.dirty) return;
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        if (tracked.positions.isEmpty()) {
            pdc.remove(key);
        } else {
            pdc.set(key, PersistentDataType.LONG_ARRAY, tracked.positions.toLongArray());
        }
        tracked.dirty = false;
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    /** Packs a block position relative to its chunk: y in the high bits, x and z in one byte. */
    private static long packLocal(Block block) {
        return ((long) block.getY() << 8) | ((block.getX() & 15) << 4) | (block.getZ() & 15);
    }

    private static final class TrackedChunk {
        final LongOpenHashSet positions;
        boolean dirty;
        boolean capWarned;

        TrackedChunk(LongOpenHashSet positions) {
            this.positions = positions;
        }
    }
}
