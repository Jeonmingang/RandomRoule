package com.signition.samskybridge.cmd;
import com.signition.samskybridge.level.LevelService;
import com.signition.samskybridge.upgrade.UpgradeService;
import com.signition.samskybridge.gui.UpgradeGUI;
import com.signition.samskybridge.data.IslandData;
import com.signition.samskybridge.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import java.util.*;
public class IslandCommand implements CommandExecutor, TabCompleter {
    private final LevelService level;
    private final UpgradeService upgrade;
    private final UpgradeGUI gui;
    public IslandCommand(LevelService level, UpgradeService upgrade, UpgradeGUI gui){
        this.level = level;
        this.upgrade = upgrade;
        this.gui = gui;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("플레이어만 사용 가능합니다.");
            return true;
        }
        Player p = (Player) sender;
        if (args.length == 0){
            p.sendMessage(Text.color("&b/섬 레벨 &7- 레벨/경험치 확인"));
            p.sendMessage(Text.color("&b/섬 업그레이드 &7- 업그레이드 GUI"));
            return true;
        }
        String sub = args[0];
        if ("레벨".equalsIgnoreCase(sub) || "level".equalsIgnoreCase(sub)){
            cmdIslandLevel(p);
            return true;
        }
        if ("업그레이드".equalsIgnoreCase(sub) || "upgrade".equalsIgnoreCase(sub)){
            gui.open(p);
            return true;
        }
        p.sendMessage(Text.color("&c알 수 없는 서브커맨드입니다."));
        return true;
    }
    private void cmdIslandLevel(Player player) {
        UUID owner = player.getUniqueId();
        int lv = level.getLevel(owner);
        long cur = level.getCurrentXp(owner);
        long need = level.requiredXpForLevel(lv + 1);
        double pct = need <= 0 ? 1.0 : Math.min(1.0, (double)cur / (double)need);
        int bars = 20;
        int filled = (int)Math.round(pct * bars);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < bars; i++) bar.append(i < filled ? "§a▮" : "§7▮");
        IslandData is = level.getIslandOf(player);
        player.sendMessage(Text.color("&b[섬 레벨] &fLv.&e" + lv));
        player.sendMessage(Text.color("&7경험치: &f" + String.format("%,d", cur) + "&7 / &a" + String.format("%,d", need) + " &8(" + (int)(pct*100) + "%)"));
        player.sendMessage(bar.toString());
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1){
            return StringUtil.copyPartialMatches(args[0], Arrays.asList("레벨","업그레이드","level","upgrade"), new ArrayList<>());
        }
        return Collections.emptyList();
    }
}
