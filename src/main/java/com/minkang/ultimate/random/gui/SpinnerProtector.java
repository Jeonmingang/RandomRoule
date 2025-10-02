
package com.minkang.ultimate.random.gui;

import com.minkang.ultimate.random.Main;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.*;

public class SpinnerProtector implements Listener {
    private final Main plugin;
    public SpinnerProtector(Main plugin){ this.plugin=plugin; }

    private boolean isSpinnerTitle(String title){
        if(title==null) return false;
        String plain= ChatColor.stripColor(title);
        return plain!=null && plain.startsWith("룰렛 뽑기:");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e){
        if(!isSpinnerTitle(e.getView().getTitle())) return;
        int topSize = e.getView().getTopInventory().getSize();
        if(e.getRawSlot() < topSize){
            e.setCancelled(true);
            e.setResult(Event.Result.DENY);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e){
        if(!isSpinnerTitle(e.getView().getTitle())) return;
        int topSize = e.getView().getTopInventory().getSize();
        for(Integer raw : e.getRawSlots()){
            if(raw < topSize){
                e.setCancelled(true);
                e.setResult(Event.Result.DENY);
                return;
            }
        }
    }
}
