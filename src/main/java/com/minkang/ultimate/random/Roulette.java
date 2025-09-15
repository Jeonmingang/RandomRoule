package com.minkang.ultimate.random;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import java.util.*;
public class Roulette implements ConfigurationSerializable {
  private String key; private List<RouletteEntry> entries=new ArrayList<>(); private ItemStack triggerItem; private transient Random random=new Random();
  public Roulette(String key){ this.key=key; }
  public String getKey(){ return key; }
  public List<RouletteEntry> getEntries(){ return entries; }
  public void setEntries(List<RouletteEntry> e){ entries=e; }
  public ItemStack getTriggerItem(){ return triggerItem; }
  public void setTriggerItem(ItemStack i){ triggerItem=i; }
  public boolean isEmpty(){ return entries==null||entries.isEmpty(); }
  public int getTotalWeight(){ int s=0; for(RouletteEntry e:entries) s+=Math.max(1,e.getWeight()); return Math.max(1,s); }
  public RouletteEntry pickByWeight(){ int total=getTotalWeight(); int r=random.nextInt(total)+1; int cum=0; for(RouletteEntry e:entries){ cum+=Math.max(1,e.getWeight()); if(r<=cum) return e; } return entries.isEmpty()?null:entries.get(random.nextInt(entries.size())); }
  public Map<String,Object> serialize(){ Map<String,Object> m=new HashMap<>(); m.put("key",key); List<Map<String,Object>> l=new ArrayList<>(); for(RouletteEntry e:entries) l.add(e.serialize()); m.put("entries",l); if(triggerItem!=null) m.put("triggerItem",triggerItem); return m; }
  public static Roulette deserialize(Map<String,Object> m){ Roulette r=new Roulette((String)m.get("key")); Object list=m.get("entries"); if(list instanceof List){ for(Object o:(List)list){ r.entries.add(RouletteEntry.deserialize((Map)o)); } } Object t=m.get("triggerItem"); if(t instanceof ItemStack) r.triggerItem=(ItemStack)t; return r; }
}