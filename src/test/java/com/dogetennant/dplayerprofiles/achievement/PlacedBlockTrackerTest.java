package com.dogetennant.dplayerprofiles.achievement;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.Fakes;
import com.dogetennant.dplayerprofiles.config.AchievementConfigLoader;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Anti-farm: blocks a player placed do not count when broken - wherever they were moved to. */
class PlacedBlockTrackerTest {

    private final DPlayerProfiles plugin = Fakes.plugin(Fakes.config());
    private final World world = mock(World.class);
    private final Map<String, Block> blocks = new HashMap<>();
    private final Map<Long, Chunk> chunks = new HashMap<>();
    private PlacedBlockTracker tracker;

    @BeforeEach
    void setUp() {
        when(world.getUID()).thenReturn(UUID.randomUUID());
        when(world.getName()).thenReturn("world");
        AchievementConfigLoader loader = mock(AchievementConfigLoader.class);
        when(loader.isBlockMaterialWatched(any())).thenReturn(true);
        when(plugin.getAchievementConfigLoader()).thenReturn(loader);
        tracker = new PlacedBlockTracker(plugin);
    }

    /** The block at x, y, z (the same mock every time, with neighbours). */
    private Block at(int x, int y, int z) {
        return blocks.computeIfAbsent(x + "," + y + "," + z, k -> {
            Block block = mock(Block.class);
            when(block.getWorld()).thenReturn(world);
            when(block.getX()).thenReturn(x);
            when(block.getY()).thenReturn(y);
            when(block.getZ()).thenReturn(z);
            Chunk chunk = chunk(x >> 4, z >> 4);
            when(block.getChunk()).thenReturn(chunk);
            for (BlockFace face : List.of(BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN,
                    BlockFace.NORTH, BlockFace.SOUTH)) {
                when(block.getRelative(face)).thenAnswer(call ->
                        at(x + face.getModX(), y + face.getModY(), z + face.getModZ()));
            }
            return block;
        });
    }

    private Chunk chunk(int cx, int cz) {
        return chunks.computeIfAbsent(((long) cx << 32) | (cz & 0xFFFFFFFFL), k -> {
            Chunk chunk = mock(Chunk.class);
            when(chunk.getX()).thenReturn(cx);
            when(chunk.getZ()).thenReturn(cz);
            when(chunk.getWorld()).thenReturn(world);
            when(chunk.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
            return chunk;
        });
    }

    @Test
    void aPlacedBlockIsRememberedAtItsSpotOnly() {
        tracker.markPlayerPlaced(at(1, 64, 1), "STONE");

        assertThat(tracker.isPlayerPlaced(at(1, 64, 1))).isTrue();
        assertThat(tracker.isPlayerPlaced(at(1, 65, 1))).isFalse();
        assertThat(tracker.isPlayerPlaced(at(17, 64, 1))).isFalse();      // the same spot in the next chunk
        assertThat(tracker.isPlayerPlaced(at(1, -60, 1))).isFalse();
    }

    @Test
    void aPlacedBlockPushedByAPistonIsStillPlacedWhereItLands() {
        tracker.markPlayerPlaced(at(1, 64, 1), "STONE");
        BlockPistonExtendEvent push = mock(BlockPistonExtendEvent.class);
        when(push.getBlocks()).thenReturn(List.of(at(1, 64, 1)));
        when(push.getDirection()).thenReturn(BlockFace.EAST);

        Fakes.fire(tracker, push);

        assertThat(tracker.isPlayerPlaced(at(2, 64, 1))).isTrue();
        assertThat(tracker.isPlayerPlaced(at(1, 64, 1))).isFalse();
    }

    @Test
    void aRowOfPlacedBlocksPushedTogetherStaysPlaced() {
        tracker.markPlayerPlaced(at(1, 64, 1), "STONE");
        tracker.markPlayerPlaced(at(2, 64, 1), "STONE");
        Block first = at(1, 64, 1);
        Block second = at(2, 64, 1);
        BlockPistonExtendEvent push = mock(BlockPistonExtendEvent.class);
        when(push.getBlocks()).thenReturn(List.of(first, second));
        when(push.getDirection()).thenReturn(BlockFace.EAST);

        Fakes.fire(tracker, push);

        assertThat(tracker.isPlayerPlaced(at(1, 64, 1))).isFalse();
        assertThat(tracker.isPlayerPlaced(at(2, 64, 1))).isTrue();
        assertThat(tracker.isPlayerPlaced(at(3, 64, 1))).isTrue();
    }

    @Test
    void aPlacedBlockPulledAcrossAChunkBorderIsStillPlaced() {
        // a sticky piston at x = 14 facing east pulls the block at 16 back to 15
        tracker.markPlayerPlaced(at(16, 64, 1), "STONE");
        BlockPistonRetractEvent pull = mock(BlockPistonRetractEvent.class);
        when(pull.getBlocks()).thenReturn(List.of(at(16, 64, 1)));
        when(pull.getDirection()).thenReturn(BlockFace.EAST);

        Fakes.fire(tracker, pull);

        assertThat(tracker.isPlayerPlaced(at(15, 64, 1))).isTrue();
        assertThat(tracker.isPlayerPlaced(at(16, 64, 1))).isFalse();
    }

    @Test
    void aNaturalBlockPushedByAPistonStaysNatural() {
        Block natural = at(1, 64, 1);
        BlockPistonExtendEvent push = mock(BlockPistonExtendEvent.class);
        when(push.getBlocks()).thenReturn(List.of(natural));
        when(push.getDirection()).thenReturn(BlockFace.EAST);

        Fakes.fire(tracker, push);

        assertThat(tracker.isPlayerPlaced(at(2, 64, 1))).isFalse();
    }
}
