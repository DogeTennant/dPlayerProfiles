package com.dogetennant.dplayerprofiles.integration;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.model.TriggerType;
import net.brcdev.shopgui.event.ShopPostTransactionEvent;
import net.brcdev.shopgui.shop.ShopManager;
import net.brcdev.shopgui.shop.ShopTransactionResult;
import net.brcdev.shopgui.shop.ShopTransactionResult.ShopTransactionResultType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ShopGUIPlusHook implements Listener {

    private final DPlayerProfiles plugin;

    public ShopGUIPlusHook(DPlayerProfiles plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPostTransaction(ShopPostTransactionEvent e) {
        ShopTransactionResult result = e.getResult();
        if (result.getResult() != ShopTransactionResultType.SUCCESS) return;

        String material = result.getShopItem().getItem().getType().name();
        int amount = result.getAmount();
        ShopManager.ShopAction action = result.getShopAction();

        if (action == ShopManager.ShopAction.BUY) {
            plugin.getAchievementManager().increment(result.getPlayer(), TriggerType.SHOP_BUY, material, amount);
        } else {
            // SELL and SELL_ALL both count as sells
            plugin.getAchievementManager().increment(result.getPlayer(), TriggerType.SHOP_SELL, material, amount);
        }
    }
}
