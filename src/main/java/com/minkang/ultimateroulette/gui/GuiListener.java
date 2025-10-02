package com.minkang.ultimateroulette.gui;

import com.minkang.ultimateroulette.UltimateRoulette;
import com.minkang.ultimateroulette.data.KeyDef;
import com.minkang.ultimateroulette.data.Reward;
import com.minkang.ultimateroulette.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class GuiListener implements Listener {
    private final UltimateRoulette plugin;

    public GuiListener(UltimateRoulette plugin) { this.plugin = plugin; }

    private boolean isEdit(String title) { return title != null && title.contains("설정: "); }
    private boolean isPreview(String title) { return title != null && title.contains("미리보기: "); }
    private boolean isClaim(String title) { return title != null && title.contains("보관함"); }

    private String keyFromEditTitle(String title) {
        String t = title;
        int at = t.indexOf("설정: ");
        if (at < 0) return null;
        t = t.substring(at + 4).trim();
        // cut page tag if present
        int paren = t.indexOf(" (");
        if (paren > 0) t = t.substring(0, paren).trim();
        return t;
    }

    private int pageFromEditTitle(String title) {
        int p = 0;
        int lb = title.indexOf("(p");
        if (lb >= 0) {
            int rb = title.indexOf(")", lb);
            if (rb > lb) {
                try {
                    String num = title.substring(lb+2, rb); // pN
                    p = Integer.parseInt(num) - 1;
                } catch (Exception ignored) {}
            }
        }
        return Math.max(0, p);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();

        if (isClaim(title)) { e.setCancelled(true); return; }

        if (isPreview(title)) {
            e.setCancelled(true);
            if (e.getRawSlot() == 49) {
                String key = title.substring(title.indexOf("미리보기: ") + 6).trim();
                KeyDef def = plugin.keys().get(key);
                if (def == null || def.getRewards().isEmpty()) { p.sendMessage(Text.color("&c보상 풀이 비어있습니다.")); return; }
                new SpinGUI(plugin, def).open(p);
            }
            return;
        }

        if (!isEdit(title)) return;
        e.setCancelled(true);

        String key = keyFromEditTitle(title);
        int page = pageFromEditTitle(title);
        KeyDef def = plugin.keys().get(key);
        if (def == null) return;

        Inventory top = e.getView().getTopInventory();
        int raw = e.getRawSlot();
        ClickType click = e.getClick();
        InventoryAction action = e.getAction();

        // paging controls
        if (raw == EditGUI.PREV_SLOT) {
            if (page > 0 && click.isLeftClick()) new EditGUI(plugin, def, page-1).open(p);
            return;
        }
        if (raw == EditGUI.NEXT_SLOT) {
            int maxIdx = def.getRewards().size();
            boolean hasNext = maxIdx > (page+1)*EditGUI.SLOTS_PER_PAGE;
            if (hasNext && click.isLeftClick()) new EditGUI(plugin, def, page+1).open(p);
            return;
        }
        if (raw == EditGUI.PAGE_SLOT) {
            // 좌: 이전, 우: 다음
            int maxIdx = def.getRewards().size();
            boolean hasPrev = page > 0;
            boolean hasNext = maxIdx > (page+1)*EditGUI.SLOTS_PER_PAGE;
            if (click.isLeftClick() && hasPrev) new EditGUI(plugin, def, page-1).open(p);
            else if (click.isRightClick() && hasNext) new EditGUI(plugin, def, page+1).open(p);
            return;
        }

        // Add by shift-click from bottom
        boolean fromBottom = raw >= top.getSize();
        ItemStack current = e.getCurrentItem();
        ItemStack cursor = e.getCursor();

        if (fromBottom && action == InventoryAction.MOVE_TO_OTHER_INVENTORY
                && current != null && current.getType() != Material.AIR) {
            def.getRewards().add(new Reward(current.clone(), 1));
            plugin.keys().save();
            new EditGUI(plugin, def, page).open(p);
            return;
        }

        // Add by placing cursor into top (0..44)
        if (!fromBottom && raw >= 0 && raw < EditGUI.SLOTS_PER_PAGE
                && cursor != null && cursor.getType() != Material.AIR) {
            def.getRewards().add(new Reward(cursor.clone(), 1));
            p.setItemOnCursor(null);
            plugin.keys().save();
            new EditGUI(plugin, def, page).open(p);
            return;
        }

        // Adjust weight or delete on top slots
        if (!fromBottom && raw >= 0 && raw < EditGUI.SLOTS_PER_PAGE) {
            int idx = page * EditGUI.SLOTS_PER_PAGE + raw;
            if (idx < def.getRewards().size()) {
                Reward r = def.getRewards().get(idx);
                switch (click) {
                    case LEFT: r.setWeight(r.getWeight()+1); break;
                    case SHIFT_LEFT: r.setWeight(r.getWeight()+10); break;
                    case RIGHT: r.setWeight(Math.max(1, r.getWeight()-1)); break;
                    case SHIFT_RIGHT: r.setWeight(Math.max(1, r.getWeight()-10)); break;
                    case DROP: def.getRewards().remove(r); break;
                    default: break;
                }
                plugin.keys().save();
                new EditGUI(plugin, def, page).open(p);
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) { /* no-op */ }
}
