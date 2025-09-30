package com.minkang.ultimate.random.pack;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import java.util.*;
public class PackageDef implements ConfigurationSerializable {
  private String name; private java.util.List<ItemStack> items=new java.util.ArrayList<>(); private ItemStack triggerItem;
  public PackageDef(String name){ this.name=name; }
  public String getName(){ return name; }
  public java.util.List<ItemStack> getItems(){ return items; }
  public void setItems(java.util.List<ItemStack> list){ this.items=list; }
  public ItemStack getTriggerItem(){ return triggerItem; }
  public void setTriggerItem(ItemStack it){ this.triggerItem=it; }
  public Map<String,Object> serialize(){ Map<String,Object> m=new HashMap<>(); m.put("name",name); m.put("items",items); if(triggerItem!=null) m.put("triggerItem", triggerItem); return m; }
  public static PackageDef deserialize(Map<String,Object> m){ PackageDef d=new PackageDef((String)m.get("name")); Object l=m.get("items"); if(l instanceof java.util.List){ for(Object o:(java.util.List)l){ if(o instanceof ItemStack) d.items.add((ItemStack)o); } } Object t=m.get("triggerItem"); if(t instanceof ItemStack) d.triggerItem=(ItemStack)t; return d; }
}