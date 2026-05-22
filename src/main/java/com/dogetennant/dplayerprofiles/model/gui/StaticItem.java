package com.dogetennant.dplayerprofiles.model.gui;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class StaticItem {

    public enum ItemType { NORMAL, PLAYER_HEAD }

    public int slot;
    public ItemType type = ItemType.NORMAL;
    public Material material;
    public String name;
    public List<String> lore = new ArrayList<>();
    /**
     * Supported actions: "back", "close", "open-gui:<id>", "toggle-privacy"
     * The "back" action is suppressed when there is no previous GUI.
     */
    public String action;
    /**
     * Visibility condition. null / "all" = always shown.
     * "self"                  = only when viewer is the profile owner.
     * "others"                = only when viewer is NOT the profile owner.
     * "perm:<node>"           = only if viewer has the permission node.
     * "self-or-perm:<node>"   = if viewer is the owner OR has the permission node.
     */
    public String visibility;
}
