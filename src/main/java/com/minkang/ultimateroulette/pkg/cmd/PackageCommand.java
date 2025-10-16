package com.minkang.ultimateroulette.pkg.cmd;

import com.minkang.ultimateroulette.UltimateRoulette;
import com.minkang.ultimateroulette.pkg.PackageDef;
import com.minkang.ultimateroulette.pkg.PackageManager;
import com.minkang.ultimateroulette.pkg.gui.PackageEditGUI;
import com.minkang.ultimateroulette.util.Text;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;

public class PackageCommand implements CommandExecutor {
    private final UltimateRoulette plugin;
    public PackageCommand(UltimateRoulette plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Player p = (sender instanceof Player) ? (Player)sender : null;
        PackageManager pm = plugin.packages();
        if (args.length == 0) { help(sender, label); return true; }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "생성":
            case "create":
                if (!p.hasPermission("ultimateroulette.admin")) { sender.sendMessage(Text.color("&c권한이 없습니다.")); return true; }
                if (args.length<2) { sender.sendMessage(Text.color("&e사용법: /"+label+" 생성 <키>")); return true; }
                sender.sendMessage(pm.create(args[1]) ? Text.color("&a패키지 생성: &f"+args[1]) : Text.color("&c이미 존재하는 키"));
                return true;
            case "삭제":
            case "delete":
                if (!p.hasPermission("ultimateroulette.admin")) { sender.sendMessage(Text.color("&c권한이 없습니다.")); return true; }
                if (args.length<2) { sender.sendMessage(Text.color("&e사용법: /"+label+" 삭제 <키>")); return true; }
                sender.sendMessage(pm.delete(args[1]) ? Text.color("&a삭제: &f"+args[1]) : Text.color("&c해당 키 없음"));
                return true;
            
            case "설정":
            case "edit":
                if (!(sender instanceof Player)) { sender.sendMessage(Text.color("&c플레이어만 사용할 수 있습니다.")); return true; }
                Player ep = (Player)sender;
                if (!ep.hasPermission("ultimateroulette.admin")) { ep.sendMessage(Text.color("&c권한이 없습니다.")); return true; }
                if (args.length < 2) { ep.sendMessage(Text.color("&e사용법: /"+label+" 설정 <키>")); return true; }
                PackageDef ed = pm.get(args[1]);
                if (ed == null) { ep.sendMessage(Text.color("&c해당 키의 패키지가 없습니다.")); return true; }
                new PackageEditGUI(plugin, ed).open(ep);
                return true;
            case "목록":
            case "list":
                List<String> list = pm.list();
                sender.sendMessage(Text.color("&a패키지 목록: &f" + (list.isEmpty() ? "(없음)" : String.join(", ", list))));
                return true;
            case "아이템":
            case "item":
                if (!p.hasPermission("ultimateroulette.admin")) { sender.sendMessage(Text.color("&c권한이 없습니다.")); return true; }
                if (args.length<2) { sender.sendMessage(Text.color("&e사용법: /"+label+" 아이템 <키> (손/핫바)")); return true; }
                PackageDef def = pm.get(args[1]);
                if (def == null) { sender.sendMessage(Text.color("&c해당 키 없음")); return true; }
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand == null || hand.getType()==Material.AIR) { sender.sendMessage(Text.color("&c손에 아이템을 들고 실행하세요.")); return true; }
                pm.setKeyItem(def.getName(), hand);
                sender.sendMessage(Text.color("&a전용 패키지 아이템 설정 완료 (로어에 구성 표시)"));
                return true;
            
            case "지급":
            case "give": {
                if (!sender.hasPermission("ultimateroulette.admin")) { sender.sendMessage(Text.color("&c권한이 없습니다.")); return true; }
                if (args.length < 4) { sender.sendMessage(Text.color("&e사용법: /" + label + " 지급 <키> <플레이어> <갯수>")); return true; }
                String keyName = args[1];
                org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayerExact(args[2]);
                if (target == null) { sender.sendMessage(Text.color("&c플레이어를 찾을 수 없습니다: " + args[2])); return true; }
                int amount;
                try { amount = Integer.parseInt(args[3]); } catch (Exception ex) { sender.sendMessage(Text.color("&c갯수는 숫자여야 합니다.")); return true; }
                com.minkang.ultimateroulette.pkg.PackageDef defG = pm.get(keyName);
                if (defG == null) { sender.sendMessage(Text.color("&c해당 패키지가 없습니다.")); return true; }
                org.bukkit.inventory.ItemStack keyItem = defG.getKeyItem();
                if (keyItem == null || keyItem.getType() == org.bukkit.Material.AIR) { sender.sendMessage(Text.color("&c이 패키지에는 전용아이템이 설정되어 있지 않습니다. /" + label + " 아이템 " + keyName + " 으로 설정하세요.")); return true; }
                int max = keyItem.getMaxStackSize();
                int left = amount;
                while (left > 0) {
                    org.bukkit.inventory.ItemStack stack = keyItem.clone();
                    int give = Math.min(max, left);
                    stack.setAmount(give);
                    java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> over = target.getInventory().addItem(stack);
                    over.values().forEach(rem -> plugin.storage().addClaim(target.getUniqueId(), rem));
                    left -= give;
                }
                sender.sendMessage(Text.color("&a지급 완료: &f" + keyName + " x" + amount + " &7→ " + target.getName()));
                target.sendMessage(Text.color("&a[관리자 지급] 전용 패키지 아이템: &f" + keyName + " x" + amount));
                return true;
            }

            default:
                help(sender, label); return true;
        }
    }

    private void help(org.bukkit.command.CommandSender sender, String label) {
        sender.sendMessage(Text.color("&6/&l"+label+" 사용법"));
        sender.sendMessage(Text.color("&e/"+label+" 생성 <키>&7 - 패키지 생성"));
        sender.sendMessage(Text.color("&e/"+label+" 삭제 <키>&7 - 패키지 삭제"));
        sender.sendMessage(Text.color("&e/"+label+" 목록&7 - 패키지 목록"));
        sender.sendMessage(Text.color("&e/"+label+" 설정 <키>&7 - 패키지 구성 편집 GUI"));
        sender.sendMessage(Text.color("&e/"+label+" 아이템 <키>&7 - 손에 든 아이템을 전용 패키지 아이템으로 지정"));
        sender.sendMessage(Text.color("&e/"+label+" 지급 <키> <플레이어> <갯수>&7 - 전용 패키지 아이템 지급 (관리자)"));
        sender.sendMessage(Text.color("&7※ 전용 패키지 아이템 우클릭 → 미리보기/받기 GUI"));
    }
}
