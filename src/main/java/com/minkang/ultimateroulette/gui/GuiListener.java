package com.minkang.ultimateroulette.gui;

import com.minkang.ultimateroulette.UltimateRoulette;
import com.minkang.ultimateroulette.data.KeyDef;
import com.minkang.ultimateroulette.data.Reward;
import com.minkang.ultimateroulette.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GuiListener implements Listener {

    private final UltimateRoulette plugin;
    public GuiListener(UltimateRoulette plugin) {
        this.plugin = plugin;
    }

    // ---- Helpers ----
    private boolean isEdit(String title) {
        return title != null && title.startsWith(Text.color("&a&l설정: "));
    }
    private int currentPageFromTitle(String title) {
        // title: &a&l설정: <키> (pN)
        try {
            int pIdx = title.lastIndexOf("(p");
            if (pIdx >= 0) {
                int end = title.indexOf(')', pIdx);
                String num = ChatColor.stripColor(title.substring(pIdx+2, end));
                return Math.max(0, Integer.parseInt(num) - 1);
            }
        } catch (Exception ignored) {}
        return 0;
    }
    private String keyFromTitle(String title) {
        // after "설정: " until space before "(p"
        try {
            String plain = ChatColor.stripColor(title);
            int s = plain.indexOf("설정: ") + 4;
            int e = plain.indexOf(" (p");
            if (s >= 0 && e > s) return plain.substring(s, e).trim();
        } catch (Exception ignored) {}
        return null;
    }

    // ---- Click: Edit GUI ----
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();
        if (!isEdit(title)) return;

        e.setCancelled(true); // 편집 GUI는 설정만 반영

        String key = keyFromTitle(title);
        KeyDef def = plugin.keys().get(key);
        if (def == null) return;

        int page = currentPageFromTitle(title);
        Inventory top = e.getView().getTopInventory();
        Inventory bottom = e.getView().getBottomInventory();
        int raw = e.getRawSlot();
        ClickType click = e.getClick();

        boolean fromBottom = (e.getClickedInventory() != null && e.getClickedInventory().equals(bottom));

        // 1) 플레이어 인벤에서 추가 (SHIFT-CLICK 포함)
        if (fromBottom) {
            ItemStack src = e.getCurrentItem();
            if (src != null && src.getType() != Material.AIR) {
                def.getRewards().add(new Reward(src.clone(), 1));
                plugin.keys().save();
                new EditGUI(plugin, def, page).open(p);
            }
            return;
        }

        // 2) 페이지 버튼
        if (raw == EditGUI.PREV_SLOT) {
            if (page > 0) new EditGUI(plugin, def, page - 1).open(p);
            return;
        }
        if (raw == EditGUI.NEXT_SLOT) {
            boolean hasNext = def.getRewards().size() > (page + 1) * EditGUI.SLOTS_PER_PAGE;
            if (hasNext) new EditGUI(plugin, def, page + 1).open(p);
            return;
        }
        if (raw == EditGUI.PAGE_SLOT) {
            // 좌=이전 / 우=다음
            if (click == ClickType.LEFT || click == ClickType.SHIFT_LEFT) {
                if (page > 0) new EditGUI(plugin, def, page - 1).open(p);
            } else if (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) {
                boolean hasNext = def.getRewards().size() > (page + 1) * EditGUI.SLOTS_PER_PAGE;
                if (hasNext) new EditGUI(plugin, def, page + 1).open(p);
            }
            return;
        }

        // 3) 상단 0..44: 가중치 조정 / 삭제 / 커서 아이템 등록
        if (raw >= 0 && raw < EditGUI.SLOTS_PER_PAGE) {
            int idx = page * EditGUI.SLOTS_PER_PAGE + raw;
            if (idx < def.getRewards().size()) {
                Reward r = def.getRewards().get(idx);
                switch (click) {
                    case LEFT: r.setWeight(r.getWeight() + 1); break;
                    case SHIFT_LEFT: r.setWeight(r.getWeight() + 10); break;
                    case RIGHT: r.setWeight(Math.max(1, r.getWeight() - 1)); break;
                    case SHIFT_RIGHT: r.setWeight(Math.max(1, r.getWeight() - 10)); break;
                    case DROP: def.getRewards().remove(r); break;
                    default: break;
                }
                plugin.keys().save();
                new EditGUI(plugin, def, page).open(p);
                return;
            }
            // 커서 아이템을 놓아 추가(소모 없음)
            ItemStack cursor = e.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                def.getRewards().add(new Reward(cursor.clone(), 1));
                plugin.keys().save();
                new EditGUI(plugin, def, page).open(p);
                return;
            }
        }
    }

    // ---- Drag: 상단에 드래그 투하 시 보상으로 추가 (소모 없음) ----
    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();
        if (!isEdit(title)) return;

        e.setCancelled(true);

        String key = keyFromTitle(title);
        KeyDef def = plugin.keys().get(key);
        if (def == null) return;

        ItemStack cursor = e.getOldCursor();
        if (cursor == null || cursor.getType() == Material.AIR) return;

        def.getRewards().add(new Reward(cursor.clone(), 1));
        plugin.keys().save();

        int page = currentPageFromTitle(title);
        new EditGUI(plugin, def, page).open(p);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        // no-op
    }

    // ==== Preview GUI protections / actions ====
    private boolean isPreviewTitle(String title) {
        if (title == null) return false;
        String plain = org.bukkit.ChatColor.stripColor(title);
        return plain != null && plain.startsWith("미리보기: ");
    }
    private String keyNameFromPreview(String title) {
        String plain = org.bukkit.ChatColor.stripColor(title);
        return plain.replaceFirst("^미리보기:\\s*", "");
    }

    @org.bukkit.event.EventHandler
    public void onPreviewClick(org.bukkit.event.inventory.InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof org.bukkit.entity.Player)) return;
        String title = e.getView().getTitle();
        if (!isPreviewTitle(title)) return;

        // Block all interactions by default
        e.setCancelled(true);

        // Only allow clicking the green 'Start' button at slot 49
        if (e.getRawSlot() != 49) return;

        org.bukkit.entity.Player p = (org.bukkit.entity.Player) e.getWhoClicked();
        String key = keyNameFromPreview(title);
        com.minkang.ultimateroulette.data.KeyDef def = plugin.keys().get(key);
        if (def == null) {
            p.sendMessage(com.minkang.ultimateroulette.util.Text.color("&c해당 키 구성이 없어 뽑기를 시작할 수 없습니다: &f" + key));
            return;
        }
        p.closeInventory();
        new com.minkang.ultimateroulette.gui.SpinGUI(plugin, def).open(p);
    }

    @org.bukkit.event.EventHandler
    public void onPreviewDrag(org.bukkit.event.inventory.InventoryDragEvent e) {
        if (isPreviewTitle(e.getView().getTitle())) {
            e.setCancelled(true);
        }
    }

}
