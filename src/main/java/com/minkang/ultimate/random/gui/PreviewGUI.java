
package com.minkang.ultimate.random.gui;

import com.minkang.ultimate.random.Main;
import com.minkang.ultimate.random.Roulette;
import com.minkang.ultimate.random.RouletteEntry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class PreviewGUI implements Listener {

    private final Main plugin;

    public PreviewGUI(Main plugin) { this.plugin = plugin; }

    public static void open(Main plugin, Player p, Roulette r) {
        String title = plugin.getConfig().getString("titles.preview", "룰렛 미리보기: %key%").replace("%key%", r.getKey());
        Inventory inv = Bukkit.createInventory(p, 54, ChatColor.translateAlternateColorCodes('&', title));
        int total = r.getTotalWeight();
        DecimalFormat df = new DecimalFormat("#.##");

        int slot = 0;
        for (RouletteEntry e : r.getEntries()) {
            if (slot >= 45) break;
            ItemStack it = e.getItem().clone();
            ItemMeta meta = it.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<String>();
                if (lore == null) lore = new ArrayList<String>();
                double chance = 100.0 * e.getWeight() / Math.max(1, total);
                lore.add("§7확률: §e" + df.format(chance) + "% §7(가중치 " + e.getWeight() + ")");
                meta.setLore(lore);
                it.setItemMeta(meta);
            }
            inv.setItem(slot++, it);
        }

        ItemStack draw = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta dm = draw.getItemMeta();
        if (dm != null) {
            dm.setDisplayName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("gui.preview_draw_button_name", "&a[ 뽑기 시작 ]")));
            List<String> lore = new ArrayList<String>();
            for (String s : plugin.getConfig().getStringList("gui.preview_draw_button_lore")) {
                lore.add(ChatColor.translateAlternateColorCodes('&', s));
            }
            dm.setLore(lore);
            draw.setItemMeta(dm);
        }
        inv.setItem(49, draw);

        p.openInventory(inv);
        p.sendMessage(plugin.msg("open_preview").replace("%key%", r.getKey()));
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        HumanEntity he = e.getWhoClicked();
        if (!(he instanceof Player)) return;
        String title = e.getView().getTitle();
        if (title == null) return;
        if (!ChatColor.stripColor(title).startsWith("룰렛 미리보기:")) return;

        e.setCancelled(true);
        if (e.getRawSlot() == 49) {
            String plain = ChatColor.stripColor(title);
            String key = plain.replace("룰렛 미리보기:", "").trim();
            int idx = key.indexOf(" ");
            if (idx != -1) key = key.substring(0, idx).trim();
            Roulette r = plugin.getManager().get(key);
            if (r == null) { ((Player) he).sendMessage(plugin.msg("not_found").replace("%key%", key)); he.closeInventory(); return; }
            if (r.isEmpty()) { ((Player) he).sendMessage(plugin.msg("no_items")); he.closeInventory(); return; }
            he.closeInventory();
            ((Player) he).playSound(he.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
            SpinnerGUI.start(plugin, (Player) he, r);
        }
    }
}
