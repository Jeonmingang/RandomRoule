package com.minkang.ultimateroulette.gui;

import com.minkang.ultimateroulette.UltimateRoulette;
import com.minkang.ultimateroulette.data.KeyDef;
import com.minkang.ultimateroulette.data.Reward;
import com.minkang.ultimateroulette.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GuiListener implements Listener {
    private final UltimateRoulette plugin;

    public GuiListener(UltimateRoulette plugin) { this.plugin = plugin; }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();
        if (title == null) return;

        // Edit GUI (설정)
        if (title.contains("설정: ")) {
            e.setCancelled(true);
            String keyName = title.substring(title.indexOf("설정: ")+4).trim();
            KeyDef def = plugin.keys().get(keyName);
            if (def == null) return;

            int slot = e.getSlot();
            ClickType click = e.getClick();
            List<Reward> rewards = def.getRewards();

            if (slot == 49 && click.isLeftClick()) {
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand == null || hand.getType() == Material.AIR) { p.sendMessage(Text.color("&c손에 아이템을 들어주세요.")); return; }
                rewards.add(new Reward(hand.clone(), 1));
                plugin.keys().save();
                new EditGUI(plugin, def).open(p);
                return;
            }

            if (slot >= 0 && slot < 45 && slot < rewards.size()) {
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
                new EditGUI(plugin, def).open(p);
            }
            return;
        }

        // Preview GUI (미리보기)
        if (title.contains("미리보기: ")) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            ItemMeta im = clicked.getItemMeta();
            String dn = (im != null && im.hasDisplayName()) ? im.getDisplayName() : "";

            if (dn != null && dn.contains("뽑기 시작")) {
                // find key from title
                String keyName = title.substring(title.indexOf("미리보기: ")+6).trim();
                KeyDef def = plugin.keys().get(keyName);
                if (def == null || def.getRewards().isEmpty()) { p.sendMessage(Text.color("&c보상 풀이 비어있습니다.")); return; }
                // consume one key item: rely on command side; if 필요시 here check PDC
                new SpinGUI(plugin, def).open(p);
            }
            return;
        }

        // Claim GUI (보관함) - 보호
        if (title.contains("보관함")) {
            e.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        // no-op
    }
}
