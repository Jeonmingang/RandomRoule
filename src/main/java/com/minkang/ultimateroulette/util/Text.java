package com.minkang.ultimateroulette.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.List;

// Bungee chat for JSON->legacy conversion (Spigot API provides this)
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;

public class Text {
    public static String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }

    private static boolean isJsonLine(String s){
        if (s == null) return false;
        String t = s.trim();
        return t.startsWith("{") && t.contains(""text""); // detect component JSON
    }

    private static String applyReplacements(String s){
        return s; // no replacements
    }

    private static String jsonToLegacy(String json){
        try {
            BaseComponent[] comps = ComponentSerializer.parse(json);
            return TextComponent.toLegacyText(comps);
        } catch (Throwable t) {
            return json; // fallback
        }
    }

    /** Sanitize display name and lore in-place. Returns true if changed. */
    public static boolean sanitize(ItemStack item){
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        boolean changed = false;

        if (meta.hasDisplayName()){
            String name = meta.getDisplayName();
            String fixed = isJsonLine(name) ? jsonToLegacy(name) : applyReplacements(name);
            if (!fixed.equals(name)){
                meta.setDisplayName(fixed);
                changed = true;
            }
        }
        if (meta.hasLore()){
            List<String> lore = meta.getLore();
            if (lore != null){
                List<String> out = new ArrayList<>(lore.size());
                boolean any=false;
                for (String line : lore){
                    String nl = isJsonLine(line) ? jsonToLegacy(line) : applyReplacements(line);
                    if (!nl.equals(line)) any=true;
                    out.add(nl);
                }
                if (any){
                    meta.setLore(out);
                    changed = true;
                }
            }
        }
        if (changed) item.setItemMeta(meta);
        return changed;
    }
}
