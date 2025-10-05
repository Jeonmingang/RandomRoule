package com.minkang.ultimateroulette.pkg.gui;

import com.minkang.ultimateroulette.UltimateRoulette;
import com.minkang.ultimateroulette.pkg.PackageDef;
import com.minkang.ultimateroulette.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PackageGUI {
    private final UltimateRoulette plugin;
    private final PackageDef def;

    public PackageGUI(UltimateRoulette plugin, PackageDef def) {
        this.plugin = plugin;
        this.def = def;
    }

    public void open(Player p) {
        String title = "&d&l패키지: " + def.getName();
        Inventory inv = Bukkit.createInventory(null, 54, Text.color(title));

        int i=0;
        for (ItemStack it : def.getItems()) {
            if (i>=45) break;
            Text.sanitize(it); inv.setItem(i++, it);
        }
        // 수령 버튼
        ItemStack take = new ItemStack(Material.LIME_WOOL);
        ItemMeta tm = take.getItemMeta();
        tm.setDisplayName(Text.color("&a&l받기"));
        java.util.List<String> l = new java.util.ArrayList<>();
        l.add(Text.color("&7전용 패키지 아이템 1개를 소비합니다."));
        tm.setLore(l);
        take.setItemMeta(tm);
        inv.setItem(49, take);

        p.openInventory(inv);
    }
}
