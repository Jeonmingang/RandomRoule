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
import org.bukkit.event.inventory.InventoryCloseEvent;
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
// 패키지 설정 GUI에서 ESC로 닫을 때도 안전 저장
@EventHandler
public void onPackageEditClose(InventoryCloseEvent e) {
    String title = e.getView().getTitle();
    String plain = org.bukkit.ChatColor.stripColor(title == null ? "" : title);
    if (plain.startsWith("패키지 설정:")) {
        try { plugin.packages().save(); } catch (Throwable ignored) {}
    }
}

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
    if (e.getRawSlot() == 53) { // next
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
// 패키지 설정 GUI에서 ESC로 닫을 때도 안전 저장
@EventHandler
public void onPackageEditClose(InventoryCloseEvent e) {
    String title = e.getView().getTitle();
    String plain = org.bukkit.ChatColor.stripColor(title == null ? "" : title);
    if (plain.startsWith("패키지 설정:")) {
        try { plugin.packages().save(); } catch (Throwable ignored) {}
    }
}

}

@EventHandler
public void onClaimClick(InventoryClickEvent e) {
    if (!(e.getWhoClicked() instanceof org.bukkit.entity.Player)) return;
    org.bukkit.entity.Player p = (org.bukkit.entity.Player)e.getWhoClicked();
    if (!ClaimGUI.isClaimTitle(e.getView().getTitle())) return;
    e.setCancelled(true);
    ClaimGUI.handleClick(plugin, p, e);
// 패키지 설정 GUI에서 ESC로 닫을 때도 안전 저장
@EventHandler
public void onPackageEditClose(InventoryCloseEvent e) {
    String title = e.getView().getTitle();
    String plain = org.bukkit.ChatColor.stripColor(title == null ? "" : title);
    if (plain.startsWith("패키지 설정:")) {
        try { plugin.packages().save(); } catch (Throwable ignored) {}
    }
}

}

@EventHandler
public void onClaimDrag(InventoryDragEvent e) {
    if (!ClaimGUI.isClaimTitle(e.getView().getTitle())) return;
    e.setCancelled(true);
// 패키지 설정 GUI에서 ESC로 닫을 때도 안전 저장
@EventHandler
public void onPackageEditClose(InventoryCloseEvent e) {
    String title = e.getView().getTitle();
    String plain = org.bukkit.ChatColor.stripColor(title == null ? "" : title);
    if (plain.startsWith("패키지 설정:")) {
        try { plugin.packages().save(); } catch (Throwable ignored) {}
    }
}

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
// 패키지 설정 GUI에서 ESC로 닫을 때도 안전 저장
@EventHandler
public void onPackageEditClose(InventoryCloseEvent e) {
    String title = e.getView().getTitle();
    String plain = org.bukkit.ChatColor.stripColor(title == null ? "" : title);
    if (plain.startsWith("패키지 설정:")) {
        try { plugin.packages().save(); } catch (Throwable ignored) {}
    }
}

}
private boolean isKeyItem(ItemStack it, KeyDef def) {
    if (it == null || it.getType() == Material.AIR || !it.hasItemMeta()) return false;
    org.bukkit.persistence.PersistentDataContainer pdc = it.getItemMeta().getPersistentDataContainer();
    String tag = pdc.getOrDefault(plugin.keyTag(), org.bukkit.persistence.PersistentDataType.STRING, null);
    return tag != null && tag.equals(def.getName());
// 패키지 설정 GUI에서 ESC로 닫을 때도 안전 저장
@EventHandler
public void onPackageEditClose(InventoryCloseEvent e) {
    String title = e.getView().getTitle();
    String plain = org.bukkit.ChatColor.stripColor(title == null ? "" : title);
    if (plain.startsWith("패키지 설정:")) {
        try { plugin.packages().save(); } catch (Throwable ignored) {}
    }
}

}

@EventHandler
public void onSpinClose(InventoryCloseEvent e) {
    String title = e.getView().getTitle();
    // 스핀 GUI 닫기 방지 (진행중일 때만)
    try {
        org.bukkit.inventory.InventoryHolder holder = e.getInventory().getHolder();
        if (holder instanceof com.minkang.ultimateroulette.gui.SpinGUIHolder) {
            com.minkang.ultimateroulette.gui.SpinGUIHolder h = (com.minkang.ultimateroulette.gui.SpinGUIHolder) holder;
            if (h.isSpinning()) {
                org.bukkit.entity.Player p = (org.bukkit.entity.Player) e.getPlayer();
                // 다음 틱에 다시 열어 줌
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    try { p.openInventory(e.getInventory()); } catch (Throwable ignored) {}
                });
            }
        }
    } catch (Throwable ignored) {}
// 패키지 설정 GUI에서 ESC로 닫을 때도 안전 저장
@EventHandler
public void onPackageEditClose(InventoryCloseEvent e) {
    String title = e.getView().getTitle();
    String plain = org.bukkit.ChatColor.stripColor(title == null ? "" : title);
    if (plain.startsWith("패키지 설정:")) {
        try { plugin.packages().save(); } catch (Throwable ignored) {}
    }
}

}

// -------- 패키지 GUI: 아이템 꺼내기 금지 & '받기' 버튼만 동작 --------
private boolean isPackageView(String title) {
    if (title == null) return false;
    String plain = org.bukkit.ChatColor.stripColor(title);
    return plain != null && plain.startsWith("패키지:");
// 패키지 설정 GUI에서 ESC로 닫을 때도 안전 저장
@EventHandler
public void onPackageEditClose(InventoryCloseEvent e) {
    String title = e.getView().getTitle();
    String plain = org.bukkit.ChatColor.stripColor(title == null ? "" : title);
    if (plain.startsWith("패키지 설정:")) {
        try { plugin.packages().save(); } catch (Throwable ignored) {}
    }
}

}

@EventHandler
public void onPackageClick(org.bukkit.event.inventory.InventoryClickEvent e) {
    if (!(e.getWhoClicked() instanceof org.bukkit.entity.Player)) return;
    org.bukkit.entity.Player p = (org.bukkit.entity.Player) e.getWhoClicked();
    String title = e.getView().getTitle();
    if (!isPackageView(title)) return;

    e.setCancelled(true); // 꺼내기 금지

    // 받기 버튼: 중앙 하단(49)
    if (e.getRawSlot() == 49) {
        // 패키지 이름 파싱
        String plain = org.bukkit.ChatColor.stripColor(title);
        String pkgName;
        try {
            int idx = plain.indexOf("패키지:") + 4;
            pkgName = plain.substring(idx).trim();
        } catch (Exception ex) { return; }

        com.minkang.ultimateroulette.pkg.PackageDef def = plugin.packages().get(pkgName);
        if (def == null) { p.sendMessage(com.minkang.ultimateroulette.util.Text.color("&c패키지 정보를 찾을 수 없습니다.")); return; }

        // 인벤토리에서 전용 패키지 아이템 1개 소비
        boolean consumed = false;
        org.bukkit.inventory.PlayerInventory inv = p.getInventory();
        for (int i=0;i<inv.getSize();i++) {
            org.bukkit.inventory.ItemStack it = inv.getItem(i);
            if (it!=null && it.hasItemMeta()) {
                org.bukkit.persistence.PersistentDataContainer pdc = it.getItemMeta().getPersistentDataContainer();
                String tag = pdc.getOrDefault(plugin.packageTag(), org.bukkit.persistence.PersistentDataType.STRING, null);
                if (tag!=null && tag.equalsIgnoreCase(def.getName())) {
                    int amt = it.getAmount();
                    if (amt <= 1) inv.setItem(i, null);
                    else it.setAmount(amt-1);
                    consumed = true;
                    break;
                }
            }
        }
        if (!consumed) { p.sendMessage(com.minkang.ultimateroulette.util.Text.color("&c전용 패키지 아이템이 필요합니다.")); return; }

        // 지급: 인벤토리 추가, 넘치는 건 보관함으로
        java.util.List<org.bukkit.inventory.ItemStack> items = def.getItems();
        if (items==null || items.isEmpty()) { p.sendMessage(com.minkang.ultimateroulette.util.Text.color("&c이 패키지에 등록된 아이템이 없습니다.")); return; }
        java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> overAll = new java.util.HashMap<>();
        for (org.bukkit.inventory.ItemStack src : items) {
            org.bukkit.inventory.ItemStack give = src.clone();
            java.util.Map<Integer, org.bukkit.inventory.ItemStack> over = inv.addItem(give);
            overAll.putAll(over);
        }
        // 남은 것들은 보관함으로 이동
        for (org.bukkit.inventory.ItemStack rem : overAll.values()) {
            if (rem == null) continue;
            plugin.storage().addClaim(p.getUniqueId(), rem);
        }

        p.closeInventory();
        p.sendMessage(com.minkang.ultimateroulette.util.Text.color("&a패키지 보상이 지급되었습니다. &7(/랜덤 보관함 으로 확인)"));
        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
    }
// 패키지 설정 GUI에서 ESC로 닫을 때도 안전 저장
@EventHandler
public void onPackageEditClose(InventoryCloseEvent e) {
    String title = e.getView().getTitle();
    String plain = org.bukkit.ChatColor.stripColor(title == null ? "" : title);
    if (plain.startsWith("패키지 설정:")) {
        try { plugin.packages().save(); } catch (Throwable ignored) {}
    }
}

}

@EventHandler
public void onPackageDrag(org.bukkit.event.inventory.InventoryDragEvent e) {
    String title = e.getView().getTitle();
    if (isPackageView(title)) e.setCancelled(true);
// 패키지 설정 GUI에서 ESC로 닫을 때도 안전 저장
@EventHandler
public void onPackageEditClose(InventoryCloseEvent e) {
    String title = e.getView().getTitle();
    String plain = org.bukkit.ChatColor.stripColor(title == null ? "" : title);
    if (plain.startsWith("패키지 설정:")) {
        try { plugin.packages().save(); } catch (Throwable ignored) {}
    }
}

}

// 패키지 설정 GUI에서 ESC로 닫을 때도 안전 저장
@EventHandler
public void onPackageEditClose(InventoryCloseEvent e) {
    String title = e.getView().getTitle();
    String plain = org.bukkit.ChatColor.stripColor(title == null ? "" : title);
    if (plain.startsWith("패키지 설정:")) {
        try { plugin.packages().save(); } catch (Throwable ignored) {}
    }
}

}