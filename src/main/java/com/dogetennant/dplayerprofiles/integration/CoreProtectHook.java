package com.dogetennant.dplayerprofiles.integration;

import com.dogetennant.dplayerprofiles.util.LogUtil;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Soft-dependency wrapper for the CoreProtect API.
 * Uses reflection so the plugin compiles and runs even when CoreProtect is absent.
 * CoreProtect API version 9+ is required.
 */
public class CoreProtectHook {

    private final Object api;            // CoreProtectAPI instance
    private final Method blockLookup;   // blockLookup(Block, int) → List<String[]>
    private final Method parseResult;   // parseResult(String[]) → ParseResult
    private final Method getActionId;   // ParseResult.getActionId() → int
    private final Method getPlayer;     // ParseResult.getPlayer() → String
    private final int lookupTime;

    private CoreProtectHook(Object api, Method blockLookup, Method parseResult,
                             Method getActionId, Method getPlayer, int lookupTime) {
        this.api = api;
        this.blockLookup = blockLookup;
        this.parseResult = parseResult;
        this.getActionId = getActionId;
        this.getPlayer = getPlayer;
        this.lookupTime = lookupTime;
    }

    /**
     * Attempts to hook into CoreProtect. Returns null if CoreProtect is not installed,
     * its API is not enabled, or its API version is below 9.
     */
    public static CoreProtectHook create(Plugin cpPlugin, int lookupTime) {
        try {
            Method getApi = cpPlugin.getClass().getMethod("getAPI");
            Object api = getApi.invoke(cpPlugin);
            if (api == null) return null;

            Class<?> apiClass = api.getClass();

            boolean enabled = (boolean) apiClass.getMethod("isEnabled").invoke(api);
            if (!enabled) return null;

            int version = (int) apiClass.getMethod("APIVersion").invoke(api);
            if (version < 9) {
                LogUtil.warn("CoreProtect API version " + version + " is too old (need 9+). Integration disabled.");
                return null;
            }

            Method blockLookup = apiClass.getMethod("blockLookup", Block.class, int.class);
            Method parseResult  = apiClass.getMethod("parseResult", String[].class);

            // ParseResult is a nested class; find its methods via the return type of parseResult
            Class<?> parseResultClass = parseResult.getReturnType();
            Method getActionId = parseResultClass.getMethod("getActionId");
            Method getPlayer   = parseResultClass.getMethod("getPlayer");

            return new CoreProtectHook(api, blockLookup, parseResult, getActionId, getPlayer, lookupTime);
        } catch (Exception e) {
            LogUtil.warn("Failed to initialise CoreProtect hook: " + e.getMessage());
            return null;
        }
    }

    /**
     * Returns true if the most recent placement at this location was by {@code playerName}.
     * Call from an async thread - CoreProtect database queries should not block the main thread.
     */
    public boolean wasPlacedBySamePlayer(Block block, String playerName) {
        return latestActorMatches(block, playerName, 1); // actionId 1 = place
    }

    /**
     * Returns true if the most recent break at this location was by {@code playerName}.
     * Call from an async thread.
     */
    public boolean wasBrokenBySamePlayer(Block block, String playerName) {
        return latestActorMatches(block, playerName, 0); // actionId 0 = break
    }

    @SuppressWarnings("unchecked")
    private boolean latestActorMatches(Block block, String playerName, int targetAction) {
        try {
            List<String[]> data = (List<String[]>) blockLookup.invoke(api, block, lookupTime);
            for (String[] entry : data) {
                Object result = parseResult.invoke(api, (Object) entry);
                int actionId  = (int) getActionId.invoke(result);
                if (actionId == targetAction) {
                    String actor = (String) getPlayer.invoke(result);
                    return actor != null && actor.equalsIgnoreCase(playerName);
                }
            }
        } catch (Exception e) {
            LogUtil.warn("CoreProtect lookup error: " + e.getMessage());
        }
        return false; // no history found for this action type → not farming
    }
}
