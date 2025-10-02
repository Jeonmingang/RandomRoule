package com.signition.samskybridge.cmd;

import com.signition.samskybridge.Main;
import com.signition.samskybridge.data.IslandData;
import com.signition.samskybridge.level.LevelService;
import com.signition.samskybridge.rank.RankingService;
import com.signition.samskybridge.upgrade.UpgradeService;
import com.signition.samskybridge.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class IslandCommand implements CommandExecutor {
    private final Main plugin;
    private final UpgradeService upgrade;
    private final LevelService level;
    private final RankingService rank;

    public IslandCommand(Main plugin, UpgradeService upgrade, LevelService level, RankingService rank){
        this.plugin = plugin;
        this.upgrade = upgrade;
        this.level = level;
        this.rank = rank;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (!(sender instanceof Player)){
            sender.sendMessage("콘솔은 사용할 수 없습니다.");
            return true;
        }
        Player p = (Player) sender;
        String baseCmd = plugin.getConfig().getString("player-command-base", "island");

        if (args.length == 0){
            p.sendMessage(Text.color("&b/섬 업그레이드 &7- GUI 열기"));
            p.sendMessage(Text.color("&b/섬 레벨 &7- 레벨/경험치 보기"));
            p.sendMessage(Text.color("&b/섬 채팅 &7- 섬 채팅 토글"));
            p.sendMessage(Text.color("&b/섬 랭킹 &7- 상위 랭킹 보기"));
            p.sendMessage(Text.color("&b/섬 설정 리로드 &7- 설정 다시읽기"));
            p.sendMessage(Text.color("&b/섬 초대 <닉>, 수락, 탈퇴, 강퇴 <닉>, 승급 <닉>, 강등 <닉>"));
            return true;
        }

        String sub = args[0];

        switch (sub){
            case "업그레이드":
                upgrade.open(p);
                return true;

            case "레벨":
                showLevel(p);
                return true;

            case "채팅":
                boolean on = plugin.getChat().toggle(p);
                p.sendMessage(on ? "§a섬 채팅을 켰습니다." : "§c섬 채팅을 껐습니다.");
                return true;

            case "랭킹":
                int topN = plugin.getConfig().getInt("ranking.top-n", 10);
                rank.sendTop(p, topN);
                return true;

            case "설정":
                if (args.length >= 2 && "리로드".equals(args[1])){
                    plugin.reloadConfig();
                    plugin.getChat().reload();
                    p.sendMessage("§a설정을 다시 읽었습니다.");
                    return true;
                }
                break;

            case "초대":
                if (args.length >= 2){
                    Bukkit.dispatchCommand(p, baseCmd + " team invite " + args[1]);
                    return true;
                }
                p.sendMessage("§c사용법: /섬 초대 <닉>");
                return true;

            case "수락":
                Bukkit.dispatchCommand(p, baseCmd + " team accept");
                return true;

            case "탈퇴":
                Bukkit.dispatchCommand(p, baseCmd + " team leave");
                return true;

            case "강퇴":
                if (args.length >= 2){
                    Bukkit.dispatchCommand(p, baseCmd + " team kick " + args[1]);
                    return true;
                }
                p.sendMessage("§c사용법: /섬 강퇴 <닉>");
                return true;

            case "승급":
                if (args.length >= 2){
                    Bukkit.dispatchCommand(p, baseCmd + " team promote " + args[1]);
                    return true;
                }
                p.sendMessage("§c사용법: /섬 승급 <닉>");
                return true;

            case "강등":
                if (args.length >= 2){
                    Bukkit.dispatchCommand(p, baseCmd + " team demote " + args[1]);
                    return true;
                }
                p.sendMessage("§c사용법: /섬 강등 <닉>");
                return true;
        }

        p.sendMessage("§c알 수 없는 하위명령입니다.");
        return true;
    }

    private void showLevel(Player p){
        UUID id = p.getUniqueId();
        int lv = level.getLevel(id);
        long cur = level.getCurrentXp(id);
        long nextReq = level.getNextXpRequirement(lv + 1);
        long need = Math.max(0L, nextReq - cur);
        double pct = nextReq > 0 ? Math.min(1.0, (double)cur / (double)nextReq) : 0.0;

        int bars = 20;
        int filled = (int) Math.round(pct * bars);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < bars; i++) bar.append(i < filled ? "§a▮" : "§7▮");

        IslandData is = level.getIslandOf(p);
        int size = is != null ? is.getSize() : 0;
        int cap  = is != null ? is.getTeamMax() : 0;

        p.sendMessage(Text.color("&b[섬 레벨] &fLv.&e" + lv));
p.sendMessage(Text.color(String.format("&7경험치: &f%,d &7/ &f%,d &7( &a%d%%&7 )", cur, need, (int)(pct*100))));
        p.sendMessage(bar.toString());
        p.sendMessage(Text.color("&7섬 크기: &f" + size + " 블럭  &8|  &7인원수: &f" + cap + " 명"));
    }
}
