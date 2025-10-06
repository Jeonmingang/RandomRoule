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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PreviewGUI {
    private final KeyDef def;

    public PreviewGUI(KeyDef def) {

/** Backward-compat: older callers passed (plugin, key). We ignore plugin and delegate. */
public PreviewGUI(UltimateRoulette plugin, KeyDef key) {
    this(key);
}
 this.def = def; }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, Text.color("&d&l미리보기: " + def.getName()));
        List<Reward> rewards = def.getRewards();
        int total = Math.max(1, rewards.stream().mapToInt(Reward::getWeight).sum());
        int idx = 0;
        for (Reward r : rewards) {
            if (idx >= 45) break;
            ItemStack it = (r.getItem() != null ? r.getItem().clone() : new ItemStack(Material.CHEST));
            ItemMeta m = it.getItemMeta();
            List<String> lore = m.hasLore() ? new ArrayList<>(m.getLore()) : new ArrayList<>();
            double pct = r.getWeight() * 100.0 / total;
            lore.add(Text.color("&7확률: &b" + String.format(Locale.US, "%.2f", pct) + "%"));
            m.setLore(lore);
            it.setItemMeta(m);
            Text.sanitize(it); inv.setItem(idx++, it);
        }
        // start button at 49
        ItemStack startBtn = new ItemStack(Material.LIME_WOOL);
        ItemMeta sm = startBtn.getItemMeta();
        sm.setDisplayName(Text.color("&a&l뽑기 시작"));
        startBtn.setItemMeta(sm);
        inv.setItem(49, startBtn);

        p.openInventory(inv);
    }
}
