package com.minkang.ultimate.random;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

import static com.minkang.ultimate.random.util.ItemIO.*;

public class RouletteEntry implements ConfigurationSerializable {
    private ItemStack item;
    private int weight;

    public RouletteEntry(ItemStack item, int weight) {
        this.item = item;
        this.weight = weight <= 0 ? 1 : weight;
    }

    public ItemStack getItem() { return item; }
    public int getWeight() { return weight; }
    public void setWeight(int w) { weight = Math.max(1, w); }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> m = new HashMap<>();
        // Base64 완전 보존 직렬화
        m.put("item64", toBase64(item));
        m.put("weight", weight);
        return m;
    }

    @SuppressWarnings("unchecked")
    public static RouletteEntry deserialize(Map<String, Object> m) {
        // 새 포맷
        Object s = m.get("item64");
        ItemStack it = null;
        if (s instanceof String) {
            it = fromBase64((String) s);
        }
        // 구버전 호환(Map 직렬화되어 ItemStack로 남아있을 수 있음)
        if (it == null) {
            Object legacy = m.get("item");
            if (legacy instanceof ItemStack) it = (ItemStack) legacy;
        }
        int w = 1;
        Object wObj = m.get("weight");
        if (wObj instanceof Number) w = Math.max(1, ((Number) wObj).intValue());
        return new RouletteEntry(it, w);
    }
}
