
package com.minkang.ultimate.random;

import com.minkang.ultimate.random.gui.SettingsGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class RandomCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;
    public RandomCommand(Main plugin){ this.plugin = plugin; }

    private boolean isAdmin(CommandSender s){
        if(!(s instanceof Player)) return true;
        Player p = (Player) s;
        return p.isOp() || p.hasPermission("ultimate.random.admin");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] a){
        String sub = a.length>0 ? a[0] : "";
        if(sub.equalsIgnoreCase("생성")){
            if(!isAdmin(sender)){ sender.sendMessage(plugin.msg("no_perm")); return true; }
            if(a.length<2){ sender.sendMessage("§c/랜덤 생성 <키>"); return true; }
            String key = a[1].toLowerCase();
            if(plugin.getManager().exists(key)){ sender.sendMessage(plugin.msg("already_exists").replace("%key%", key)); return true; }
            plugin.getManager().create(key);
            sender.sendMessage(plugin.msg("created").replace("%key%", key));
            return true;
        }
        if(sub.equalsIgnoreCase("삭제")){
            if(!isAdmin(sender)){ sender.sendMessage(plugin.msg("no_perm")); return true; }
            if(a.length<2){ sender.sendMessage("§c/랜덤 삭제 <키>"); return true; }
            String key=a[1].toLowerCase();
            boolean ok = plugin.getManager().delete(key);
            sender.sendMessage(ok? plugin.msg("deleted").replace("%key%", key) : plugin.msg("not_found").replace("%key%", key));
            return true;
        }
        if(sub.equalsIgnoreCase("목록")){
            List<String> list = plugin.getManager().all().stream().map(Roulette::getKey).sorted().collect(Collectors.toList());
            sender.sendMessage("§b룰렛 키: §7" + (list.isEmpty()?"없음":String.join(", ", list)));
            return true;
        }
        if(sub.equalsIgnoreCase("설정")){
            if(!(sender instanceof Player)){ sender.sendMessage("§c플레이어만 사용 가능"); return true; }
            if(a.length<2){ sender.sendMessage("§c/랜덤 설정 <키>"); return true; }
            Roulette r = plugin.getManager().get(a[1]);
            if(r==null){ sender.sendMessage(plugin.msg("not_found").replace("%key%", a[1])); return true; }
            SettingsGUI.open(plugin, (Player)sender, r);
            return true;
        }
        if(sub.equalsIgnoreCase("아이템")){
            if(!(sender instanceof Player)){ sender.sendMessage("§c플레이어만 사용 가능"); return true; }
            if(a.length<2){ sender.sendMessage("§c/랜덤 아이템 <키>  §7(손에 든 아이템을 트리거 아이템으로 저장)"); return true; }
            Roulette r = plugin.getManager().get(a[1]);
            if(r==null){ sender.sendMessage(plugin.msg("not_found").replace("%key%", a[1])); return true; }
            ItemStack inHand = ((Player)sender).getInventory().getItemInMainHand();
            if(inHand == null || inHand.getType().isAir()){ sender.sendMessage("§c손에 든 아이템이 없습니다."); return true; }
            r.setTriggerItem(inHand.clone());
            plugin.getManager().save();
            sender.sendMessage(plugin.msg("set_item_bind").replace("%key%", r.getKey()));
            return true;
        }
        if(sub.equalsIgnoreCase("지급")){
            if(!isAdmin(sender)){ sender.sendMessage(plugin.msg("no_perm")); return true; }
            if(a.length<4){ sender.sendMessage("§c/랜덤 지급 <키> <플레이어> <개수>"); return true; }
            Roulette r = plugin.getManager().get(a[1]);
            if(r==null){ sender.sendMessage(plugin.msg("not_found").replace("%key%", a[1])); return true; }
            OfflinePlayer target = Bukkit.getOfflinePlayer(a[2]);
            int amount;
            try{ amount = Math.max(1, Integer.parseInt(a[3])); }catch(Exception e){ sender.sendMessage("§c개수 숫자"); return true; }
            if(!target.isOnline()){
                sender.sendMessage("§e대상 플레이어가 온라인이 아닙니다. 온라인일 때 사용하세요.");
                return true;
            }
            Player tp = target.getPlayer();
            for(int i=0;i<amount;i++){
                RouletteEntry win = r.pickByWeight();
                if(win==null || win.getItem()==null || win.getItem().getType().isAir()) continue;
                ItemStack gi = win.getItem().clone();
HashMap<Integer, ItemStack> left = tp.getInventory().addItem(gi);
                for(ItemStack lf : left.values()) tp.getWorld().dropItemNaturally(tp.getLocation(), lf);
            }
            sender.sendMessage(plugin.color("&b[랜덤]&7 지급 완료: &f" + r.getKey() + " &7x" + amount + " → &a" + tp.getName()));
            return true;
        }

        sender.sendMessage("§b/랜덤 생성 <키> §7- 새 룰렛 생성");
        sender.sendMessage("§b/랜덤 삭제 <키> §7- 삭제");
        sender.sendMessage("§b/랜덤 목록 §7- 키 목록");
        sender.sendMessage("§b/랜덤 설정 <키> §7- 설정 GUI");
        sender.sendMessage("§b/랜덤 아이템 <키> §7- 손 아이템을 트리거로 저장");
        sender.sendMessage("§b/랜덤 지급 <키> <플레이어> <개수> §7- 즉시 지급");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String alias, String[] a){
        if(a.length==1) return Arrays.asList("생성","삭제","설정","아이템","지급","목록");
        if(a.length==2 && !a[0].equalsIgnoreCase("목록")){
            List<String> keys = plugin.getManager().all().stream().map(Roulette::getKey).sorted().collect(Collectors.toList());
            return keys.stream().filter(k->k.startsWith(a[1].toLowerCase())).collect(Collectors.toList());
        }
        if(a.length==3 && a[0].equalsIgnoreCase("지급")){
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n->n.toLowerCase().startsWith(a[2].toLowerCase())).collect(Collectors.toList());
        }
        if(a.length==4 && a[0].equalsIgnoreCase("지급")){
            return Collections.singletonList("1");
        }
        return Collections.emptyList();
    }
}
