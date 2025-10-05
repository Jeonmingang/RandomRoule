package com.minkang.ultimateroulette.cmd;

import com.minkang.ultimateroulette.UltimateRoulette;
import com.minkang.ultimateroulette.data.KeyDef;
import com.minkang.ultimateroulette.data.KeyManager;
import com.minkang.ultimateroulette.data.Reward;
import com.minkang.ultimateroulette.gui.EditGUI;
import com.minkang.ultimateroulette.gui.PreviewGUI;
import com.minkang.ultimateroulette.gui.ClaimGUI;
import com.minkang.ultimateroulette.util.Text;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class RouletteCommand implements CommandExecutor {

    private final UltimateRoulette plugin;
    public RouletteCommand(UltimateRoulette plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Player p = (sender instanceof Player) ? (Player)sender : null;
        KeyManager km = plugin.keys();

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "아이템":
            case "item":
                if (p == null) { sender.sendMessage(Text.color("&c플레이어만 사용 가능합니다.")); return true; }
                if (!p.hasPermission("ultimateroulette.admin")) { sender.sendMessage(Text.color("&c권한이 없습니다.")); return true; }
                if (args.length < 2) { sender.sendMessage(Text.color("&e사용법: /" + label + " 아이템 <키>")); return true; }
                org.bukkit.inventory.ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand == null || hand.getType() == org.bukkit.Material.AIR) {
                    sender.sendMessage(Text.color("&c손에 아이템을 들고 사용하세요.")); return true;
                }
                KeyDef def = km.get(args[1]);
                if (def == null) { sender.sendMessage(Text.color("&c존재하지 않는 키: &f" + args[1])); return true; }
                // write PDC + lore onto the held item and persist
                km.setKeyItem(args[1], hand);
                // ensure player's item becomes the tagged version immediately
                org.bukkit.inventory.ItemStack replaced = def.getKeyItem();
                if (replaced != null) {
                    p.getInventory().setItemInMainHand(replaced.clone());
                    p.updateInventory();
                }
                sender.sendMessage(Text.color("&a전용 아이템 설정 완료: &f" + args[1] + " &7(우클릭으로 미리보기/스핀)"));
                return true;

            case "생성": // create key
            case "create":
                if (!p.hasPermission("ultimateroulette.admin")) {
                    sender.sendMessage(Text.color("&c권한이 없습니다.")); return true;
                }
                if (args.length < 2) { sender.sendMessage(Text.color("&e사용법: /" + label + " 생성 <키>")); return true; }
                if (km.create(args[1])) sender.sendMessage(Text.color("&a키 생성: &f" + args[1]));
                else sender.sendMessage(Text.color("&c이미 존재하는 키입니다."));
                return true;

            case "삭제": // delete key
            case "delete":
                if (!p.hasPermission("ultimateroulette.admin")) { sender.sendMessage(Text.color("&c권한이 없습니다.")); return true; }
                if (args.length < 2) { sender.sendMessage(Text.color("&e사용법: /" + label + " 삭제 <키>")); return true; }
                if (km.delete(args[1])) sender.sendMessage(Text.color("&a키 삭제: &f" + args[1]));
                else sender.sendMessage(Text.color("&c해당 키가 없습니다."));
                return true;

            case "설정": // open edit GUI
            case "set":
                if (!p.hasPermission("ultimateroulette.admin")) { sender.sendMessage(Text.color("&c권한이 없습니다.")); return true; }
                if (args.length < 2) { sender.sendMessage(Text.color("&e사용법: /" + label + " 설정 <키>")); return true; }
                KeyDef def = km.get(args[1]);
                if (def == null) { sender.sendMessage(Text.color("&c해당 키가 없습니다.")); return true; }
                new EditGUI(plugin, def).open(p);
                return true;

            case "목록": // list keys
            case "list":
                List<String> list = km.list();
                sender.sendMessage(Text.color("&a키 목록: &f" + (list.isEmpty() ? "(없음)" : String.join(", ", list))));
                return true;

            case "아이템": // set key item from item in hand
            case "item":
                if (!p.hasPermission("ultimateroulette.admin")) { sender.sendMessage(Text.color("&c권한이 없습니다.")); return true; }
                if (args.length < 2) { sender.sendMessage(Text.color("&e사용법: /" + label + " 아이템 <키> (손에 들고 실행)")); return true; }
                KeyDef def2 = km.get(args[1]);
                if (def2 == null) { sender.sendMessage(Text.color("&c해당 키가 없습니다.")); return true; }
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand == null || hand.getType() == Material.AIR) { sender.sendMessage(Text.color("&c손에 아이템을 들고 실행하세요.")); return true; }
                km.setKeyItem(def2.getName(), hand);
                sender.sendMessage(Text.color("&a전용아이템 설정 완료. 로어에 미리보기/확률이 표시됩니다."));
                return true;

            case "보관함":
            case "claim":
                new ClaimGUI(plugin, p.getUniqueId()).open(p);
                return true;

            case "리로드":
            case "reload":
                if (!p.hasPermission("ultimateroulette.admin")) { sender.sendMessage(Text.color("&c권한이 없습니다.")); return true; }
                plugin.reloadConfig();
                plugin.keys().load();
                sender.sendMessage(Text.color("&a리로드 완료."));
                return true;
            case "리프레시":
            case "refresh":
                if (!p.hasPermission("ultimateroulette.admin")) { sender.sendMessage(Text.color("&c권한이 없습니다.")); return true; }
                if (args.length < 2) { sender.sendMessage(Text.color("&e사용법: /" + label + " 리프레시 <키>")); return true; }
                com.minkang.ultimateroulette.data.KeyDef d = plugin.keys().get(args[1]);
                if (d == null) { sender.sendMessage(Text.color("&c해당 키가 없습니다.")); return true; }
                org.bukkit.inventory.PlayerInventory inv = p.getInventory();
                for (int i=0;i<inv.getSize();i++) {
                    org.bukkit.inventory.ItemStack is = inv.getItem(i);
                    if (is!=null && is.hasItemMeta() && is.getItemMeta().getPersistentDataContainer().has(plugin.keyTag(), org.bukkit.persistence.PersistentDataType.STRING)) {
                        String tag = is.getItemMeta().getPersistentDataContainer().get(plugin.keyTag(), org.bukkit.persistence.PersistentDataType.STRING);
                        if (tag!=null && tag.equalsIgnoreCase(d.getName())) {
                            inv.setItem(i, plugin.keys().buildKeyItemLore(is, d));
                        }
                    }
                }
                sender.sendMessage(Text.color("&a전용키 로어 최신화 완료."));
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
                com.minkang.ultimateroulette.data.KeyDef defG = plugin.keys().get(keyName);
                if (defG == null) { sender.sendMessage(Text.color("&c해당 키가 없습니다.")); return true; }
                org.bukkit.inventory.ItemStack keyItem = defG.getKeyItem();
                if (keyItem == null || keyItem.getType() == org.bukkit.Material.AIR) { sender.sendMessage(Text.color("&c이 키에는 전용아이템이 설정되어 있지 않습니다. /" + label + " 아이템 " + keyName + " 으로 설정하세요.")); return true; }
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
                target.sendMessage(Text.color("&a[관리자 지급] 전용아이템: &f" + keyName + " x" + amount));
                return true;
            }

            default:
                sendHelp(sender, label);
                return true;
        }
    }

    private void sendHelp(org.bukkit.command.CommandSender sender, String label) {
        sender.sendMessage(Text.color("&6/&l" + label + " 사용법"));
        sender.sendMessage(Text.color("&e/" + label + " 생성 <키>&7 - 키생성"));
        sender.sendMessage(Text.color("&e/" + label + " 삭제 <키>&7 - 키삭제"));
        sender.sendMessage(Text.color("&e/" + label + " 설정 <키>&7 - 설정GUI(가중치 조정)"));
        sender.sendMessage(Text.color("&e/" + label + " 목록&7 - 키 목록"));
        sender.sendMessage(Text.color("&e/" + label + " 아이템 <키>&7 - 손에든 아이템을 전용아이템으로 설정 (우클릭시 미리보기/스핀)"));
        sender.sendMessage(Text.color("&e/" + label + " 보관함&7 - 당첨 아이템 수령"));
        sender.sendMessage(Text.color("&e/" + label + " 지급 <키> <플레이어> <갯수>&7 - 전용아이템 지급 (관리자)"));
        sender.sendMessage(Text.color("&e/" + label + " 리로드&7 - 설정/키 파일 새로고침"));
        sender.sendMessage(Text.color("&e/" + label + " 리프레시 <키>&7 - 인벤 전용키 로어 최신화"));
    }
}
