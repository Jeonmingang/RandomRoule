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
    String title = ChatColor.GOLD + "Preview";
    try {
        // Try to show key name if available via toString()
        if (key != null) {
            String k = String.valueOf(key);
            if (k != null && !k.trim().isEmpty() && !"null".equalsIgnoreCase(k.trim())) {
                title = ChatColor.GOLD + "Preview: " + ChatColor.YELLOW + k;
            }
        }
    } catch (Throwable ignored) {}
    Inventory inv = Bukkit.createInventory(null, 27, title);
    // Minimal placeholder layout; no dependency on KeyDef methods
    return inv;
}

}
