package com.minkang.ultimateroulette.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** 홀더를 사용해 스핀 진행중 여부를 추적 (ESC로 닫기 방지용) */
public class SpinGUIHolder implements InventoryHolder {
    private boolean spinning = false;
    private Inventory inv;

    public boolean isSpinning() { return spinning; }
    public void setSpinning(boolean spinning) { this.spinning = spinning; }

    @Override
    public Inventory getInventory() { return inv; }
    public void setInventory(Inventory inv) { this.inv = inv; }
}