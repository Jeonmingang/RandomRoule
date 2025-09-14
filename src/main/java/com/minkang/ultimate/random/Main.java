
package com.minkang.ultimate.random;

import com.minkang.ultimate.random.gui.PreviewGUI;
import com.minkang.ultimate.random.gui.SettingsGUI;
import com.minkang.ultimate.random.listener.InteractListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private RouletteManager manager;
    private NamespacedKey pdcKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.manager = new RouletteManager(this);
        this.pdcKey = new NamespacedKey(this, "ultimate_random_key");

        RandomCommand cmd = new RandomCommand(this);
        PluginCommand pc = getCommand("random");
        if (pc != null) {
            pc.setExecutor(cmd);
            pc.setTabCompleter(cmd);
        }

        Bukkit.getPluginManager().registerEvents(new SettingsGUI(this), this);
        Bukkit.getPluginManager().registerEvents(new PreviewGUI(this), this);
        Bukkit.getPluginManager().registerEvents(new InteractListener(this), this);

        getLogger().info("UltimateRandomRoulette v1.2.0 enabled.");
    }

    @Override
    public void onDisable() {
        manager.save();
    }

    public RouletteManager getManager() {
        return manager;
    }

    public NamespacedKey getPdcKey() {
        return pdcKey;
    }

    public String msg(String path) {
        String s = getConfig().getString("messages." + path, "");
        String prefix = getConfig().getString("messages.prefix", "");
        return ChatColor.translateAlternateColorCodes('&', prefix + s);
    }

    public String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
