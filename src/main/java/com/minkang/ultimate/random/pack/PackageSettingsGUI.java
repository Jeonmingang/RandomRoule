package com.minkang.ultimate.random.pack;
import com.minkang.ultimate.random.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
public class PackageSettingsGUI implements Listener {
  private final Main plugin;
  public PackageSettingsGUI(Main p){ this.plugin=p; }
  public static void open(Main plugin, Player p, PackageDef d){
    String title=plugin.getConfig().getString("titles.pkg_settings","패키지 설정: %name% (닫으면 저장)").replace("%name%", d.getName());
    Inventory inv=Bukkit.createInventory(p,54, ChatColor.translateAlternateColorCodes('&', title));
    int i=0; for(ItemStack it: d.getItems()){ if(i>=54) break; if(it!=null) inv.setItem(i++, it.clone()); }
    p.openInventory(inv);
  }
  @EventHandler public void onClick(InventoryClickEvent e){
    if(!(e.getWhoClicked() instanceof Player)) return;
    String title=e.getView().getTitle(); if(title==null) return;
    if(!ChatColor.stripColor(title).startsWith("패키지 설정:")) return;
    if(e.getRawSlot() < e.getView().getTopInventory().getSize()){
      if(e.getClick()==ClickType.DROP || e.getClick()==ClickType.CONTROL_DROP || e.getClick()==ClickType.MIDDLE){
        e.setCurrentItem(null);
        ((Player)e.getWhoClicked()).playSound(e.getWhoClicked().getLocation(), Sound.ENTITY_ITEM_BREAK, 0.6f, 1.2f);
        e.setCancelled(true);
      }
    }
  }
  @EventHandler public void onClose(InventoryCloseEvent e){
    String t=e.getView().getTitle(); if(t==null) return;
    String plain=ChatColor.stripColor(t); if(!plain.startsWith("패키지 설정:")) return;
    String name=plain.replace("패키지 설정:","").trim(); int idx=name.indexOf(" "); if(idx!=-1) name=name.substring(0,idx).trim();
    PackageDef def=((Main)org.bukkit.Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).getPackageManager().get(name); if(def==null) return;
    Inventory inv=e.getInventory();
    java.util.List<ItemStack> list=new java.util.ArrayList<>();
    for(int i=0;i<inv.getSize();i++){ ItemStack it=inv.getItem(i); if(it!=null){ list.add(it.clone()); } }
    def.setItems(list);
    ((Main)org.bukkit.Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).getPackageManager().save();
    if(e.getPlayer() instanceof Player){ Player p=(Player)e.getPlayer(); p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f); p.sendMessage(((Main)org.bukkit.Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).msg("pkg_saved_settings").replace("%count%", String.valueOf(list.size()))); }
  }
}