package com.minkang.ultimateroulette.gui;

import com.minkang.ultimateroulette.UltimateRoulette;
import com.minkang.ultimateroulette.data.KeyDef;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class PreviewGUI {

    private final KeyDef key;

    public PreviewGUI(KeyDef key) {
        this.key = key;
    }

    /** Backward-compat: callers may pass (plugin, key). Plugin is not needed here. */
    public PreviewGUI(UltimateRoulette plugin, KeyDef key) {
        this(key);
    }

    /** Open preview GUI for player (simple placeholder implementation). */
    public void open(Player player) {
        Inventory inv = buildInventory();
        player.openInventory(inv);
    }

    private Inventory buildInventory() {
        // 27-slot simple inventory; you can replace with your existing layout logic
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.GOLD + "Preview");
        // If KeyDef contains preview items, put them here (placeholder)
        ItemStack reward = key != null ? key.getPreviewItem() : null;
        if (reward != null) inv.setItem(13, reward);
        return inv;
    }
}
