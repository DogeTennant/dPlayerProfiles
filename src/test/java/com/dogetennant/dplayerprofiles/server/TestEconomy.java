package com.dogetennant.dplayerprofiles.server;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** A Vault economy that keeps balances in memory, with cents like most economy plugins. */
final class TestEconomy {

    private final Map<UUID, Double> balances = new ConcurrentHashMap<>();
    final Economy vault = mock(Economy.class);

    TestEconomy() {
        when(vault.getName()).thenReturn("TestEconomy");
        when(vault.isEnabled()).thenReturn(true);
        when(vault.fractionalDigits()).thenReturn(2);
        when(vault.format(anyDouble())).thenAnswer(call -> String.format(java.util.Locale.ROOT, "$%.2f",
                call.<Double>getArgument(0)));
        when(vault.getBalance(any(OfflinePlayer.class))).thenAnswer(call -> balance(call.getArgument(0)));
        when(vault.has(any(OfflinePlayer.class), anyDouble())).thenAnswer(call ->
                balance(call.getArgument(0)) >= call.<Double>getArgument(1));
        when(vault.depositPlayer(any(OfflinePlayer.class), anyDouble())).thenAnswer(call ->
                change(call.getArgument(0), call.getArgument(1)));
        when(vault.withdrawPlayer(any(OfflinePlayer.class), anyDouble())).thenAnswer(call ->
                change(call.getArgument(0), -call.<Double>getArgument(1)));
    }

    double balance(OfflinePlayer player) {
        return balances.getOrDefault(player.getUniqueId(), 0.0);
    }

    void set(OfflinePlayer player, double amount) {
        balances.put(player.getUniqueId(), amount);
    }

    private EconomyResponse change(OfflinePlayer player, double amount) {
        double now = balances.merge(player.getUniqueId(), amount, Double::sum);
        return new EconomyResponse(Math.abs(amount), now, EconomyResponse.ResponseType.SUCCESS, null);
    }
}
