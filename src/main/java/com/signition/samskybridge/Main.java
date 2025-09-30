package com.signition.samskybridge;
import com.signition.samskybridge.data.DataStore;
import com.signition.samskybridge.level.LevelService;
import com.signition.samskybridge.upgrade.UpgradeService;
import com.signition.samskybridge.gui.UpgradeGUI;
import com.signition.samskybridge.cmd.IslandCommand;
import com.signition.samskybridge.util.VaultHook;
import org.bukkit.plugin.java.JavaPlugin;
public class Main extends JavaPlugin {
    private static Main INSTANCE;
    private DataStore dataStore;
    private LevelService levelService;
    private UpgradeService upgradeService;
    private UpgradeGUI upgradeGUI;
    public static Main get(){ return INSTANCE; }
    @Override
    public void onEnable() {
        INSTANCE = this;
        saveDefaultConfig();
        this.dataStore = new DataStore();
        this.levelService = new LevelService(this, dataStore);
        this.upgradeService = new UpgradeService(levelService);
        this.upgradeGUI = new UpgradeGUI(this, levelService, upgradeService);
        VaultHook.init();
        getServer().getPluginManager().registerEvents(upgradeGUI, this);
        IslandCommand island = new IslandCommand(levelService, upgradeService, upgradeGUI);
        getCommand("island").setExecutor(island);
        getCommand("island").setTabCompleter(island);
        getLogger().info("SamSkyBridge enabled.");
    }
    @Override
    public void onDisable() {
        getLogger().info("SamSkyBridge disabled.");
    }
    public LevelService level(){ return levelService; }
    public UpgradeService upgrade(){ return upgradeService; }
    public DataStore store(){ return dataStore; }
}
