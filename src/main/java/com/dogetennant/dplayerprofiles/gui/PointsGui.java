package com.dogetennant.dplayerprofiles.gui;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.lang.MessageKey;
import com.dogetennant.dplayerprofiles.lang.Placeholder;
import com.dogetennant.dplayerprofiles.model.PlayerProfile;
import com.dogetennant.dplayerprofiles.util.ColorUtil;
import com.dogetennant.dplayerprofiles.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Zig-zag (serpentine) points progression GUI - every path slot is a numbered node.
 *
 * Layout (6 rows × 9 columns):
 *   Row 0 (slots  0- 8) - decorative fill
 *   Row 1 (slots  9-17) - HIGH segment + top of turns
 *   Row 2 (slots 18-26) - vertical turn mid-upper
 *   Row 3 (slots 27-35) - vertical turn mid-lower
 *   Row 4 (slots 36-44) - LOW segment + bottom of turns
 *   Row 5 (slots 45-53) - navigation (back · ◄ scroll ► · info)
 *
 * 14 nodes per 8-column period:
 *   p  0, 1, 2  → HIGH   row 1,  cols period×8 + 0,1,2
 *   p  3, 4, 5, 6 → TURN DOWN   col period×8+3,  rows 1,2,3,4
 *   p  7, 8, 9  → LOW    row 4,  cols period×8 + 4,5,6
 *   p 10,11,12,13 → TURN UP     col period×8+7,  rows 4,3,2,1
 *
 * Pagination slides one column at a time - the viewport never jumps a full page.
 *
 * Milestone nodes (every N-th, configurable) use solid concrete so they visually stand out.
 */
public class PointsGui extends BaseGui {

    private static final int ROWS = 6;
    private static final int COLS = 9;
    private static final int NODES_PER_PERIOD = 14;
    private static final int COLS_PER_PERIOD  = 8;

    // Regular node materials
    private static final Material NODE_LOCKED     = Material.RED_STAINED_GLASS_PANE;
    private static final Material NODE_UNLOCKED   = Material.GREEN_STAINED_GLASS_PANE;
    // Milestone node materials (every N-th node)
    private static final Material MILESTONE_LOCKED   = Material.RED_CONCRETE;
    private static final Material MILESTONE_UNLOCKED = Material.GREEN_CONCRETE;

    private final DPlayerProfiles plugin;
    private final PlayerProfile profile;
    private final BaseGui previousGui;

    private final int pointsPerNode;
    private final int nodeCount;
    private final int milestoneInterval; // 0 = disabled

    // Recomputed on every render
    private int totalPoints;
    private int unlockedCount; // nodes unlocked so far (0 = none)

    private int columnOffset; // leftmost visible global column

    private final Map<Integer, Consumer<Player>> clickHandlers = new HashMap<>();

    public PointsGui(DPlayerProfiles plugin, PlayerProfile profile, BaseGui previousGui) {
        this.plugin = plugin;
        this.profile = profile;
        this.previousGui = previousGui;
        this.pointsPerNode     = plugin.getConfigManager().get().pointsPerNode;
        this.nodeCount         = plugin.getConfigManager().get().pointsNodeCount;
        this.milestoneInterval = plugin.getConfigManager().get().pointsMilestoneInterval;
    }

    // 
    // BaseGui contract
    // 

    @Override
    public void open(Player viewer) {
        String rawTitle = plugin.getLangManager().getRaw(MessageKey.POINTS_GUI_TITLE,
                Placeholder.of("player", profile.getUsername()));
        inventory = Bukkit.createInventory(null, ROWS * COLS, ColorUtil.parse(rawTitle));

        recalculate();
        columnOffset = computeInitialOffset();

        render(viewer);
        viewer.openInventory(inventory);
    }

    @Override
    public void handleClick(int slot, Player clicker) {
        Consumer<Player> handler = clickHandlers.get(slot);
        if (handler != null) handler.accept(clicker);
    }

    // 
    // Rendering
    // 

    private void render(Player viewer) {
        clickHandlers.clear();
        inventory.clear();
        recalculate();

        // Gray filler behind everything
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < ROWS * COLS; i++) {
            inventory.setItem(i, filler);
        }

        // Place every node that falls within the current 9-column viewport
        boolean isAdmin = viewer.hasPermission("dplayerprofiles.admin");
        for (int nodeIndex = 0; nodeIndex < nodeCount; nodeIndex++) {
            int[] pos = nodePosition(nodeIndex); // [globalCol, row]
            int localCol = pos[0] - columnOffset;
            if (localCol < 0 || localCol >= COLS) continue;
            int slot = pos[1] * 9 + localCol;
            inventory.setItem(slot, buildNodeItem(nodeIndex, isAdmin));
            if (isAdmin) {
                final int ni = nodeIndex;
                clickHandlers.put(slot, p -> plugin.getGuiManager().open(p,
                        new NodeRewardEditorGui(plugin, ni, this)));
            }
        }

        renderNavigation();
    }

    private void recalculate() {
        totalPoints = plugin.getAchievementManager().calculateTotalPoints(profile);
        unlockedCount = (nodeCount > 0 && pointsPerNode > 0)
                ? Math.min(totalPoints / pointsPerNode, nodeCount)
                : 0;
    }

    // 
    // Navigation row (row 5)
    // 

    private void renderNavigation() {
        // Slot 45 - Back to profile
        inventory.setItem(45, new ItemBuilder(Material.ARROW)
                .name(plugin.getLangManager().getRaw(MessageKey.POINTS_BTN_BACK)).build());
        clickHandlers.put(45, p -> {
            if (previousGui != null) plugin.getGuiManager().open(p, previousGui);
        });

        // Slot 47 - Scroll left
        if (columnOffset > 0) {
            inventory.setItem(47, new ItemBuilder(Material.ARROW)
                    .name(plugin.getLangManager().getRaw(MessageKey.POINTS_BTN_SCROLL_LEFT)).build());
            clickHandlers.put(47, p -> {
                if (columnOffset > 0) { columnOffset--; render(p); }
            });
        }

        // Slot 49 - Summary info
        inventory.setItem(49, buildInfoItem());

        // Slot 51 - Scroll right
        if (columnOffset < computeMaxOffset()) {
            inventory.setItem(51, new ItemBuilder(Material.ARROW)
                    .name(plugin.getLangManager().getRaw(MessageKey.POINTS_BTN_SCROLL_RIGHT)).build());
            clickHandlers.put(51, p -> {
                int max = computeMaxOffset();
                if (columnOffset < max) { columnOffset++; render(p); }
            });
        }
    }

    // 
    // Item builders
    // 

    private ItemStack buildNodeItem(int nodeIndex, boolean isAdmin) {
        boolean isUnlocked = nodeIndex < unlockedCount;
        // "Current" = the most recently unlocked node; glow advances when the next node unlocks.
        boolean isCurrent   = isUnlocked && nodeIndex == unlockedCount - 1;
        boolean isMilestone = milestoneInterval > 0 && (nodeIndex + 1) % milestoneInterval == 0;
        int pointsNeeded    = (nodeIndex + 1) * pointsPerNode;

        Material mat = isMilestone
                ? (isUnlocked ? MILESTONE_UNLOCKED : MILESTONE_LOCKED)
                : (isUnlocked ? NODE_UNLOCKED      : NODE_LOCKED);

        String name = plugin.getLangManager().getRaw(MessageKey.POINTS_NODE_NAME,
                Placeholder.of("node", nodeIndex + 1));

        MessageKey statusKey = isCurrent ? MessageKey.POINTS_NODE_STATUS_CURRENT
                : (isUnlocked ? MessageKey.POINTS_NODE_STATUS_UNLOCKED : MessageKey.POINTS_NODE_STATUS_LOCKED);

        List<String> lore = new ArrayList<>();
        lore.add(plugin.getLangManager().getRaw(statusKey));
        lore.add(plugin.getLangManager().getRaw(MessageKey.POINTS_NODE_PROGRESS,
                Placeholder.of("current", Math.min(totalPoints, pointsNeeded)),
                Placeholder.of("target", pointsNeeded)));
        if (isMilestone) {
            lore.add(plugin.getLangManager().getRaw(MessageKey.POINTS_MILESTONE_INDICATOR));
        }

        List<String> rewardLore = plugin.getAchievementRewardStorage()
                .getRewardLore("points_node_" + nodeIndex);
        if (!rewardLore.isEmpty()) {
            lore.add("");
            lore.add(plugin.getLangManager().getRaw(MessageKey.POINTS_REWARDS_HEADER));
            lore.addAll(rewardLore);
        }

        if (isAdmin) {
            lore.add("");
            lore.add(plugin.getLangManager().getRaw(MessageKey.POINTS_NODE_ADMIN_HINT));
        }

        return new ItemBuilder(mat)
                .name(name)
                .lore(lore.toArray(new String[0]))
                .glow(isCurrent)
                .build();
    }

private ItemStack buildInfoItem() {
        List<String> lore = new ArrayList<>();
        lore.add(plugin.getLangManager().getRaw(MessageKey.POINTS_INFO_TOTAL,
                Placeholder.of("total", totalPoints)));
        lore.add(plugin.getLangManager().getRaw(MessageKey.POINTS_INFO_CURRENT_NODE,
                Placeholder.of("node", unlockedCount),
                Placeholder.of("total_nodes", nodeCount)));
        if (unlockedCount < nodeCount) {
            int remaining = (unlockedCount + 1) * pointsPerNode - totalPoints;
            lore.add(plugin.getLangManager().getRaw(MessageKey.POINTS_INFO_NEXT_IN,
                    Placeholder.of("pts", remaining)));
        } else {
            lore.add(plugin.getLangManager().getRaw(MessageKey.POINTS_INFO_ALL_UNLOCKED));
        }
        return new ItemBuilder(Material.NETHER_STAR)
                .name(plugin.getLangManager().getRaw(MessageKey.POINTS_INFO_TITLE))
                .lore(lore.toArray(new String[0]))
                .build();
    }

    // 
    // Column-offset helpers
    // 

    /** On open: centre the viewport on the first locked node (or last if all unlocked). */
    private int computeInitialOffset() {
        if (nodeCount <= 0) return 0;
        int targetNode = Math.min(unlockedCount, nodeCount - 1);
        int targetGc   = nodePosition(targetNode)[0];
        int offset     = Math.max(0, targetGc - 4); // put target node at viewport col 4
        return Math.min(offset, computeMaxOffset());
    }

    /** Maximum offset so the last node always sits inside the 9-column viewport. */
    private int computeMaxOffset() {
        if (nodeCount <= 0) return 0;
        return Math.max(0, nodePosition(nodeCount - 1)[0] - (COLS - 1));
    }

    // 
    // Path geometry
    // 

    /**
     * Returns {globalColumn, row} for a 0-indexed node.
     *
     * The path snakes through a 4-row (rows 1-4) corridor:
     *   p  0-2  : HIGH    → row 1,  cols period×8 + p
     *   p  3-6  : TURN↓   → col period×8+3,  rows 1,2,3,4  (top → bottom)
     *   p  7-9  : LOW     → row 4,  cols period×8 + 4+(p-7)
     *   p 10-13 : TURN↑   → col period×8+7,  rows 4,3,2,1  (bottom → top)
     */
    private static int[] nodePosition(int nodeIndex) {
        int period = nodeIndex / NODES_PER_PERIOD;
        int p      = nodeIndex % NODES_PER_PERIOD;
        int gc, row;
        if (p < 3) {
            gc  = period * COLS_PER_PERIOD + p;
            row = 1;
        } else if (p < 7) {
            gc  = period * COLS_PER_PERIOD + 3;
            row = 1 + (p - 3);       // rows 1,2,3,4
        } else if (p < 10) {
            gc  = period * COLS_PER_PERIOD + 4 + (p - 7);
            row = 4;
        } else {
            gc  = period * COLS_PER_PERIOD + 7;
            row = 4 - (p - 10);      // rows 4,3,2,1
        }
        return new int[]{gc, row};
    }
}
