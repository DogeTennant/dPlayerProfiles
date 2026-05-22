package com.dogetennant.dplayerprofiles.model.gui;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class GuiLayout {

    public String id;
    public String titleTemplate;  // supports {player} placeholder
    public int rows = 6;
    public List<StaticItem> items = new ArrayList<>();
    public List<GuiComponent> components = new ArrayList<>();
    public FillConfig fill;

    public static class FillConfig {
        public Material material = Material.GRAY_STAINED_GLASS_PANE;
        public String name = " ";
    }
}
