package com.minkang.ultimateroulette.gui;

import com.minkang.ultimateroulette.UltimateRoulette;
import com.minkang.ultimateroulette.data.KeyDef;
import com.minkang.ultimateroulette.data.Reward;
import com.minkang.ultimateroulette.util.Text;
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
import java.util.Locale;
import java.util.Random;

public class SpinGUI {
    public static final int[] TRACK_SLOTS = {9,10,11,12,13,14,15,16,17};
    public static final int TOP_CENTER = 4;
    public static final int BOTTOM_CENTER = 22;

    private final UltimateRoulette plugin;
    private final KeyDef def;
    private Inventory inv;

    public SpinGUI(UltimateRoulette plugin, KeyDef def) {
        this.plugin = plugin;
        this.def = def;
    }

    public static boolean isSpinTitle(String title) {
        if (title == null) return false;
        String plain = ChatColor.stripColor(title);
        return plain != null && plain.startsWith("룰렛 스핀:");
    }

    private String title() {
        return Text.color("&6&l룰렛 스핀: &f" + def.getName());
    }

    public void open(Player p) {
        inv = Bukkit.createInventory(null, 27, title());
        // Markers
        ItemStack mark = new ItemStack(Material.HOPPER);
        ItemMeta mm = mark.getItemMeta();
        if (mm != null) {
            mm.setDisplayName(Text.color("&7▼"));
            mark.setItemMeta(mm);
        }
        inv.setItem(TOP_CENTER, mark);
        inv.setItem(BOTTOM_CENTER, mark);

        // Fill others with glass to prevent pickup feel
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        if (fm != null) {
            fm.setDisplayName(Text.color("&8"));
            filler.setItemMeta(fm);
        }
        for (int i=0;i<27;i++) {
            if (i==TOP_CENTER || i==BOTTOM_CENTER) continue;
            inv.setItem(i, filler);
        }

        p.openInventory(inv);
        startSpin(p);
    }

    private void startSpin(final Player p) {
        // Build weighted reel
        List<Reward> rewards = def.getRewards();
        List<Reward> reel = new ArrayList<Reward>();
        int totalW = 0;
        for (Reward r : rewards) {
            int w = Math.max(0, r.getWeight());
            totalW += w;
            for (int i=0;i<w;i++) reel.add(r);
        }
        if (reel.isEmpty()) {
            // fallback
            Reward dummy = new Reward(new ItemStack(Material.BARRIER), 1);
            reel.add(dummy);
            totalW = 1;
        }
        // Precompute display items with probability overlay (only for spin GUI, not final item)
        List<ItemStack> reelItems = new ArrayList<ItemStack>(reel.size());
        for (Reward r : reel) {
            ItemStack base = (r.getItem()!=null ? r.getItem().clone() : new ItemStack(Material.CHEST));
            ItemMeta m = base.getItemMeta();
            if (m != null) {
                List<String> lore = (m.hasLore() ? new ArrayList<String>(m.getLore()) : new ArrayList<String>());
                double pct = totalW>0 ? (r.getWeight()*100.0/totalW) : 0.0;
                lore.add(Text.color("&7확률: &b" + String.format(Locale.US, "%.2f", pct) + "%"));
                m.setLore(lore);
                base.setItemMeta(m);
            }
            reelItems.add(base);
        }

        final int size = reelItems.size();
        final int[] idx = { new Random().nextInt(size) };
        final int baseSteps = Math.max(40, size*3);
        final int slowSteps = 40;
        final int totalSteps = baseSteps + slowSteps;
        final int[] step = {0};
        final int[] interval = {1}; // shift every 'interval' ticks
        final int[] tick = {0};

        new BukkitRunnable() {
            @Override public void run() {
                if (inv == null || !SpinGUI.isSpinTitle(p.getOpenInventory().getTitle())) { cancel(); return; }

                // Deceleration: after baseSteps, gradually increase interval
                if (step[0] > baseSteps) {
                    if ((step[0]-baseSteps) % 8 == 0 && interval[0] < 5) interval[0]++;
                }

                if (tick[0] % interval[0] == 0) {
                    // shift window render
                    for (int i=0;i<TRACK_SLOTS.length;i++) {
                        int reelIndex = (idx[0] + i) % size;
                        inv.setItem(TRACK_SLOTS[i], reelItems.get(reelIndex));
                    }
                    idx[0] = (idx[0] + 1) % size;
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.6f);
                }
                tick[0]++;
                step[0]++;

                if (step[0] >= totalSteps) {
                    this.cancel();
                    // Center item: TRACK_SLOTS[4] corresponds to reel index (idx -1 + 4)
                    int centerReelIndex = ( (idx[0] - 1 + 4) % size + size ) % size;
                    Reward win = reel.get(centerReelIndex);
                    ItemStack prize = (win.getItem()!=null ? win.getItem().clone() : new ItemStack(org.bukkit.Material.CHEST));
                    plugin.storage().addClaim(p.getUniqueId(), prize);
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                    p.sendMessage(Text.color("&a보상이 보관함에 지급되었습니다."));
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
}
