package com.dogetennant.dplayerprofiles.gui;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.database.DatabaseManager.LeaderboardEntry;
import com.dogetennant.dplayerprofiles.lang.MessageKey;
import com.dogetennant.dplayerprofiles.lang.Placeholder;
import com.dogetennant.dplayerprofiles.model.AchievementConfig;
import com.dogetennant.dplayerprofiles.model.BadgeConfig;
import com.dogetennant.dplayerprofiles.model.PlayerProfile;
import com.dogetennant.dplayerprofiles.model.gui.GuiComponent;
import com.dogetennant.dplayerprofiles.model.gui.GuiLayout;
import com.dogetennant.dplayerprofiles.model.gui.StaticItem;
import com.dogetennant.dplayerprofiles.util.ColorUtil;
import com.dogetennant.dplayerprofiles.util.ItemBuilder;
import com.dogetennant.dplayerprofiles.util.LogUtil;
import com.dogetennant.dplayerprofiles.util.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.sql.SQLException;
import java.util.*;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A GUI that renders its layout from a {@link GuiLayout} config object.
 * Replaces the hardcoded ProfileGui, AchievementsGui, and LeaderboardGui.
 */
public class DynamicGui extends BaseGui {

    private final DPlayerProfiles plugin;
    private final GuiLayout layout;
    /** May be null for GUIs that don't display player-specific data (e.g. leaderboard). */
    private final PlayerProfile profile;
    /** Previous GUI to return to when a "back" action is triggered. */
    private DynamicGui previousGui;

    //  per-instance render state 
    /** Currently active category filter; null means "All". */
    private String selectedCategory = null;
    /** Current page per list component ID (0-based). */
    private final Map<String, Integer> pages = new HashMap<>();
    /** Total pages per list component ID - populated during list rendering. */
    private final Map<String, Integer> totalPages = new HashMap<>();

    /** Slot → click handler, rebuilt on every render. */
    private final Map<Integer, Consumer<Player>> clickHandlers = new HashMap<>();

    /** Set at the start of each render pass; used by helpers that need viewer context. */
    private Player currentViewer;

    public DynamicGui(DPlayerProfiles plugin, GuiLayout layout, PlayerProfile profile, DynamicGui previousGui) {
        this.plugin = plugin;
        this.layout = layout;
        this.profile = profile;
        this.previousGui = previousGui;
    }

    // 
    // BaseGui contract
    // 

    @Override
    public void open(Player viewer) {
        String title = resolvePlaceholders(layout.titleTemplate);
        inventory = Bukkit.createInventory(null, layout.rows * 9, ColorUtil.parse(title));
        render(viewer);
        viewer.openInventory(inventory);
        triggerAsyncLoads(viewer);
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
        currentViewer = viewer;
        clickHandlers.clear();
        inventory.clear();

        // 1. Static items
        for (StaticItem si : layout.items) {
            if ("back".equals(si.action) && previousGui == null) continue;
            if (!isVisible(si.visibility, viewer)) continue;
            ItemStack item = buildStaticItem(si);
            if (item == null) continue;
            inventory.setItem(si.slot, item);
            if (si.action != null) {
                final String action = si.action;
                clickHandlers.put(si.slot, p -> executeAction(action, p));
            }
        }

        // 2. List components first (populates totalPages map needed by nav components)
        for (GuiComponent comp : layout.components) {
            switch (comp.type) {
                case ACHIEVEMENT_LIST -> renderAchievementList(comp);
                case GROUPED_ACHIEVEMENT_LIST -> renderGroupedAchievementList(comp);
                case BADGE_LIST -> renderBadgeList(comp);
                case LEADERBOARD -> renderLeaderboardPlaceholder(comp);
                default -> { /* handled in next pass */ }
            }
        }

        // 3. Navigation / tab components
        for (GuiComponent comp : layout.components) {
            switch (comp.type) {
                case PAGE_NAV -> renderPageNav(comp, viewer);
                case PAGE_INFO -> renderPageInfo(comp);
                case CATEGORY_TABS -> renderCategoryTabs(comp, viewer);
                case CATEGORY_TAB -> renderCategoryTab(comp, viewer);
                default -> { /* already handled */ }
            }
        }

        // 4. Fill remaining empty slots
        if (layout.fill != null) {
            ItemStack filler = new ItemBuilder(layout.fill.material)
                    .name(layout.fill.name).build();
            for (int i = 0; i < layout.rows * 9; i++) {
                if (inventory.getItem(i) == null) {
                    inventory.setItem(i, filler);
                }
            }
        }
    }

    //  list component renderers 

    private void renderAchievementList(GuiComponent comp) {
        if (comp.slots.isEmpty()) return;

        List<AchievementConfig> list = getFilteredAchievements(comp);
        int perPage = comp.slots.size();
        int tp = Math.max(1, (int) Math.ceil((double) list.size() / perPage));
        int page = Math.min(pages.getOrDefault(comp.id, 0), tp - 1);
        if (comp.id != null) {
            pages.put(comp.id, page);
            totalPages.put(comp.id, tp);
        }

        for (int slot : comp.slots) inventory.setItem(slot, null);

        if (list.isEmpty()) {
            Material emptyMat = comp.emptyMaterial != null ? comp.emptyMaterial : Material.BARRIER;
            String emptyName = comp.emptyName != null ? comp.emptyName
                    : plugin.getLangManager().getRaw(MessageKey.PROFILE_NO_ACHIEVEMENTS);
            inventory.setItem(comp.slots.get(0), new ItemBuilder(emptyMat).name(emptyName).build());
            return;
        }

        int start = page * perPage;
        for (int i = 0; i < perPage; i++) {
            int idx = start + i;
            if (idx >= list.size()) break;
            inventory.setItem(comp.slots.get(i), buildAchievementItem(list.get(idx)));
        }
    }

    private void renderGroupedAchievementList(GuiComponent comp) {
        if (comp.slots.isEmpty()) return;

        List<AchievementConfig> acs = getFilteredAchievements(comp);

        Map<String, List<AchievementConfig>> groups = new LinkedHashMap<>();
        for (AchievementConfig ac : acs) {
            String key = ac.group != null ? ac.group : " " + ac.id;
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(ac);
        }

        List<List<AchievementConfig>> seriesList = new ArrayList<>();
        List<AchievementConfig> standalones = new ArrayList<>();
        for (List<AchievementConfig> g : groups.values()) {
            if (g.size() > 1) seriesList.add(g);
            else standalones.add(g.get(0));
        }

        int rowWidth = 9;
        int rowCount = comp.slots.size() / rowWidth;
        if (rowCount < 2) {
            for (int s : comp.slots) inventory.setItem(s, null);
            return;
        }
        int seriesRowCount = rowCount - 1;

        // rowChunks[r] = ordered list of 9-slot chunks for that row across pages
        List<List<List<Object>>> rowChunks = new ArrayList<>();
        for (int r = 0; r < seriesRowCount; r++) rowChunks.add(new ArrayList<>());

        int rowCursor = 0;
        for (List<AchievementConfig> series : seriesList) {
            List<Object> flat = new ArrayList<>();
            for (int i = 0; i < series.size(); i++) {
                if (i > 0) flat.add(null); // connector sentinel
                flat.add(series.get(i));
            }
            int assignedRow = rowCursor % seriesRowCount;
            for (int offset = 0; offset < flat.size(); offset += rowWidth) {
                rowChunks.get(assignedRow).add(new ArrayList<>(flat.subList(offset, Math.min(offset + rowWidth, flat.size()))));
            }
            rowCursor++;
        }

        int maxSeriesPages = rowChunks.stream().mapToInt(List::size).max().orElse(0);
        int standalonePages = standalones.isEmpty() ? 0 : (int) Math.ceil((double) standalones.size() / rowWidth);
        int tp = Math.max(1, Math.max(maxSeriesPages, standalonePages));
        int page = Math.min(pages.getOrDefault(comp.id, 0), tp - 1);
        if (comp.id != null) { pages.put(comp.id, page); totalPages.put(comp.id, tp); }

        for (int s : comp.slots) inventory.setItem(s, null);

        if (acs.isEmpty()) {
            Material emptyMat = comp.emptyMaterial != null ? comp.emptyMaterial : Material.BARRIER;
            String emptyName = comp.emptyName != null ? comp.emptyName
                    : plugin.getLangManager().getRaw(MessageKey.PROFILE_NO_ACHIEVEMENTS);
            inventory.setItem(comp.slots.get(0), new ItemBuilder(emptyMat).name(emptyName).build());
            return;
        }

        Material cMat = comp.connectorMaterial != null ? comp.connectorMaterial : Material.LIME_STAINED_GLASS_PANE;
        String cName = comp.connectorName != null ? comp.connectorName : "<green>-->";

        // Render each series row
        for (int r = 0; r < seriesRowCount; r++) {
            List<List<Object>> chunks = rowChunks.get(r);
            if (page >= chunks.size()) continue;
            List<Object> chunk = chunks.get(page);
            for (int col = 0; col < chunk.size(); col++) {
                int slotIndex = r * rowWidth + col;
                if (slotIndex >= comp.slots.size()) break;
                int slot = comp.slots.get(slotIndex);
                Object item = chunk.get(col);
                if (item instanceof AchievementConfig ac) {
                    inventory.setItem(slot, buildAchievementItem(ac));
                } else {
                    inventory.setItem(slot, new ItemBuilder(cMat).name(cName).build());
                }
            }
        }

        // Render standalone row (last content row before navigation)
        int standaloneRowStart = seriesRowCount * rowWidth;
        int standaloneOffset = page * rowWidth;
        for (int col = 0; col < rowWidth; col++) {
            int slotIndex = standaloneRowStart + col;
            if (slotIndex >= comp.slots.size()) break;
            int idx = standaloneOffset + col;
            if (idx >= standalones.size()) break;
            inventory.setItem(comp.slots.get(slotIndex), buildAchievementItem(standalones.get(idx)));
        }
    }

        private void renderBadgeList(GuiComponent comp) {
        if (comp.slots.isEmpty() || profile == null) return;

        // Sort badges by grant time (oldest first = earn order)
        List<Map.Entry<String, Long>> badges = profile.getBadges().entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toList());
        int perPage = comp.slots.size();
        int tp = Math.max(1, (int) Math.ceil((double) badges.size() / perPage));
        int page = Math.min(pages.getOrDefault(comp.id, 0), tp - 1);
        if (comp.id != null) {
            pages.put(comp.id, page);
            totalPages.put(comp.id, tp);
        }

        for (int slot : comp.slots) inventory.setItem(slot, null);

        if (badges.isEmpty()) {
            Material emptyMat = comp.emptyMaterial != null ? comp.emptyMaterial : Material.BARRIER;
            String emptyName = comp.emptyName != null ? comp.emptyName
                    : plugin.getLangManager().getRaw(MessageKey.PROFILE_NO_BADGES);
            inventory.setItem(comp.slots.get(0), new ItemBuilder(emptyMat).name(emptyName).build());
            return;
        }

        boolean isOwner = currentViewer != null && profile.getUuid().equals(currentViewer.getUniqueId());
        List<String> pinned = profile.getPinnedBadges();

        int start = page * perPage;
        for (int i = 0; i < comp.slots.size(); i++) {
            int idx = start + i;
            if (idx >= badges.size()) break;
            String badgeId = badges.get(idx).getKey();
            long grantedAt = badges.get(idx).getValue();
            BadgeConfig bc = plugin.getBadgeConfigLoader().get(badgeId);
            if (bc == null) continue;

            boolean isPinned = pinned.contains(badgeId);
            List<String> lore = new ArrayList<>();
            lore.add(bc.description);
            lore.add(plugin.getLangManager().getRaw(MessageKey.PROFILE_BADGE_GRANTED,
                    Placeholder.of("date", TimeUtil.formatDate(grantedAt))));
            if (isPinned) lore.add(plugin.getLangManager().getRaw(MessageKey.PROFILE_BADGE_PINNED));

            ItemStack item = new ItemBuilder(bc.icon)
                    .name(bc.displayName)
                    .lore(lore.toArray(new String[0]))
                    .glow(isPinned)
                    .build();
            int slot = comp.slots.get(i);
            inventory.setItem(slot, item);

            if (isOwner) {
                final String bid = badgeId;
                clickHandlers.put(slot, p -> {
                    List<String> cur = new ArrayList<>(profile.getPinnedBadges());
                    if (cur.contains(bid)) {
                        cur.remove(bid);
                        plugin.getLangManager().send(p, MessageKey.PROFILE_BADGE_PIN_REMOVED);
                    } else if (cur.size() >= 3) {
                        plugin.getLangManager().send(p, MessageKey.PROFILE_BADGE_PIN_FULL);
                        return;
                    } else {
                        cur.add(bid);
                        plugin.getLangManager().send(p, MessageKey.PROFILE_BADGE_PIN_ADDED);
                    }
                    plugin.getProfileManager().setPinnedBadges(p.getUniqueId(), cur);
                    render(p);
                });
            }
        }
    }

    private void renderLeaderboardPlaceholder(GuiComponent comp) {
        if (comp.slots.isEmpty()) return;
        String key = comp.id != null ? comp.id : "__leaderboard";
        totalPages.put(key, 1);
        inventory.setItem(comp.slots.get(0), new ItemBuilder(Material.CLOCK)
                .name("<gray>Loading...").build());
    }

    //  nav / tab component renderers 

    private void renderPageNav(GuiComponent comp, Player viewer) {
        if (comp.slot < 0 || comp.direction == null || comp.component == null) return;

        int cur = pages.getOrDefault(comp.component, 0);
        int tp = totalPages.getOrDefault(comp.component, 1);

        boolean show = comp.direction.equals("prev") ? cur > 0 : cur < tp - 1;
        if (!show) return;

        Material mat = comp.material != null ? comp.material : Material.ARROW;
        String name = comp.name != null ? comp.name
                : (comp.direction.equals("prev") ? "&7Previous" : "&7Next");

        inventory.setItem(comp.slot, new ItemBuilder(mat).name(name).build());

        final String compId = comp.component;
        final int delta = comp.direction.equals("prev") ? -1 : 1;
        clickHandlers.put(comp.slot, player -> {
            int c = pages.getOrDefault(compId, 0);
            int t = totalPages.getOrDefault(compId, 1);
            int n = c + delta;
            if (n >= 0 && n < t) {
                pages.put(compId, n);
                render(player);
            }
        });
    }

    private void renderPageInfo(GuiComponent comp) {
        if (comp.slot < 0 || comp.component == null) return;

        int cur = pages.getOrDefault(comp.component, 0) + 1;
        int tp = totalPages.getOrDefault(comp.component, 1);

        Material mat = comp.material != null ? comp.material : Material.PAPER;
        String name = comp.name != null ? comp.name : "&7Page {page} / {total_pages}";
        name = name.replace("{page}", String.valueOf(cur))
                   .replace("{total_pages}", String.valueOf(tp));

        inventory.setItem(comp.slot, new ItemBuilder(mat).name(name).build());
    }

    private void renderCategoryTabs(GuiComponent comp, Player viewer) {
        List<String> cats = getAvailableCategories();

        // "All" tab
        Material allMat = selectedCategory == null
                ? or(comp.allMaterialSelected, Material.LIME_STAINED_GLASS_PANE)
                : or(comp.allMaterial, Material.WHITE_STAINED_GLASS_PANE);
        inventory.setItem(comp.startSlot,
                new ItemBuilder(allMat).name(comp.allName != null ? comp.allName : "&aAll").build());
        clickHandlers.put(comp.startSlot, p -> {
            selectedCategory = null;
            resetDynamicPages();
            render(p);
        });

        // Category tabs
        for (int i = 0; i < Math.min(cats.size(), comp.maxTabs - 1); i++) {
            String cat = cats.get(i);
            int slot = comp.startSlot + 1 + i;
            Material tabMat = cat.equals(selectedCategory)
                    ? or(comp.tabMaterialSelected, Material.LIME_STAINED_GLASS_PANE)
                    : or(comp.tabMaterial, Material.CYAN_STAINED_GLASS_PANE);
            inventory.setItem(slot, new ItemBuilder(tabMat).name("&b" + cat).build());
            final String catFinal = cat;
            clickHandlers.put(slot, p -> {
                selectedCategory = catFinal;
                resetDynamicPages();
                render(p);
            });
        }
    }

    private void renderCategoryTab(GuiComponent comp, Player viewer) {
        if (comp.slot < 0) return;
        if (!canViewCategory(comp.category)) return; // hide locked category tabs entirely
        boolean active = Objects.equals(comp.category, selectedCategory);

        Material mat = active
                ? or(comp.selectedMaterial, comp.material != null ? comp.material : Material.LIME_STAINED_GLASS_PANE)
                : or(comp.material, Material.CYAN_STAINED_GLASS_PANE);
        String name = comp.name != null ? comp.name
                : (comp.category != null ? "&b" + comp.category : "&aAll");

        inventory.setItem(comp.slot, new ItemBuilder(mat).name(name).build());
        final String cat = comp.category;
        clickHandlers.put(comp.slot, p -> {
            selectedCategory = cat;
            resetDynamicPages();
            render(p);
        });
    }

    // 
    // Item builders
    // 

    private ItemStack buildStaticItem(StaticItem si) {
        String resolvedName = resolvePlaceholders(si.name != null ? si.name : "");
        List<String> resolvedLore = si.lore.stream()
                .map(this::resolvePlaceholders)
                .collect(Collectors.toList());

        if (si.type == StaticItem.ItemType.PLAYER_HEAD && profile != null) {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            OfflinePlayer op = Bukkit.getOfflinePlayer(profile.getUuid());
            meta.setOwningPlayer(op);
            meta.displayName(ColorUtil.parse(resolvedName));
            List<Component> loreComponents = resolvedLore.stream()
                    .map(ColorUtil::parse)
                    .collect(Collectors.toList());
            meta.lore(loreComponents);
            skull.setItemMeta(meta);
            return skull;
        }

        Material mat = si.material != null ? si.material : Material.STONE;
        ItemBuilder builder = new ItemBuilder(mat).name(resolvedName);
        if (!resolvedLore.isEmpty()) {
            builder.lore(resolvedLore.toArray(new String[0]));
        }
        return builder.build();
    }

    private ItemStack buildAchievementItem(AchievementConfig ac) {
        if (profile == null) return new ItemBuilder(ac.icon).name(ac.displayName).build();

        boolean isCompleted = profile.isCompleted(ac.id);
        boolean isHidden = ac.hidden && !isCompleted;

        if (isHidden) {
            return new ItemBuilder(ac.hiddenIcon)
                    .name(plugin.getLangManager().getRaw(MessageKey.ACHIEVEMENTS_HIDDEN_NAME))
                    .lore(plugin.getLangManager().getRaw(MessageKey.ACHIEVEMENTS_HIDDEN_LORE))
                    .build();
        }

        ItemBuilder builder = new ItemBuilder(ac.icon).name(ac.displayName);
        List<String> lore = new ArrayList<>();
        lore.add(ac.description);
        lore.add("");

        if (isCompleted) {
            long completedAt = profile.getAchievements().get(ac.id).completedAt();
            lore.add(plugin.getLangManager().getRaw(MessageKey.ACHIEVEMENTS_COMPLETED,
                    Placeholder.of("date", TimeUtil.formatDate(completedAt))));
        } else {
            long progress = profile.getProgress(ac.id);
            if (ac.triggerCount > 1) {
                if (ac.triggerType == com.dogetennant.dplayerprofiles.model.TriggerType.TIME_PLAYED) {
                    lore.add(plugin.getLangManager().getRaw(MessageKey.ACHIEVEMENTS_PROGRESS_TIME,
                            Placeholder.of("current", String.valueOf(progress / 3600)),
                            Placeholder.of("target", String.valueOf(ac.triggerCount / 3600))));
                } else {
                    lore.add(plugin.getLangManager().getRaw(MessageKey.ACHIEVEMENTS_PROGRESS,
                            Placeholder.of("current", String.valueOf(progress)),
                            Placeholder.of("target", String.valueOf(ac.triggerCount))));
                }
            }

            boolean prereqsMet = ac.requires.stream().allMatch(profile::isCompleted);
            if (!prereqsMet) {
                lore.add(plugin.getLangManager().getRaw(MessageKey.ACHIEVEMENTS_LOCKED));
                String reqList = ac.requires.stream()
                        .filter(r -> !profile.isCompleted(r))
                        .map(r -> {
                            AchievementConfig req = plugin.getAchievementConfigLoader().get(r);
                            return req != null ? ColorUtil.stripFormatting(req.displayName) : r;
                        })
                        .collect(Collectors.joining(", "));
                lore.add(plugin.getLangManager().getRaw(MessageKey.ACHIEVEMENTS_REQUIRES,
                        Placeholder.of("list", reqList)));
            }

            boolean badgesMet = ac.requiredBadges.stream().allMatch(profile::hasBadge);
            if (!badgesMet) {
                lore.add(plugin.getLangManager().getRaw(MessageKey.ACHIEVEMENTS_BADGE_LOCKED));
                String badgeList = ac.requiredBadges.stream()
                        .filter(b -> !profile.hasBadge(b))
                        .map(b -> {
                            BadgeConfig bc = plugin.getBadgeConfigLoader().get(b);
                            return bc != null ? ColorUtil.stripFormatting(bc.displayName) : b;
                        })
                        .collect(Collectors.joining(", "));
                lore.add(plugin.getLangManager().getRaw(MessageKey.ACHIEVEMENTS_REQUIRES_BADGES,
                        Placeholder.of("list", badgeList)));
            }
        }

        builder.lore(lore.toArray(new String[0]));
        return builder.build();
    }

    private ItemStack buildLeaderboardEntry(LeaderboardEntry entry, int rank) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        OfflinePlayer op = Bukkit.getOfflinePlayer(entry.uuid());
        meta.setOwningPlayer(op);
        meta.displayName(ColorUtil.parse("<yellow>#" + rank + " <white>" + entry.username()));
        meta.lore(List.of(
                ColorUtil.parse(plugin.getLangManager().getRaw(MessageKey.LEADERBOARD_ENTRY_ACHIEVEMENTS,
                        Placeholder.of("count", String.valueOf(entry.completedAchievements())))),
                ColorUtil.parse(plugin.getLangManager().getRaw(MessageKey.LEADERBOARD_ENTRY_BADGES,
                        Placeholder.of("count", String.valueOf(entry.badges()))))));
        skull.setItemMeta(meta);
        return skull;
    }

    // 
    // Async leaderboard loading
    // 

    private void triggerAsyncLoads(Player viewer) {
        for (GuiComponent comp : layout.components) {
            if (comp.type == GuiComponent.Type.LEADERBOARD) {
                loadLeaderboardAsync(comp, viewer);
            }
        }
    }

    private void loadLeaderboardAsync(GuiComponent comp, Player viewer) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                int size = Math.min(
                        comp.slots.size(),
                        plugin.getConfigManager().get().leaderboardSize);
                List<LeaderboardEntry> entries = plugin.getDatabaseManager().getLeaderboard(size);

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    for (int slot : comp.slots) inventory.setItem(slot, null);

                    if (entries.isEmpty()) {
                        if (!comp.slots.isEmpty()) {
                            inventory.setItem(comp.slots.get(0), new ItemBuilder(Material.BARRIER)
                                    .name(plugin.getLangManager().getRaw(MessageKey.LEADERBOARD_EMPTY)).build());
                        }
                    } else {
                        for (int i = 0; i < Math.min(entries.size(), comp.slots.size()); i++) {
                            inventory.setItem(comp.slots.get(i), buildLeaderboardEntry(entries.get(i), i + 1));

                            // Register click handler if click-action is configured
                            if (comp.clickAction != null && comp.clickAction.startsWith("open-gui:")) {
                                String targetGuiId = comp.clickAction.substring(9);
                                UUID entryUuid = entries.get(i).uuid();
                                int slot = comp.slots.get(i);
                                clickHandlers.put(slot, p -> {
                                    plugin.getProfileManager().loadProfileByUUIDAsync(entryUuid, lbProfile -> {
                                        GuiLayout targetLayout = plugin.getGuiLayoutLoader().get(targetGuiId);
                                        if (targetLayout != null && lbProfile != null) {
                                            plugin.getGuiManager().open(p, new DynamicGui(plugin, targetLayout, lbProfile, this));
                                        }
                                    });
                                });
                            }
                        }
                    }

                    // Re-fill any newly empty slots
                    if (layout.fill != null) {
                        ItemStack filler = new ItemBuilder(layout.fill.material)
                                .name(layout.fill.name).build();
                        for (int slot : comp.slots) {
                            if (inventory.getItem(slot) == null) inventory.setItem(slot, filler);
                        }
                    }
                });
            } catch (SQLException e) {
                LogUtil.severe("Failed to load leaderboard", e);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!comp.slots.isEmpty()) {
                        inventory.setItem(comp.slots.get(0), new ItemBuilder(Material.BARRIER)
                                .name(plugin.getLangManager().getRaw(MessageKey.LEADERBOARD_EMPTY)).build());
                    }
                });
            }
        });
    }

    // 
    // Data helpers
    // 

    private List<AchievementConfig> getFilteredAchievements(GuiComponent comp) {
        if (profile == null) return List.of();

        Collection<AchievementConfig> all = plugin.getAchievementConfigLoader().getAll().values();
        Stream<AchievementConfig> stream = all.stream()
                .filter(ac -> canViewCategory(ac.category));

        String filter = comp.filter != null ? comp.filter : "all";
        stream = switch (filter) {
            case "dynamic" -> selectedCategory != null
                    ? stream.filter(ac -> selectedCategory.equals(ac.category))
                    : stream;
            case "completed" -> stream.filter(ac -> profile.isCompleted(ac.id));
            case "incomplete" -> stream.filter(ac -> !profile.isCompleted(ac.id));
            default -> {
                if (filter.startsWith("category:")) {
                    String cat = filter.substring(9);
                    yield stream.filter(ac -> cat.equals(ac.category));
                }
                yield stream; // "all"
            }
        };

        String sort = comp.sort != null ? comp.sort : "id";
        if ("recent".equals(sort)) {
            return stream
                    .filter(ac -> profile.isCompleted(ac.id))
                    .sorted((a, b) -> {
                        long ca = profile.getAchievements().get(a.id).completedAt();
                        long cb = profile.getAchievements().get(b.id).completedAt();
                        return Long.compare(cb, ca);
                    })
                    .collect(Collectors.toList());
        }
        return stream.sorted(Comparator.comparingInt((AchievementConfig ac) -> ac.points).thenComparing(ac -> ac.id)).collect(Collectors.toList());
    }

    private List<String> getAvailableCategories() {
        Set<String> cats = new LinkedHashSet<>();
        for (AchievementConfig ac : plugin.getAchievementConfigLoader().getAll().values()) {
            if (ac.category != null && canViewCategory(ac.category)) cats.add(ac.category);
        }
        return new ArrayList<>(cats);
    }

    /** Returns true if the current viewer has access to the given category (null = no category, always visible). */
    private boolean canViewCategory(String category) {
        if (category == null || currentViewer == null) return true;
        String required = plugin.getConfigManager().get().categoryPermissions.get(category);
        return required == null || currentViewer.hasPermission(required);
    }

    private void resetDynamicPages() {
        for (GuiComponent comp : layout.components) {
            if (comp.type == GuiComponent.Type.ACHIEVEMENT_LIST
                    && "dynamic".equals(comp.filter)
                    && comp.id != null) {
                pages.remove(comp.id);
            }
        }
    }

    // 
    // Placeholder resolution
    // 

    private String resolvePlaceholders(String text) {
        if (text == null) return "";
        if (profile != null) {
            int total = plugin.getAchievementConfigLoader().getAll().size();
            text = text
                    .replace("{player}", profile.getUsername())
                    .replace("{username}", profile.getUsername())
                    .replace("{first_seen}", TimeUtil.formatDate(profile.getFirstSeen()))
                    .replace("{last_seen}", TimeUtil.formatDate(profile.getLastSeen()))
                    .replace("{playtime}", TimeUtil.format(profile.getPlaytimeSeconds()))
                    .replace("{streak}", String.valueOf(profile.getLoginStreak()))
                    .replace("{achievements_completed}", String.valueOf(profile.completedAchievementCount()))
                    .replace("{achievements_total}", String.valueOf(total))
                    .replace("{badges_count}", String.valueOf(profile.getBadges().size()))
                    .replace("{privacy}", profile.isPrivate()
                            ? plugin.getLangManager().getRaw(MessageKey.PROFILE_PRIVACY_ON)
                            : plugin.getLangManager().getRaw(MessageKey.PROFILE_PRIVACY_OFF))
                    .replace("{points}", String.valueOf(
                            plugin.getAchievementManager().calculateTotalPoints(profile)));
        }
        return text;
    }

    // 
    // Action handling
    // 

    private void executeAction(String action, Player player) {
        switch (action) {
            case "back" -> {
                if (previousGui != null) plugin.getGuiManager().open(player, previousGui);
            }
            case "close" -> player.closeInventory();
            case "toggle-privacy" -> {
                if (profile != null && player.getUniqueId().equals(profile.getUuid())) {
                    boolean newState = !profile.isPrivate();
                    plugin.getProfileManager().setPrivacy(profile.getUuid(), newState);
                    MessageKey confirmKey = newState ? MessageKey.PROFILE_PRIVACY_ENABLED : MessageKey.PROFILE_PRIVACY_DISABLED;
                    plugin.getLangManager().send(player, confirmKey);
                    render(player); // refresh button text
                }
            }
            default -> {
                if (action.startsWith("open-gui:")) {
                    String targetId = action.substring(9);
                    if (targetId.equals("points")) {
                        if (plugin.getConfigManager().get().pointsEnabled
                                && profile != null
                                && player.getUniqueId().equals(profile.getUuid())) {
                            plugin.getGuiManager().open(player, new PointsGui(plugin, profile, this));
                        }
                        return;
                    }
                    GuiLayout target = plugin.getGuiLayoutLoader().get(targetId);
                    if (target != null) {
                        plugin.getGuiManager().open(player, new DynamicGui(plugin, target, profile, this));
                    } else {
                        LogUtil.warn("GUI layout not found: " + targetId);
                    }
                }
            }
        }
    }

    private boolean isVisible(String visibility, Player viewer) {
        if (visibility == null || visibility.equals("all")) return true;
        boolean isSelf = profile != null && viewer.getUniqueId().equals(profile.getUuid());
        return switch (visibility) {
            case "self" -> isSelf;
            case "others" -> !isSelf;
            default -> {
                if (visibility.startsWith("perm:")) {
                    yield viewer.hasPermission(visibility.substring(5));
                }
                if (visibility.startsWith("self-or-perm:")) {
                    yield isSelf || viewer.hasPermission(visibility.substring(13));
                }
                yield true;
            }
        };
    }

    // 
    // Utilities
    // 

    private static Material or(Material a, Material fallback) {
        return a != null ? a : fallback;
    }
}
