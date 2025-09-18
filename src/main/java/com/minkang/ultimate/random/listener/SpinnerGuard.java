package com.minkang.ultimate.random.listener;

import com.minkang.ultimate.random.Main;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Prevents players from taking or moving items in the Spinner (룰렛 뽑기) GUI.
 * Does NOT affect Settings GUI where editing is intended.
 */
public class SpinnerGuard implements Listener {

    private final String spinnerPrefix;

    public SpinnerGuard(Main plugin) {
        String conf = plugin.getConfig().getString("titles.spinner", "룰렛 뽑기: %key%");
        String plain = ChatColor.stripColor(conf == null ? "" : conf);
        int idx = plain.indexOf("%key%");
        if (idx >= 0) {
            plain = plain.substring(0, idx);
        }
        this.spinnerPrefix = plain.trim().isEmpty() ? "룰렛 뽑기:" : plain.trim();
    }

    private boolean isSpinnerTitle(String title) {
        if (title == null) return false;
        String plain = ChatColor.stripColor(title);
        return plain != null && plain.startsWith(spinnerPrefix);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (!isSpinnerTitle(title)) return;

        // Block any interaction that can move items in or out of the spinner GUI
        // Block clicks in the top inventory
        if (e.getClickedInventory() != null && e.getClickedInventory().equals(e.getView().getTopInventory())) {
            e.setCancelled(true);
            return;
        }

        // Block attempts to push/pull items between inventories (shift-click, hotbar swap, collect to cursor, etc.)
        InventoryAction act = e.getAction();
        switch (act) {
            case MOVE_TO_OTHER_INVENTORY:
            case HOTBAR_SWAP:
            case HOTBAR_MOVE_AND_READD:
            case COLLECT_TO_CURSOR:
            case UNKNOWN:
            case DROP_ALL_CURSOR:
            case DROP_ONE_CURSOR:
            case DROP_ALL_SLOT:
            case DROP_ONE_SLOT:
            case SWAP_WITH_CURSOR:
            case PLACE_ALL:
            case PLACE_ONE:
            case PLACE_SOME:
            case PICKUP_ALL:
            case PICKUP_HALF:
            case PICKUP_ONE:
            case PICKUP_SOME:
                e.setCancelled(true);
                break;
            default:
                break;
        }

        if (e.isShiftClick()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        String title = e.getView().getTitle();
        if (!isSpinnerTitle(title)) return;
        // If any dragged slot targets the top inventory, cancel.
        for (int slot : e.getRawSlots()) {
            if (slot < e.getView().getTopInventory().getSize()) {
                e.setCancelled(true);
                return;
            }
        }
    }
}
