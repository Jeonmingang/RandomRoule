package com.minkang.ultimateroulette.gui;

import com.minkang.ultimateroulette.UltimateRoulette;
import com.minkang.ultimateroulette.data.KeyDef;
import com.minkang.ultimateroulette.data.Reward;
import com.minkang.ultimateroulette.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PreviewGUI {

    public static final int SLOTS_PER_PAGE = 45;
    public static final int PREV_SLOT = 45;
    public static final int START_SLOT = 49;
    public static final int NEXT_SLOT = 53;

    private final UltimateRoulette plugin;
    private final KeyDef def;
    private final int page;

    public PreviewGUI(UltimateRoulette plugin, KeyDef def) { this(plugin, def, 0); }
    public PreviewGUI(UltimateRoulette plugin, KeyDef def, int page) {
        this.plugin = plugin;
        this.def = def;
        this.page = Math.max(0, page);
    }

    public void open(Player p) {
        int total = Math.max(1, def.getRewards().size());
        int pages = (int) Math.ceil(total / (double) SLOTS_PER_PAGE);
        int cur = Math.min(page, Math.max(0, pages-1));

        String title = Text.color("&d&l미리보기: " + def.getName() + (pages>1 ? " &7(" + (cur+1) + "/" + pages + ")" : ""));
        Inventory inv = Bukkit.createInventory(new Holder(def, cur), 54, title);

        // fill items on current page
        int start = cur * SLOTS_PER_PAGE;
        int end = Math.min(total, start + SLOTS_PER_PAGE);
        int idx = 0;
        for (int i = start; i < end; i++) {
            Reward r = def.getRewards().get(i);
            ItemStack it = r.getItem().clone();
            ItemMeta m = it.getItemMeta();
            if (m != null) {
                // 확률 표시는 기존 데이터 훼손 없이 보조로만 추가 (선택)
                double pct = (def.totalWeight() > 0 ? (r.getWeight() * 100.0 / def.totalWeight()) : 0.0);
                List<String> lore = (m.hasLore() && m.getLore()!=null) ? new ArrayList<>(m.getLore()) : new ArrayList<>();
                lore.add(Text.color("&7확률: &b" + String.format(Locale.US, "%.2f", pct) + "%"));
                m.setLore(lore);
                it.setItemMeta(m);
            }
            Text.sanitize(it);
            inv.setItem(idx++, it);
        }

        // nav buttons
        ItemStack prev = new ItemStack(pages>1 && cur>0 ? Material.ARROW : Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta pm = prev.getItemMeta();
        if (pm != null) { pm.setDisplayName(Text.color(cur>0 ? "&e이전 페이지" : "&7이전 없음")); prev.setItemMeta(pm); }
        inv.setItem(PREV_SLOT, prev);

        ItemStack startBtn = new ItemStack(Material.LIME_WOOL);
        ItemMeta sm = startBtn.getItemMeta();
        if (sm != null) { sm.setDisplayName(Text.color("&a&l뽑기 시작")); startBtn.setItemMeta(sm); }
        inv.setItem(START_SLOT, startBtn);

        ItemStack next = new ItemStack(pages>1 && (cur+1)<pages ? Material.ARROW : Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta nm = next.getItemMeta();
        if (nm != null) { nm.setDisplayName(Text.color((cur+1)<pages ? "&e다음 페이지" : "&7다음 없음")); next.setItemMeta(nm); }
        inv.setItem(NEXT_SLOT, next);

        p.openInventory(inv);
    }

    public static class Holder implements InventoryHolder {
        public final String keyName;
        public final int page;
        public Holder(KeyDef def, int page) {
            this.keyName = def.getName();
            this.page = page;
        }
        @Override public Inventory getInventory() { return null; }
    }
}
