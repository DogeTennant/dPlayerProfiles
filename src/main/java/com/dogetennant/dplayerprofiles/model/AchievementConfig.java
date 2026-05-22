package com.dogetennant.dplayerprofiles.model;

import com.dogetennant.dplayerprofiles.reward.RewardEntry;
import org.bukkit.Material;

import java.util.List;

public class AchievementConfig {

    public String id;
    public String displayName;
    public String description;
    public Material icon;
    public String category;       // null = no category
    /** Progression series key. Achievements sharing the same group in the same category
     *  are rendered in one row with connector items between them in the GUI. null = standalone. */
    public String group;
    public List<String> requires;        // IDs of prerequisite achievements
    public List<String> requiredBadges; // badge IDs the player must hold
    public String permission;            // empty = no restriction
    public boolean hidden;
    public Material hiddenIcon;    // icon shown before the achievement is revealed
    public boolean broadcast = false; // whether completion is announced server-wide

    public TriggerType triggerType; // null for MANUAL
    public String triggerTarget;    // EntityType or Material name; null when not applicable
    public long triggerCount;       // progress threshold; 0 for instant

    public int points = 0; // achievement point value for the points system

    public List<RewardEntry> rewards;
}
