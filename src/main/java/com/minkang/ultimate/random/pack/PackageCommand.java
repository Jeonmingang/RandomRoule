package com.minkang.ultimate.random.pack;
import com.minkang.ultimate.random.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;
import java.util.stream.Collectors;
public class PackageCommand implements CommandExecutor, TabCompleter {
  private final Main plugin;
  public PackageCommand(Main p){ this.plugin=p; }
  private boolean isAdmin(CommandSender s){ if(!(s instanceof Player)) return true; Player p=(Player)s; return p.isOp()||p.hasPermission("ultimate.random.admin"); }
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] a){
    String sub=a.length>0?a[0].toLowerCase():"";
    if(sub.equals("생성")||sub.equals("create")){ if(!isAdmin(sender)){ sender.sendMessage("권한이 없습니다."); return true; } if(a.length<2){ sender.sendMessage("/"+label+" 생성 <이름>"); return true; } String name=a[1]; if(plugin.getPackageManager().exists(name)){ sender.sendMessage(((Main)Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).msg("pkg_exists").replace("%name%",name)); return true;} plugin.getPackageManager().create(name); sender.sendMessage(((Main)Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).msg("pkg_created").replace("%name%",name)); return true; }
    if(sub.equals("삭제")||sub.equals("delete")){ if(!isAdmin(sender)){ sender.sendMessage("권한이 없습니다."); return true; } if(a.length<2){ sender.sendMessage("/"+label+" 삭제 <이름>"); return true; } String name=a[1]; boolean ok=plugin.getPackageManager().delete(name); sender.sendMessage(ok?((Main)Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).msg("pkg_deleted").replace("%name%",name):((Main)Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).msg("pkg_not_found").replace("%name%",name)); return true; }
    if(sub.equals("목록")||sub.equals("list")){ java.util.List<String> list=plugin.getPackageManager().all().stream().map(PackageDef::getName).sorted().collect(Collectors.toList()); sender.sendMessage("§d[패키지] §f목록: §e"+(list.isEmpty()?"없음":String.join(", ",list))); return true; }
    if(sub.equals("설정")||sub.equals("settings")){ if(!isAdmin(sender)){ sender.sendMessage("권한이 없습니다."); return true; } if(!(sender instanceof Player)){ sender.sendMessage("플레이어만 사용 가능합니다."); return true; } if(a.length<2){ sender.sendMessage("/"+label+" 설정 <이름>"); return true; } String name=a[1]; PackageDef def=plugin.getPackageManager().get(name); if(def==null){ sender.sendMessage(((Main)Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).msg("pkg_not_found").replace("%name%",name)); return true; } com.minkang.ultimate.random.pack.PackageSettingsGUI.open((Main)Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette"),(Player)sender,def); sender.sendMessage(((Main)Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).msg("pkg_open_settings").replace("%name%",name)); return true; }
    if(sub.equals("아이템")||sub.equals("item")){ if(!isAdmin(sender)){ sender.sendMessage("권한이 없습니다."); return true; } if(!(sender instanceof Player)){ sender.sendMessage("플레이어만 사용 가능합니다."); return true; } if(a.length<2){ sender.sendMessage("/"+label+" 아이템 <이름>"); return true; } String name=a[1]; PackageDef def=plugin.getPackageManager().get(name); if(def==null){ sender.sendMessage(((Main)Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).msg("pkg_not_found").replace("%name%",name)); return true; } Player p=(Player)sender; ItemStack hand=p.getInventory().getItemInMainHand(); if(hand==null||hand.getType()==Material.AIR){ p.sendMessage(((Main)Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).msg("pkg_need_item_in_hand")); return true; } ItemStack copy=hand.clone(); ItemMeta meta=copy.getItemMeta(); if(meta!=null){ meta.getPersistentDataContainer().set(((Main)Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).getPkgPdcKey(), org.bukkit.persistence.PersistentDataType.STRING, def.getName()); java.util.List<String> lore=new java.util.ArrayList<String>(); lore.add(((Main)Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).color(((Main)Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).getConfig().getString("package.key-header","&6&l[ 패키지 ] &e%name%").replace("%name%", def.getName()))); lore.add(((Main)Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).color(((Main)Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).getConfig().getString("package.key-subtitle","&8━ &7보상 목록"))); java.util.List<ItemStack> items=def.getItems(); int shown=0, limit=12; for(ItemStack it: items){ if(it==null||it.getType()==Material.AIR) continue; String in=it.hasItemMeta()&&it.getItemMeta().hasDisplayName()? it.getItemMeta().getDisplayName(): it.getType().name(); String line=((Main)Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).getConfig().getString("package.key-entry","&7- &f%item% &7x&ex%amount%"); line=line.replace("%item%", org.bukkit.ChatColor.stripColor(in)).replace("%amount%", String.valueOf(it.getAmount())); lore.add(((Main)Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).color(line)); shown++; if(shown>=limit) break; } int remain=items.size()-shown; if(remain>0) lore.add(((Main)Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).color("&7... 외 &e"+remain+"&7개")); lore.add(((Main)Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).color(((Main)Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).getConfig().getString("package.key-footer","&8클릭하여 수령 GUI 열기"))); lore.add("§7[패키지키: "+def.getName()+"]"); meta.setLore(lore); copy.setItemMeta(meta);} p.getInventory().setItemInMainHand(copy); plugin.getPackageManager().get(name).setTriggerItem(copy); plugin.getPackageManager().save(); p.sendMessage(((Main)Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).msg("pkg_set_item_bind").replace("%name%",name)); return true; }
    sender.sendMessage("§d/패키지 생성 <이름> §7- 새 패키지 생성");
    sender.sendMessage("§d/패키지 삭제 <이름> §7- 패키지 삭제");
    sender.sendMessage("§d/패키지 설정 <이름> §7- GUI로 아이템 등록(닫으면 저장)");
    sender.sendMessage("§d/패키지 아이템 <이름> §7- 손 아이템을 패키지 수령키로 지정");
    
if(sub.equals("지급")||sub.equals("give")){
  if(!isAdmin(sender)){ sender.sendMessage(ChatColor.RED+"권한이 없습니다."); return true; }
  if(a.length<2){ sender.sendMessage("§d/패키지 지급 <패키지> [플레이어] [수량]"); return true; }
  String name=a[1].toLowerCase();
  PackageDef d=plugin.getPackageManager().get(name);
  if(d==null){ sender.sendMessage(plugin.msg("pkg_not_found").replace("%name%", name)); return true; }
  org.bukkit.inventory.ItemStack key=d.getTriggerItem();
  if(key==null){ sender.sendMessage(plugin.msg("pkg_give_no_item").replace("%name%", name)); return true; }
  int count=1;
  if(a.length>=4){
    try{ count=Math.max(1, Integer.parseInt(a[3])); }catch(Exception ignored){}
  }
  org.bukkit.entity.Player target=null;
  if(a.length>=3){
    target=org.bukkit.Bukkit.getPlayerExact(a[2]);
  }else if(sender instanceof org.bukkit.entity.Player){
    target=(org.bukkit.entity.Player)sender;
  }
  if(target==null){ sender.sendMessage(plugin.msg("player_not_found").replace("%player%", (a.length>=3?a[2]:"(콘솔)"))); return true; }
  org.bukkit.inventory.ItemStack give=key.clone();
  give.setAmount(count);
  java.util.Map<Integer, org.bukkit.inventory.ItemStack> left=target.getInventory().addItem(give);
  if(!left.isEmpty()){
    for(org.bukkit.inventory.ItemStack rest:left.values()){
      target.getWorld().dropItemNaturally(target.getLocation(), rest);
    }
  }
  target.sendMessage(plugin.msg("pkg_received_key").replace("%name%", name).replace("%count%", String.valueOf(count)));
  sender.sendMessage(plugin.msg("pkg_given_to_player").replace("%player%", target.getName()).replace("%name%", name).replace("%count%", String.valueOf(count)));
  return true;
}
  public java.util.List<String> onTabComplete(CommandSender s, Command c, String alias, String[] a){
    if(a.length==1) return java.util.Arrays.asList("생성","삭제","설정","아이템","목록").stream().filter(x->x.startsWith(a[0])).collect(java.util.stream.Collectors.toList());
    if(a.length==2 && !a[0].equalsIgnoreCase("목록")){ java.util.List<String> keys=new java.util.ArrayList<>(); for(PackageDef d: ((Main)Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).getPackageManager().all()) keys.add(d.getName()); java.util.List<String> out=new java.util.ArrayList<>(); for(String k:keys) if(k.startsWith(a[1])) out.add(k); return out; }
    return java.util.Collections.emptyList();
  }
}