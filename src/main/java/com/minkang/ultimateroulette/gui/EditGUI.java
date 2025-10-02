package com.minkang.ultimateroulette.gui;

import com.minkang.ultimateroulette.UltimateRoulette;
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

public class EditGUI {
    private final UltimateRoulette plugin;
    private final KeyDef def;
    private final int page; // 0-based
    private final Inventory inv;

    public static final int SLOTS_PER_PAGE = 45;
    public static final int PREV_SLOT = 45;
    public static final int PAGE_SLOT = 49;
    public static final int NEXT_SLOT = 53;

    public EditGUI(UltimateRoulette plugin, KeyDef def) {
        this(plugin, def, 0);
    }

    public EditGUI(UltimateRoulette plugin, KeyDef def, int page) {
        this.plugin = plugin;
        this.def = def;
        this.page = Math.max(0, page);
        String title = String.format("&a&l설정: %s &8(&7p%d&8)", def.getName(), this.page+1);
        this.inv = Bukkit.createInventory(null, 54, Text.color(title));
    }

    public void open(Player p) {
        rebuild();
        p.openInventory(inv);
    }

    private ItemStack btn(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(Text.color(name));
        if (lore != null) m.setLore(lore);
        it.setItemMeta(m);
        return it;
    }

    public void rebuild() {
        inv.clear();
        List<Reward> rewards = def.getRewards();
        int total = Math.max(1, rewards.stream().mapToInt(Reward::getWeight).sum());

        int start = page * SLOTS_PER_PAGE;
        int end = Math.min(rewards.size(), start + SLOTS_PER_PAGE);
        int idx = 0;
        for (int i=start; i<end && idx< SLOTS_PER_PAGE; i++, idx++) {
            Reward r = rewards.get(i);
            ItemStack it = (r.getItem() != null ? r.getItem().clone() : new ItemStack(Material.CHEST));
            ItemMeta m = it.getItemMeta();
            List<String> lore = m.hasLore() ? new ArrayList<>(m.getLore()) : new ArrayList<>();
            lore.add(Text.color("&7가중치: &f" + r.getWeight()));
            double pct = r.getWeight() * 100.0 / total;
            lore.add(Text.color("&7확률: &b" + String.format(Locale.US, "%.2f", pct) + "%"));
            lore.add(Text.color("&8좌(+1) 쉬프트좌(+10) 우(-1) 쉬프트우(-10) Q(삭제)"));
            lore.add(Text.color("&8플레이어 인벤에서 드래그-드롭으로 보상 추가"));
            m.setLore(lore);
            it.setItemMeta(m);
            inv.setItem(idx, it);
        }

        // paging controls
        boolean hasPrev = page > 0;
        boolean hasNext = rewards.size() > (page+1) * SLOTS_PER_PAGE;

        inv.setItem(PREV_SLOT, btn(hasPrev?Material.ARROW:Material.GRAY_STAINED_GLASS_PANE,
                hasPrev? "&e이전 페이지" : "&7이전 페이지 없음",
                null));
        List<String> pl = new ArrayList<>();
        pl.add(Text.color("&7좌/우 클릭으로 페이지 이동"));
        inv.setItem(PAGE_SLOT, btn(Material.PAPER, "&b페이지: " + (page+1), pl));
        inv.setItem(NEXT_SLOT, btn(hasNext?Material.ARROW:Material.GRAY_STAINED_GLASS_PANE,
                hasNext? "&e다음 페이지" : "&7다음 페이지 없음",
                null));
    }

    public Inventory getInventory() { return inv; }
    public KeyDef getDef() { return def; }
    public int getPage() { return page; }
}
