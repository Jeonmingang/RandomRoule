package com.minkang.ultimateroulette.gui;

import com.minkang.ultimateroulette.data.KeyDef;
import com.minkang.ultimateroulette.data.Reward;
import com.minkang.ultimateroulette.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 미리보기 GUI (54칸) - 0~44 미리보기, 45=이전, 46=다음, 49=뽑기 시작
 * 나머지 기능/구조는 기존과 동일하게 유지.
 */
public class PreviewGUI {
    public static final int GUI_SIZE = 54;
    public static final int CONTENT_SIZE = 45; // 0..44
    public static final int PREV_SLOT = 45;
    public static final int NEXT_SLOT = 53;
    public static final int START_SLOT = 49;

    private final KeyDef def;

    public PreviewGUI(KeyDef def) { this.def = def; }

    public void open(Player p) { open(p, 0); }

    public void open(Player p, int page) {
        if (def == null) {
            p.sendMessage(Text.color("&c키 정보가 없습니다."));
            return;
        }
        int total = Math.max(1, (int)Math.ceil(def.getRewards().size() / (double)CONTENT_SIZE));
        page = Math.max(0, Math.min(page, total - 1));

        String title = Text.color("&6미리보기: &e" + def.getName() + " &7(p" + (page + 1) + "/" + total + ")");
        PreviewGUIHolder holder = new PreviewGUIHolder(def.getName(), page);
        Inventory inv = Bukkit.createInventory(holder, GUI_SIZE, title);
        holder.setInventory(inv);

        // 채우기
        fill(inv, page);

        // 버튼
        inv.setItem(PREV_SLOT, named(Material.ARROW, "&e이전 페이지"));
        inv.setItem(NEXT_SLOT, named(Material.ARROW, "&a다음 페이지"));
        inv.setItem(START_SLOT, named(Material.LIME_WOOL, "&a&l뽑기 시작"));

        p.openInventory(inv);
    }

    private void fill(Inventory inv, int page) {
        // 0~44 비우고 채우기
        for (int i = 0; i < CONTENT_SIZE; i++) inv.setItem(i, null);

        List<Reward> list = def.getRewards();
        int from = page * CONTENT_SIZE;
        int to = Math.min(from + CONTENT_SIZE, list.size());
        int totalW = Math.max(1, def.totalWeight());
        int slot = 0;
        for (int i = from; i < to; i++) {
            Reward r = list.get(i);
            ItemStack it = r.getItem().clone();
            ItemMeta m = it.getItemMeta();
            if (m != null) {
                List<String> lore = (m.getLore() == null ? new ArrayList<>() : new ArrayList<>(m.getLore()));
                double pct = (r.getWeight() * 100.0) / totalW;
                lore.add(Text.color("&7확률: &b" + String.format(Locale.US, "%.2f", pct) + "%"));
                m.setLore(lore);
                it.setItemMeta(m);
            }
            inv.setItem(slot++, it);
        }
    }

    private ItemStack named(Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName(Text.color(name));
            it.setItemMeta(m);
        }
        return it;
    }
}
