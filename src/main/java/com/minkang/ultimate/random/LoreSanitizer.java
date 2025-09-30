package com.minkang.ultimate.random;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** GUI 표시용 '확률/가중치' 줄을 보상 아이템에서는 제거한다. */
public final class LoreSanitizer {
    private LoreSanitizer() {}
    private static final Pattern ADMIN_LINE = Pattern.compile(
        "(?i)^(?:§[0-9A-FK-OR])*\\s*(?:-|•)?\\s*(가중치|weight)\\s*[:：].*"
    );
    public static ItemStack strip(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return item;
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) return item;
        List<String> cleaned = new ArrayList<>(lore.size());
        for (String line : lore) {
            String plain = line == null ? "" : line.replaceAll("§[0-9A-FK-OR]", "");
            if (!ADMIN_LINE.matcher(plain).matches()) cleaned.add(line);
        }
        meta.setLore(cleaned);
        item.setItemMeta(meta);
        return item;
    }
}