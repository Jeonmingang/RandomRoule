package com.minkang.ultimateroulette.gui;

import com.minkang.ultimateroulette.UltimateRoulette;
import com.minkang.ultimateroulette.data.KeyDef;
import com.minkang.ultimateroulette.data.Reward;
import com.minkang.ultimateroulette.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PreviewGUI {
    private final UltimateRoulette plugin;
    private final KeyDef def;
    private final int page;

    public PreviewGUI(UltimateRoulette plugin, KeyDef def) { this(plugin, def, 0); }
    public PreviewGUI(UltimateRoulette plugin, KeyDef def, int page) {
        this.plugin = plugin;
        this.def = def;
        this.page = Math.max(0, page);
    }

    public void open(Player p) {
        String title = plugin.getConfig().getString("titles.preview", "&d&l미리보기: %KEY%").replace("%KEY%", def.getName());
        String ptag = page>0?(" &7(p" + (page+1) + ")"):"";
        Inventory inv = Bukkit.createInventory(null, 54, Text.color(title + ptag));

        int total = Math.max(1, def.totalWeight());
        int idx = 0;
        int start = page*45;
        java.util.List<Reward> all = new java.util.ArrayList<>(def.getRewards());
        for (int i = start; i < all.size() && idx < 45; i++) {
            Reward r = all.get(i);
            ItemStack it = r.getItem().clone();
            ItemMeta m = it.getItemMeta();
            java.util.List<String> lore = (m.hasLore() ? m.getLore() : new java.util.ArrayList<>());
            double pct = (r.getWeight() * 100.0) / total;
            lore.add(Text.color("&7확률: &b" + String.format(java.util.Locale.US,"%.2f", pct) + "%"));
            m.setLore(lore);
            it.setItemMeta(m);
            inv.setItem(idx++, it);
            if (idx >= 45) break;
        }

        
        // navigation buttons
        org.bukkit.inventory.ItemStack prev = new org.bukkit.inventory.ItemStack(Material.ARROW);
        org.bukkit.inventory.meta.ItemMeta pm = prev.getItemMeta();
        pm.setDisplayName(Text.color("&7이전 페이지"));
        prev.setItemMeta(pm);
        org.bukkit.inventory.ItemStack next = new org.bukkit.inventory.ItemStack(Material.ARROW);
        org.bukkit.inventory.meta.ItemMeta nm = next.getItemMeta();
        nm.setDisplayName(Text.color("&7다음 페이지"));
        next.setItemMeta(nm);
        inv.setItem(45, prev);
        inv.setItem(53, next);

        // start button
        ItemStack start = new ItemStack(Material.LIME_WOOL);
        ItemMeta sm = start.getItemMeta();
        sm.setDisplayName(Text.color("&a&l뽑기 시작"));
        java.util.List<String> sl = new java.util.ArrayList<>();
        sl.add(Text.color("&7전용 아이템 1개를 소비합니다."));
        sm.setLore(sl);
        start.setItemMeta(sm);
        inv.setItem(49, start);

        p.openInventory(inv);
    }
}
