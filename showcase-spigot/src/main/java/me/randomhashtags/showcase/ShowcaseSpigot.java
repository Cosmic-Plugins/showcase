package me.randomhashtags.showcase;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class ShowcaseSpigot extends JavaPlugin {

    public static ShowcaseSpigot getPlugin;
    public boolean placeholderapi;

    @Override
    public void onEnable() {
        getPlugin = this;
        saveSettings();
        getCommand("showcase").setExecutor(ShowcaseAPI.INSTANCE);
        enable();
    }

    @Override
    public void onDisable() {
        disable();
    }

    private void saveSettings() {
        saveDefaultConfig();
    }

    public void enable() {
        saveSettings();
        placeholderapi = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        ShowcaseAPI.INSTANCE.load();
    }
    public void disable() {
        placeholderapi = false;
        ShowcaseAPI.INSTANCE.unload();
    }
}
