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

  private boolean isAdmin(CommandSender s){
    if(!(s instanceof Player)) return true;
    Player p=(Player)s;
    return p.isOp() || p.hasPermission("ultimate.random.admin");
  }

  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] a){
    String sub = a.length>0 ? a[0].toLowerCase() : "";

    // help first
    if(sub.isEmpty() || sub.equals("help") || sub.equals("도움말")){
      sender.sendMessage("§d/패키지 생성 <이름> §7- 새 패키지 생성");
      sender.sendMessage("§d/패키지 삭제 <이름> §7- 패키지 삭제");
      sender.sendMessage("§d/패키지 설정 <이름> §7- GUI로 아이템 등록(닫으면 저장)");
      sender.sendMessage("§d/패키지 아이템 <이름> §7- 손 아이템을 패키지 수령키로 지정");
      sender.sendMessage("§d/패키지 지급 <이름> [플레이어] [수량] §7- 룰렛 없이 즉시 보상 지급");
      sender.sendMessage("§d/패키지 키지급 <이름> [플레이어] [수량] §7- 수령키 아이템만 지급");
      return true;
    }

    if(sub.equals("생성")||sub.equals("create")){
      if(!isAdmin(sender)){ sender.sendMessage(ChatColor.RED+"권한이 없습니다."); return true; }
      if(a.length<2){ sender.sendMessage("§d/패키지 생성 <이름>"); return true; }
      String name=a[1].toLowerCase();
      if(plugin.getPackageManager().get(name)!=null){ sender.sendMessage(plugin.msg("pkg_exists").replace("%name%",name)); return true; }
      plugin.getPackageManager().create(name);
      sender.sendMessage(plugin.msg("pkg_created").replace("%name%",name));
      return true;
    }

    if(sub.equals("삭제")||sub.equals("delete")){
      if(!isAdmin(sender)){ sender.sendMessage(ChatColor.RED+"권한이 없습니다."); return true; }
      if(a.length<2){ sender.sendMessage("§d/패키지 삭제 <이름>"); return true; }
      String name=a[1].toLowerCase();
      if(!plugin.getPackageManager().delete(name)){ sender.sendMessage(plugin.msg("pkg_not_found").replace("%name%",name)); return true; }
      sender.sendMessage(plugin.msg("pkg_deleted").replace("%name%",name));
      return true;
    }

    if(sub.equals("목록")||sub.equals("list")){
      List<String> list = new ArrayList<>(java.util.stream.StreamSupport.stream(plugin.getPackageManager().all().spliterator(), false).map(PackageDef::getName).collect(java.util.stream.Collectors.toList()));
      sender.sendMessage("§d패키지 목록: §f"+(list.isEmpty()?"없음":String.join(", ",list)));
      return true;
    }

    if(sub.equals("설정")||sub.equals("settings")){
      if(!(sender instanceof Player)){ sender.sendMessage(ChatColor.RED+"게임 내에서만 사용 가능합니다."); return true; }
      if(a.length<2){ sender.sendMessage("§d/패키지 설정 <이름>"); return true; }
      String name=a[1].toLowerCase();
      PackageDef d=plugin.getPackageManager().get(name);
      if(d==null){ sender.sendMessage(plugin.msg("pkg_not_found").replace("%name%",name)); return true; }
      com.minkang.ultimate.random.pack.PackageSettingsGUI.open(plugin, (Player)sender, d);
      sender.sendMessage(plugin.msg("pkg_open_settings").replace("%name%",name));
      return true;
    }

    if(sub.equals("아이템")||sub.equals("item")){
      if(!(sender instanceof Player)){ sender.sendMessage(ChatColor.RED+"게임 내에서만 사용 가능합니다."); return true; }
      if(!isAdmin(sender)){ sender.sendMessage(ChatColor.RED+"권한이 없습니다."); return true; }
      if(a.length<2){ sender.sendMessage("§d/패키지 아이템 <이름>"); return true; }
      String name=a[1].toLowerCase();
      PackageDef d=plugin.getPackageManager().get(name);
      if(d==null){ sender.sendMessage(plugin.msg("pkg_not_found").replace("%name%", name)); return true; }
      Player p=(Player)sender;
      ItemStack hand=p.getInventory().getItemInMainHand();
      if(hand==null || hand.getType()==Material.AIR){ sender.sendMessage(plugin.msg("pkg_give_no_item").replace("%name%", name)); return true; }
      ItemMeta m=hand.getItemMeta();
      if(m!=null){
        m.getPersistentDataContainer().set(plugin.getPkgPdcKey(), PersistentDataType.STRING, name);
        hand.setItemMeta(m);
      }
      d.setTriggerItem(hand.clone());
      sender.sendMessage(plugin.msg("pkg_set_item_bind").replace("%name%",name));
      return true;
    }

    if(sub.equals("지급")||sub.equals("give")){
      if(!isAdmin(sender)){ sender.sendMessage(ChatColor.RED+"권한이 없습니다."); return true; }
      if(a.length<2){ sender.sendMessage("§d/패키지 지급 <패키지> [플레이어] [수량]"); return true; }
      String name=a[1].toLowerCase();
      PackageDef d=plugin.getPackageManager().get(name);
      if(d==null){ sender.sendMessage(plugin.msg("pkg_not_found").replace("%name%", name)); return true; }
      int count=1; if(a.length>=4){ try{ count=Math.max(1, Integer.parseInt(a[3])); }catch(Exception ignored){} }
      Player target=null;
      if(a.length>=3){ target=Bukkit.getPlayerExact(a[2]); }
      else if(sender instanceof Player){ target=(Player)sender; }
      if(target==null){ sender.sendMessage(plugin.msg("player_not_found").replace("%player%", (a.length>=3?a[2]:"(콘솔)"))); return true; }
      for(int i=0;i<count;i++){
        for(ItemStack it : d.getItems()){
          if(it==null) continue;
          com.minkang.ultimate.random.RewardGiver.giveClean(target, it.clone());
        }
      }
      target.sendMessage(plugin.msg("pkg_claim_success").replace("%name%", name));
      sender.sendMessage(plugin.msg("pkg_given_to_player").replace("%player%", target.getName()).replace("%name%", name).replace("%count%", String.valueOf(count)));
      return true;
    }

    if(sub.equals("키지급")||sub.equals("key")){
      if(!isAdmin(sender)){ sender.sendMessage(ChatColor.RED+"권한이 없습니다."); return true; }
      if(a.length<2){ sender.sendMessage("§d/패키지 키지급 <패키지> [플레이어] [수량]"); return true; }
      String name=a[1].toLowerCase();
      PackageDef d=plugin.getPackageManager().get(name);
      if(d==null){ sender.sendMessage(plugin.msg("pkg_not_found").replace("%name%", name)); return true; }
      ItemStack key=d.getTriggerItem();
      if(key==null){ sender.sendMessage(plugin.msg("pkg_give_no_item").replace("%name%", name)); return true; }
      int count=1; if(a.length>=4){ try{ count=Math.max(1, Integer.parseInt(a[3])); }catch(Exception ignored){} }
      Player target=null;
      if(a.length>=3){ target=Bukkit.getPlayerExact(a[2]); }
      else if(sender instanceof Player){ target=(Player)sender; }
      if(target==null){ sender.sendMessage(plugin.msg("player_not_found").replace("%player%", (a.length>=3?a[2]:"(콘솔)"))); return true; }
      ItemStack give=key.clone(); give.setAmount(count);
      java.util.Map<Integer, ItemStack> left = com.minkang.ultimate.random.RewardGiver.giveClean(target, give);
      if(!left.isEmpty()){
        for(ItemStack rest: left.values()) com.minkang.ultimate.random.RewardGiver.giveClean(target, rest);
      }
      target.sendMessage(plugin.msg("pkg_received_key").replace("%name%", name).replace("%count%", String.valueOf(count)));
      sender.sendMessage(plugin.msg("pkg_given_to_player").replace("%player%", target.getName()).replace("%name%", name).replace("%count%", String.valueOf(count)));
      return true;
    }

    sender.sendMessage(ChatColor.GRAY+"알 수 없는 하위 명령입니다. /패키지 도움말");
    return true;
  }

  @Override
  public java.util.List<String> onTabComplete(CommandSender s, Command c, String alias, String[] a){
    if(a.length==1) return java.util.Arrays.asList("생성","삭제","목록","설정","아이템","지급","키지급","도움말");
    if(a.length==2 && !a[0].equalsIgnoreCase("목록")){
      java.util.List<String> keys = new java.util.ArrayList<>(java.util.stream.StreamSupport.stream(plugin.getPackageManager().all().spliterator(), false).map(PackageDef::getName).collect(java.util.stream.Collectors.toList()));
      return keys.stream().filter(k->k.startsWith(a[1].toLowerCase())).collect(java.util.stream.Collectors.toList());
    }
    return java.util.Collections.emptyList();
  }
}
