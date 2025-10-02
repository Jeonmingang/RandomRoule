package com.minkang.ultimateroulette;

import com.minkang.ultimateroulette.cmd.RouletteCommand;
import com.minkang.ultimateroulette.data.KeyManager;
import com.minkang.ultimateroulette.data.Storage;
import com.minkang.ultimateroulette.gui.GuiListener;
import com.minkang.ultimateroulette.pkg.PackageManager;

import com.minkang.ultimateroulette.util.Text;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class UltimateRoulette extends JavaPlugin {

    private static UltimateRoulette inst;
    private KeyManager keyManager;
    private Storage storage;
    private PackageManager packageManager;
    private NamespacedKey keyTag;
    private NamespacedKey packageTag; // PDC tag to mark key items
    private BukkitTask autosaveTask;

    public static UltimateRoulette inst() { return inst; }
    public KeyManager keys() { return keyManager; }
    public Storage storage() { return storage; }
    public NamespacedKey keyTag() { return keyTag; }
    public NamespacedKey packageTag() { return packageTag; }
    public PackageManager packages() { return packageManager; }

    @Override public void onEnable() {
        inst = this;
        saveDefaultConfig();
        keyTag = new NamespacedKey(this, "roulette-key");
        packageTag = new NamespacedKey(this, "package-key");

        storage = new Storage(this);
        storage.loadAll();

        keyManager = new KeyManager(this);
        keyManager.load();
        packageManager = new PackageManager(this);
        packageManager.load();

        
        getCommand("roulette").setExecutor(new RouletteCommand(this));
        if (getCommand("package") != null) {
            com.minkang.ultimateroulette.pkg.cmd.PackageCommand pc = new com.minkang.ultimateroulette.pkg.cmd.PackageCommand(this);
            getCommand("package").setExecutor(pc);
}
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
int saveTicks = getConfig().getInt("storage.save_interval_ticks", 600);
        autosaveTask = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                storage.saveClaims();
                keyManager.save();
            } catch (Exception e) {
                getLogger().warning("Auto-save failed: " + e.getMessage());
            }
        }, saveTicks, saveTicks);

        getLogger().info(Text.color("&aUltimateRoulette enabled."));
    }

    @Override public void onDisable() {
        if (autosaveTask != null) autosaveTask.cancel();
        storage.saveClaims();
        keyManager.save();
        packageManager.save();
        getLogger().info(Text.color("&cUltimateRoulette disabled."));
    }
}
