
package com.signition.samskybridge.data;

import com.signition.samskybridge.Main;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Extremely simple persistent store for IslandData keyed by owner UUID.
 * Not optimized; sufficient for this plugin's needs.
 */
public class DataStore {
    private final Main plugin;
    private final File file;
    private final Map<UUID, IslandData> islands = new ConcurrentHashMap<>();

    public DataStore(Main plugin){
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        load();
    }

    public IslandData getIsland(java.util.UUID id) {
        return islands.get(id);
    }
    


    public synchronized IslandData getOrCreate(UUID id, String name){
        IslandData is = islands.get(id);
        if (is == null){
            is = new IslandData(id, name != null ? name : id.toString(), 1, 0L, 
                    plugin.getConfig().getInt("defaults.size", 120),
                    plugin.getConfig().getInt("defaults.team-max", 4));
            islands.put(id, is);
            save();
        } else if (name != null && !name.isEmpty()) {
            is.setName(name);
        }
        return is;
    }

    public synchronized List<IslandData> all(){
        return new ArrayList<>(islands.values());
    }

    public synchronized void put(IslandData is){
        islands.put(is.getId(), is);
        save();
    }

    public synchronized void save(){
        YamlConfiguration y = new YamlConfiguration();
        List<Map<String,Object>> list = islands.values().stream().map(is -> {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("id", is.getId().toString());
            m.put("name", is.getName());
            m.put("level", is.getLevel());
            m.put("xp", is.getXp());
            m.put("size", is.getSize());
            m.put("teamMax", is.getTeamMax());
            return m;
        }).collect(Collectors.toList());
        y.set("islands", list);
        try { y.save(file); } catch (IOException ignored){}
    }

    public synchronized void load(){
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        List<?> list = y.getList("islands");
        if (list == null) return;
        for (Object o : list){
            if (o instanceof Map){
                @SuppressWarnings("unchecked")
                Map<String,Object> m = (Map<String,Object>) o;
                try {
                    UUID id = UUID.fromString(String.valueOf(m.get("id")));
                    String name = String.valueOf(m.getOrDefault("name", id.toString()));
                    int level = ((Number)m.getOrDefault("level", 1)).intValue();
                    long xp = ((Number)m.getOrDefault("xp", 0L)).longValue();
                    int size = ((Number)m.getOrDefault("size", 120)).intValue();
                    int team = ((Number)m.getOrDefault("teamMax", 4)).intValue();
                    islands.put(id, new IslandData(id, name, level, xp, size, team));
                } catch (Exception ignored){}
            }
        }
    }
}
