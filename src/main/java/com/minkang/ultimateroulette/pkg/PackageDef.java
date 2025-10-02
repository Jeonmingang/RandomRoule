package com.minkang.ultimateroulette.pkg;

import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class PackageDef {
    private final String name;
    private ItemStack keyItem; // 전용 패키지 아이템
    private final List<ItemStack> items = new ArrayList<>(); // 지급될 아이템들

    public PackageDef(String name) { this.name = name; }

    public String getName() { return name; }
    public ItemStack getKeyItem() { return keyItem; }
    public void setKeyItem(ItemStack it) { this.keyItem = it; }
    public List<ItemStack> getItems() { return items; }
}
