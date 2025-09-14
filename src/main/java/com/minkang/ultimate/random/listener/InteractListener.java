package com.minkang.ultimate.random.listener;
import com.minkang.ultimate.random.*; import org.bukkit.*; import org.bukkit.entity.Player; import org.bukkit.event.*; import org.bukkit.event.player.PlayerInteractEvent; import org.bukkit.inventory.*; import org.bukkit.persistence.PersistentDataType;
public class InteractListener implements Listener{
  private final Main plugin; public InteractListener(Main p){ this.plugin=p; }
  @EventHandler public void onInteract(PlayerInteractEvent e){ if(e.getHand()!=EquipmentSlot.HAND) return; Player p=e.getPlayer(); ItemStack it=p.getInventory().getItemInMainHand(); if(it==null||it.getType()==Material.AIR) return; if(it.getItemMeta()==null) return;
    String key=it.getItemMeta().getPersistentDataContainer().get(plugin.getPdcKey(), PersistentDataType.STRING); if(key==null||key.isEmpty()) return; Roulette r=plugin.getManager().get(key); if(r==null) return; e.setCancelled(true); com.minkang.ultimate.random.gui.PreviewGUI.open(plugin,p,r); }
}