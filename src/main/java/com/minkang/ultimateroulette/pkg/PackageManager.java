package com.minkang.ultimateroulette.pkg;

import com.minkang.ultimateroulette.UltimateRoulette;
import com.minkang.ultimateroulette.util.ItemIO;
import com.minkang.ultimateroulette.util.Text;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class PackageManager {
    private final UltimateRoulette plugin;
    private final Map<String, PackageDef> packs = new HashMap<>();
    private File file;
    private FileConfiguration conf;

    public PackageManager(UltimateRoulette plugin) {
        this.plugin = plugin;
        file = new File(plugin.getDataFolder(), "packages.yml");
        conf = new YamlConfiguration();
        conf.options().copyDefaults(true);
    }

    public void load() {
        packs.clear();
        if (!file.exists()) { save(); return; }
        try {
            conf.load(file);
            ConfigurationSection root = conf.getConfigurationSection("packages");
            if (root != null) {
                for (String k : root.getKeys(false)) {
                    ConfigurationSection sec = root.getConfigurationSection(k);
                    PackageDef def = new PackageDef(k);
                    if (sec.isItemStack("keyItem")) def.setKeyItem(sec.getItemStack("keyItem"));
                    List<ItemStack> list = new ArrayList<>();
                    if (sec.isList("items64")) {
                        for (Object o : sec.getList("items64")) if (o instanceof String) list.add(ItemIO.fromBase64((String)o));
                    } else if (sec.isList("items")) {
                        for (Object o : sec.getList("items")) if (o instanceof ItemStack) list.add((ItemStack)o);
                    }
                    def.getItems().addAll(list);
                    packs.put(k.toLowerCase(Locale.ROOT), def);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load packages.yml: " + e.getMessage());
        }
    }

    public void save() {
        try {
            conf = new YamlConfiguration();
            for (Map.Entry<String, PackageDef> e : packs.entrySet()) {
                String path = "packages." + e.getKey();
                PackageDef def = e.getValue();
                conf.set(path + ".keyItem", def.getKeyItem());
                List<String> enc = def.getItems().stream().map(ItemIO::toBase64).collect(Collectors.toList());
                conf.set(path + ".items64", enc);
            }
            conf.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save packages.yml: " + e.getMessage());
        }
    }

    public boolean create(String key) {
        String k = key.toLowerCase(Locale.ROOT);
        if (packs.containsKey(k)) return false;
        packs.put(k, new PackageDef(k));
        save(); return true;
    }

    public boolean delete(String key) {
        String k = key.toLowerCase(java.util.Locale.ROOT);
        if (packs.remove(k) != null) { save(); return true; }
        return false;
    }

    // Small trick for save in ternary
    private void saveDummy() {}
    private boolean save(boolean dummy) { save(); return true; }

    public PackageDef get(String key) { return packs.get(key.toLowerCase(Locale.ROOT)); }
    public java.util.List<String> list() { return new ArrayList<>(packs.keySet()); }

    public ItemStack buildKeyItemLore(ItemStack base, PackageDef def) {
        ItemStack copy = base.clone();
        ItemMeta meta = copy.getItemMeta();
        java.util.List<String> lore = new java.util.ArrayList<>();
        org.bukkit.configuration.file.FileConfiguration conf = plugin.getConfig();
        java.util.List<String> header = conf.getStringList("lore_templates.package.header");
        java.util.List<String> footer = conf.getStringList("lore_templates.package.footer");
        String lineT = conf.getString("lore_templates.package.line", "&7- &f%NAME%%COUNT%");
        for (String h : header) lore.add(Text.color(h.replace("%KEY%", def.getName())));
        if (def.getItems().isEmpty()) {
            lore.add(Text.color("&8(아이템 미설정)"));
        } else {
            int idx = 0;
            for (org.bukkit.inventory.ItemStack it : def.getItems()) {
                String name = (it.hasItemMeta() && it.getItemMeta().hasDisplayName()) ? it.getItemMeta().getDisplayName() : it.getType().name();
                String countStr = (it.getAmount()>1? " x"+it.getAmount() : "");
                String line = lineT.replace("%KEY%", def.getName())
                                   .replace("%INDEX%", String.valueOf(++idx))
                                   .replace("%NAME%", name)
                                   .replace("%COUNT%", countStr);
                lore.add(Text.color(line));
                if (lore.size() > 14) { lore.add(Text.color("&8...")); break; }
            }
        }
        for (String f : footer) lore.add(Text.color(f.replace("%KEY%", def.getName())));
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        // tag
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(plugin.packageTag(), PersistentDataType.STRING, def.getName());
        copy.setItemMeta(meta);
        return copy;
    }

    public void setKeyItem(String key, ItemStack hand) {
        PackageDef def = get(key);
        if (def == null) return;
        ItemStack withLore = buildKeyItemLore(hand, def);
        def.setKeyItem(withLore);
        save();
    }
}
