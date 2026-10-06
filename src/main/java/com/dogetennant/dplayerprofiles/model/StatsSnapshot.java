package com.dogetennant.dplayerprofiles.model;

import java.util.UUID;

/**
 * One player's exported statistics as written to the '<prefix>player_stats' table.
 * The vanilla counters come from Minecraft's own statistics (in memory for online players,
 * from world/stats/<uuid>.json for offline ones); the balance comes from Vault.
 *
 * @param username     may be null when the name is not known (offline backfill of a player
 *                     that never joined since the plugin was installed)
 * @param playtimeSeconds vanilla total playtime - includes AFK time, unlike PlayerProfile's
 * @param balance      null = unknown (no Vault economy, or not read yet); an upsert with a
 *                     null balance keeps whatever the database already has
 */
public record StatsSnapshot(UUID uuid, String username, long mobKills, long playerKills,
                            long deaths, long playtimeSeconds, long blocksMined, Double balance) {

    public StatsSnapshot withBalance(Double balance) {
        return new StatsSnapshot(uuid, username, mobKills, playerKills, deaths,
                playtimeSeconds, blocksMined, balance);
    }
}
