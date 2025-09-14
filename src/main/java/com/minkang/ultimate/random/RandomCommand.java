
package com.minkang.ultimate.random;

import com.minkang.ultimate.random.gui.SettingsGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RandomCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public RandomCommand(Main plugin) { this.plugin = plugin; }

    private boolean isAdmin(CommandSender sender) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        boolean ok = p.isOp();
        if (!ok) { ok = p.hasPermission("ultimate.random.admin"); }
        return ok;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length > 0 ? args[0] : "";
        if (sub.equalsIgnoreCase("생성")) {
            if (!isAdmin(sender)) { sender.sendMessage("권한이 없습니다."); return true; }
            if (args.length < 2) { sender.sendMessage("/" + label + " 생성 <키>"); return true; }
            String key = args[1];
            if (plugin.getManager().exists(key)) { sender.sendMessage(plugin.msg("exists").replace("%key%", key)); return true; }
            plugin.getManager().create(key);
            sender.sendMessage(plugin.msg("created").replace("%key%", key));
            return true;

        } else if (sub.equalsIgnoreCase("삭제")) {
            if (!isAdmin(sender)) { sender.sendMessage("권한이 없습니다."); return true; }
            if (args.length < 2) { sender.sendMessage("/" + label + " 삭제 <키>"); return true; }
            String key = args[1];
            boolean ok = plugin.getManager().delete(key);
            if (ok) sender.sendMessage(plugin.msg("deleted").replace("%key%", key));
            else sender.sendMessage(plugin.msg("not_found").replace("%key%", key));
            return true;

        } else if (sub.equalsIgnoreCase("목록")) {
            List<String> list = plugin.getManager().all().stream().map(Roulette::getKey).sorted().collect(Collectors.toList());
            sender.sendMessage("§b[룰렛] §f키 목록: §e" + (list.isEmpty() ? "없음" : String.join(", ", list)));
            return true;

        } else if (sub.equalsIgnoreCase("설정")) {
            if (!isAdmin(sender)) { sender.sendMessage("권한이 없습니다."); return true; }
            if (!(sender instanceof Player)) { sender.sendMessage("플레이어만 사용 가능합니다."); return true; }
            if (args.length < 2) { sender.sendMessage("/" + label + " 설정 <키>"); return true; }
            String key = args[1];
            Roulette r = plugin.getManager().get(key);
            if (r == null) { sender.sendMessage(plugin.msg("not_found").replace("%key%", key)); return true; }
            Player p = (Player) sender;
            SettingsGUI.open(plugin, p, r);
            p.sendMessage(plugin.msg("open_settings").replace("%key%", key));
            p.sendMessage(plugin.color(plugin.getConfig().getString("messages.setting_hint_1")));
            p.sendMessage(plugin.color(plugin.getConfig().getString("messages.setting_hint_2")));
            return true;

        } else if (sub.equalsIgnoreCase("아이템")) {
            if (!isAdmin(sender)) { sender.sendMessage("권한이 없습니다."); return true; }
            if (!(sender instanceof Player)) { sender.sendMessage("플레이어만 사용 가능합니다."); return true; }
            if (args.length < 2) { sender.sendMessage("/" + label + " 아이템 <키> - 손에 든 아이템을 우클릭 뽑기 키로 지정"); return true; }
            String key = args[1];
            Roulette r = plugin.getManager().get(key);
            if (r == null) { sender.sendMessage(plugin.msg("not_found").replace("%key%", key)); return true; }
            Player p = (Player) sender;
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == Material.AIR) { p.sendMessage(plugin.msg("need_item_in_hand")); return true; }
            ItemStack copy = tagAsTrigger(hand.clone(), r.getKey());
            p.getInventory().setItemInMainHand(copy);
            plugin.getManager().setTriggerItem(key, copy);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
            p.sendMessage(plugin.msg("set_item_bind").replace("%key%", key));
            return true;

        } else if (sub.equalsIgnoreCase("지급")) {
            if (!isAdmin(sender)) { sender.sendMessage("권한이 없습니다."); return true; }
            if (args.length < 4) { sender.sendMessage("/" + label + " 지급 <키> <플레이어> <갯수>"); return true; }
            String key = args[1];
            Roulette r = plugin.getManager().get(key);
            if (r == null) { sender.sendMessage(plugin.msg("not_found").replace("%key%", key)); return true; }
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) { sender.sendMessage(plugin.msg("give_player_not_found")); return true; }
            int amount = 0;
            try { amount = Integer.parseInt(args[3]); } catch (Exception ignored) {}
            if (amount <= 0) { sender.sendMessage(plugin.msg("give_invalid_amount")); return true; }

            ItemStack base = r.getTriggerItem();
            if (base == null || base.getType() == Material.AIR) {
                sender.sendMessage(plugin.msg("give_need_trigger").replace("%key%", key));
                return true;
            }
            // Ensure tag present
            base = tagAsTrigger(base.clone(), r.getKey());

            int remaining = amount;
            int max = base.getMaxStackSize();
            while (remaining > 0) {
                ItemStack give = base.clone();
                int part = Math.min(max, remaining);
                give.setAmount(part);
                Map<Integer, ItemStack> left = target.getInventory().addItem(give);
                if (!left.isEmpty()) {
                    for (ItemStack leftover : left.values()) {
                        target.getWorld().dropItemNaturally(target.getLocation(), leftover);
                    }
                }
                remaining -= part;
            }
            sender.sendMessage(plugin.msg("give_success").replace("%player%", target.getName()).replace("%key%", key).replace("%amount%", String.valueOf(amount)));
            return true;
        }

        sender.sendMessage("§b/랜덤 생성 <키> §7- 새 룰렛 생성");
        sender.sendMessage("§b/랜덤 삭제 <키> §7- 룰렛 삭제");
        sender.sendMessage("§b/랜덤 설정 <키> §7- GUI로 아이템 등록/가중치 조절 (닫으면 저장)");
        sender.sendMessage("§b/랜덤 아이템 <키> §7- 손 아이템을 우클릭시 뽑기 키로 지정");
        sender.sendMessage("§b/랜덤 지급 <키> <플레이어> <갯수> §7- 해당 열쇠 아이템 지급");
        sender.sendMessage("§b/랜덤 목록 §7- 키 목록");
        return true;
    }

    private ItemStack tagAsTrigger(ItemStack item, String key) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(plugin.getPdcKey(), PersistentDataType.STRING, key);
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<String>();
        if (lore == null) lore = new ArrayList<String>();
        boolean hasTag = false;
        for (String l : lore) if (l.contains("룰렛키: ")) { hasTag = true; break; }
        if (!hasTag) lore.add("§7[룰렛키: " + key + "]");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("생성", "삭제", "설정", "아이템", "목록", "지급").stream()
                    .filter(s -> s.startsWith(args[0]))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("목록")) {
            List<String> keys = plugin.getManager().all().stream().map(Roulette::getKey).collect(Collectors.toList());
            if (args[0].equalsIgnoreCase("생성")) return Collections.singletonList("<키>");
            return keys.stream().filter(k -> k.startsWith(args[1])).collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("지급")) {
            List<String> names = new ArrayList<String>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            return names.stream().filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("지급")) {
            return Collections.singletonList("1");
        }
        return Collections.emptyList();
    }
}
