package com.minkang.ultimateroulette.gui;

import com.minkang.ultimateroulette.UltimateRoulette;
import com.minkang.ultimateroulette.data.KeyDef;
import com.minkang.ultimateroulette.data.Reward;
import com.minkang.ultimateroulette.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EditGUI {
    private final UltimateRoulette plugin;
    private final KeyDef def;
    private final Inventory inv;
    private static final int ADD_SLOT = 49;

    public EditGUI(UltimateRoulette plugin, KeyDef def) {
        this.plugin = plugin;
        this.def = def;
        this.inv = Bukkit.createInventory(null, 54, Text.color("&a&l설정: " + def.getName()));
    }

    public void open(Player p) {
        rebuild();
        p.openInventory(inv);
    }

    private void rebuild() {
        inv.clear();
        List<Reward> rewards = def.getRewards();
        int total = Math.max(1, rewards.stream().mapToInt(Reward::getWeight).sum());
        int idx = 0;
        for (Reward r : rewards) {
            if (idx >= 45) break;
            ItemStack it = (r.getItem() != null ? r.getItem().clone() : new ItemStack(Material.CHEST));
            ItemMeta m = it.getItemMeta();
            List<String> lore = m.hasLore() ? new ArrayList<>(m.getLore()) : new ArrayList<>();
            lore.add(Text.color("&7가중치: &f" + r.getWeight()));
            double pct = r.getWeight() * 100.0 / total;
            lore.add(Text.color("&7확률: &b" + String.format(Locale.US, "%.2f", pct) + "%"));
            lore.add(Text.color("&8좌(+1) 쉬프트좌(+10) 우(-1) 쉬프트우(-10) Q(삭제)"));
            m.setLore(lore);
            it.setItemMeta(m);
            inv.setItem(idx++, it);
        }
        // add button
        ItemStack add = new ItemStack(Material.LIME_WOOL);
        ItemMeta am = add.getItemMeta();
        am.setDisplayName(Text.color("&a&l보상 추가"));
        List<String> al = new ArrayList<>();
        al.add(Text.color("&7손에 든 아이템을 보상으로 추가합니다."));
        al.add(Text.color("&7기본 가중치: &f1"));
        am.setLore(al);
        add.setItemMeta(am);
        inv.setItem(ADD_SLOT, add);
    }

    public void onClick(Player p, InventoryClickEvent e) {
        if (!e.getView().getTitle().contains("설정: ")) return;
        e.setCancelled(true);
        int slot = e.getSlot();
        ClickType click = e.getClick();
        List<Reward> rewards = def.getRewards();

        if (slot == ADD_SLOT && click.isLeftClick()) {
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == Material.AIR) { p.sendMessage(Text.color("&c손에 아이템을 들어주세요.")); return; }
            rewards.add(new Reward(hand.clone(), 1));
            plugin.keys().save();
            rebuild(); p.updateInventory(); return;
        }

        if (slot < 45 && slot >= 0 && slot < rewards.size()) {
            Reward r = rewards.get(slot);
            switch (click) {
                case LEFT: r.setWeight(r.getWeight()+1); break;
                case SHIFT_LEFT: r.setWeight(r.getWeight()+10); break;
                case RIGHT: r.setWeight(Math.max(1, r.getWeight()-1)); break;
                case SHIFT_RIGHT: r.setWeight(Math.max(1, r.getWeight()-10)); break;
                case DROP: rewards.remove(r); break;
                default: break;
            }
            plugin.keys().save();
            rebuild(); p.updateInventory(); return;
        }
    }
}
