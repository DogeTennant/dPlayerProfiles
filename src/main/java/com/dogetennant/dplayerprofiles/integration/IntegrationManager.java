package com.dogetennant.dplayerprofiles.integration;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.util.LogUtil;
import org.bukkit.Bukkit;

public class IntegrationManager {

    private final DPlayerProfiles plugin;
    private PlaceholderAPIHook papiHook;
    private VaultHook vaultHook;

    public IntegrationManager(DPlayerProfiles plugin) {
        this.plugin = plugin;
    }

    public void init() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            papiHook = new PlaceholderAPIHook(plugin);
            papiHook.register();
            LogUtil.info("PlaceholderAPI integration enabled.");
        }

        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            vaultHook = new VaultHook();
            if (vaultHook.isAvailable()) {
                LogUtil.info("Vault economy integration enabled.");
            } else {
                LogUtil.warn("Vault is installed but no economy plugin registered with it - balances will not be exported.");
            }
        }

        if (Bukkit.getPluginManager().getPlugin("OneInTheChamberReborn") != null) {
            Bukkit.getPluginManager().registerEvents(new OITCHook(plugin), plugin);
            LogUtil.info("OneInTheChamberReborn integration enabled.");
        }

        if (Bukkit.getPluginManager().getPlugin("dTournaments") != null) {
            Bukkit.getPluginManager().registerEvents(new DTournamentsHook(plugin), plugin);
            LogUtil.info("dTournaments integration enabled.");
        }

        if (Bukkit.getPluginManager().getPlugin("MythicMobs") != null) {
            Bukkit.getPluginManager().registerEvents(new MythicMobsHook(plugin), plugin);
            LogUtil.info("MythicMobs integration enabled.");
        }

        if (Bukkit.getPluginManager().getPlugin("mcMMO") != null) {
            Bukkit.getPluginManager().registerEvents(new McMMOHook(plugin), plugin);
            LogUtil.info("mcMMO integration enabled.");
        }

        var fluxShops = Bukkit.getPluginManager().getPlugin("FluxShops");
        if (fluxShops != null && fluxShops.isEnabled()) {
            Bukkit.getPluginManager().registerEvents(new FluxShopsHook(plugin), plugin);
            LogUtil.info("FluxShops integration enabled.");
        } else if (fluxShops != null) {
            LogUtil.warn("FluxShops is loaded but not enabled - integration skipped. Check FluxShops startup errors.");
        }

        if (Bukkit.getPluginManager().getPlugin("Jobs") != null) {
            Bukkit.getPluginManager().registerEvents(new JobsHook(plugin), plugin);
            LogUtil.info("Jobs Reborn integration enabled.");
        }

        if (Bukkit.getPluginManager().getPlugin("AuraSkills") != null) {
            Bukkit.getPluginManager().registerEvents(new AuraSkillsHook(plugin), plugin);
            LogUtil.info("AuraSkills integration enabled.");
        }

        if (Bukkit.getPluginManager().getPlugin("EliteMobs") != null) {
            Bukkit.getPluginManager().registerEvents(new EliteMobsHook(plugin), plugin);
            LogUtil.info("EliteMobs integration enabled.");
        }

        if (Bukkit.getPluginManager().getPlugin("CrazyCrates") != null) {
            Bukkit.getPluginManager().registerEvents(new CrazyCratesHook(plugin), plugin);
            LogUtil.info("CrazyCrates integration enabled.");
        }

        if (Bukkit.getPluginManager().getPlugin("ShopGUIPlus") != null) {
            Bukkit.getPluginManager().registerEvents(new ShopGUIPlusHook(plugin), plugin);
            LogUtil.info("ShopGUI+ integration enabled.");
        }

        if (Bukkit.getPluginManager().getPlugin("PinataParty") != null) {
            Bukkit.getPluginManager().registerEvents(new PinataPartyHook(plugin), plugin);
            LogUtil.info("PinataParty integration enabled.");
        }

        if (Bukkit.getPluginManager().getPlugin("Duels") != null) {
            Bukkit.getPluginManager().registerEvents(new DuelsHook(plugin), plugin);
            LogUtil.info("Duels integration enabled.");
        }
    }

    public void shutdown() {
        if (papiHook != null) papiHook.unregister();
    }

    /** Null when Vault is not installed. Check {@link VaultHook#isAvailable()} before use. */
    public VaultHook getVaultHook() {
        return vaultHook;
    }
}
