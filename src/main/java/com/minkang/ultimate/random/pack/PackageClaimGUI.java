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
import org.bukkit.persistence.PersistentDataType;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
public class PackageClaimGUI implements Listener {
  private static final ConcurrentHashMap<UUID, Long> lastClaim = new ConcurrentHashMap<>();
  private final Main plugin;
  public PackageClaimGUI(Main p){ this.plugin=p; }
  public static void open(Main plugin, Player p, PackageDef d){
    String title=plugin.getConfig().getString("titles.pkg_claim","패키지 수령: %name%").replace("%name%", d.getName());
    Inventory inv=org.bukkit.Bukkit.createInventory(p,54, org.bukkit.ChatColor.translateAlternateColorCodes('&', title));
    int i=0; for(ItemStack it: d.getItems()){ if(i>=45) break; if(it!=null) inv.setItem(i++, it.clone()); }
    ItemStack claim=new ItemStack(Material.EMERALD_BLOCK);
    ItemMeta m=claim.getItemMeta(); if(m!=null){
      m.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("package.claim-button-name","&a[ 보상 수령 ]")));
      java.util.List<String> lore=new java.util.ArrayList<String>();
      for(String s: plugin.getConfig().getStringList("package.claim-button-lore")) lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', s));
      m.setLore(new java.util.ArrayList<>(new java.util.LinkedHashSet<>(lore))); claim.setItemMeta(m);
    }
    inv.setItem(49, claim);
    p.openInventory(inv);
    p.sendMessage(plugin.msg("pkg_open_claim").replace("%name%", d.getName()));
  }
  @EventHandler public void onClick(InventoryClickEvent e){
    if(!(e.getWhoClicked() instanceof Player)) return;
    String title=e.getView().getTitle(); if(title==null) return;
    if(!org.bukkit.ChatColor.stripColor(title).startsWith("패키지 수령:")) return;
    e.setCancelled(true);
    if(e.getRawSlot()==49){
      Player p=(Player)e.getWhoClicked();
      // debounce: ignore duplicate clicks within 1 second
      UUID pu = p.getUniqueId();
      long now = System.currentTimeMillis();
      Long prev = lastClaim.get(pu);
      if(prev != null && now - prev < 1000L) { p.sendMessage(plugin.msg("pkg_claim_busy")); return; }
      lastClaim.put(pu, now);
      String plain=org.bukkit.ChatColor.stripColor(title);
      String name=plain.replace("패키지 수령:","").trim();
      int idx=name.indexOf(" "); if(idx!=-1) name=name.substring(0,idx).trim();
      final String pkgName=name;
      final Main main=((Main)org.bukkit.Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette"));
      PackageDef d=plugin.getPackageManager().get(pkgName);
      if(d==null){ lastClaim.remove(p.getUniqueId());
      p.closeInventory(); return; }
      boolean consume=plugin.getConfig().getBoolean("package.consume-on-claim", true);
      if(consume){
        org.bukkit.inventory.PlayerInventory pinv=p.getInventory();
        java.util.function.Predicate<ItemStack> isKey=(it)->{
          if(it==null||it.getType()==Material.AIR||it.getItemMeta()==null) return false;
          String tag=it.getItemMeta().getPersistentDataContainer().get(plugin.getPkgPdcKey(), org.bukkit.persistence.PersistentDataType.STRING);
          return pkgName.equalsIgnoreCase(tag);
        };
        ItemStack mainH=pinv.getItemInMainHand(); if(isKey.test(mainH)){ if(mainH.getAmount()<=1) pinv.setItemInMainHand(new ItemStack(Material.AIR)); else{ mainH.setAmount(mainH.getAmount()-1); pinv.setItemInMainHand(mainH);} }
        else { try{ ItemStack off=pinv.getItemInOffHand(); if(isKey.test(off)){ if(off.getAmount()<=1) pinv.setItemInOffHand(new ItemStack(Material.AIR)); else{ off.setAmount(off.getAmount()-1); pinv.setItemInOffHand(off);} } else{ boolean done=false; for(int i2=0;i2<pinv.getSize();i2++){ ItemStack it=pinv.getItem(i2); if(isKey.test(it)){ if(it.getAmount()<=1) pinv.setItem(i2,new ItemStack(Material.AIR)); else{ it.setAmount(it.getAmount()-1); pinv.setItem(i2,it);} done=true; break; } } } }catch(Throwable ignored){} }
      }
      for(ItemStack it: d.getItems()){ if(it==null||it.getType()==Material.AIR) continue; ItemStack give=it.clone(); java.util.Map<Integer,ItemStack> left=com.minkang.ultimate.random.RewardGiver.giveClean(p, give); if(!left.isEmpty()) for(ItemStack lf: left.values()) com.minkang.ultimate.random.RewardGiver.giveClean(p, lf); }
      p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.1f);
      p.sendMessage(plugin.msg("pkg_claim_success").replace("%name%", pkgName));
      lastClaim.remove(p.getUniqueId());
      p.closeInventory();
    }
  }
}