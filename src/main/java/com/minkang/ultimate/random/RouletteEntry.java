
package com.minkang.ultimate.random;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class RouletteEntry implements ConfigurationSerializable {
    private ItemStack item;
    private int weight;

    public RouletteEntry(ItemStack item, int weight) {
        this.item = item;
        this.weight = weight <= 0 ? 1 : weight;
    }

    public ItemStack getItem() { return item; }
    public void setItem(ItemStack item) { this.item = item; }
    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = Math.max(1, weight); }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("item", item);
        map.put("weight", weight);
        return map;
    }

    public static RouletteEntry deserialize(Map<String, Object> map) {
        ItemStack item = (ItemStack) map.get("item");
        int weight = 1;
        Object w = map.get("weight");
        if (w instanceof Number) weight = ((Number) w).intValue();
        return new RouletteEntry(item, weight);
    }
}
