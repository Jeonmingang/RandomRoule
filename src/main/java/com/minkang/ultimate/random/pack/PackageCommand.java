
package com.minkang.ultimate.random.pack;

import com.minkang.ultimate.random.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
    public boolean onCommand(CommandSender sender, Command command, String label, String[] a){
        String sub = a.length>0 ? a[0] : "";
        if(sub.equalsIgnoreCase("지급")){
            if(!isAdmin(sender)){ sender.sendMessage(plugin.msg("no_perm")); return true; }
            if(a.length<4){ sender.sendMessage("§c/패키지 지급 <이름> <플레이어> <수량>"); return true; }
            String name=a[1];
            PackageDef def = plugin.getPackageManager().get(name);
            if(def==null){ sender.sendMessage(plugin.color("&c패키지 없음: &f"+name)); return true; }

            OfflinePlayer op = Bukkit.getOfflinePlayer(a[2]);
            int cnt;
            try{ cnt = Math.max(1, Integer.parseInt(a[3])); }catch(Exception e){ sender.sendMessage("§c수량 숫자"); return true; }
            if(!op.isOnline()){
                sender.sendMessage("§e플레이어 온라인 아님");
                return true;
            }
            Player p = op.getPlayer();
            for(int i=0;i<cnt;i++){
                for(ItemStack it : def.getItems()){
                    if(it==null || it.getType().isAir()) continue;
                    ItemStack give = it.clone();
                    give.setAmount(Math.max(1, give.getAmount()));
                    Map<Integer, ItemStack> left = p.getInventory().addItem(give);
                    for(ItemStack lf : left.values()) p.getWorld().dropItemNaturally(p.getLocation(), lf);
                }
            }
            p.sendMessage(plugin.color("&a패키지 수령: &f"+def.getName()+" &7x"+cnt));
            sender.sendMessage(plugin.color("&b[패키지]&7 지급 완료 → &a"+p.getName()));
            return true;
        }

        sender.sendMessage("§b/패키지 지급 <이름> <플레이어> <수량>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String alias, String[] a){
        if(a.length==1) return Arrays.asList("지급");
        if(a.length==2){
            List<String> keys = plugin.getPackageManager().all().stream().map(PackageDef::getName).sorted().collect(Collectors.toList());
            return keys.stream().filter(k->k.startsWith(a[1].toLowerCase())).collect(Collectors.toList());
        }
        if(a.length==3 && a[0].equalsIgnoreCase("지급")){
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n->n.toLowerCase().startsWith(a[2].toLowerCase())).collect(Collectors.toList());
        }
        if(a.length==4 && a[0].equalsIgnoreCase("지급")) return Collections.singletonList("1");
        return Collections.emptyList();
    }
}
