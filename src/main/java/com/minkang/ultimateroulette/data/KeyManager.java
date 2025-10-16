package com.minkang.ultimateroulette.data;

import com.minkang.ultimateroulette.UltimateRoulette;
import com.minkang.ultimateroulette.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemFlag;
import com.minkang.ultimateroulette.util.ItemIO;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class KeyManager {
    private final java.util.Map<String, Integer> keyVersion = new java.util.HashMap<>();
    private final UltimateRoulette plugin;
    private final Map<String, KeyDef> keys = new HashMap<>();
    private File file;
    private FileConfiguration conf;

    public KeyManager(UltimateRoulette plugin) {
        this.plugin = plugin;
        file = new File(plugin.getDataFolder(), "keys.yml");
        conf = new YamlConfiguration();
        conf.options().copyDefaults(true);
    }

    public void load() {
        keys.clear();
        if (!file.exists()) {
            save();
            return;
        }
        try {
            conf.load(file);
            ConfigurationSection root = conf.getConfigurationSection("keys");
            if (root != null) {
                for (String k : root.getKeys(false)) {
                    ConfigurationSection sec = root.getConfigurationSection(k);
                    KeyDef def = new KeyDef(k);
                    if (sec.isItemStack("keyItem")) def.setKeyItem(sec.getItemStack("keyItem"));
                    ConfigurationSection rs = sec.getConfigurationSection("rewards");
                    if (rs != null) {
                        for (String i : rs.getKeys(false)) {
                            org.bukkit.configuration.ConfigurationSection rsec = rs.getConfigurationSection(i);
                            org.bukkit.inventory.ItemStack item;
                            if (rsec.isString("item64")) item = ItemIO.fromBase64(rsec.getString("item64"));
                            else item = rsec.getItemStack("item");
                            int weight = rsec.getInt("weight", 1);
                            def.getRewards().add(new Reward(item, weight));
                        }
                    }
                    keys.put(k.toLowerCase(Locale.ROOT), def);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load keys.yml: " + e.getMessage());
        }
    }

    public void save() {
        try {
            conf = new YamlConfiguration();
            for (Map.Entry<String, KeyDef> e : keys.entrySet()) {
                String path = "keys." + e.getKey();
                KeyDef def = e.getValue();
                conf.set(path + ".keyItem", def.getKeyItem());
                int idx = 0;
                for (Reward r : def.getRewards()) {
                    String rp = path + ".rewards." + (idx++);
                    conf.set(rp + ".item64", ItemIO.toBase64(r.getItem()));
                    conf.set(rp + ".weight", r.getWeight());
                }
            }
            conf.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save keys.yml: " + e.getMessage());
        }
    }

    public boolean create(String key) {
        String k = key.toLowerCase(Locale.ROOT);
        if (keys.containsKey(k)) return false;
        keys.put(k, new KeyDef(k));
        save();
        return true;
    }

    public boolean delete(String key) {
        String k = key.toLowerCase(Locale.ROOT);
        if (keys.remove(k) != null) { save(); return true; }
        return false;
    }

    public KeyDef get(String key) {
        return keys.get(key.toLowerCase(Locale.ROOT));
    }

    public List<String> list() {
        return new ArrayList<>(keys.keySet());
    }

    public ItemStack buildKeyItemLore(org.bukkit.inventory.ItemStack base, KeyDef def) {
        org.bukkit.inventory.ItemStack out = base.clone();
        org.bukkit.inventory.meta.ItemMeta meta = out.getItemMeta();
        java.util.List<String> lore = new java.util.ArrayList<>();

        // Header from config (optional)
        org.bukkit.configuration.file.FileConfiguration gconf = plugin.getConfig();
        java.util.List<String> header = gconf.getStringList("lore_templates.roulette.header");
        if (header != null) {
            for (String h : header) lore.add(com.minkang.ultimateroulette.util.Text.color(h.replace("%KEY%", def.getName())));
        }

        // Rewards summary (cap to 15 lines)
        int total = Math.max(1, def.getRewards().stream().mapToInt(com.minkang.ultimateroulette.data.Reward::getWeight).sum());
        java.util.List<String> linesT = gconf.getStringList("lore_templates.roulette.lines");
        String lineT = (linesT != null && !linesT.isEmpty()) ? linesT.get(0) : "&7- %NAME%%COUNT% (&f%PERCENT%%%&7)";

        for (com.minkang.ultimateroulette.data.Reward r : def.getRewards()) {
            org.bukkit.inventory.ItemStack it = r.getItem() != null ? r.getItem().clone() : new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHEST);
            String name = (it.hasItemMeta() && it.getItemMeta().hasDisplayName()) ? it.getItemMeta().getDisplayName() : it.getType().name();
            double pct = (r.getWeight() * 100.0) / total;
            String countStr = (it.getAmount() > 1 ? " x" + it.getAmount() : "");
            String line = lineT.replace("%KEY%", def.getName())
                               .replace("%NAME%", name)
                               .replace("%COUNT%", countStr)
                               .replace("%PERCENT%", String.format(java.util.Locale.US, "%.2f", pct));
            lore.add(com.minkang.ultimateroulette.util.Text.color(line));
            if (lore.size() > 14) { lore.add(com.minkang.ultimateroulette.util.Text.color("&8...")); break; }
        }

        // Footer
        java.util.List<String> footer = gconf.getStringList("lore_templates.roulette.footer");
        if (footer != null) {
            for (String f : footer) lore.add(com.minkang.ultimateroulette.util.Text.color(f.replace("%KEY%", def.getName())));
        }

        meta.setLore(lore);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);

        // Tag with key name
        org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(plugin.keyTag(), org.bukkit.persistence.PersistentDataType.STRING, def.getName());
        out.setItemMeta(meta);
        return out;
    }


        private int getKeyVersion(String key) {
        Integer v = keyVersion.get(key);
        return v == null ? 0 : v;
    }

public void setKeyItem(String key, org.bukkit.inventory.ItemStack hand) {
        keyVersion.put(key, getKeyVersion(key) + 1);
        KeyDef def = get(key);
        if (def == null) return;
        org.bukkit.inventory.ItemStack withLore = buildKeyItemLore(hand, def);
        def.setKeyItem(withLore);
        save();
    }

}