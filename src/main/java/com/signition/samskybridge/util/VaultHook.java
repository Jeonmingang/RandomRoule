package com.signition.samskybridge.util;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
public final class VaultHook {
    private static Economy ECON = null;
    public static void init(){
        try{
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) ECON = rsp.getProvider();
        }catch(Throwable ignore){}
    }
    public static boolean hasEconomy(){ return ECON != null; }
    public static boolean withdraw(OfflinePlayer p, double amount){
        if (ECON == null || p == null || amount <= 0) return false;
        return ECON.withdrawPlayer(p, amount).transactionSuccess();
    }
}
