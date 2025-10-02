package com.minkang.ultimateroulette.data;

import com.minkang.ultimateroulette.UltimateRoulette;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import com.minkang.ultimateroulette.util.ItemIO;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Storage {
    private final UltimateRoulette plugin;

    private File claimFile;
    private FileConfiguration claimConf;

    public Storage(UltimateRoulette plugin) {
        this.plugin = plugin;
        claimFile = new File(plugin.getDataFolder(), "claims.yml");
        claimConf = new YamlConfiguration();
    }

    public void loadAll() {
        loadClaims();
    }

    public void loadClaims() {
        try {
            if (claimFile.exists()) claimConf.load(claimFile);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load claims.yml: " + e.getMessage());
        }
    }

    public void saveClaims() {
        try {
            claimConf.save(claimFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save claims.yml: " + e.getMessage());
        }
    }

    public List<ItemStack> getClaimList(UUID uuid) {
        java.util.List<?> raw = claimConf.getList("claims." + uuid.toString(), new ArrayList<>());
        java.util.List<ItemStack> out = new ArrayList<>();
        for (Object o : raw) {
            if (o instanceof String) {
                try { out.add(ItemIO.fromBase64((String)o)); } catch (Exception ignore) {}
            } else if (o instanceof ItemStack) {
                out.add((ItemStack)o);
            }
        }
        return out;
    }

    public void setClaimList(UUID uuid, List<ItemStack> items) {
        java.util.List<String> enc = new ArrayList<>();
        for (ItemStack it : items) enc.add(ItemIO.toBase64(it));
        claimConf.set("claims." + uuid.toString(), enc);
    }

    public void addClaim(UUID uuid, ItemStack item) {
        List<ItemStack> list = getClaimList(uuid);
        list.add(item);
        setClaimList(uuid, list);
    }
}
