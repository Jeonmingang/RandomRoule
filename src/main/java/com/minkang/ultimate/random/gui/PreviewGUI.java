package com.minkang.ultimate.random.gui;
import com.minkang.ultimate.random.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
public class PreviewGUI implements Listener{
  private final Main plugin; public PreviewGUI(Main p){ this.plugin=p; }
  public static void open(Main plugin, Player p, Roulette r){
    String title=plugin.getConfig().getString("titles.preview","룰렛 미리보기: %key%").replace("%key%", r.getKey());
    Inventory inv=Bukkit.createInventory(p,54, ChatColor.translateAlternateColorCodes('&', title));
    boolean showWeight=plugin.getConfig().getBoolean("preview.show-weight", true);
    boolean showChance=plugin.getConfig().getBoolean("preview.show-chance", true);
    String weightFmt=ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("preview.weight-format","&7가중치: &e%weight%"));
    String chanceFmt=ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("preview.chance-format","&7확률: &e%chance%%"));
    int total=r.getTotalWeight(); java.text.DecimalFormat df=new java.text.DecimalFormat("#.##");
    int slot=0; for(RouletteEntry e:r.getEntries()){
      if(slot>=45) break;
      ItemStack it=e.getItem().clone(); ItemMeta m=it.getItemMeta();
      if(m!=null){
        java.util.List<String> lore=m.hasLore()?m.getLore():new java.util.ArrayList<String>(); if(lore==null) lore=new java.util.ArrayList<String>();
        if(showChance){ double chance=100.0*e.getWeight()/Math.max(1,total); lore.add(chanceFmt.replace("%chance%", df.format(chance))); }
        if(showWeight){ lore.add(weightFmt.replace("%weight%", String.valueOf(e.getWeight()))); }
        m.setLore(lore); it.setItemMeta(m);
      }
      inv.setItem(slot++, it);
    }
    ItemStack draw=new ItemStack(Material.EMERALD_BLOCK);
    ItemMeta dm=draw.getItemMeta();
    if(dm!=null){
      dm.setDisplayName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("gui.preview_draw_button_name","&a[ 뽑기 시작 ]")));
      java.util.List<String> lore=new java.util.ArrayList<String>();
      for(String s: plugin.getConfig().getStringList("gui.preview_draw_button_lore")) lore.add(ChatColor.translateAlternateColorCodes('&', s));
      dm.setLore(lore); draw.setItemMeta(dm);
    }
    inv.setItem(49, draw);
    p.openInventory(inv);
    p.sendMessage(plugin.msg("open_preview").replace("%key%", r.getKey()));
  }
  private boolean hasKeyTag(Main plugin, ItemStack it, String key){
    if(it==null||it.getType()==Material.AIR) return false;
    if(it.getItemMeta()==null) return false;
    String tag=it.getItemMeta().getPersistentDataContainer().get(plugin.getPdcKey(), PersistentDataType.STRING);
    return key.equalsIgnoreCase(tag);
  }
  // ★ Use PlayerInventory to access main/off-hand methods
  private boolean consumeOneKey(Main plugin, Player p, String key){
    PlayerInventory pinv = p.getInventory();
    // main-hand
    ItemStack main=pinv.getItemInMainHand();
    if(hasKeyTag(plugin, main, key)){
      if(main.getAmount()<=1) pinv.setItemInMainHand(new ItemStack(Material.AIR));
      else{ main.setAmount(main.getAmount()-1); pinv.setItemInMainHand(main); }
      return true;
    }
    // off-hand (1.9+)
    try{
      ItemStack off=pinv.getItemInOffHand();
      if(hasKeyTag(plugin, off, key)){
        if(off.getAmount()<=1) pinv.setItemInOffHand(new ItemStack(Material.AIR));
        else{ off.setAmount(off.getAmount()-1); pinv.setItemInOffHand(off); }
        return true;
      }
    }catch(Throwable ignored){}
    // inventory contents
    for(int i=0;i<pinv.getSize();i++){
      ItemStack it=pinv.getItem(i);
      if(hasKeyTag(plugin, it, key)){
        if(it.getAmount()<=1) pinv.setItem(i, new ItemStack(Material.AIR));
        else{ it.setAmount(it.getAmount()-1); pinv.setItem(i, it); }
        return true;
      }
    }
    return false;
  }
  @EventHandler public void onClick(InventoryClickEvent e){
    if(!(e.getWhoClicked() instanceof Player)) return;
    String title=e.getView().getTitle(); if(title==null) return;
    if(!ChatColor.stripColor(title).startsWith("룰렛 미리보기:")) return;
    e.setCancelled(true);
    if(e.getRawSlot()==49){
      String plain=ChatColor.stripColor(title);
      String key=plain.replace("룰렛 미리보기:","").trim();
      int idx=key.indexOf(" "); if(idx!=-1) key=key.substring(0,idx).trim();
      Roulette r=plugin.getManager().get(key);
      Player pl=(Player)e.getWhoClicked();
      if(r==null){ pl.sendMessage(plugin.msg("not_found").replace("%key%", key)); pl.closeInventory(); return; }
      if(r.isEmpty()){ pl.sendMessage(plugin.msg("no_items")); pl.closeInventory(); return; }
      boolean need=plugin.getConfig().getBoolean("gui.draw_consumes_key", true);
      if(need && !consumeOneKey(plugin, pl, r.getKey())){
        pl.sendMessage(plugin.msg("need_key").replace("%key%", r.getKey()));
        pl.playSound(pl.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 0.8f);
        return;
      }
      pl.closeInventory();
      pl.playSound(pl.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
      SpinnerGUI.start(plugin, pl, r);
    }
  }
}