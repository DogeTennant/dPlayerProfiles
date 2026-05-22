package com.dogetennant.dplayerprofiles.lang;

import com.dogetennant.dplayerprofiles.util.ColorUtil;
import com.dogetennant.dplayerprofiles.util.LogUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {

    private final Plugin plugin;
    private final Map<String, String> messages = new HashMap<>();
    private String currentLanguage;

    public LanguageManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load(String languageCode) {
        this.currentLanguage = languageCode;
        messages.clear();

        File langFile = new File(plugin.getDataFolder(), "translations/" + languageCode + ".yml");

        if (!langFile.exists()) {
            InputStream resource = plugin.getResource("translations/" + languageCode + ".yml");
            if (resource != null) {
                plugin.saveResource("translations/" + languageCode + ".yml", false);
            } else {
                LogUtil.warn("Language file not found: " + languageCode + ".yml - falling back to en_us");
                loadFromJar("en_us");
                return;
            }
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(langFile);

        InputStream resource = plugin.getResource("translations/" + languageCode + ".yml");
        if (resource != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(resource, StandardCharsets.UTF_8));
            config.setDefaults(defaults);
        }

        for (String key : config.getKeys(true)) {
            if (config.isString(key)) {
                messages.put(key, config.getString(key, ""));
            }
        }

        LogUtil.info("Loaded language: " + languageCode);
    }

    private void loadFromJar(String code) {
        InputStream resource = plugin.getResource("translations/" + code + ".yml");
        if (resource == null) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new InputStreamReader(resource, StandardCharsets.UTF_8));
        for (String key : config.getKeys(true)) {
            if (config.isString(key)) {
                messages.put(key, config.getString(key, ""));
            }
        }
    }

    public boolean switchLanguage(String code) {
        InputStream resource = plugin.getResource("translations/" + code + ".yml");
        File external = new File(plugin.getDataFolder(), "translations/" + code + ".yml");
        if (resource == null && !external.exists()) {
            return false;
        }
        load(code);
        plugin.getConfig().set("language", code);
        plugin.saveConfig();
        return true;
    }

    public String getRaw(MessageKey key, Placeholder... placeholders) {
        String raw = messages.getOrDefault(key.getKey(), key.getKey());
        String prefix = messages.getOrDefault(MessageKey.PREFIX.getKey(), "");
        raw = raw.replace("{prefix}", prefix);
        for (Placeholder p : placeholders) {
            raw = raw.replace("{" + p.key() + "}", p.value());
        }
        return raw;
    }

    public Component get(MessageKey key, Placeholder... placeholders) {
        return ColorUtil.parse(getRaw(key, placeholders));
    }

    public void send(Player player, MessageKey key, Placeholder... placeholders) {
        player.sendMessage(get(key, placeholders));
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }
}
