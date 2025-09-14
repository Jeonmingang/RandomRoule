
package com.minkang.ultimate.random;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Roulette implements ConfigurationSerializable {
    private String key;
    private List<RouletteEntry> entries = new ArrayList<RouletteEntry>();
    private ItemStack triggerItem;

    private transient Random random = new Random();

    public Roulette(String key) { this.key = key; }
    public String getKey() { return key; }
    public List<RouletteEntry> getEntries() { return entries; }
    public void setEntries(List<RouletteEntry> entries) { this.entries = entries; }
    public ItemStack getTriggerItem() { return triggerItem; }
    public void setTriggerItem(ItemStack triggerItem) { this.triggerItem = triggerItem; }
    public boolean isEmpty() { return entries == null || entries.isEmpty(); }

    public int getTotalWeight() {
        int sum = 0;
        for (RouletteEntry e : entries) sum += Math.max(1, e.getWeight());
        return Math.max(1, sum);
    }

    public RouletteEntry pickByWeight() {
        int total = getTotalWeight();
        int r = random.nextInt(total) + 1;
        int cum = 0;
        for (RouletteEntry e : entries) {
            cum += Math.max(1, e.getWeight());
            if (r <= cum) return e;
        }
        if (entries.isEmpty()) return null;
        return entries.get(random.nextInt(entries.size()));
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("key", key);
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (RouletteEntry e : entries) list.add(e.serialize());
        map.put("entries", list);
        if (triggerItem != null) map.put("triggerItem", triggerItem);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static Roulette deserialize(Map<String, Object> map) {
        String key = (String) map.get("key");
        Roulette r = new Roulette(key);
        Object listObj = map.get("entries");
        if (listObj instanceof List) {
            List<Object> list = (List<Object>) listObj;
            for (Object o : list) {
                if (o instanceof Map) {
                    RouletteEntry e = RouletteEntry.deserialize((Map<String, Object>) o);
                    r.entries.add(e);
                }
            }
        }
        Object trig = map.get("triggerItem");
        if (trig instanceof ItemStack) r.triggerItem = (ItemStack) trig;
        return r;
    }
}
