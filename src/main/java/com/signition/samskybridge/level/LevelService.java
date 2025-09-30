package com.signition.samskybridge.level;
import com.signition.samskybridge.Main;
import com.signition.samskybridge.data.DataStore;
import com.signition.samskybridge.data.IslandData;
import com.signition.samskybridge.util.Text;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.util.UUID;
public class LevelService {
    private final Main plugin;
    private final DataStore store;
    private long baseXp;
    private long perLevel;
    private double growth;
    private int maxLevel;
    private boolean notifyOnLevelUp;
    private String levelUpMsg;
    public LevelService(Main plugin, DataStore store) {
        this.plugin = plugin;
        this.store = store;
        reloadConfig();
    }
    public final void reloadConfig() {
        FileConfiguration c = plugin.getConfig();
        this.baseXp = c.getLong("level.base-xp", 1000L);
        this.perLevel = c.getLong("level.per-level", 500L);
        this.growth  = Math.max(1.0, c.getDouble("level.growth", 1.0));
        this.maxLevel = c.getInt("level.max-level", 1000);
        this.notifyOnLevelUp = c.getBoolean("level.notify-on-levelup", true);
        this.levelUpMsg = c.getString("level.messages.level-up", "&a섬 레벨이 &e<level>&a 레벨이 되었습니다!");
    }
    public IslandData getIslandOf(Player p){
        return store.getOrCreate(p.getUniqueId(), p.getName());
    }
    public void onPlace(Material m, Location l, Player p){}
    public void reloadBlocks(){}
    public long getNextXpRequirement(int nextLevel){
        if (nextLevel <= 1) return 0L;
        int k = nextLevel - 1;
        double inc = (baseXp + perLevel * k) * Math.pow(growth, Math.max(0, k - 1));
        long need = (long)Math.max(1L, Math.floor(inc));
        return need;
    }
    public long requiredXpForLevel(int level){
        return getNextXpRequirement(level);
    }
    public long getCurrentXp(UUID owner){
        try { return store.getOrCreate(owner, null).getXp(); } catch (Throwable ignore){}
        return 0L;
    }
    public int getLevel(UUID owner){
        try { return store.getOrCreate(owner, null).getLevel(); } catch (Throwable ignore){}
        return 1;
    }
    public void setLevel(UUID owner, int newLevel){
        IslandData is = store.getOrCreate(owner, null);
        int clamped = Math.max(1, Math.min(maxLevel, newLevel));
        is.setLevel(clamped);
    }
    public void addXp(Player player, long amount){
        if (player == null || amount <= 0) return;
        IslandData is = getIslandOf(player);
        addXpInternal(player, is, amount);
    }
    public void addXp(UUID owner, long amount){
        if (owner == null || amount <= 0) return;
        IslandData is = store.getOrCreate(owner, null);
        addXpInternal(null, is, amount);
    }
    public void applyXpPurchase(IslandData island, long amount){
        addXpInternal(null, island, amount);
    }
    private void addXpInternal(Player maybeOnline, IslandData is, long amount){
        if (is == null || amount <= 0) return;
        is.addXp(amount);
        while (is.getLevel() < maxLevel) {
            long need = getNextXpRequirement(is.getLevel() + 1);
            if (is.getXp() >= need) {
                is.setXp(is.getXp() - need);
                is.setLevel(is.getLevel() + 1);
                if (notifyOnLevelUp && maybeOnline != null) {
                    String msg = Text.color(levelUpMsg.replace("<level>", String.valueOf(is.getLevel())));
                    maybeOnline.sendMessage(msg);
                }
            } else break;
        }
    }
}
