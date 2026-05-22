package com.dogetennant.dplayerprofiles.lang;

public enum MessageKey {
    PREFIX("prefix"),
    NO_PERMISSION("no-permission"),
    PLAYER_ONLY("player-only"),
    UNKNOWN_COMMAND("unknown-command"),
    PLAYER_NOT_FOUND("player-not-found"),
    RELOAD_SUCCESS("reload-success"),

    CMD_LANGUAGE_USAGE("cmd-language-usage"),
    CMD_LANGUAGE_SUCCESS("cmd-language-success"),
    CMD_LANGUAGE_NOT_FOUND("cmd-language-not-found"),

    CMD_RESET_USAGE("cmd-reset-usage"),
    CMD_RESET_SUCCESS("cmd-reset-success"),
    CMD_RESETACH_USAGE("cmd-resetach-usage"),
    CMD_RESETACH_SUCCESS("cmd-resetach-success"),
    CMD_RESETACH_NOT_FOUND("cmd-resetach-not-found"),

    CMD_COMPLETE_USAGE("cmd-complete-usage"),
    CMD_COMPLETE_SUCCESS("cmd-complete-success"),
    CMD_COMPLETE_ALREADY("cmd-complete-already"),
    CMD_COMPLETE_NOT_FOUND("cmd-complete-not-found"),

    CMD_BADGE_USAGE("cmd-badge-usage"),
    CMD_BADGE_GIVE_SUCCESS("cmd-badge-give-success"),
    CMD_BADGE_REMOVE_SUCCESS("cmd-badge-remove-success"),
    CMD_BADGE_NOT_FOUND("cmd-badge-not-found"),
    CMD_BADGE_ALREADY_HAS("cmd-badge-already-has"),
    CMD_BADGE_DOESNT_HAVE("cmd-badge-doesnt-have"),

    CMD_HELP_HEADER("cmd-help-header"),
    CMD_HELP_ENTRY("cmd-help-entry"),

    CMD_DESC_HELP("cmd-desc-help"),
    CMD_DESC_PROFILE("cmd-desc-profile"),
    CMD_DESC_ACHIEVEMENTS("cmd-desc-achievements"),
    CMD_DESC_LEADERBOARD("cmd-desc-leaderboard"),
    CMD_DESC_LANGUAGE("cmd-desc-language"),
    CMD_DESC_RELOAD("cmd-desc-reload"),
    CMD_DESC_RESET("cmd-desc-reset"),
    CMD_DESC_RESETACH("cmd-desc-resetach"),
    CMD_DESC_COMPLETE("cmd-desc-complete"),
    CMD_DESC_BADGE("cmd-desc-badge"),

    ACHIEVEMENT_COMPLETE_TITLE("achievement-complete-title"),
    ACHIEVEMENT_COMPLETE_SUBTITLE("achievement-complete-subtitle"),
    ACHIEVEMENT_COMPLETE_CHAT("achievement-complete-chat"),
    ACHIEVEMENT_BROADCAST("achievement-broadcast"),
    ACHIEVEMENT_REWARD_ITEM("achievement-reward-item"),
    ACHIEVEMENT_REWARD_COMMAND("achievement-reward-command"),

    PROFILE_GUI_TITLE("profile-gui-title"),
    PROFILE_SKULL_NAME("profile-skull-name"),
    PROFILE_SKULL_FIRST_SEEN("profile-skull-first-seen"),
    PROFILE_SKULL_LAST_SEEN("profile-skull-last-seen"),
    PROFILE_SKULL_PLAYTIME("profile-skull-playtime"),
    PROFILE_SKULL_STREAK("profile-skull-streak"),
    PROFILE_SKULL_ACHIEVEMENTS("profile-skull-achievements"),
    PROFILE_SKULL_BADGES("profile-skull-badges"),
    PROFILE_BADGES_HEADER("profile-badges-header"),
    PROFILE_ACHIEVEMENTS_HEADER("profile-achievements-header"),
    PROFILE_NO_BADGES("profile-no-badges"),
    PROFILE_NO_ACHIEVEMENTS("profile-no-achievements"),
    PROFILE_BTN_ACHIEVEMENTS("profile-btn-achievements"),
    PROFILE_BTN_LEADERBOARD("profile-btn-leaderboard"),
    PROFILE_BADGE_GRANTED("profile-badge-granted"),

    ACHIEVEMENTS_GUI_TITLE("achievements-gui-title"),
    ACHIEVEMENTS_GUI_TITLE_PLAYER("achievements-gui-title-player"),
    ACHIEVEMENTS_NAV_PREV("achievements-nav-prev"),
    ACHIEVEMENTS_NAV_NEXT("achievements-nav-next"),
    ACHIEVEMENTS_NAV_INFO("achievements-nav-info"),
    ACHIEVEMENTS_PROGRESS("achievements-progress"),
    ACHIEVEMENTS_PROGRESS_TIME("achievements-progress-time"),
    ACHIEVEMENTS_COMPLETED("achievements-completed"),
    ACHIEVEMENTS_LOCKED("achievements-locked"),
    ACHIEVEMENTS_REQUIRES("achievements-requires"),
    ACHIEVEMENTS_BADGE_LOCKED("achievements-badge-locked"),
    ACHIEVEMENTS_REQUIRES_BADGES("achievements-requires-badges"),
    ACHIEVEMENTS_HIDDEN_NAME("achievements-hidden-name"),
    ACHIEVEMENTS_HIDDEN_LORE("achievements-hidden-lore"),
    ACHIEVEMENTS_CATEGORY_ALL("achievements-category-all"),
    ACHIEVEMENTS_BACK("achievements-back"),
    PROFILE_PAGE_PREV("profile-page-prev"),
    PROFILE_PAGE_NEXT("profile-page-next"),
    PROFILE_PAGE_INFO("profile-page-info"),

    LEADERBOARD_GUI_TITLE("leaderboard-gui-title"),
    LEADERBOARD_ENTRY_ACHIEVEMENTS("leaderboard-entry-achievements"),
    LEADERBOARD_ENTRY_BADGES("leaderboard-entry-badges"),
    LEADERBOARD_EMPTY("leaderboard-empty"),

    // Rewards GUI
    REWARDS_GUI_TITLE("rewards-gui-title"),
    REWARDS_GUI_CLICK_HINT("rewards-gui-click-hint"),
    REWARDS_GUI_NO_REWARDS("rewards-gui-no-rewards"),
    REWARDS_GUI_REWARD_COUNT("rewards-gui-reward-count"),
    REWARDS_EDITOR_TITLE("rewards-editor-title"),
    REWARDS_EDITOR_ADD_COMMAND("rewards-editor-add-command"),
    REWARDS_EDITOR_ADD_ITEM("rewards-editor-add-item"),
    REWARDS_EDITOR_ADD_BADGE("rewards-editor-add-badge"),
    REWARDS_EDITOR_CLEAR("rewards-editor-clear"),
    REWARDS_EDITOR_REMOVE_HINT("rewards-editor-remove-hint"),
    REWARDS_EDITOR_COMMAND_LABEL("rewards-editor-command-label"),
    REWARDS_EDITOR_ITEM_LABEL("rewards-editor-item-label"),
    REWARDS_EDITOR_BADGE_LABEL("rewards-editor-badge-label"),
    REWARDS_EDITOR_EMPTY("rewards-editor-empty"),
    REWARDS_EDITOR_NO_ITEM("rewards-editor-no-item"),
    REWARDS_EDITOR_SAVED("rewards-editor-saved"),
    REWARDS_CMD_INPUT_PROMPT("rewards-cmd-input-prompt"),
    REWARDS_BADGE_INPUT_PROMPT("rewards-badge-input-prompt"),
    CMD_DESC_REWARDS("cmd-desc-rewards"),

    CHAT_BADGE_HEADER("chat-badge-header"),
    CHAT_BADGE_ENTRY("chat-badge-entry"),

    PROFILE_PRIVATE("profile-private"),
    PROFILE_PRIVACY_ON("profile-privacy-on"),
    PROFILE_PRIVACY_OFF("profile-privacy-off"),
    PROFILE_PRIVACY_ENABLED("profile-privacy-enabled"),
    PROFILE_PRIVACY_DISABLED("profile-privacy-disabled"),

    CMD_DESC_PRIVATE("cmd-desc-private"),
    CMD_PRIVATE_USAGE("cmd-private-usage"),
    CMD_PRIVATE_ON("cmd-private-on"),
    CMD_PRIVATE_OFF("cmd-private-off"),

    PROFILE_BADGE_PINNED("profile-badge-pinned"),
    PROFILE_BADGE_PIN_ADDED("profile-badge-pin-added"),
    PROFILE_BADGE_PIN_REMOVED("profile-badge-pin-removed"),
    PROFILE_BADGE_PIN_FULL("profile-badge-pin-full"),

    // Points system
    POINTS_GUI_TITLE("points-gui-title"),
    POINTS_NODE_UNLOCKED_CHAT("points-node-unlocked-chat"),
    POINTS_NODE_NAME("points-node-name"),
    POINTS_NODE_STATUS_UNLOCKED("points-node-status-unlocked"),
    POINTS_NODE_STATUS_CURRENT("points-node-status-current"),
    POINTS_NODE_STATUS_LOCKED("points-node-status-locked"),
    POINTS_NODE_PROGRESS("points-node-progress"),
    POINTS_INFO_TITLE("points-info-title"),
    POINTS_INFO_TOTAL("points-info-total"),
    POINTS_INFO_CURRENT_NODE("points-info-current-node"),
    POINTS_INFO_NEXT_IN("points-info-next-in"),
    POINTS_INFO_ALL_UNLOCKED("points-info-all-unlocked"),
    POINTS_BTN_SCROLL_LEFT("points-btn-scroll-left"),
    POINTS_BTN_SCROLL_RIGHT("points-btn-scroll-right"),
    POINTS_BTN_BACK("points-btn-back"),
    PROFILE_BTN_POINTS("profile-btn-points"),
    POINTS_MILESTONE_INDICATOR("points-milestone-indicator"),
    POINTS_REWARDS_HEADER("points-rewards-header"),
    POINTS_REWARD_COMMAND("points-reward-command"),
    POINTS_REWARD_ITEM("points-reward-item"),
    POINTS_REWARD_BADGE("points-reward-badge"),

    // Node reward editor
    POINTS_NODE_EDITOR_TITLE("points-node-editor-title"),
    POINTS_NODE_EDITOR_SAVED("points-node-editor-saved"),
    POINTS_NODE_ADMIN_HINT("points-node-admin-hint");

    private final String key;

    MessageKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
