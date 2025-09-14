
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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SettingsGUI implements Listener {

    private final Main plugin;

    public SettingsGUI(Main plugin) { this.plugin = plugin; }

    public static void open(Main plugin, Player p, Roulette r) {
        String title = plugin.getConfig().getString("titles.settings", "룰렛 설정: %key% (닫으면 저장)").replace("%key%", r.getKey());
        Inventory inv = Bukkit.createInventory(p, 27, ChatColor.translateAlternateColorCodes('&', title));

        for (int i = 0; i < r.getEntries().size() && i < 27; i++) {
            RouletteEntry e = r.getEntries().get(i);
            ItemStack it = e.getItem().clone();
            ItemMeta meta = it.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<String>();
                if (lore == null) lore = new ArrayList<String>();
                lore.add("§7가중치: §e" + e.getWeight());
                meta.setLore(lore);
                it.setItemMeta(meta);
            }
            inv.setItem(i, it);
        }
        p.openInventory(inv);
    }

    private int parseWeight(ItemStack it) {
        if (it == null || !it.hasItemMeta() || !it.getItemMeta().hasLore()) return 1;
        for (String line : it.getItemMeta().getLore()) {
            String s = ChatColor.stripColor(line);
            if (s.startsWith("가중치:")) {
                String num = s.replace("가중치:", "").trim();
                try { return Math.max(1, Integer.parseInt(num)); } catch (Exception ignored) {}
            }
        }
        return 1;
    }

    private void setWeightLore(ItemStack it, int weight) {
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return;
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<String>();
        if (lore == null) lore = new ArrayList<String>();
        boolean replaced = false;
        for (int i = 0; i < lore.size(); i++) {
            String plain = ChatColor.stripColor(lore.get(i));
            if (plain.startsWith("가중치:")) {
                lore.set(i, "§7가중치: §e" + weight);
                replaced = true;
                break;
            }
        }
        if (!replaced) lore.add("§7가중치: §e" + weight);
        meta.setLore(lore);
        it.setItemMeta(meta);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        HumanEntity he = e.getWhoClicked();
        if (!(he instanceof Player)) return;
        String title = e.getView().getTitle();
        if (title == null) return;
        if (!ChatColor.stripColor(title).startsWith("룰렛 설정:")) return;

        if (e.getRawSlot() < e.getView().getTopInventory().getSize()) {
            ItemStack current = e.getCurrentItem();
            if (current != null && current.getType() != Material.AIR) {
                int delta = 0;
                if (e.getClick() == ClickType.LEFT) delta = 1;
                else if (e.getClick() == ClickType.RIGHT) delta = -1;
                else if (e.getClick() == ClickType.SHIFT_LEFT) delta = 10;
                else if (e.getClick() == ClickType.SHIFT_RIGHT) delta = -10;

                if (delta != 0) {
                    int w = parseWeight(current);
                    w = Math.max(1, w + delta);
                    setWeightLore(current, w);
                    ((Player) he).playSound(he.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, delta > 0 ? 1.4f : 0.8f);
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        String title = e.getView().getTitle();
        if (title == null) return;
        String plain = ChatColor.stripColor(title);
        if (!plain.startsWith("룰렛 설정:")) return;
        String key = plain.replace("룰렛 설정:", "").trim();
        int idx = key.indexOf(" ");
        if (idx != -1) key = key.substring(0, idx).trim();
        Roulette r = plugin.getManager().get(key);
        if (r == null) return;

        Inventory inv = e.getInventory();
        List<RouletteEntry> list = new ArrayList<RouletteEntry>();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType() == Material.AIR) continue;
            int w = parseWeight(it);
            list.add(new RouletteEntry(it.clone(), w));
        }
        r.setEntries(list);
        plugin.getManager().save();

        if (e.getPlayer() instanceof Player) {
            Player p = (Player) e.getPlayer();
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            p.sendMessage(plugin.msg("saved_settings").replace("%count%", String.valueOf(list.size())));
        }
    }
}
