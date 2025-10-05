package com.minkang.ultimateroulette.pkg.gui;

import com.minkang.ultimateroulette.UltimateRoulette;
import com.minkang.ultimateroulette.pkg.PackageDef;
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

public class PackageEditGUI {
    private static UltimateRoulette plugin;
    private static PackageDef def;

    public PackageEditGUI(UltimateRoulette plugin, PackageDef def) {
        PackageEditGUI.plugin = plugin;
        PackageEditGUI.def = def;
    }

    public void open(Player p) {
        String title = Text.color("&a&l패키지 설정: " + def.getName());
        Inventory inv = Bukkit.createInventory(null, 54, title);
        redraw(inv);
        p.openInventory(inv);
    }

    private void redraw(Inventory inv) {
        inv.clear();
        int idx = 0;
        for (ItemStack it : def.getItems()) {
            ItemStack copy = it.clone();
            ItemMeta m = copy.getItemMeta();
            List<String> lore = m.hasLore() ? m.getLore() : new ArrayList<>();
            lore.add(Text.color("&7수량: &e" + copy.getAmount()));
            lore.add(Text.color("&8좌클릭: +1 | 쉬프트+좌클릭: +10"));
            lore.add(Text.color("&8우클릭: -1 | 쉬프트+우클릭: -10"));
            lore.add(Text.color("&cQ(드롭): 제거"));
            m.setLore(lore);
            copy.setItemMeta(m);
            Text.sanitize(copy); inv.setItem(idx++, copy);
            if (idx >= 45) break;
        }
        // add buttons
        ItemStack add = new ItemStack(Material.ANVIL);
        ItemMeta am = add.getItemMeta();
        am.setDisplayName(Text.color("&b&l아이템 추가"));
        List<String> al = new ArrayList<>();
        al.add(Text.color("&7좌클릭: 손에 든 아이템 1개 추가"));
        al.add(Text.color("&7쉬프트+좌클릭: 핫바(0~8) 전체 추가"));
        am.setLore(al);
        add.setItemMeta(am);
        inv.setItem(49, add);
    }

    public static void handleClick(Player p, InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (!title.contains("패키지 설정:")) return;
        if (e.getClickedInventory() == null || e.getCurrentItem() == null) return;

        int slot = e.getSlot();
        if (slot == 49) {
            if (e.isShiftClick()) {
                boolean any = false;
                for (int i=0; i<=8; i++) {
                    ItemStack it = p.getInventory().getItem(i);
                    if (it != null && it.getType() != Material.AIR) {
                        def.getItems().add(it.clone());
                        any = true;
                    }
                }
                if (!any) { p.sendMessage(Text.color("&c핫바에 아이템이 없습니다.")); return; }
            } else {
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand == null || hand.getType() == Material.AIR) { p.sendMessage(Text.color("&c손에 아이템을 들고 추가하세요.")); return; }
                def.getItems().add(hand.clone());
            }
            plugin.packages().save();
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            new PackageEditGUI(plugin, def).redraw(e.getInventory());
            return;
        }

        if (slot < 45 && slot >= 0 && slot < def.getItems().size()) {
            ItemStack it = def.getItems().get(slot);
            ClickType ct = e.getClick();
            int amt = it.getAmount();
            if (ct.isLeftClick()) {
                amt += (e.isShiftClick() ? 10 : 1);
            } else if (ct.isRightClick()) {
                amt -= (e.isShiftClick() ? 10 : 1);
                if (amt < 1) amt = 1;
            } else if (ct == ClickType.DROP) {
                def.getItems().remove(slot);
                plugin.packages().save();
                new PackageEditGUI(plugin, def).redraw(e.getInventory());
                return;
            }
            it.setAmount(Math.min(64, Math.max(1, amt)));
            plugin.packages().save();
            new PackageEditGUI(plugin, def).redraw(e.getInventory());
        }
    }
}
