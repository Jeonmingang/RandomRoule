package com.minkang.ultimate.random;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.Map;
import java.util.HashMap;

public final class RewardGiver {
    private static final java.util.Map<java.util.UUID, java.util.Map<String, Long>> lastGiven = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long DEDUPE_MS = 1000;
    private RewardGiver(){}

    /** 
     * 지급을 표준화한다: LoreSanitizer로 '가중치' 줄 제거 후 지급.
     * return: Inventory#addItem 과 동일한 leftover map (드랍 시엔 빈 맵)
     */
    private static String keyFor(org.bukkit.inventory.ItemStack it){
        if(it==null) return "null";
        org.bukkit.inventory.meta.ItemMeta m = it.getItemMeta();
        String name = (m!=null && m.hasDisplayName()) ? org.bukkit.ChatColor.stripColor(m.getDisplayName()) : "";
        java.util.List<String> lore = (m!=null && m.hasLore()) ? m.getLore() : java.util.Collections.emptyList();
        return it.getType().name()+"|"+name+"|"+lore.toString();
    }
    public static Map<Integer, ItemStack> giveClean(Player p, ItemStack item){
        if(!com.minkang.ultimate.random.GrantService.shouldGive(p)) return new java.util.HashMap<>();
        String k = keyFor(item);
        long now = System.currentTimeMillis();
        java.util.Map<String, Long> m = lastGiven.computeIfAbsent(p.getUniqueId(), u->new java.util.concurrent.ConcurrentHashMap<>());
        Long prev = m.get(k);
        if(prev!=null && now - prev < DEDUPE_MS){
            return new java.util.HashMap<>();
        }
        m.put(k, now);
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
