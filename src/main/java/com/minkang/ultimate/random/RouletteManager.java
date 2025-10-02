package com.minkang.ultimate.random;

import com.minkang.ultimate.random.util.ItemIO;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RouletteManager {
    private final Main plugin;
    private final File dataFile;
    private YamlConfiguration data;
    private final Map<String, Roulette> map = new HashMap<>();

    public RouletteManager(Main plugin){
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "roulettes.yml");
        reload();
    }

    public void reload(){
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
            // entries
            List<RouletteEntry> out = new ArrayList<>();
            List<?> raw = sec.getList("entries");
            if (raw != null) {
                for (Object o : raw) {
                    if (o instanceof Map) {
                        Map<?,?> m = (Map<?,?>) o;
                        // item64 우선
                        ItemStack parsed = null;
                        Object s = m.get("item64");
                        if (s instanceof String) {
                            try { parsed = ItemIO.fromBase64((String) s); } catch (Exception ignored) {}
                        }
                        // 구버전(item: ItemStack)
                        if (parsed == null) {
                            Object legacy = m.get("item");
                            if (legacy instanceof ItemStack) parsed = (ItemStack) legacy;
                        }
                        if (parsed != null) {
                            int w = 1;
                            Object wObj = m.get("weight");
                            if (wObj instanceof Number) w = Math.max(1, ((Number) wObj).intValue());
                            out.add(new RouletteEntry(parsed, w));
                        }
                    }
                }
            }
            r.setEntries(out);
            ItemStack trig = sec.getItemStack("triggerItem");
            if (trig != null) r.setTriggerItem(trig);
            map.put(key.toLowerCase(), r);
        }
    }

    public void save(){
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<String, Roulette> e : map.entrySet()) {
            String key = e.getKey();
            Roulette r = e.getValue();
            ConfigurationSection sec = y.createSection(key);
            // entries -> item64/weight
            List<Map<String,Object>> list = new ArrayList<>();
            if (r.getEntries() != null) {
                for (RouletteEntry re : r.getEntries()) {
                    list.add(re.serialize());
                }
            }
            sec.set("entries", list);
            if (r.getTriggerItem() != null) sec.set("triggerItem", r.getTriggerItem());
        }
        try { y.save(dataFile); } catch (IOException ignored) {}
    }

    public boolean exists(String key){ return map.containsKey(key.toLowerCase()); }
    public Roulette create(String key){ key=key.toLowerCase(); Roulette r=new Roulette(key); map.put(key,r); save(); return r; }
    public boolean delete(String key){ key=key.toLowerCase(); Roulette removed=map.remove(key); save(); return removed!=null; }
    public Roulette get(String key){ return key==null?null:map.get(key.toLowerCase()); }
    public Collection<Roulette> all(){ return map.values(); }
    public void setTriggerItem(String key, ItemStack item){
        Roulette r = get(key);
        if (r==null) return;
        r.setTriggerItem(item);
        save();
    }
}
