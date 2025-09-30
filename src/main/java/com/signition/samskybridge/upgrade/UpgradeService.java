package com.signition.samskybridge.upgrade;
import com.signition.samskybridge.level.LevelService;
import com.signition.samskybridge.data.IslandData;
import com.signition.samskybridge.util.Text;
import com.signition.samskybridge.util.VaultHook;
import org.bukkit.entity.Player;
public class UpgradeService {
    private final LevelService level;
    private final int[] SIZE_STEPS = {100, 120, 130, 140, 150, 160};
    private final int[] MEMBER_STEPS = {2, 3, 4, 5, 6};
    public UpgradeService(LevelService level){
        this.level = level;
    }
    public int getProtectedSize(IslandData is){
        int tier = Math.max(0, is.getSizeTier());
        return SIZE_STEPS[Math.min(tier, SIZE_STEPS.length-1)];
    }
    public int getNextSize(IslandData is){
        int tier = Math.max(0, is.getSizeTier() + 1);
        return SIZE_STEPS[Math.min(tier, SIZE_STEPS.length-1)];
    }
    public int getSizeTier(IslandData is){ return is.getSizeTier(); }
    public int getRequiredLevelForSize(IslandData is){ return 10 + is.getSizeTier()*2; }
    public long getPriceForSize(IslandData is){ return 50000L + (long)is.getSizeTier()*15000L; }
    public boolean canUpgradeSize(IslandData is, Player p){
        if (is.getSizeTier() >= SIZE_STEPS.length-1){
            p.sendMessage(Text.color("&c최대 크기입니다.")); return false;
        }
        int needLv = getRequiredLevelForSize(is);
        if (level.getLevel(p.getUniqueId()) < needLv){
            p.sendMessage(Text.color("&c요구 레벨 &eLv." + needLv + "&c이 필요합니다.")); return false;
        }
        return true;
    }
    public void doUpgradeSize(IslandData is){ is.setSizeTier(is.getSizeTier()+1); }
    public int getMemberCap(IslandData is){
        int tier = Math.max(0, is.getMemberCapTier());
        return MEMBER_STEPS[Math.min(tier, MEMBER_STEPS.length-1)];
    }
    public int getNextMemberCap(IslandData is){
        int tier = Math.max(0, is.getMemberCapTier()+1);
        return MEMBER_STEPS[Math.min(tier, MEMBER_STEPS.length-1)];
    }
    public int getRequiredLevelForMemberCap(IslandData is){ return 8 + is.getMemberCapTier()*2; }
    public long getPriceForMemberCap(IslandData is){ return 40000L + (long)is.getMemberCapTier()*12000L; }
    public boolean canUpgradeMemberCap(IslandData is, Player p){
        if (is.getMemberCapTier() >= MEMBER_STEPS.length-1){
            p.sendMessage(Text.color("&c최대 인원수입니다.")); return false;
        }
        int needLv = getRequiredLevelForMemberCap(is);
        if (level.getLevel(p.getUniqueId()) < needLv){
            p.sendMessage(Text.color("&c요구 레벨 &eLv." + needLv + "&c이 필요합니다.")); return false;
        }
        return true;
    }
    public void doUpgradeMemberCap(IslandData is){ is.setMemberCapTier(is.getMemberCapTier()+1); }
    private boolean withdraw(Player p, long price){
        if (!VaultHook.hasEconomy()) return true;
        return VaultHook.withdraw(p, price);
    }
    public void buyExperience(Player player, long xp) {
        if (xp <= 0L || player == null) return;
        IslandData is = level.getIslandOf(player);
        level.applyXpPurchase(is, xp);
        player.sendMessage(Text.color("&a경험치 &f" + xp + "&a를 구매했습니다."));
    }
    public void tryUpgradeSize(Player p){
        IslandData is = level.getIslandOf(p);
        long price = getPriceForSize(is);
        if (!canUpgradeSize(is, p)) return;
        if (!withdraw(p, price)){
            p.sendMessage(Text.color("&c잔액이 부족합니다.")); return;
        }
        doUpgradeSize(is);
        p.sendMessage(Text.color("&a섬 크기를 업그레이드했습니다."));
    }
    public void tryUpgradeMemberCap(Player p){
        IslandData is = level.getIslandOf(p);
        long price = getPriceForMemberCap(is);
        if (!canUpgradeMemberCap(is, p)) return;
        if (!withdraw(p, price)){
            p.sendMessage(Text.color("&c잔액이 부족합니다.")); return;
        }
        doUpgradeMemberCap(is);
        p.sendMessage(Text.color("&a섬 인원수를 업그레이드했습니다."));
    }
}
