package com.minkang.ultimateroulette.gui;

import com.minkang.ultimateroulette.UltimateRoulette;
import com.minkang.ultimateroulette.data.KeyDef;
import com.minkang.ultimateroulette.data.Reward;
import com.minkang.ultimateroulette.util.Text;
import com.minkang.ultimateroulette.pkg.PackageDef;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Unified GUI listener:
 * - Edit GUI (키 설정)
 * - Preview GUI (미리보기)
 * - Spin GUI (스핀 중 조작/닫힘 방지)
 * - Claim GUI (보관함)
 * - Package GUI (패키지 미리보기/수령)
 * - Package Edit GUI 닫힘 저장
 */
public class GuiListener implements Listener {

    private final UltimateRoulette plugin;
    public GuiListener(UltimateRoulette plugin) { this.plugin = plugin; }

    // ----- title helpers -----
    private boolean isEdit(String title) {
        if (title == null) return false;
        String plain = ChatColor.stripColor(title);
        return plain != null && plain.startsWith("설정:");
    }
    private boolean isPreview(String title) {
        if (title == null) return false;
        String plain = ChatColor.stripColor(title);
        return plain != null && plain.startsWith("미리보기:");
    }
    private boolean isSpin(String title) {
        if (title == null) return false;
        String plain = ChatColor.stripColor(title);
        return plain != null && plain.startsWith("룰렛 스핀:");
    }
    private boolean isPackagePreview(String title) {
        if (title == null) return false;
        String plain = ChatColor.stripColor(title);
        return plain != null && plain.startsWith("패키지:") && !plain.contains("설정");
    }
    private boolean isPackageEdit(String title) {
        if (title == null) return false;
        String plain = ChatColor.stripColor(title);
        return plain != null && plain.startsWith("패키지 설정:");
    }

    private int currentPageFromTitle(String title) {
        try {
            String plain = ChatColor.stripColor(title);
            int pIdx = plain.lastIndexOf("(p");
            if (pIdx >= 0) {
                int end = plain.indexOf(')', pIdx);
                String num = plain.substring(pIdx + 2, end);
                // In claim it's pX/Y; handle like "p2/5" -> take before slash
                int slash = num.indexOf('/');
                if (slash >= 0) num = num.substring(0, slash);
                return Math.max(0, Integer.parseInt(num) - 1);
            }
        } catch (Exception ignored) {}
        return 0;
    }
    private String keyFromTitle(String title) {
        try {
            String plain = ChatColor.stripColor(title);
            int s = plain.indexOf("설정:") + 3;
            // allow optional space after ':'
            if (s > 0 && s < plain.length() && plain.charAt(s) == ' ') s++;
            int e = plain.indexOf(" (p");
            if (e < 0) e = plain.length();
            if (s >= 0 && e > s) return plain.substring(s, e).trim();
        } catch (Exception ignored) {}
        return null;
    }
    private String packageNameFromTitle(String title) {
        try {
            String plain = ChatColor.stripColor(title);
            int s = plain.indexOf("패키지:") + 4;
            if (s > 0 && s < plain.length() && plain.charAt(s) == ' ') s++;
            int e = plain.indexOf(" (");
            if (e < 0) e = plain.length();
            if (s >= 0 && e > s) return plain.substring(s, e).trim();
        } catch (Exception ignored) {}
        return null;
    }

    // ----- Edit GUI: click -----
    @EventHandler
    public void onEditClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();
        if (!isEdit(title)) return;

        e.setCancelled(true);

        String key = keyFromTitle(title);
        if (key == null) return;

        KeyDef def = plugin.keys().get(key);
        if (def == null) return;

        int raw = e.getRawSlot();
        int page = currentPageFromTitle(title);

        if (raw == EditGUI.PREV_SLOT) {
            if (page > 0) new EditGUI(plugin, def, page - 1).open(p);
            return;
        }
        if (raw == EditGUI.NEXT_SLOT) {
            int totalPages = Math.max(1, (int) Math.ceil(def.getRewards().size() / (double) EditGUI.SLOTS_PER_PAGE));
            if (page + 1 < totalPages) new EditGUI(plugin, def, page + 1).open(p);
            return;
        }
        if (raw == EditGUI.PAGE_SLOT) {
            return;
        }

        // reward slot
        if (raw >= 0 && raw < EditGUI.SLOTS_PER_PAGE) {
            int idx = page * EditGUI.SLOTS_PER_PAGE + raw;
            if (idx < def.getRewards().size()) {
                Reward r = def.getRewards().get(idx);
                ClickType c = e.getClick();
                if (c == ClickType.LEFT) r.setWeight(r.getWeight() + 1);
                else if (c == ClickType.SHIFT_LEFT) r.setWeight(r.getWeight() + 10);
                else if (c == ClickType.RIGHT) r.setWeight(Math.max(1, r.getWeight() - 1));
                else if (c == ClickType.SHIFT_RIGHT) r.setWeight(Math.max(1, r.getWeight() - 10));
                else if (c == ClickType.DROP) def.getRewards().remove(r);
                plugin.keys().save();
                new EditGUI(plugin, def, page).open(p);
                return;
            }
            ItemStack cursor = e.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                def.getRewards().add(new Reward(cursor.clone(), 1));
                plugin.keys().save();
                new EditGUI(plugin, def, page).open(p);
                return;
            }
        }
    }

    // ----- Edit GUI: drag -----
    @EventHandler
    public void onEditDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        String title = e.getView().getTitle();
        if (!isEdit(title)) return;
        e.setCancelled(true);
    }

    // ----- Preview GUI -----
    @EventHandler
    public void onPreviewClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player)e.getWhoClicked();
        String title = e.getView().getTitle();
        if (!isPreview(title)) return;
        e.setCancelled(true);

        // Resolve KeyDef and current page from title
        String plain = ChatColor.stripColor(title);
        String key = null;
        try {
            int s = plain.indexOf("미리보기:") + 5;
            if (s > 0 && s < plain.length() && plain.charAt(s) == ' ') s++;
            int eIdx = plain.indexOf(" (p");
            if (eIdx < 0) eIdx = plain.length();
            key = plain.substring(s, eIdx).trim();
        } catch (Exception ignored) {}
        if (key == null || key.isEmpty()) {
            p.sendMessage(Text.color("&c키 정보를 찾을 수 없습니다."));
            return;
        }
        KeyDef def = plugin.keys().get(key);
        if (def == null) {
            p.sendMessage(Text.color("&c키 정보를 찾을 수 없습니다."));
            return;
        }
        int page = currentPageFromTitle(title);

        // Prev / Next
        if (e.getRawSlot() == 45) { // prev
            if (page > 0) new PreviewGUI(def).open(p, page - 1);
            return;
        }
        if (e.getRawSlot() == 53) { // next
            int total = Math.max(1, (int)Math.ceil(def.getRewards().size() / 45.0));
            if (page + 1 < total) new PreviewGUI(def).open(p, page + 1);
            return;
        }

        // Start button (slot 49)
        if (e.getRawSlot() == 49) {
            if (!consumeKey(p, def)) {
                p.sendMessage(Text.color("&c전용 아이템이 부족합니다."));
                return;
            }
            new SpinGUI(plugin, def).open(p);
        }
    }

    @EventHandler
    public void onPreviewDrag(InventoryDragEvent e) {
        String title = e.getView().getTitle();
        if (isPreview(title)) e.setCancelled(true);
    }

    // ----- Spin GUI -----
    @EventHandler public void onSpinClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (isSpin(title)) e.setCancelled(true);
    }
    @EventHandler public void onSpinDrag(InventoryDragEvent e) {
        String title = e.getView().getTitle();
        if (isSpin(title)) e.setCancelled(true);
    }
    @EventHandler public void onSpinClose(InventoryCloseEvent e) {
        String title = e.getView().getTitle();
        if (!isSpin(title)) return;
        Inventory inv = e.getInventory();
        if (inv == null) return;
        org.bukkit.inventory.InventoryHolder holder = inv.getHolder();
        if (holder instanceof SpinGUIHolder) {
            SpinGUIHolder h = (SpinGUIHolder) holder;
            if (h.isSpinning() && e.getPlayer() instanceof Player) {
                Player p = (Player) e.getPlayer();
                Bukkit.getScheduler().runTask(plugin, () -> p.openInventory(inv));
            }
        } else {
            // Fallback: if no holder, just reopen once to avoid ESC skip during animation
            if (e.getPlayer() instanceof Player) {
                Player p = (Player) e.getPlayer();
                Bukkit.getScheduler().runTask(plugin, () -> p.openInventory(inv));
            }
        }
    }

    // ----- Claim GUI -----
    @EventHandler
    public void onClaimClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ClaimGUI.handleClick(plugin, p, e);
    }
    @EventHandler
    public void onClaimDrag(InventoryDragEvent e) {
        if (ClaimGUI.isClaimTitle(e.getView().getTitle())) e.setCancelled(true);
    }

    // ----- Package GUI (Preview/Take) -----
    @EventHandler
    public void onPackageClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();
        if (!isPackagePreview(title)) return;

        e.setCancelled(true); // no taking items out
        int raw = e.getRawSlot();
        if (raw != 49) return; // only via '받기' button

        String name = packageNameFromTitle(title);
        if (name == null || name.isEmpty()) return;
        PackageDef def = plugin.packages().get(name);
        if (def == null) return;

        if (!consumePackageKey(p, def)) {
            p.sendMessage(Text.color("&c전용 패키지 아이템이 부족합니다."));
            return;
        }

        // Give items: inventory first, leftover -> 보관함
        for (ItemStack it : def.getItems()) {
            if (it == null || it.getType() == Material.AIR) continue;
            ItemStack give = it.clone();
            java.util.HashMap<Integer, ItemStack> left = p.getInventory().addItem(give);
            for (ItemStack rem : left.values()) {
                plugin.storage().addClaim(p.getUniqueId(), rem);
            }
        }
        p.sendMessage(Text.color("&a패키지 보상이 지급되었습니다. &7(/랜덤 보관함 으로 확인)"));
        p.closeInventory();
    }
    @EventHandler
    public void onPackageDrag(InventoryDragEvent e) {
        String title = e.getView().getTitle();
        if (isPackagePreview(title)) e.setCancelled(true);
    }

    // Package Edit GUI: ensure save on ESC close
    @EventHandler
    public void onPackageEditClose(InventoryCloseEvent e) {
        String title = e.getView().getTitle();
        if (isPackageEdit(title)) {
            try { plugin.packages().save(); } catch (Throwable ignored) {}
        }
    }

    // ----- helpers -----
    private boolean consumeKey(Player p, KeyDef def) {
        // main hand
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (isKeyItem(hand, def)) {
            int amt = hand.getAmount();
            if (amt <= 1) p.getInventory().setItemInMainHand(null);
            else hand.setAmount(amt - 1);
            return true;
        }
        // inventory scan
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack it = p.getInventory().getItem(i);
            if (isKeyItem(it, def)) {
                int amt = it.getAmount();
                if (amt <= 1) p.getInventory().setItem(i, null);
                else it.setAmount(amt - 1);
                return true;
            }
        }
        return false;
    }
    private boolean isKeyItem(ItemStack it, KeyDef def) {
        if (it == null || it.getType() == Material.AIR || !it.hasItemMeta()) return false;
        String tag = it.getItemMeta().getPersistentDataContainer()
                .getOrDefault(plugin.keyTag(), PersistentDataType.STRING, null);
        return tag != null && tag.equals(def.getName());
    }

    private boolean consumePackageKey(Player p, PackageDef def) {
        // Try main hand first
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (isPackageKeyItem(hand, def)) {
            int amt = hand.getAmount();
            if (amt <= 1) p.getInventory().setItemInMainHand(null);
            else hand.setAmount(amt - 1);
            return true;
        }
        // Search inventory
        for (int i=0;i<p.getInventory().getSize();i++) {
            ItemStack it = p.getInventory().getItem(i);
            if (isPackageKeyItem(it, def)) {
                int amt = it.getAmount();
                if (amt <= 1) p.getInventory().setItem(i, null);
                else it.setAmount(amt - 1);
                return true;
            }
        }
        return false;
    }
    private boolean isPackageKeyItem(ItemStack it, PackageDef def) {
        if (it == null || it.getType() == Material.AIR || !it.hasItemMeta()) return false;
        String tag = it.getItemMeta().getPersistentDataContainer()
                .getOrDefault(plugin.packageTag(), PersistentDataType.STRING, null);
        return tag != null && tag.equals(def.getName());
    }
}