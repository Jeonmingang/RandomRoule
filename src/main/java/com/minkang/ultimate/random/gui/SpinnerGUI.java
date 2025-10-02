
package com.minkang.ultimate.random.gui;

import com.minkang.ultimate.random.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SpinnerGUI {
    public static void start(final Main plugin, final Player p, final Roulette r){
        if(!plugin.tryBeginSpin(p)){
            p.sendMessage(plugin.msg("already_spinning"));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 0.7f);
            return;
        }

        final String title = plugin.getConfig().getString("titles.spinner","룰렛 뽑기: %key%").replace("%key%", r.getKey());
        final Inventory inv = Bukkit.createInventory(p, 27, ChatColor.translateAlternateColorCodes('&', title));
        p.openInventory(inv);

        final List<RouletteEntry> entries = r.getEntries()==null ? new ArrayList<>() : r.getEntries();
        if(entries.isEmpty()){
            p.closeInventory();
            plugin.endSpin(p);
            p.sendMessage(plugin.color("&c설정된 보상이 없습니다."));
            return;
        }

        // Precompute probability
        final int totalWeight = r.getTotalWeight();
        final List<ItemStack> display = new ArrayList<>();
        for(RouletteEntry e : entries){
            ItemStack it = e.getItem()==null? new ItemStack(Material.BARRIER): e.getItem().clone();
            ItemMeta im = it.getItemMeta();
            if(im!=null){
                List<String> lore = new ArrayList<>();
                double prob = (100.0 * Math.max(1, e.getWeight()) / Math.max(1, totalWeight));
                lore.add(ChatColor.GRAY + "확률: " + ChatColor.AQUA + String.format("%.2f", prob) + "%");
                im.setLore(lore);            // ← 원본 로어 제거하고 확률만
                it.setItemMeta(im);
            }
            it.setAmount(1);
            display.add(it);
        }

        final int size = display.size();
        final Random rnd = new Random();
        final int totalSteps = 80 + rnd.nextInt(60); // 80~139 ticks
        final int[] pointer = {0};
        final int[] stepsLeft = {totalSteps};
        final int[] cooldown = {0};
        final int[] delay = {0};

        new BukkitRunnable(){
            @Override public void run(){
                if(!p.isOnline()){ plugin.endSpin(p); cancel(); return; }
                if(stepsLeft[0] <= 0){
                    // Decide winner by weight
                    RouletteEntry win = r.pickByWeight();
                    ItemStack reward = (win==null || win.getItem()==null) ? new ItemStack(Material.AIR) : win.getItem().clone();
                    if(reward.getType()!=Material.AIR){

                        java.util.Map<Integer, ItemStack> left = p.getInventory().addItem(reward);
                        for(ItemStack lf : left.values()) p.getWorld().dropItemNaturally(p.getLocation(), lf);
                    }
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
                    plugin.endSpin(p);
                    p.closeInventory();
                    cancel();
                    return;
                }

                if(cooldown[0] > 0){ cooldown[0]--; return; }

                // Paint center row (slots 9..17)
                for(int i=0;i<9;i++){
                    int idx = (pointer[0] + i) % size;
                    inv.setItem(9 + i, display.get(idx));
                }
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.1f - Math.min(0.8f, (float)(totalSteps-stepsLeft[0])/(float)totalSteps));

                pointer[0] = (pointer[0]+1) % size;
                stepsLeft[0]--;

                if(((totalSteps - stepsLeft[0]) % 10) == 0) delay[0]++;
                if(((totalSteps - stepsLeft[0]) % 20) == 0) delay[0]++;
                cooldown[0] = Math.max(1, delay[0]);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
