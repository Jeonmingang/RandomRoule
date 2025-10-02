package com.signition.samskybridge.gui;

import com.signition.samskybridge.Main;
import com.signition.samskybridge.level.LevelService;
import com.signition.samskybridge.upgrade.UpgradeService;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

/** Lightweight wrapper that delegates to UpgradeService to build/open GUI. */
public class UpgradeGUI {
    private final Main plugin;
    private final LevelService level;
    private final Economy economy;
    private final UpgradeService upgrade;

    public UpgradeGUI(Main plugin, LevelService level, Economy economy){
        this.plugin = plugin;
        this.level = level;
        this.economy = economy;
        this.upgrade = new UpgradeService(plugin, plugin.getDataStore(), level, plugin.getVault());
    }

    public void open(Player p){
        upgrade.open(p);
    }
}
