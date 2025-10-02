package com.minkang.ultimateroulette.gui;

import com.minkang.ultimateroulette.UltimateRoulette;
import com.minkang.ultimateroulette.data.KeyDef;
import com.minkang.ultimateroulette.data.Reward;
import com.minkang.ultimateroulette.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
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
    private static UltimateRoulette plugin;
    private static KeyDef def;

    public EditGUI(UltimateRoulette plugin, KeyDef def) {
        EditGUI.plugin = plugin;
        EditGUI.def = def;
    }

    public void open(Player p) {
        String title = plugin.getConfig().getString("titles.edit", "&a&l설정: %KEY%").replace("%KEY%", def.getName());
        Inventory inv = Bukkit.createInventory(null, 54, Text.color(title));
        redraw(inv);
        p.openInventory(inv);
    }

    private void redraw(Inventory inv) {
        inv.clear();
        int idx = 0;
        for (Reward r : def.getRewards()) {
            ItemStack it = r.getItem().clone();
            ItemMeta m = it.getItemMeta();
            List<String> lore = m.hasLore() ? m.getLore() : new ArrayList<>();
            lore.add(Text.color("&7가중치: &e" + r.getWeight()));
            lore.add(Text.color("&8좌클릭: +1  | 쉬프트+좌클릭: +10"));
            lore.add(Text.color("&8우클릭: -1  | 쉬프트+우클릭: -10"));
            lore.add(Text.color("&cQ(드롭): 제거"));
            m.setLore(lore);
            it.setItemMeta(m);
            inv.setItem(idx++, it);
            if (idx >= 45) break;
        }
        // Add button
        ItemStack add = new ItemStack(Material.ANVIL);
        ItemMeta am = add.getItemMeta();
        am.setDisplayName(Text.color("&b&l손에 든 아이템을 보상으로 추가"));
        List<String> al = new ArrayList<>();
        al.add(Text.color("&7기본 가중치 1로 추가됩니다."));
        am.setLore(al);
        add.setItemMeta(am);
        inv.setItem(49, add);
    }

    public static void handleClick(Player p, InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (!title.contains("설정")) return;
        if (e.getClickedInventory() == null) return;
        if (e.getCurrentItem() == null) return;

        int slot = e.getSlot();
        if (slot == 49) {
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == Material.AIR) {
                p.sendMessage(Text.color("&c손에 아이템을 들고 추가하세요."));
                return;
            }
            def.getRewards().add(new Reward(hand.clone(), 1));
            plugin.keys().save();
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            Inventory inv = e.getInventory();
            new EditGUI(plugin, def).redraw(inv);
            return;
        }

        if (slot < 45 && slot >= 0 && slot < def.getRewards().size()) {
            Reward r = def.getRewards().get(slot);
            ClickType ct = e.getClick();
            if (ct.isLeftClick()) {
                int delta = e.isShiftClick() ? 10 : 1;
                r.setWeight(r.getWeight() + delta);
            } else if (ct.isRightClick()) {
                int delta = e.isShiftClick() ? 10 : 1;
                r.setWeight(Math.max(0, r.getWeight() - delta));
            } else if (ct == ClickType.DROP) {
                def.getRewards().remove(slot);
            }
            plugin.keys().save();
            Inventory inv = e.getInventory();
            new EditGUI(plugin, def).redraw(inv);
        }
    }
}
