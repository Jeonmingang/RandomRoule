package com.minkang.ultimateroulette.gui;

import com.minkang.ultimateroulette.UltimateRoulette;
import com.minkang.ultimateroulette.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

public class ClaimGUI {
    private static UltimateRoulette plugin;
    private static UUID owner;
    private static int page = 0;

    public ClaimGUI(UltimateRoulette plugin, UUID owner) {
        ClaimGUI.plugin = plugin;
        ClaimGUI.owner = owner;
    }

    public void open(Player p) { page = 0; openPage(p, 0);} 
    private void openPage(Player p, int pg) {
        String title = plugin.getConfig().getString("titles.claim", "&b&l보관함 (클레임)");
        String ptag = pg>0?(" &7(p" + (pg+1) + ")"):"";
        Inventory inv = Bukkit.createInventory(null, 54, Text.color(title + ptag));
        List<ItemStack> items = plugin.storage().getClaimList(owner);
        int idx = 0;
        int start = pg*45;
        for (int i=start; i<items.size() && idx<45; i++) {
            ItemStack it = items.get(i);
            inv.setItem(idx++, it);
            if (idx >= 45) break;
        }
        org.bukkit.inventory.ItemStack prev = new org.bukkit.inventory.ItemStack(Material.ARROW);
        org.bukkit.inventory.meta.ItemMeta pm = prev.getItemMeta(); pm.setDisplayName(Text.color("&7이전 페이지")); prev.setItemMeta(pm);
        org.bukkit.inventory.ItemStack next = new org.bukkit.inventory.ItemStack(Material.ARROW);
        org.bukkit.inventory.meta.ItemMeta nm = next.getItemMeta(); nm.setDisplayName(Text.color("&7다음 페이지")); next.setItemMeta(nm);
        inv.setItem(45, prev); inv.setItem(53, next);
        // take all button
        ItemStack all = new ItemStack(Material.CHEST);
        ItemMeta am = all.getItemMeta();
        am.setDisplayName(Text.color("&a&l모두 받기"));
        all.setItemMeta(am);
        inv.setItem(49, all);
        p.openInventory(inv);
    }

    public static void handleClick(Player p, InventoryClickEvent e) {
        if (!e.getView().getTitle().contains("보관함")) return;
        e.setCancelled(true);
        int slot = e.getSlot();
        if (slot >=0 && slot < 45 && e.getCurrentItem()!=null && e.getCurrentItem().getType()!=Material.AIR) {
            java.util.List<org.bukkit.inventory.ItemStack> items = plugin.storage().getClaimList(owner);
            int current = 0; String t = e.getView().getTitle(); if (t.contains("(p")) { try { current = Math.max(0, Integer.parseInt(t.substring(t.indexOf("(p")+2, t.indexOf(")"))) -1);} catch (Exception ignore) {} }
            int absoluteIndex = current*45 + slot;
            if (absoluteIndex < items.size()) {
                org.bukkit.inventory.ItemStack it = items.remove(absoluteIndex);
                java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> left = p.getInventory().addItem(it);
                left.values().forEach(rem -> p.getWorld().dropItemNaturally(p.getLocation(), rem));
                plugin.storage().setClaimList(owner, items);
                try { org.bukkit.Sound s = org.bukkit.Sound.valueOf(plugin.getConfig().getString("sounds.claim","ENTITY_ITEM_PICKUP")); p.playSound(p.getLocation(), s, 1f, 1f);} catch (Exception ex) { p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);} 
                new ClaimGUI(plugin, owner).openPage(p, current);
            }
            return;
        }
        if (e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.ARROW && e.getCurrentItem().hasItemMeta()) {
            boolean next = e.getCurrentItem().getItemMeta().getDisplayName().contains("다음");
            int current = 0;
            String t = e.getView().getTitle();
            if (t.contains("(p")) { try { current = Math.max(0, Integer.parseInt(t.substring(t.indexOf("(p")+2, t.indexOf(")"))) -1);} catch (Exception ignore) {} }
            int newPage = next ? current+1 : Math.max(0, current-1);
            new ClaimGUI(plugin, owner).openPage(p, newPage);
            return;
        }
        if (slot == 49) {
            // give as many as fits
            List<ItemStack> items = plugin.storage().getClaimList(owner);
            for (ItemStack it : items) {
                HashMap<Integer, ItemStack> left = p.getInventory().addItem(it);
                // drop leftovers at player location
                left.values().forEach(rem -> p.getWorld().dropItemNaturally(p.getLocation(), rem));
            }
            plugin.storage().setClaimList(owner, new java.util.ArrayList<>());
            p.sendMessage(Text.color("&a보관함의 아이템을 모두 수령했습니다."));
            p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
            p.closeInventory();
        }
    }
}
