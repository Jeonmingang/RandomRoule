package com.minkang.ultimate.random;

import org.bukkit.configuration.ConfigurationSection;
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
    private final Map<String, Roulette> map = new HashMap<>();

    public RouletteManager(Main plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "roulettes.yml");
        reload();
    }

    public void reload() {
        map.clear();
        try {
            if (!dataFile.exists()) {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            }
        } catch (IOException ignored) {}
        data = YamlConfiguration.loadConfiguration(dataFile);

        for (String key : data.getKeys(false)) {
            ConfigurationSection sec = data.getConfigurationSection(key);
            if (sec == null) continue;

            Roulette r = new Roulette(key);

            // Load entries
            List<?> rawList = sec.getList("entries");
            if (rawList != null) {
                List<RouletteEntry> out = new ArrayList<>();
                for (Object o : rawList) {
                    if (o instanceof Map) {
                        Map<?, ?> m = (Map<?, ?>) o;
                        Object itemObj = m.get("item");
                        if (itemObj instanceof ItemStack) {
                            ItemStack item = (ItemStack) itemObj;
                            int weight = 1;
                            Object wObj = m.get("weight");
                            if (wObj instanceof Number) weight = Math.max(1, ((Number) wObj).intValue());
                            out.add(new RouletteEntry(item, weight));
                        }
                    }
                }
                r.setEntries(out);
            }

            // Load trigger item
            ItemStack trigger = sec.getItemStack("triggerItem");
            if (trigger != null) r.setTriggerItem(trigger);

            map.put(key.toLowerCase(), r);
        }
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<String, Roulette> e : map.entrySet()) {
            String key = e.getKey();
            Roulette r = e.getValue();

            // Save as a proper section to ensure reload works across versions
            y.set(key + ".key", r.getKey());
            List<Map<String, Object>> list = new ArrayList<>();
            for (RouletteEntry re : r.getEntries()) list.add(re.serialize());
            y.set(key + ".entries", list);
            if (r.getTriggerItem() != null) y.set(key + ".triggerItem", r.getTriggerItem());
        }
        try { y.save(dataFile); } catch (IOException ignored) {}
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

    public Roulette get(String key) { return key == null ? null : map.get(key.toLowerCase()); }

    public Collection<Roulette> all() { return map.values(); }

    public void setTriggerItem(String key, ItemStack item) {
        Roulette r = get(key);
        if (r == null) return;
        r.setTriggerItem(item);
        save();
    }
}
