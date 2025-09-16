package com.minkang.ultimate.random;
import org.bukkit.configuration.file.*;
import org.bukkit.inventory.ItemStack;
import java.io.*;
import java.util.*;
public class RouletteManager {
  private final Main plugin; private final File dataFile; private FileConfiguration data; private final Map<String,Roulette> map=new HashMap<>();
  public RouletteManager(Main plugin){ this.plugin=plugin; this.dataFile=new File(plugin.getDataFolder(),"roulettes.yml"); reload(); }
  public void reload(){
    map.clear();
    if(!dataFile.exists()){ try{ plugin.getDataFolder().mkdirs(); dataFile.createNewFile(); }catch(IOException ignored){} }
    data=YamlConfiguration.loadConfiguration(dataFile);
    for(String key: data.getKeys(false)){
      Object o=data.get(key);
      if(o instanceof Map){ map.put(key, Roulette.deserialize((Map<String,Object>)o)); }
    }
  }
  public void save(){ YamlConfiguration y=new YamlConfiguration(); for(Map.Entry<String,Roulette> e: map.entrySet()) y.set(e.getKey(), e.getValue().serialize()); try{ y.save(dataFile);}catch(IOException ignored){} }
  public boolean exists(String key){ return map.containsKey(key.toLowerCase()); }
  public Roulette create(String key){ key=key.toLowerCase(); Roulette r=new Roulette(key); map.put(key,r); save(); return r; }
  public boolean delete(String key){ key=key.toLowerCase(); Roulette removed=map.remove(key); save(); return removed!=null; }
  public Roulette get(String key){ return key==null?null:map.get(key.toLowerCase()); }
  public Collection<Roulette> all(){ return map.values(); }
  public void setTriggerItem(String key, ItemStack item){ Roulette r=get(key); if(r==null) return; r.setTriggerItem(item); save(); }
}