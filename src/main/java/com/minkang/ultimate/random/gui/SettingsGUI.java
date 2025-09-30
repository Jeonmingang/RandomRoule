package com.minkang.ultimate.random.gui;
import com.minkang.ultimate.random.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;
public class SettingsGUI implements Listener{
  private final Main plugin; public SettingsGUI(Main p){ this.plugin=p; }
  public static void open(Main plugin, Player p, Roulette r){
    String title=plugin.getConfig().getString("titles.settings","룰렛 설정: %key% (닫으면 저장)").replace("%key%", r.getKey());
    Inventory inv=Bukkit.createInventory(p,54, ChatColor.translateAlternateColorCodes('&', title));
    int i=0; for(RouletteEntry e: r.getEntries()){
      if(i>=54) break;
      ItemStack it=e.getItem().clone();
      ItemMeta m=it.getItemMeta();
      if(m!=null){
        java.util.List<String> lore=m.hasLore()?m.getLore():new java.util.ArrayList<String>(); if(lore==null) lore=new java.util.ArrayList<String>();
        boolean replaced=false;
        for(int li=0; li<lore.size(); li++){ String plain=ChatColor.stripColor(lore.get(li)); plain=plain.replace(" ",""); if(plain.toLowerCase().startsWith("가중치".toLowerCase())){ lore.set(li, "§7가중치: §e"+e.getWeight()); replaced=true; break; } }
        if(!replaced) lore.add("§7가중치: §e"+e.getWeight());
        m.setLore(new java.util.ArrayList<>(new java.util.LinkedHashSet<>(lore))); it.setItemMeta(m);
      }
      inv.setItem(i++, it);
    }
    p.openInventory(inv);
  }
  private int parseWeight(ItemStack it){
    if(it==null||!it.hasItemMeta()||!it.getItemMeta().hasLore()) return 1;
    for(String line: it.getItemMeta().getLore()){
      String s=ChatColor.stripColor(line).replace(" ","");
      if(s.startsWith("가중치")){
        s=s.substring("가중치".length()); s=s.replace(":","").trim();
        try{ return Math.max(1, Integer.parseInt(s)); }catch(Exception ignore){}
      }
    }
    return 1;
  }
  private void setWeightLore(ItemStack it, int weight){
    ItemMeta m=it.getItemMeta(); if(m==null) return;
    java.util.List<String> lore=m.hasLore()?m.getLore():new java.util.ArrayList<String>(); if(lore==null) lore=new java.util.ArrayList<String>();
    boolean replaced=false;
    for(int i=0;i<lore.size();i++){ String plain=ChatColor.stripColor(lore.get(i)).replace(" ",""); if(plain.toLowerCase().startsWith("가중치".toLowerCase())){ lore.set(i, "§7가중치: §e"+weight); replaced=true; break; } }
    if(!replaced) lore.add("§7가중치: §e"+weight);
    m.setLore(new java.util.ArrayList<>(new java.util.LinkedHashSet<>(lore))); it.setItemMeta(m);
  }
  @EventHandler public void onClick(InventoryClickEvent e){
    if(!(e.getWhoClicked() instanceof Player)) return;
    String title=e.getView().getTitle(); if(title==null) return;
    if(!ChatColor.stripColor(title).startsWith("룰렛 설정:")) return;
    if(e.getRawSlot() < e.getView().getTopInventory().getSize()){
      ItemStack cur=e.getCurrentItem();
      if(cur!=null && cur.getType()!=Material.AIR){
        if(e.getClick()==ClickType.DROP || e.getClick()==ClickType.CONTROL_DROP || e.getClick()==ClickType.MIDDLE){
          e.setCurrentItem(new ItemStack(Material.AIR));
          ((Player)e.getWhoClicked()).playSound(e.getWhoClicked().getLocation(), Sound.ENTITY_ITEM_BREAK, 0.6f, 1.2f);
          e.setCancelled(true);
          return;
        }
        int delta=0;
        if(e.getClick()==ClickType.LEFT) delta=1;
        else if(e.getClick()==ClickType.RIGHT) delta=-1;
        else if(e.getClick()==ClickType.SHIFT_LEFT) delta=10;
        else if(e.getClick()==ClickType.SHIFT_RIGHT) delta=-10;
        if(delta!=0){
          int w=parseWeight(cur); w=Math.max(1, w+delta); setWeightLore(cur, w);
          ((Player)e.getWhoClicked()).playSound(e.getWhoClicked().getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, delta>0?1.4f:0.8f);
          e.setCancelled(true);
          return;
        }
      }
    }
  }
  @EventHandler public void onClose(InventoryCloseEvent e){
    String t=e.getView().getTitle(); if(t==null) return;
    String plain=ChatColor.stripColor(t); if(!plain.startsWith("룰렛 설정:")) return;
    String key=plain.replace("룰렛 설정:","").trim(); int idx=key.indexOf(" "); if(idx!=-1) key=key.substring(0,idx).trim();
    Roulette r=((com.minkang.ultimate.random.Main)org.bukkit.Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).getManager().get(key); if(r==null) return;
    Inventory inv=e.getInventory();
    java.util.List<RouletteEntry> list=new java.util.ArrayList<>();
    for(int i=0;i<inv.getSize();i++){
      ItemStack it=inv.getItem(i);
      if(it==null||it.getType()==Material.AIR) continue;
      int w=parseWeight(it);
      list.add(new RouletteEntry(it.clone(), w));
    }
    r.setEntries(list);
    ((com.minkang.ultimate.random.Main)org.bukkit.Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).getManager().save();
    if(e.getPlayer() instanceof Player){
      Player p=(Player)e.getPlayer();
      p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
      p.sendMessage(((com.minkang.ultimate.random.Main)org.bukkit.Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).msg("saved_settings").replace("%count%", String.valueOf(list.size())));
    }
  }
}