package com.minkang.ultimateroulette.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

import java.util.HashMap;
import java.util.Map;

@SerializableAs("RouletteReward")
public class Reward implements ConfigurationSerializable {
    private ItemStack item;
    private int weight;

    public Reward(ItemStack item, int weight) {
        this.item = item;
        this.weight = weight;
    }

    public ItemStack getItem() { return item; }
    public int getWeight() { return weight; }
    public void setWeight(int w) { this.weight = Math.max(0, w); }

    @Override public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("item", item);
        map.put("weight", weight);
        return map;
    }

    public static Reward deserialize(Map<String, Object> map) {
        ItemStack item = (ItemStack) map.get("item");
        int weight = (int) map.getOrDefault("weight", 1);
        return new Reward(item, weight);
    }

    public static Reward fromSection(ConfigurationSection sec) {
        ItemStack item = sec.getItemStack("item");
        int weight = sec.getInt("weight", 1);
        return new Reward(item, weight);
    }
}
