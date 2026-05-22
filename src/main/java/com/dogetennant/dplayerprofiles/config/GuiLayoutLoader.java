package com.dogetennant.dplayerprofiles.config;

import com.dogetennant.dplayerprofiles.model.gui.GuiComponent;
import com.dogetennant.dplayerprofiles.model.gui.GuiLayout;
import com.dogetennant.dplayerprofiles.model.gui.StaticItem;
import com.dogetennant.dplayerprofiles.util.LogUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GuiLayoutLoader {

    private static final String[] DEFAULTS = {"profile.yml", "achievements.yml", "leaderboard.yml"};

    private final Plugin plugin;
    private final Map<String, GuiLayout> layouts = new LinkedHashMap<>();

    public GuiLayoutLoader(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        layouts.clear();

        File dir = new File(plugin.getDataFolder(), "gui-layouts");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Save bundled defaults only if they don't already exist.
        for (String name : DEFAULTS) {
            File dest = new File(dir, name);
            if (!dest.exists()) {
                plugin.saveResource("gui-layouts/" + name, false);
            }
        }

        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                GuiLayout layout = parse(cfg, file.getName());
                if (layout != null) layouts.put(layout.id, layout);
            } catch (Exception e) {
                LogUtil.warn("Failed to load GUI layout " + file.getName() + ": " + e.getMessage());
            }
        }

        LogUtil.info("Loaded " + layouts.size() + " GUI layout(s).");
    }

    // 
    // Parsing
    // 

    private GuiLayout parse(YamlConfiguration cfg, String filename) {
        String id = cfg.getString("id");
        if (id == null || id.isBlank()) {
            LogUtil.warn("GUI layout in " + filename + " is missing 'id' - skipping.");
            return null;
        }

        GuiLayout layout = new GuiLayout();
        layout.id = id;
        layout.titleTemplate = cfg.getString("title", "&8Menu");
        layout.rows = Math.max(1, Math.min(6, cfg.getInt("rows", 6)));

        // fill section
        if (cfg.isConfigurationSection("fill")) {
            layout.fill = new GuiLayout.FillConfig();
            String matStr = cfg.getString("fill.material", "GRAY_STAINED_GLASS_PANE");
            Material mat = Material.matchMaterial(matStr.toUpperCase());
            layout.fill.material = mat != null ? mat : Material.GRAY_STAINED_GLASS_PANE;
            layout.fill.name = cfg.getString("fill.name", " ");
        }

        // static items
        List<?> itemList = cfg.getList("items");
        if (itemList != null) {
            for (Object obj : itemList) {
                if (obj instanceof Map<?, ?> map) {
                    StaticItem item = parseItem(map, filename);
                    if (item != null) layout.items.add(item);
                }
            }
        }

        // components
        List<?> compList = cfg.getList("components");
        if (compList != null) {
            for (Object obj : compList) {
                if (obj instanceof Map<?, ?> map) {
                    GuiComponent comp = parseComponent(map, filename);
                    if (comp != null) layout.components.add(comp);
                }
            }
        }

        return layout;
    }

    private StaticItem parseItem(Map<?, ?> map, String filename) {
        int slot = getInt(map, "slot", -1);
        if (slot < 0) {
            LogUtil.warn("Static item in " + filename + " has no valid 'slot' - skipping.");
            return null;
        }

        StaticItem item = new StaticItem();
        item.slot = slot;
        item.name = getString(map, "name", "");
        item.lore = getStringList(map, "lore");
        item.action = getString(map, "action", null);
        item.visibility = getString(map, "visibility", null);

        String typeStr = getString(map, "type", "normal").toLowerCase();
        item.type = typeStr.equals("player-head") ? StaticItem.ItemType.PLAYER_HEAD : StaticItem.ItemType.NORMAL;

        String matStr = getString(map, "material", "STONE");
        Material mat = Material.matchMaterial(matStr.toUpperCase());
        item.material = mat != null ? mat : Material.STONE;

        return item;
    }

    private GuiComponent parseComponent(Map<?, ?> map, String filename) {
        String typeStr = getString(map, "type", "")
                .toUpperCase().replace("-", "_");
        if (typeStr.isBlank()) {
            LogUtil.warn("Component in " + filename + " is missing 'type' - skipping.");
            return null;
        }

        GuiComponent comp = new GuiComponent();
        try {
            comp.type = GuiComponent.Type.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            LogUtil.warn("Unknown component type '" + typeStr + "' in " + filename + " - skipping.");
            return null;
        }

        comp.id = getString(map, "id", null);
        comp.slot = getInt(map, "slot", -1);
        comp.name = getString(map, "name", null);
        comp.lore = getStringList(map, "lore");
        comp.material = parseMaterial(getString(map, "material", null));
        comp.slots = getIntList(map, "slots");
        comp.emptyMaterial = parseMaterial(getString(map, "empty-material", null));
        comp.emptyName = getString(map, "empty-name", null);

        comp.clickAction = getString(map, "click-action", null);

        switch (comp.type) {
            case ACHIEVEMENT_LIST -> {
                comp.filter = getString(map, "filter", "all");
                comp.sort = getString(map, "sort", "id");
            }
            case GROUPED_ACHIEVEMENT_LIST -> {
                comp.filter = getString(map, "filter", "all");
                comp.sort = getString(map, "sort", "id");
                comp.connectorMaterial = parseMaterial(getString(map, "connector-material", null));
                comp.connectorName = getString(map, "connector-name", null);

            }
            case PAGE_NAV -> {
                comp.direction = getString(map, "direction", "next");
                comp.component = getString(map, "component", null);
            }
            case PAGE_INFO -> {
                comp.component = getString(map, "component", null);
            }
            case CATEGORY_TABS -> {
                comp.startSlot = getInt(map, "start-slot", 0);
                comp.maxTabs = getInt(map, "max-tabs", 9);
                comp.allName = getString(map, "all-name", "&aAll");
                comp.allMaterial = parseMaterial(getString(map, "all-material", "WHITE_STAINED_GLASS_PANE"));
                comp.allMaterialSelected = parseMaterial(getString(map, "all-material-selected", "LIME_STAINED_GLASS_PANE"));
                comp.tabMaterial = parseMaterial(getString(map, "tab-material", "CYAN_STAINED_GLASS_PANE"));
                comp.tabMaterialSelected = parseMaterial(getString(map, "tab-material-selected", "LIME_STAINED_GLASS_PANE"));
            }
            case CATEGORY_TAB -> {
                comp.category = getString(map, "category", null);
                comp.selectedMaterial = parseMaterial(getString(map, "selected-material", null));
            }
            default -> { /* BADGE_LIST, LEADERBOARD need no extra fields */ }
        }

        return comp;
    }

    // 
    // Map helpers
    // 

    private String getString(Map<?, ?> map, String key, String def) {
        Object val = map.get(key);
        return val instanceof String s ? s : def;
    }

    private int getInt(Map<?, ?> map, String key, int def) {
        Object val = map.get(key);
        return val instanceof Number n ? n.intValue() : def;
    }

    private List<String> getStringList(Map<?, ?> map, String key) {
        Object val = map.get(key);
        if (val instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object o : list) {
                if (o instanceof String s) result.add(s);
            }
            return result;
        }
        return new ArrayList<>();
    }

    private List<Integer> getIntList(Map<?, ?> map, String key) {
        Object val = map.get(key);
        if (val instanceof List<?> list) {
            List<Integer> result = new ArrayList<>();
            for (Object o : list) {
                if (o instanceof Number n) result.add(n.intValue());
            }
            return result;
        }
        return new ArrayList<>();
    }

    private Material parseMaterial(String str) {
        if (str == null || str.isBlank()) return null;
        Material mat = Material.matchMaterial(str.toUpperCase());
        return mat; // caller handles null
    }

    // 
    // Access
    // 

    public GuiLayout get(String id) {
        return layouts.get(id);
    }

    public Map<String, GuiLayout> getAll() {
        return Collections.unmodifiableMap(layouts);
    }
}
