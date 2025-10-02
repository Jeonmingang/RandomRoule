
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

    private org.bukkit.inventory.ItemStack decorateKeyItem(com.minkang.ultimate.random.pack.PackageDef def, org.bukkit.inventory.ItemStack base){
        org.bukkit.inventory.ItemStack item = base.clone();
        boolean deco = plugin.getConfig().getBoolean("package.key.decorate", true);
        if(!deco) return item;
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add(org.bukkit.ChatColor.YELLOW + plugin.getConfig().getString("package.key.header", "§e[패키지 키] §f%name%").replace("%name%", def.getName()));
        java.util.List<org.bukkit.inventory.ItemStack> items = def.getItems();
        int max = plugin.getConfig().getInt("package.key.preview-max", 10);
        int i=0;
        if(items!=null){
            for(org.bukkit.inventory.ItemStack it : items){
                if(it==null || it.getType().isAir()) continue;
                String n = (it.hasItemMeta() && it.getItemMeta().hasDisplayName()) ? it.getItemMeta().getDisplayName() : it.getType().name();
                lore.add(org.bukkit.ChatColor.GRAY + "- " + org.bukkit.ChatColor.AQUA + n + org.bukkit.ChatColor.GRAY + " x" + it.getAmount());
                i++; if(i>=max) { lore.add(org.bukkit.ChatColor.DARK_GRAY + "..."); break; }
            }
        }
        lore.add(org.bukkit.ChatColor.DARK_GRAY + plugin.getConfig().getString("package.key.footer","우클릭으로 수령 GUI 열기"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

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
        
        if(sub.equalsIgnoreCase("생성")){
            if(!isAdmin(sender)){ sender.sendMessage(plugin.msg("no_perm")); return true; }
            if(a.length<2){ sender.sendMessage("§c/패키지 생성 <이름>"); return true; }
            String name=a[1];
            if(plugin.getPackageManager().exists(name)){ sender.sendMessage(plugin.color("&c이미 존재: &f"+name)); return true; }
            plugin.getPackageManager().create(name);
            sender.sendMessage(plugin.color("&a패키지 생성: &f"+name));
            return true;
        }
        if(sub.equalsIgnoreCase("삭제")){
            if(!isAdmin(sender)){ sender.sendMessage(plugin.msg("no_perm")); return true; }
            if(a.length<2){ sender.sendMessage("§c/패키지 삭제 <이름>"); return true; }
            String name=a[1];
            if(!plugin.getPackageManager().delete(name)){ sender.sendMessage(plugin.color("&c패키지 없음: &f"+name)); return true; }
            sender.sendMessage(plugin.color("&a패키지 삭제: &f"+name));
            return true;
        }
        if(sub.equalsIgnoreCase("설정")){
            if(!(sender instanceof Player)){ sender.sendMessage("§c플레이어만 사용 가능"); return true; }
            if(a.length<2){ sender.sendMessage("§c/패키지 설정 <이름>"); return true; }
            String name=a[1];
            com.minkang.ultimate.random.pack.PackageDef def = plugin.getPackageManager().get(name);
            if(def==null){ sender.sendMessage(plugin.color("&c패키지 없음: &f"+name)); return true; }
            com.minkang.ultimate.random.pack.PackageSettingsGUI.open(plugin, (Player)sender, def);
            return true;
        }
        if(sub.equalsIgnoreCase("아이템")){
            if(!(sender instanceof Player)){ sender.sendMessage("§c플레이어만 사용 가능"); return true; }
            if(a.length<2){ sender.sendMessage("§c/패키지 아이템 <이름>  §7(손에 든 아이템을 키로 지정)"); return true; }
            String name=a[1];
            com.minkang.ultimate.random.pack.PackageDef def = plugin.getPackageManager().get(name);
            if(def==null){ sender.sendMessage(plugin.color("&c패키지 없음: &f"+name)); return true; }
            org.bukkit.inventory.ItemStack hand = ((Player)sender).getInventory().getItemInMainHand();
            if(hand==null || hand.getType().isAir()){ sender.sendMessage(plugin.msg("need_item_in_hand")); return true; }
            org.bukkit.inventory.ItemStack decorated = decorateKeyItem(def, hand);
            def.setTriggerItem(decorated);
            ((org.bukkit.entity.Player)sender).getInventory().setItemInMainHand(decorated);
            plugin.getPackageManager().save();
            sender.sendMessage(plugin.color("&a키 아이템 지정 완료: &f"+name));
            return true;
        }

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
        if(a.length==1) return Arrays.asList("생성","설정","삭제","아이템","지급");
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
