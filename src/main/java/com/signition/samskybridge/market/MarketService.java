
package com.signition.samskybridge.market;

import com.signition.samskybridge.Main;
import com.signition.samskybridge.data.DataStore;
import com.signition.samskybridge.data.IslandData;
import com.signition.samskybridge.util.VaultHook;
import com.signition.samskybridge.integration.BentoSync;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.*;

public class MarketService {
    private final Main plugin;
    private final DataStore store;
    private final VaultHook vault;
    private final BentoSync bento;
    private final File file;
    private final FileConfiguration data;

    public static class Listing {
        public java.util.UUID islandId;
        public String owner;
        public double price;
    }

    public MarketService(Main plugin, DataStore store, VaultHook vault, BentoSync bento){
        this.plugin = plugin;
        this.store = store;
        this.vault = vault;
        this.bento = bento;
        this.file = new File(plugin.getDataFolder(), "market.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public void save(){
        java.util.List<java.util.Map<String,Object>> all = new java.util.ArrayList<>();
        for (Listing l : getAll()){
            java.util.Map<String,Object> m = new java.util.HashMap<>();
            m.put("island", l.islandId.toString());
            m.put("owner", l.owner);
            m.put("price", l.price);
            all.add(m);
        }
        data.set("listings", all);
        try { data.save(file);} catch(Exception ignored){}
    }

    public java.util.List<Listing> getAll(){
        java.util.List<Listing> out = new java.util.ArrayList<>();
        for (Object o : data.getList("listings", java.util.Collections.emptyList())){
            if (!(o instanceof java.util.Map)) continue;
            java.util.Map<?,?> m = (java.util.Map<?,?>)o;
            try{
                Listing l = new Listing();
                l.islandId = java.util.UUID.fromString(String.valueOf(m.get("island")));
                l.owner = String.valueOf(m.get("owner"));
                l.price = Double.parseDouble(String.valueOf(m.get("price")));
                out.add(l);
            }catch(Exception ignored){}
        }
        return out;
    }

    public boolean register(Player owner, double price){
        IslandData is = store.getOrCreate(owner.getUniqueId(), owner.getName());
        if (!owner.getName().equalsIgnoreCase(is.getName())){
            owner.sendMessage("§c섬장만 매물을 등록할 수 있습니다.");
            return false;
        }
        Listing l = new Listing();
        l.islandId = is.getId();
        l.owner = owner.getName();
        l.price = price;

        java.util.List<java.util.Map<String,Object>> saved = new java.util.ArrayList<>();
        boolean replaced = false;
        for (Listing x : getAll()){
            if (x.islandId.equals(l.islandId)){
                if (!replaced){
                    java.util.Map<String,Object> m = new java.util.HashMap<>();
                    m.put("island", l.islandId.toString());
                    m.put("owner", l.owner);
                    m.put("price", l.price);
                    saved.add(m);
                    replaced = true;
                }
            } else {
                java.util.Map<String,Object> m = new java.util.HashMap<>();
                m.put("island", x.islandId.toString());
                m.put("owner", x.owner);
                m.put("price", x.price);
                saved.add(m);
            }
        }
        if (!replaced){
            java.util.Map<String,Object> m = new java.util.HashMap<>();
            m.put("island", l.islandId.toString());
            m.put("owner", l.owner);
            m.put("price", l.price);
            saved.add(m);
        }
        data.set("listings", saved);
        save();
        owner.sendMessage("§a섬이 매물로 등록되었습니다. 가격: §f"+(long)price);
        return true;
    }

    public void openGui(Player viewer, int page){
        java.util.List<Listing> ls = getAll();
        int size = 54;
        Inventory inv = Bukkit.createInventory(null, size, plugin.getConfig().getString("gui.market.title","섬 매물"));
        int start = (page-1)*45;
        int end = Math.min(ls.size(), start+45);
        for (int i=start;i<end;i++){
            Listing l = ls.get(i);
            IslandData is = store.getOrCreate(l.islandId, l.owner);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            OfflinePlayer op = Bukkit.getOfflinePlayer(l.owner);
            meta.setOwningPlayer(op);
            meta.setDisplayName("§a"+l.owner+"§7의 섬");
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add("§7가격: §f"+((long)l.price));
            lore.add("§7크기: §f"+is.getSize()+"  §7인원: §f"+is.getTeamMax());
            lore.add("§7레벨: §f"+is.getLevel()+"  §7경험치: §f"+is.getXp());
            lore.add("§8좌클릭: 구매  §8우클릭: 방문");
            meta.setLore(lore);
            head.setItemMeta(meta);
            int slot = (i-start);
            inv.setItem(slot, head);
        }
        viewer.openInventory(inv);
    }

    public void click(Player who, ItemStack item, boolean rightClick){
        if (item == null || item.getType() != Material.PLAYER_HEAD) return;
        ItemMeta im = item.getItemMeta();
        String name = im.getDisplayName().replace("§a","").replace("§7의 섬","").trim();
        Listing target = null;
        for (Listing l : getAll()){
            if (l.owner.equalsIgnoreCase(name)) { target = l; break; }
        }
        if (target == null){ who.sendMessage("§c해당 매물을 찾을 수 없습니다."); return; }
        if (rightClick){
            boolean ok = false;
            try { who.performCommand("is visit " + target.owner); ok = true; } catch(Throwable ignored){}
            if (!ok) try { who.performCommand("is warp " + target.owner); ok = true; } catch(Throwable ignored){}
            if (!ok) who.sendMessage("§7방문 명령이 지원되지 않습니다.");
            return;
        }
        if (!vault.withdraw(who.getName(), target.price)){
            who.sendMessage("§c잔액이 부족합니다.");
            return;
        }
        vault.deposit(target.owner, target.price);
        try { Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "is admin transfer " + target.owner + " " + who.getName()); } catch (Throwable ignored){}
        try { Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "is admin setowner " + who.getName()); } catch (Throwable ignored){}
        IslandData is = store.getOrCreate(target.islandId, target.owner);
        is.setName(who.getName());
        java.util.List<java.util.Map<String,Object>> left = new java.util.ArrayList<>();
        for (Listing l : getAll()){
            if (!l.islandId.equals(target.islandId)){
                java.util.Map<String,Object> m = new java.util.HashMap<>();
                m.put("island", l.islandId.toString());
                m.put("owner", l.owner);
                m.put("price", l.price);
                left.add(m);
            }
        }
        data.set("listings", left);
        save();
        who.sendMessage("§a섬을 구매했습니다! 새로운 섬장: "+who.getName());
    }
}
