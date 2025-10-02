package com.minkang.ultimateroulette.data;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class KeyDef {
    private final String name;
    private ItemStack keyItem; // 전용 아이템 (우클릭 시 미리보기/스핀)
    private final List<Reward> rewards = new ArrayList<>();

    public KeyDef(String name) { this.name = name; }

    public String getName() { return name; }
    public ItemStack getKeyItem() { return keyItem; }
    public void setKeyItem(ItemStack it) { this.keyItem = it; }
    public List<Reward> getRewards() { return rewards; }

    public int totalWeight() {
        return rewards.stream().mapToInt(Reward::getWeight).sum();
    }
}
