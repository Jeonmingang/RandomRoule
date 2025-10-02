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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SpinGUI {
    private final UltimateRoulette plugin;
    private final KeyDef def;
    private final int[] TRACK = {9,10,11,12,13,14,15,16,17};

    public SpinGUI(UltimateRoulette plugin, KeyDef def) {
        this.plugin = plugin;
        this.def = def;
    }

    private ItemStack markerPane(String name) {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m = pane.getItemMeta();
        m.setDisplayName(Text.color("&7" + name));
        pane.setItemMeta(m);
        return pane;
    }

    /** Reel of rewards expanded by weight (Reward references, not items) */
    private List<Reward> buildReelRewards(List<Reward> rewards) {
        List<Reward> reel = new ArrayList<>();
        for (Reward r : rewards) {
            int w = Math.max(1, r.getWeight());
            for (int i=0;i<w;i++) reel.add(r);
        }
        if (reel.isEmpty()) {
            // fallback dummy
            Reward dummy = new Reward(new ItemStack(Material.BARRIER), 1);
            reel.add(dummy);
        }
        Collections.shuffle(reel);
        return reel;
    }

    private ItemStack withProbLore(ItemStack base, double pct) {
        ItemStack it = base.clone();
        ItemMeta m = it.getItemMeta();
        List<String> lore = (m != null && m.hasLore()) ? new ArrayList<>(m.getLore()) : new ArrayList<>();
        lore.add(Text.color("&7확률: &b" + String.format(Locale.US, "%.2f", pct) + "%"));
        if (m != null) {
            m.setLore(lore);
            it.setItemMeta(m);
        }
        return it;
    }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, Text.color("&6&l룰렛 스핀!"));
        inv.setItem(4, markerPane("▼"));
        inv.setItem(22, markerPane("▼"));
        p.openInventory(inv);

        List<Reward> rewards = def.getRewards();
        int totalWeight = Math.max(1, rewards.stream().mapToInt(Reward::getWeight).sum());
        final List<Reward> reel = buildReelRewards(rewards);

        final int[] idx = {0};
        final int totalSteps = Math.max(40, 6 * TRACK.length); // 최소 회전 보장

        new BukkitRunnable() {
            int ticks = 0;
            @Override public void run() {
                // markers
                inv.setItem(4, markerPane("▼"));
                inv.setItem(22, markerPane("▼"));

                // fill track with probability overlay
                for (int i=0;i<TRACK.length;i++) {
                    Reward rr = reel.get((idx[0]+i) % reel.size());
                    ItemStack showBase = (rr.getItem()!=null ? rr.getItem() : new ItemStack(Material.CHEST));
                    double pct = rr.getWeight() * 100.0 / totalWeight;
                    ItemStack show = withProbLore(showBase, pct); // 표시용만 확률 추가
                    inv.setItem(TRACK[i], show);
                }
                idx[0]++; ticks++;
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.5f);

                if (ticks >= totalSteps) {
                    this.cancel();
                    // Determine winning reward = item currently at center index of reel
                    Reward winR = reel.get((idx[0]-1 + 4) % reel.size()); // -(1) because idx advanced after render; +4 offset to center(13) in TRACK
                    ItemStack prize = (winR.getItem()!=null ? winR.getItem().clone() : new ItemStack(Material.CHEST));
                    plugin.storage().addClaim(p.getUniqueId(), prize); // 원본 메타 그대로 지급
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                    p.sendMessage(Text.color("&a보상이 보관함에 지급되었습니다."));
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
}
