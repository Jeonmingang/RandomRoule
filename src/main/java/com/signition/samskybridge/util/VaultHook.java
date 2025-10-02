package com.signition.samskybridge.util;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class VaultHook {
    private final JavaPlugin plugin;
    private Economy economy;

    public VaultHook(JavaPlugin plugin){
        this.plugin = plugin;
        try {
            if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
                RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
                if (rsp != null) {
                    economy = rsp.getProvider();
                    plugin.getLogger().info("Vault economy hooked: " + economy.getName());
                } else {
                    plugin.getLogger().warning("Vault economy provider missing. Economy features limited.");
                }
            } else {
                plugin.getLogger().warning("Vault is not installed. Economy features disabled.");
            }
        } catch (Throwable t){
            plugin.getLogger().warning("Vault hook error: " + t.getMessage());
        }
    }

    public boolean withdraw(String playerName, double amount){
        if (economy == null) return false;
        try {
            OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
            if (!economy.has(op, amount)) return false;
            return economy.withdrawPlayer(op, amount).transactionSuccess();
        } catch (Throwable t){
            plugin.getLogger().warning("Vault withdraw error: " + t.getMessage());
            return false;
        }
    }

    public boolean deposit(String playerName, double amount){
        if (economy == null) return false;
        try {
            OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
            return economy.depositPlayer(op, amount).transactionSuccess();
        } catch (Throwable t){
            plugin.getLogger().warning("Vault deposit error: " + t.getMessage());
            return false;
        }
    }

    public boolean has(String playerName, double amount){
        if (economy == null) return false;
        try {
            OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
            return economy.has(op, amount);
        } catch (Throwable t){
            return false;
        }
    }

    public Economy getEconomy(){
        return economy;
    }
}
