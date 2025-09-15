package com.minkang.ultimate.random;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import java.util.HashMap;
import java.util.Map;
public class RouletteEntry implements ConfigurationSerializable {
  private ItemStack item; private int weight;
  public RouletteEntry(ItemStack item,int weight){ this.item=item; this.weight=weight<=0?1:weight; }
  public ItemStack getItem(){ return item; }
  public int getWeight(){ return weight; }
  public void setWeight(int w){ weight=Math.max(1,w); }
  public Map<String,Object> serialize(){ Map<String,Object> m=new HashMap<>(); m.put("item",item); m.put("weight",weight); return m; }
  public static RouletteEntry deserialize(Map<String,Object> m){ return new RouletteEntry((ItemStack)m.get("item"), ((Number)m.getOrDefault("weight",1)).intValue()); }
}