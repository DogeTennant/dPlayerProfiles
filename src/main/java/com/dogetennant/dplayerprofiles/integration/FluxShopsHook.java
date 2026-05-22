package com.dogetennant.dplayerprofiles.integration;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.model.TriggerType;
import com.dogetennant.fluxshops.api.event.ShopCreateEvent;
import com.dogetennant.fluxshops.api.event.ShopTransactionEvent;
import com.dogetennant.fluxshops.shop.TransactionRecord;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class FluxShopsHook implements Listener {

    private final DPlayerProfiles plugin;

    public FluxShopsHook(DPlayerProfiles plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTransaction(ShopTransactionEvent e) {
        // Skip self-trades: player buying from or selling to their own shop
        if (!e.getShop().isAdminShop() && e.getShop().getOwnerUuid().equals(e.getActor().getUniqueId())) return;
        String material = e.getShop().getItem().getType().name();
        TriggerType type = e.getType() == TransactionRecord.Type.BUY ? TriggerType.SHOP_BUY : TriggerType.SHOP_SELL;
        plugin.getAchievementManager().increment(e.getActor(), type, material, e.getTotalItems());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShopCreate(ShopCreateEvent e) {
        plugin.getAchievementManager().increment(e.getCreator(), TriggerType.SHOP_CREATE, "*", 1);
    }
}
