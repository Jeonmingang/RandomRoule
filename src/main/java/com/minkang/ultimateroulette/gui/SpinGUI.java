package com.minkang.ultimateroulette.gui;

import com.minkang.ultimateroulette.UltimateRoulette;
import com.minkang.ultimateroulette.data.KeyDef;
import com.minkang.ultimateroulette.data.Reward;
import com.minkang.ultimateroulette.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class SpinGUI {
    private final UltimateRoulette plugin;
    private final KeyDef def;
    private final java.util.function.Consumer<ItemStack> onFinish;
    private final Random rnd = new Random();

    public SpinGUI(UltimateRoulette plugin, KeyDef def, java.util.function.Consumer<ItemStack> onFinish) {
        this.plugin = plugin;
        this.def = def;
        this.onFinish = onFinish;
    }

    public void open(Player p) {
        String title = plugin.getConfig().getString("titles.spinner", "&6&l룰렛 스핀!");
        Inventory inv = Bukkit.createInventory(null, 27, Text.color(title));
        // fill with glass
        for (int i=0;i<27;i++) inv.setItem(i, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));

        // rolling slot at middle (13)
        ItemStack pointer = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta pm = pointer.getItemMeta();
        pm.setDisplayName(Text.color("&c&l스핀 중..."));
        pointer.setItemMeta(pm);
        inv.setItem(13, pointer);

        p.openInventory(inv);

        int total = Math.max(1, def.totalWeight());
        // pick result first (weighted)
        int ticket = rnd.nextInt(total) + 1;
        Reward chosen = null;
        int sum = 0;
        for (Reward r : def.getRewards()) {
            sum += r.getWeight();
            if (ticket <= sum) { chosen = r; break; }
        }
        if (chosen == null) chosen = def.getRewards().get(0);
        ItemStack result = chosen.getItem().clone();

        int totalMs = plugin.getConfig().getInt("spin.total_ms", 3000);
        int tickDelay = Math.max(1, plugin.getConfig().getInt("spin.tick_delay", 2));
        int ticks = Math.max(1, totalMs / (tickDelay*50));

        new BukkitRunnable() {
            int i = 0;
            @Override public void run() {
                // show a random reward in slot 13
                Reward r = def.getRewards().get(rnd.nextInt(def.getRewards().size()));
                ItemStack it = r.getItem().clone();
                ItemMeta m = it.getItemMeta();
                m.setDisplayName(Text.color("&e스핀..."));
                it.setItemMeta(m);
                inv.setItem(13, it);
                try { org.bukkit.Sound s = org.bukkit.Sound.valueOf(plugin.getConfig().getString("sounds.spin_tick","UI_BUTTON_CLICK")); p.playSound(p.getLocation(), s, 0.5f, 1.6f);} catch (Exception ex) { p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.6f);}
                i++;
                if (i >= ticks) {
                    inv.setItem(13, result);
                    try { org.bukkit.Sound s2 = org.bukkit.Sound.valueOf(plugin.getConfig().getString("sounds.spin_end","ENTITY_PLAYER_LEVELUP")); p.playSound(p.getLocation(), s2, 1f, 1f);} catch (Exception ex) { p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);}
                    cancel();
                    // finish after slight delay to ensure GUI shows final item
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        p.closeInventory();
                        onFinish.accept(result);
                    }, 20L);
                }
            }
        }.runTaskTimer(plugin, 0L, tickDelay);
    }
}
