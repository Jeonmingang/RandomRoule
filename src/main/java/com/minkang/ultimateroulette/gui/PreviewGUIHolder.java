package com.minkang.ultimateroulette.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** 간단한 페이지/메타 보관용 홀더 */
public class PreviewGUIHolder implements InventoryHolder {
    private final String keyName;
    private int page;
    private Inventory inv;

    public PreviewGUIHolder(String keyName, int page) {
        this.keyName = keyName;
        this.page = Math.max(0, page);
    }

    public String getKeyName() { return keyName; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = Math.max(0, page); }

    @Override
    public Inventory getInventory() { return inv; }

    public void setInventory(Inventory inv) { this.inv = inv; }
}
