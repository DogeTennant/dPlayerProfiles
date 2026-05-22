package com.dogetennant.dplayerprofiles.model;

public enum TriggerType {
    MOB_KILL,
    PLAYER_KILL,
    BLOCK_BREAK,
    BLOCK_PLACE,
    FISH,
    CRAFT,
    TIME_PLAYED,
    LOGIN_STREAK,
    LOGIN_COUNT,
    MANUAL,
    /** Auto-completes when all entries in {@code requires} and {@code required-badges} are satisfied. */
    CHAIN,
    /** Kill in an OneInTheChamberReborn game. target = arena name or * for any. */
    OITC_KILL,
    /** Win an OneInTheChamberReborn game. target = arena name or * for any. */
    OITC_WIN,
    /** Win a dTournaments tournament. target = tournament config ID or * for any. */
    TOURNAMENT_WIN,
    /** Kill a MythicMobs mob. target = mob internal name or * for any. */
    MYTHICMOBS_KILL,
    /** Reach a skill level in mcMMO. count = target level. target = skill name or * for any. SET semantics: amount is the new level. */
    MCMMO_LEVEL_UP,
    /** Buy items from a FluxShops shop. target = Material name or * for any item. */
    SHOP_BUY,
    /** Sell items to a FluxShops shop. target = Material name or * for any item. */
    SHOP_SELL,
    /** Create a FluxShops shop. target unused (*). */
    SHOP_CREATE,
    /** Reach a level in a Jobs Reborn job. count = target level. target = job name or * for any. SET semantics. */
    JOBS_LEVEL_UP,
    /** Join a Jobs Reborn job. target = job name or * for any. */
    JOBS_JOIN,
    /** Reach a skill level in AuraSkills. count = target level. target = skill key (e.g. mining) or * for any. SET semantics. */
    AURASKILLS_LEVEL_UP,
    /** Kill an EliteMobs elite mob. target = entity type name (e.g. ZOMBIE) or * for any. */
    ELITEMOBS_KILL,
    /** Complete an EliteMobs dungeon. target = dungeon name or * for any. */
    ELITEMOBS_DUNGEON_COMPLETE,
    /** Complete an EliteMobs arena. target = arena name or * for any. */
    ELITEMOBS_ARENA_COMPLETE,
    /** Complete an EliteMobs quest. target = quest name or * for any. */
    ELITEMOBS_QUEST_COMPLETE,
    /** Open a CrazyCrates crate. target = crate file name (no .yml) or * for any. */
    CRATE_OPEN,
    /** Participate in killing a PinataParty pinata (hit it at least once). target unused (*). */
    PINATA_KILL,
    /** Win a Duels match (any reason). target = kit name or * for any kit. */
    DUEL_WIN,
    /** Kill the opponent in a Duels match (reason = OPPONENT_DEFEAT only). target = kit name or * for any. */
    DUEL_KILL
}
