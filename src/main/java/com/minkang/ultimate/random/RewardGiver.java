package com.minkang.ultimate.random;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.Map;
import java.util.HashMap;

public final class RewardGiver {
    private RewardGiver(){}

    /** 
     * 지급을 표준화한다: LoreSanitizer로 '가중치' 줄 제거 후 지급.
     * return: Inventory#addItem 과 동일한 leftover map (드랍 시엔 빈 맵)
     */
    public static Map<Integer, ItemStack> giveClean(Player p, ItemStack item){
        if(!com.minkang.ultimate.random.GrantService.shouldGive(p)) return new java.util.HashMap<>();
        Map<Integer, ItemStack> leftovers = new HashMap<>();
        if(p==null || item==null) return leftovers;
        ItemStack clean = LoreSanitizer.strip(item);
        if(p.getInventory().firstEmpty()==-1){
            p.getWorld().dropItemNaturally(p.getLocation(), clean);
            return leftovers; // nothing left
        }else{
            return p.getInventory().addItem(clean);
        }
    }
}
