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

/**
 * Level / XP service.
 * - Stores per-island (owner UUID) level and current XP (toward next level).
 * - Provides addXp() with auto level-up loop.
 * - Next level requirement is configurable via config.yml.
 */
public class LevelService {
    private final Main plugin;
    private final DataStore store;

    // Config-driven parameters (with sane defaults)
    private long baseXp;          // base XP needed for level 2
    private long perLevel;        // linear increment per next level
    private double growth;        // exponential growth multiplier per level (>=1.0)
    private int maxLevel;
    private boolean notifyOnLevelUp;
    private String levelUpMsg;

    public LevelService(Main plugin, DataStore store) {
        this.plugin = plugin;
        this.store = store;
        reloadConfig();
    }

    /** (Re)load tuning from config. */
    public final void reloadConfig() {
        FileConfiguration c = plugin.getConfig();
        this.baseXp = c.getLong("level.base-xp", 1000L);
        this.perLevel = c.getLong("level.per-level", 500L);
        this.growth  = Math.max(1.0, c.getDouble("level.growth", 1.0));
        this.maxLevel = c.getInt("level.max-level", 1000);
        this.notifyOnLevelUp = c.getBoolean("level.notify-on-levelup", true);
        this.levelUpMsg = c.getString("level.messages.level-up", "&a섬 레벨이 &e<level>&a 레벨이 되었습니다!");
    }

    /** Convenience: get or create IslandData for a player. */
    public IslandData getIslandOf(Player p){
        return store.getOrCreate(p.getUniqueId(), p.getName());
    }

    /** Block-based XP was removed; keep stubs for compatibility. */
    public void onPlace(Material m, Location l, Player p){ /* no-op */ }

    /** Compatibility stub: previously reloaded block XP tables. */
    public void reloadBlocks(){ /* no-op */ }

    /**
     * XP required to go from (nextLevel-1) -> nextLevel.
     * For nextLevel <= 1, returns 0.
     *
     * NOTE: Alias method {@link #requiredXpForLevel(int)} exists for older code.
     */
    public long getNextXpRequirement(int nextLevel){
        if (nextLevel <= 1) return 0L;
        int k = nextLevel - 1; // step index
        double inc = (baseXp + perLevel * k) * Math.pow(growth, Math.max(0, k - 1));
        long need = (long)Math.max(1L, Math.floor(inc));
        return need;
    }

    /**
     * Backward-compat alias used by UpgradeService/IslandCommand.
     * Semantics: XP needed to reach the given level from the previous one.
     */
    public long requiredXpForLevel(int level){
        return getNextXpRequirement(level);
    }

    /** Current XP toward next level for given island owner UUID. */
    public long getCurrentXp(UUID owner){
        try {
            IslandData is = store.getOrCreate(owner, null);
            return is.getXp();
        } catch (Throwable ignore) {}
        return 0L;
    }

    /** Get current level for island owner UUID (defaults to 1 on failure). */
    public int getLevel(UUID owner){
        try {
            IslandData is = store.getOrCreate(owner, null);
            return is.getLevel();
        } catch (Throwable ignore) {}
        return 1;
    }

    /** Set level directly (clamped), leaving XP as-is if under next requirement. */
    public void setLevel(UUID owner, int newLevel){
        IslandData is = store.getOrCreate(owner, null);
        int clamped = Math.max(1, Math.min(maxLevel, newLevel));
        is.setLevel(clamped);
    }

    /** Add XP to a Player's island and handle level-ups + notifications. */
    public void addXp(Player player, long amount){
        if (player == null || amount <= 0) return;
        IslandData is = getIslandOf(player);
        addXpInternal(player, is, amount);
    }

    /** Add XP by owner UUID (no chat notification, safe on offline). */
    public void addXp(UUID owner, long amount){
        if (owner == null || amount <= 0) return;
        IslandData is = store.getOrCreate(owner, null);
        addXpInternal(null, is, amount);
    }

    /**
     * Backward-compat API used by UpgradeService: when XP를 "구매"로 추가할 때 호출.
     * (오프라인 알림 없음)
     */
    public void applyXpPurchase(IslandData island, long amount){
        addXpInternal(null, island, amount);
    }

    private void addXpInternal(Player maybeOnline, IslandData is, long amount){
        if (is == null || amount <= 0) return;
        is.addXp(amount);

        // Level-up loop
        while (is.getLevel() < maxLevel) {
            long need = getNextXpRequirement(is.getLevel() + 1);
            if (is.getXp() >= need) {
                is.setXp(is.getXp() - need);
                is.setLevel(is.getLevel() + 1);
                if (notifyOnLevelUp && maybeOnline != null) {
                    String msg = Text.color(levelUpMsg.replace("<level>", String.valueOf(is.getLevel())));
                    maybeOnline.sendMessage(msg);
                }
            } else {
                break;
            }
        }
    }
}
