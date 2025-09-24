package com.minkang.ultimate.random.pack;
import com.minkang.ultimate.random.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
public class PackageClaimGUI implements Listener {
  private final Main plugin;
  public PackageClaimGUI(Main p){ this.plugin=p; }
  public static void open(Main plugin, Player p, PackageDef d){
    String title=plugin.getConfig().getString("titles.pkg_claim","패키지 수령: %name%").replace("%name%", d.getName());
    Inventory inv=Bukkit.createInventory(p,54, ChatColor.translateAlternateColorCodes('&', title));
    int i=0; for(ItemStack it: d.getItems()){ if(i>=45) break; if(it!=null) inv.setItem(i++, it.clone()); }
    ItemStack claim=new ItemStack(Material.EMERALD_BLOCK);
    ItemMeta m=claim.getItemMeta(); if(m!=null){
      m.setDisplayName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("package.claim-button-name","&a[ 보상 수령 ]")));
      java.util.List<String> lore=new java.util.ArrayList<String>();
      for(String s: plugin.getConfig().getStringList("package.claim-button-lore")) lore.add(ChatColor.translateAlternateColorCodes('&', s));
      m.setLore(lore); claim.setItemMeta(m);
    }
    inv.setItem(49, claim);
    p.openInventory(inv);
    p.sendMessage(plugin.msg("pkg_open_claim").replace("%name%", d.getName()));
  }
  @EventHandler public void onClick(InventoryClickEvent e){
    if(!(e.getWhoClicked() instanceof Player)) return;
    String title=e.getView().getTitle(); if(title==null) return;
    if(!ChatColor.stripColor(title).startsWith("패키지 수령:")) return;
    e.setCancelled(true);
    if(e.getRawSlot()==49){
      Player p=(Player)e.getWhoClicked();
      String plain=ChatColor.stripColor(title); String name=plain.replace("패키지 수령:","").trim(); int idx=name.indexOf(" "); if(idx!=-1) name=name.substring(0,idx).trim();
      PackageDef d=((com.minkang.ultimate.random.Main)Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).getPackageManager().get(name);
      if(d==null){ p.closeInventory(); return; }
      boolean consume=((com.minkang.ultimate.random.Main)Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).getConfig().getBoolean("package.consume-on-claim", true);
      if(consume){
        // consume one key from inventory (main, off, anywhere)
        org.bukkit.inventory.PlayerInventory pinv=p.getInventory();
        java.util.function.Predicate<ItemStack> isKey=(it)->{ if(it==null||it.getType()==Material.AIR||it.getItemMeta()==null) return false; String tag=it.getItemMeta().getPersistentDataContainer().get(((com.minkang.ultimate.random.Main)Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).getPkgPdcKey(), org.bukkit.persistence.PersistentDataType.STRING); return name.equalsIgnoreCase(tag); };
        ItemStack main=pinv.getItemInMainHand(); if(isKey.test(main)){ if(main.getAmount()<=1) pinv.setItemInMainHand(new ItemStack(Material.AIR)); else{ main.setAmount(main.getAmount()-1); pinv.setItemInMainHand(main);} }
        else { try{ ItemStack off=pinv.getItemInOffHand(); if(isKey.test(off)){ if(off.getAmount()<=1) pinv.setItemInOffHand(new ItemStack(Material.AIR)); else{ off.setAmount(off.getAmount()-1); pinv.setItemInOffHand(off);} } else{ boolean done=false; for(int i=0;i<pinv.getSize();i++){ ItemStack it=pinv.getItem(i); if(isKey.test(it)){ if(it.getAmount()<=1) pinv.setItem(i,new ItemStack(Material.AIR)); else{ it.setAmount(it.getAmount()-1); pinv.setItem(i,it);} done=true; break; } } } }catch(Throwable ignored){} }
      }
      // give items
      for(ItemStack it: d.getItems()){ if(it==null||it.getType()==Material.AIR) continue; ItemStack give=it.clone(); java.util.Map<Integer,ItemStack> left=p.getInventory().addItem(give); if(!left.isEmpty()) for(ItemStack lf: left.values()) p.getWorld().dropItemNaturally(p.getLocation(), lf); }
      p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.1f);
      p.sendMessage(((com.minkang.ultimate.random.Main)Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).msg("pkg_claim_success").replace("%name%", name));
      p.closeInventory();
    }
  }
}