package com.minkang.ultimate.random;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class RewardGiver {
    private RewardGiver(){}
    public static void giveClean(Player p, ItemStack item){
        if(p==null || item==null) return;
        ItemStack clean = LoreSanitizer.strip(item);
        if(p.getInventory().firstEmpty()==-1)
            p.getWorld().dropItemNaturally(p.getLocation(), clean);
        else
            p.getInventory().addItem(clean);
    }
}
