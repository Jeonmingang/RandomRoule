package com.signition.samskybridge;

import com.signition.samskybridge.listener.InstantRefreshListener;

import com.signition.samskybridge.cmd.IslandCommand;
import com.signition.samskybridge.data.DataStore;
import com.signition.samskybridge.level.LevelService;
import com.signition.samskybridge.listener.JoinListener;
import com.signition.samskybridge.listener.GuiListener;
import com.signition.samskybridge.listener.PortalBlocker;
import com.signition.samskybridge.rank.RankingService;
import com.signition.samskybridge.market.MarketService;
import com.signition.samskybridge.integration.BentoSync;
import com.signition.samskybridge.upgrade.UpgradeService;
import com.signition.samskybridge.util.Text;
import com.signition.samskybridge.util.VaultHook;
import org.bukkit.Bukkit;
import com.signition.samskybridge.chat.IslandChat;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    public IslandChat getChat(){ return chat; }
    private IslandChat chat;
private static Main inst;
    private DataStore dataStore;
    private LevelService levelService;
    private UpgradeService upgradeService;
    private RankingService rankingService;
    private VaultHook vault;
    private BentoSync bento;

    @Override
    public void onEnable(){
inst = this;
        saveDefaultConfig();
        saveResource("messages_ko.yml", false);

        this.vault = new VaultHook(this);
        this.bento = new BentoSync(this);
        this.dataStore = new DataStore(this);
this.levelService = new LevelService(this, dataStore);
        this.upgradeService = new UpgradeService(this, dataStore, levelService, vault);
        this.rankingService = new RankingService(this, dataStore, levelService);
        this.rankingService.rebuildRanks();
        getServer().getPluginManager().registerEvents(this.rankingService, this);
        getServer().getPluginManager().registerEvents(new InstantRefreshListener(this, this.rankingService), this);
        // periodic refresh to recover from other plugins overwriting tab
        getServer().getScheduler().
        getServer().getScheduler().runTaskTimer(this, () -> { try { this.rankingService.rebuildRanks(); } catch (Throwable ignore){} }, getConfig().getLong("rank-refresh-interval-ticks", 1200L), getConfig().getLong("rank-refresh-interval-ticks", 1200L));
runTaskTimer(this, () -> { try { this.rankingService.refreshTabAll(); } catch (Throwable ignore){} }, 20L*10, 20L*10);


        getCommand("섬").setExecutor(new IslandCommand(this, upgradeService, levelService, rankingService));

        Bukkit.getPluginManager().registerEvents(new GuiListener(this, upgradeService), this);
        Bukkit.getPluginManager().registerEvents(new PortalBlocker(this), this);
        Bukkit.getPluginManager().registerEvents(new JoinListener(this, dataStore, bento), this);

        long ticks = getConfig().getLong("rank-refresh-interval-ticks", 1200L);
        Bukkit.getScheduler().runTaskTimer(this, () -> rankingService.refreshRanking(), 60L, ticks);

        long saveTicks = getConfig().getLong("save-interval-ticks", 1200L);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> dataStore.save(), 100L, saveTicks);

        getLogger().info(Text.color("&aSamSkyBridge enabled."));
        // Island chat listener
        chat = new IslandChat(this, dataStore);
        getServer().getPluginManager().registerEvents(chat, this);
    
        // Register Upgrade GUI for XP purchase
    }


    @Override
    public void onDisable() {
        try { dataStore.save(); } catch (Exception ignored) {}
    }

    public static Main get(){ return inst; }
    public VaultHook getVault(){ return vault; }
    public BentoSync getBento(){ return bento; }
    public com.signition.samskybridge.data.DataStore getDataStore(){ return dataStore; }
// Open our XP Upgrade GUI if available; otherwise forward to BentoBox upgrades
private boolean openUpgradeOrForward(org.bukkit.entity.Player p){
    org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp =
        getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
    boolean guiOpened = false;
    if (getConfig().getBoolean("xp-purchase.enabled", true) && rsp != null){
        try {
            upgradeService.open(p);
            guiOpened = true;
        } catch (Throwable ignore) {}
    }
    if (!guiOpened){
        String base = getConfig().getString("player-command-base", "island");
        String[] tries = new String[] { base + " upgrades", base + " upgrade", base + " 업그레이드" };
        for (String t : tries) { if (p.performCommand(t)) return true; }
        p.sendMessage("§c업그레이드 애드온이 없거나 GUI를 열 수 없습니다.");
    }
    return true;
}

private void showLevelInfo(org.bukkit.entity.Player p){
    java.util.UUID id = p.getUniqueId();
    int level = levelService.getLevel(id);
    long cur = levelService.getCurrentXp(id);
    long next = levelService.getNextXpRequirement(level + 1);
    p.sendMessage("§a섬 레벨: §e" + level + " §7| §a경험치: §e" + cur + "§7/§e" + next);
}
}