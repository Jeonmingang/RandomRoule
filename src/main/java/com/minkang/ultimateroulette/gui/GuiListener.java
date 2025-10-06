package com.minkang.ultimateroulette.gui;

import com.minkang.ultimateroulette.UltimateRoulette;
import com.minkang.ultimateroulette.data.KeyDef;
import com.minkang.ultimateroulette.data.Reward;
import com.minkang.ultimateroulette.gui.ClaimGUI;
import com.minkang.ultimateroulette.util.Text;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GuiListener implements Listener {

    private final UltimateRoulette plugin;
    public GuiListener(UltimateRoulette plugin) { this.plugin = plugin; }

    // ---------- Title helpers ----------
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
    private int currentPageFromTitle(String title) {
        try {
            String plain = ChatColor.stripColor(title);
            int pIdx = plain.lastIndexOf("(p");
            if (pIdx >= 0) {
                int end = plain.indexOf(')', pIdx);
                String num = plain.substring(pIdx + 2, end);
                return Math.max(0, Integer.parseInt(num) - 1);
            }
        } catch (Exception ignored) {}
        return 0;
    }
    private String keyFromTitle(String title) {
        try {
            String plain = ChatColor.stripColor(title);
            int s = plain.indexOf("설정: ") + 4;
            int e = plain.indexOf(" (p");
            if (s >= 0 && e > s) return plain.substring(s, e).trim();
        } catch (Exception ignored) {}
        return null;
    }

    // ---------- Edit GUI: click ----------
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();
        if (!isEdit(title)) return;

        e.setCancelled(true); // 편집 GUI는 실제 이동 금지, 설정만 반영

        String key = keyFromTitle(title);
        KeyDef def = plugin.keys().get(key);
        if (def == null) return;

        int page = currentPageFromTitle(title);
        Inventory top = e.getView().getTopInventory();
        Inventory bottom = e.getView().getBottomInventory();
        int raw = e.getRawSlot();
        boolean fromBottom = (e.getClickedInventory() != null && e.getClickedInventory().equals(bottom));

        // 1) 하단 → 추가 (클릭/쉬프트클릭)
        if (fromBottom) {
            ItemStack src = e.getCurrentItem();
            if (src != null && src.getType() != Material.AIR) {
                def.getRewards().add(new Reward(src.clone(), 1));
                plugin.keys().save();
                new EditGUI(plugin, def, page).open(p);
            }
            return;
        }

        // 2) 페이징
        if (raw == EditGUI.PREV_SLOT) { if (page > 0) new EditGUI(plugin, def, page - 1).open(p); return; }
        if (raw == EditGUI.NEXT_SLOT) { boolean hasNext = def.getRewards().size() > (page + 1) * EditGUI.SLOTS_PER_PAGE; if (hasNext) new EditGUI(plugin, def, page + 1).open(p); return; }
        if (raw == EditGUI.PAGE_SLOT) { return; }

        // 3) 상단 0..44: 가중치 조정/삭제 또는 커서 아이템 추가
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

    // ---------- Edit GUI: drag ----------
    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();
        if (!isEdit(title)) return;

        e.setCancelled(true); // 실제 이동 금지

        String key = keyFromTitle(title);
        KeyDef def = plugin.keys().get(key);
        if (def == null) return;

        ItemStack cursor = e.getOldCursor();
        if (cursor == null || cursor.getType() == Material.AIR) return;

        int page = currentPageFromTitle(title);
        def.getRewards().add(new Reward(cursor.clone(), 1));
        plugin.keys().save();
        new EditGUI(plugin, def, page).open(p);
    }

    // ---------- Spin GUI: 모든 조작 차단 ----------
    

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
        int eIdx = plain.indexOf(" (p");
        if (s >= 0) {
            if (eIdx > s) key = plain.substring(s, eIdx).trim();
            else key = plain.substring(s).trim();
        }
    } catch (Exception ignored) {}
    KeyDef def = (key != null ? plugin.keys().get(key) : null);
    if (def == null) {
        p.sendMessage(Text.color("&c키 정보를 찾을 수 없습니다."));
        return;
    }
    int page = currentPageFromTitle(title);

    // Prev / Next
    if (e.getRawSlot() == 45) { // prev
        if (page > 0) {
            new PreviewGUI(def).open(p, page - 1);
        }
        return;
    }
    if (e.getRawSlot() == 46) { // next
        int total = Math.max(1, (int)Math.ceil(def.getRewards().size() / 45.0));
        if (page + 1 < total) {
            new PreviewGUI(def).open(p, page + 1);
        }
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
public void onClaimClick(InventoryClickEvent e) {
    if (!(e.getWhoClicked() instanceof org.bukkit.entity.Player)) return;
    org.bukkit.entity.Player p = (org.bukkit.entity.Player)e.getWhoClicked();
    if (!ClaimGUI.isClaimTitle(e.getView().getTitle())) return;
    e.setCancelled(true);
    ClaimGUI.handleClick(plugin, p, e);
}

@EventHandler
public void onClaimDrag(InventoryDragEvent e) {
    if (!ClaimGUI.isClaimTitle(e.getView().getTitle())) return;
    e.setCancelled(true);
}
@EventHandler
    public void onSpinClick(org.bukkit.event.inventory.InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (isSpin(title)) e.setCancelled(true);
    }
    @EventHandler
    public void onSpinDrag(org.bukkit.event.inventory.InventoryDragEvent e) {
        String title = e.getView().getTitle();
        if (isSpin(title)) e.setCancelled(true);
    }


private boolean consumeKey(Player p, KeyDef def) {
    // Try main hand first
    ItemStack hand = p.getInventory().getItemInMainHand();
    if (isKeyItem(hand, def)) {
        int amt = hand.getAmount();
        if (amt <= 1) p.getInventory().setItemInMainHand(null);
        else hand.setAmount(amt - 1);
        return true;
    }
    // Search inventory
    for (int i=0;i<p.getInventory().getSize();i++) {
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
    org.bukkit.persistence.PersistentDataContainer pdc = it.getItemMeta().getPersistentDataContainer();
    String tag = pdc.getOrDefault(plugin.keyTag(), org.bukkit.persistence.PersistentDataType.STRING, null);
    return tag != null && tag.equals(def.getName());
}
}