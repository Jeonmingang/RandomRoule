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
    String title = com.minkang.ultimateroulette.util.Text.color("&d&l미리보기: " + def.getName());
    org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 54, title);

    java.util.List<com.minkang.ultimateroulette.data.Reward> rewards = def.getRewards();
    int idx = 0;
    for (com.minkang.ultimateroulette.data.Reward r : rewards) {
        if (idx >= 45) break;
        org.bukkit.inventory.ItemStack it = (r.getItem() != null ? r.getItem().clone() : new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHEST));
        inv.setItem(idx++, it);
    }

    org.bukkit.inventory.ItemStack startBtn = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LIME_WOOL);
    org.bukkit.inventory.meta.ItemMeta sm = startBtn.getItemMeta();
    sm.setDisplayName(com.minkang.ultimateroulette.util.Text.color("&a&l뽑기 시작"));
    java.util.List<String> sl = new java.util.ArrayList<>();
    sl.add(com.minkang.ultimateroulette.util.Text.color("&7전용아이템 1개를 소비하고 스핀합니다."));
    sm.setLore(sl);
    startBtn.setItemMeta(sm);
    inv.setItem(49, startBtn);

    p.openInventory(inv);
}
}
