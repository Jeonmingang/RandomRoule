
package com.minkang.ultimate.random;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RouletteManager {
    private final Main plugin;
    private final File dataFile;
    private FileConfiguration data;
    private final Map<String, Roulette> map = new HashMap<String, Roulette>();

    public RouletteManager(Main plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "roulettes.yml");
        reload();
    }

    public void reload() {
        map.clear();
        if (!dataFile.exists()) {
            try { plugin.getDataFolder().mkdirs(); dataFile.createNewFile(); } catch (IOException ignored) {}
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : data.getKeys(false)) {
            Object o = data.get(key);
            if (o instanceof Map) {
                @SuppressWarnings("unchecked")
                Roulette r = Roulette.deserialize((Map<String, Object>) o);
                map.put(key, r);
            }
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<String, Roulette> e : map.entrySet()) {
            yml.set(e.getKey(), e.getValue().serialize());
        }
        try { yml.save(dataFile); } catch (IOException ignored) {}
    }

    public boolean exists(String key) { return map.containsKey(key.toLowerCase()); }

    public Roulette create(String key) {
        key = key.toLowerCase();
        Roulette r = new Roulette(key);
        map.put(key, r);
        save();
        return r;
    }

    public boolean delete(String key) {
        key = key.toLowerCase();
        Roulette removed = map.remove(key);
        save();
        return removed != null;
    }

    public Roulette get(String key) {
        if (key == null) return null;
        return map.get(key.toLowerCase());
    }

    public Collection<Roulette> all() { return map.values(); }

    public void setTriggerItem(String key, ItemStack item) {
        Roulette r = get(key);
        if (r == null) return;
        r.setTriggerItem(item);
        save();
    }
}
