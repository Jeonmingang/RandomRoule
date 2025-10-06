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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClaimGUI {
    private final UltimateRoulette plugin;
    private final UUID owner;
    public static final int SLOTS_PER_PAGE = 45; // 0..44
    public static final int PREV_SLOT = 45;
    public static final int ALL_SLOT  = 49;
    public static final int NEXT_SLOT = 53;

    public ClaimGUI(UltimateRoulette plugin, UUID owner) {
        this.plugin = plugin;
        this.owner = owner;
    }

    public static boolean isClaimTitle(String title) {
        if (title == null) return false;
        String t = org.bukkit.ChatColor.stripColor(title);
        return t != null && t.startsWith("보관함");
    }

    private String title(int page, int maxPage) {
        return Text.color("&b보관함 &7(클레임) &8(p" + (page+1) + "/" + Math.max(1,maxPage) + ")");
    }

    public void open(Player p) { openPage(p, 0); }

    public void openPage(Player p, int page) {
        List<ItemStack> items = new ArrayList<>(plugin.storage().getClaimList(owner));
        int maxPage = (int)Math.ceil(items.size() / (double)SLOTS_PER_PAGE);
        if (maxPage <= 0) maxPage = 1;
        if (page < 0) page = 0;
        if (page >= maxPage) page = maxPage - 1;

        Inventory inv = Bukkit.createInventory(p, 54, title(page, maxPage));

        // Fill page items
        int start = page * SLOTS_PER_PAGE;
        for (int i=0;i<SLOTS_PER_PAGE;i++) {
            int idx = start + i;
            if (idx >= items.size()) break;
            ItemStack _tmp = items.get(idx).clone(); Text.sanitize(_tmp); inv.setItem(i, _tmp);
        }

        // Nav & All buttons
        ItemStack prev = new ItemStack(Material.ARROW);
        ItemMeta pm = prev.getItemMeta();
        if (pm != null) { pm.setDisplayName(Text.color("&7◀ 이전 페이지")); prev.setItemMeta(pm); }
        ItemStack all = new ItemStack(Material.CHEST);
        ItemMeta am = all.getItemMeta();
        if (am != null) { am.setDisplayName(Text.color("&a모두 받기")); all.setItemMeta(am); }
        ItemStack next = new ItemStack(Material.ARROW);
        ItemMeta nm = next.getItemMeta();
        if (nm != null) { nm.setDisplayName(Text.color("&7다음 페이지 ▶")); next.setItemMeta(nm); }

        inv.setItem(PREV_SLOT, prev);
        inv.setItem(ALL_SLOT, all);
        inv.setItem(NEXT_SLOT, next);

        p.openInventory(inv);
    }

    public static void handleClick(UltimateRoulette plugin, Player p, InventoryClickEvent e) {
        if (!isClaimTitle(e.getView().getTitle())) return;
        e.setCancelled(true); // 보관함에서 꺼내기 금지

        String title = org.bukkit.ChatColor.stripColor(e.getView().getTitle());
        int page = 0;
        int pIdx = title.lastIndexOf("(p");
        if (pIdx >= 0) {
            try { 
                int slash = title.indexOf('/', pIdx);
                int end = title.indexOf(')', pIdx);
                page = Integer.parseInt(title.substring(pIdx+2, slash)) - 1;
            } catch (Exception ignored) {}
        }

        if (e.getRawSlot() == ALL_SLOT) {
            List<ItemStack> items = new ArrayList<>(plugin.storage().getClaimList(p.getUniqueId()));
            for (ItemStack it : items) {
                java.util.HashMap<Integer, ItemStack> left = p.getInventory().addItem(it);
                left.values().forEach(rem -> p.getWorld().dropItemNaturally(p.getLocation(), rem));
            }
            plugin.storage().setClaimList(p.getUniqueId(), new ArrayList<>());
            p.sendMessage(Text.color("&a보관함의 아이템을 모두 수령했습니다."));
            p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
            new ClaimGUI(plugin, p.getUniqueId()).openPage(p, page); // refresh (will be empty)
            return;
        }

        // Single item claim if clicked on top grid
        if (e.getRawSlot() >= 0 && e.getRawSlot() < SLOTS_PER_PAGE) {
            int absolute = page * SLOTS_PER_PAGE + e.getRawSlot();
            List<ItemStack> items = new ArrayList<>(plugin.storage().getClaimList(p.getUniqueId()));
            if (absolute < items.size()) {
                ItemStack it = items.remove(absolute);
                java.util.HashMap<Integer, ItemStack> left = p.getInventory().addItem(it);
                left.values().forEach(rem -> p.getWorld().dropItemNaturally(p.getLocation(), rem));
                plugin.storage().setClaimList(p.getUniqueId(), items);
                p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
                new ClaimGUI(plugin, p.getUniqueId()).openPage(p, page);
            }
        }

        // Prev/Next
        if (e.getRawSlot() == PREV_SLOT) {
            new ClaimGUI(plugin, p.getUniqueId()).openPage(p, Math.max(0, page-1));
        } else if (e.getRawSlot() == NEXT_SLOT) {
            List<ItemStack> items = new ArrayList<>(plugin.storage().getClaimList(p.getUniqueId()));
            int maxPage = (int)Math.ceil(items.size() / (double)SLOTS_PER_PAGE);
            if (maxPage <= 0) maxPage = 1;
            if (page + 1 < maxPage) new ClaimGUI(plugin, p.getUniqueId()).openPage(p, page+1);
        }
    }
}
