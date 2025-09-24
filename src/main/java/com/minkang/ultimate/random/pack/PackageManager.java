package com.minkang.ultimate.random.pack;
import com.minkang.ultimate.random.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import java.io.File;
import java.io.IOException;
import java.util.*;
public class PackageManager {
  private final Main plugin; private final File dataFile; private FileConfiguration data; private final Map<String,PackageDef> map=new HashMap<>();
  public PackageManager(Main plugin){ this.plugin=plugin; this.dataFile=new File(plugin.getDataFolder(),"packages.yml"); reload(); }
  public void reload(){
    map.clear();
    try{ if(!dataFile.exists()){ plugin.getDataFolder().mkdirs(); dataFile.createNewFile(); } }catch(IOException ignored){}
    data=YamlConfiguration.loadConfiguration(dataFile);
    for(String key: data.getKeys(false)){
      ConfigurationSection sec=data.getConfigurationSection(key); if(sec==null) continue;
      PackageDef def=new PackageDef(key);
      java.util.List<?> list=sec.getList("items"); if(list!=null){ java.util.List<ItemStack> items=new java.util.ArrayList<>(); for(Object o:list){ if(o instanceof ItemStack) items.add((ItemStack)o); } def.setItems(items); }
      ItemStack trig=sec.getItemStack("triggerItem"); if(trig!=null) def.setTriggerItem(trig);
      map.put(key.toLowerCase(), def);
    }
  }
  public void save(){
    YamlConfiguration y=new YamlConfiguration();
    for(Map.Entry<String,PackageDef> e: map.entrySet()){
      String key=e.getKey(); PackageDef d=e.getValue();
      y.set(key+".name", d.getName());
      y.set(key+".items", d.getItems());
      if(d.getTriggerItem()!=null) y.set(key+".triggerItem", d.getTriggerItem());
    }
    try{ y.save(dataFile);}catch(IOException ignored){} }
  public boolean exists(String name){ return map.containsKey(name.toLowerCase()); }
  public PackageDef create(String name){ name=name.toLowerCase(); PackageDef d=new PackageDef(name); map.put(name,d); save(); return d; }
  public boolean delete(String name){ name=name.toLowerCase(); PackageDef rm=map.remove(name); save(); return rm!=null; }
  public PackageDef get(String name){ return name==null?null:map.get(name.toLowerCase()); }
  public java.util.Collection<PackageDef> all(){ return map.values(); }
}