package com.dogetennant.dplayerprofiles.model.gui;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * Flat data class representing one dynamic component inside a GUI layout.
 * Which fields are used depends on the component type.
 */
public class GuiComponent {

    public enum Type {
        /** Paginated list of achievements. */
        ACHIEVEMENT_LIST,
        /**
         * Like ACHIEVEMENT_LIST but renders achievements sharing the same 'group' field
         * in a consecutive row with a connector item between each tier.
         * Standalone achievements (no group) appear individually.
         */
        GROUPED_ACHIEVEMENT_LIST,
        /** Paginated list of player badges. */
        BADGE_LIST,
        /** Async-loaded leaderboard entries. */
        LEADERBOARD,
        /** Prev / next page button for a list component. */
        PAGE_NAV,
        /** "Page X / Y" info item for a list component. */
        PAGE_INFO,
        /** Auto-generated category tab row driven by achievement categories. */
        CATEGORY_TABS,
        /** Single explicit category tab button. */
        CATEGORY_TAB,
    }

    //  shared 
    public Type type;
    /** Stable identifier; PAGE_NAV / PAGE_INFO reference list components by this. */
    public String id;

    //  list components (ACHIEVEMENT_LIST, BADGE_LIST, LEADERBOARD) 
    public List<Integer> slots = new ArrayList<>();
    /** Material shown when the list is empty. Defaults to BARRIER. */
    public Material emptyMaterial;
    /** Display name shown when the list is empty. */
    public String emptyName;

    //  ACHIEVEMENT_LIST 
    /**
     * Filter for achievements:
     * "all"            - all achievements (default)
     * "dynamic"        - filtered by the GUI's current selectedCategory (null = all)
     * "completed"      - only completed achievements
     * "incomplete"     - only achievements not yet completed
     * "category:<name>" - fixed to a specific category
     */
    public String filter = "all";
    /**
     * Sort order:
     * "id"     - alphabetically by achievement ID (default)
     * "recent" - completed achievements sorted by completedAt desc
     */
    public String sort = "id";

    //  single-slot components (PAGE_NAV, PAGE_INFO, CATEGORY_TABS, CATEGORY_TAB) 
    public int slot = -1;
    public Material material;
    public String name;
    public List<String> lore = new ArrayList<>();

    //  LEADERBOARD 
    /**
     * Optional action executed when a leaderboard entry is clicked.
     * Currently supported: "open-gui:<id>" (opens the layout with the entry's profile).
     */
    public String clickAction;

    //  PAGE_NAV 
    /** "prev" or "next" */
    public String direction;
    /** ID of the list component to paginate. */
    public String component;

    //  PAGE_INFO 
    // uses `component`, `slot`, `material`, `name` (supports {page} / {total_pages})

    //  CATEGORY_TABS 
    public int startSlot = 0;
    public int maxTabs = 9;
    public String allName = "&aAll";
    public Material allMaterial;
    public Material allMaterialSelected;
    public Material tabMaterial;
    public Material tabMaterialSelected;

    //  CATEGORY_TAB 
    /** null means "All" (no category filter). */
    public String category;
    /** Material used when this tab is the active filter. */
    public Material selectedMaterial;

    //  GROUPED_ACHIEVEMENT_LIST 
    /** Material for the connector item placed between tiers of the same series. */
    public Material connectorMaterial;
    /** Display name for the connector item. */
    public String connectorName;

}
