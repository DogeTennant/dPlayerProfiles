package com.dogetennant.dplayerprofiles.integration;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Read-only access to the Vault economy, used by the web statistics export.
 * Only instantiate when the Vault plugin is present - this class references Vault's API types.
 */
public class VaultHook {

    private final Economy economy;

    public VaultHook() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        this.economy = rsp == null ? null : rsp.getProvider();
    }

    /** False when Vault is installed but no economy plugin has registered with it. */
    public boolean isAvailable() {
        return economy != null;
    }

    /**
     * The player's balance, 0 for players without an account, or null if it could not be read.
     * Main thread only - economy plugins are not required to be thread-safe.
     */
    public Double getBalance(OfflinePlayer player) {
        if (economy == null) return null;
        try {
            if (!economy.hasAccount(player)) return 0.0;
            return economy.getBalance(player);
        } catch (Exception e) {
            return null;
        }
    }
}
