package com.minkang.ultimateroulette.gui;

import com.minkang.ultimateroulette.UltimateRoulette;
import com.minkang.ultimateroulette.data.KeyDef;
import com.minkang.ultimateroulette.data.Reward;
import com.minkang.ultimateroulette.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class SpinGUI {
    public static final int[] TRACK_SLOTS = {9,10,11,12,13,14,15,16,17};
    public static final int TOP_CENTER = 4;
    public static final int BOTTOM_CENTER = 22;

    private final UltimateRoulette plugin;
    private final KeyDef def;
    private Inventory inv;

    public SpinGUI(UltimateRoulette plugin, KeyDef def) {
        this.plugin = plugin;
        this.def = def;
    }

    public static boolean isSpinTitle(String title) {
        if (title == null) return false;
        String plain = ChatColor.stripColor(title);
        return plain != null && plain.startsWith("룰렛 스핀:");
    }

    private String title() {
        return Text.color("&6&l룰렛 스핀: &f" + def.getName());
    }

    public void open(Player p) {
        SpinGUIHolder holder = new SpinGUIHolder();
        inv = Bukkit.createInventory(holder, 27, title());
        holder.setInventory(inv);
        // Markers
        ItemStack mark = new ItemStack(Material.HOPPER);
        ItemMeta mm = mark.getItemMeta();
        if (mm != null) {
            mm.setDisplayName(Text.color("&7▼"));
            mark.setItemMeta(mm);
        }
        inv.setItem(TOP_CENTER, mark);
        inv.setItem(BOTTOM_CENTER, mark);

        // Fill others with glass to prevent pickup feel
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        if (fm != null) {
            fm.setDisplayName(Text.color("&8"));
            filler.setItemMeta(fm);
        }
        for (int i=0;i<27;i++) {
            if (i==TOP_CENTER || i==BOTTOM_CENTER) continue;
            inv.setItem(i, filler);
        }

        p.openInventory(inv);
        startSpin(p);
    }

    private void startSpin(final Player p) {
        // 스핀 시작 표시 (ESC 방지)
        try { ((SpinGUIHolder) inv.getHolder()).setSpinning(true);} catch (Throwable ignored) {}
        // Build weighted reel
        List<Reward> rewards = def.getRewards();
        List<Reward> reel = new ArrayList<Reward>();
        int totalW = 0;
        for (Reward r : rewards) {
            int w = Math.max(0, r.getWeight());
            totalW += w;
            for (int i=0;i<w;i++) reel.add(r);
        }
        if (reel.isEmpty()) {
            // fallback
            Reward dummy = new Reward(new ItemStack(Material.BARRIER), 1);
            reel.add(dummy);
            totalW = 1;
        }
        // Precompute display items with probability overlay (only for spin GUI, not final item)
        List<ItemStack> reelItems = new ArrayList<ItemStack>(reel.size());
        for (Reward r : reel) {
            ItemStack base = (r.getItem()!=null ? r.getItem().clone() : new ItemStack(Material.CHEST));
            ItemMeta m = base.getItemMeta();
            if (m != null) {
                List<String> lore = (m.hasLore() ? new ArrayList<String>(m.getLore()) : new ArrayList<String>());
                double pct = totalW>0 ? (r.getWeight()*100.0/totalW) : 0.0;
                lore.add(Text.color("&7확률: &b" + String.format(Locale.US, "%.2f", pct) + "%"));
                m.setLore(lore);
                base.setItemMeta(m);
            }
            reelItems.add(base);
        }

        final int size = reelItems.size();
        final int[] idx = { new Random().nextInt(size) };
        final int baseSteps = Math.max(40, size*3);
        final int slowSteps = 40;
        final int totalSteps = baseSteps + slowSteps;
        final int[] step = {0};
        final int[] interval = {1}; // shift every 'interval' ticks
        final int[] tick = {0};

        new BukkitRunnable() {
            @Override public void run() {
                if (inv == null || !SpinGUI.isSpinTitle(p.getOpenInventory().getTitle())) { cancel(); return; }

                // Deceleration: after baseSteps, gradually increase interval
                if (step[0] > baseSteps) {
                    if ((step[0]-baseSteps) % 8 == 0 && interval[0] < 5) interval[0]++;
                }

                if (tick[0] % interval[0] == 0) {
                    // shift window render
                    for (int i=0;i<TRACK_SLOTS.length;i++) {
                        int reelIndex = (idx[0] + i) % size;
                        inv.setItem(TRACK_SLOTS[i], reelItems.get(reelIndex));
                    }
                    idx[0] = (idx[0] + 1) % size;
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.6f);
                }
                tick[0]++;
                step[0]++;

                if (step[0] >= totalSteps) {
                    this.cancel();
                    // Center item: TRACK_SLOTS[4] corresponds to reel index (idx -1 + 4)
                    int centerReelIndex = ( (idx[0] - 1 + 4) % size + size ) % size;
                    Reward win = reel.get(centerReelIndex);
                    ItemStack prize = (win.getItem()!=null ? win.getItem().clone() : new ItemStack(org.bukkit.Material.CHEST));
                    plugin.storage().addClaim(p.getUniqueId(), prize);
                    // === Broadcast if this is a lowest-probability reward ===
                    try {
                        boolean bcEnabled = plugin.getConfig().getBoolean("broadcast.lowest_chance.enabled", false);
                        if (bcEnabled) {
                            // find min and max weight among rewards for this key
                            int minW = Integer.MAX_VALUE;
                            int maxW = 0;
                            for (Reward rwd : def.getRewards()) {
                                int w = Math.max(0, rwd.getWeight());
                                if (w == 0) continue;
                                if (w < minW) minW = w;
                                if (w > maxW) maxW = w;
                            }
                            // Only broadcast if there exists a strict rarity (min < max),
                            // and the winning reward's weight equals the minimum.
                            int wWin = Math.max(0, win.getWeight());
                            if (minW != Integer.MAX_VALUE && minW < maxW && wWin == minW) {
                                String raw = plugin.getConfig().getString(
                                    "broadcast.lowest_chance.message",
                                    "&6[랜덤룰렛] &e{player}&7님이 &d{key}&7 랜덤 뽑기에서 &b{chance}%&7의 확률을 뚫고 &a{item}&7을 얻었습니다!"
                                );
                                String itemName;
                                if (prize.hasItemMeta() && prize.getItemMeta().hasDisplayName()) {
                                    itemName = prize.getItemMeta().getDisplayName();
                                } else {
                                    itemName = prize.getType().name();
                                }
                                
                                // Compute chance percent for the winning reward
                                int totalWgt = 0;
                                for (Reward rw : def.getRewards()) {
                                    totalWgt += Math.max(0, rw.getWeight());
                                }
                                double chancePct = (totalWgt > 0 ? (wWin * 100.0 / totalWgt) : 0.0);
                                String chanceStr = String.format(java.util.Locale.US, "%.2f", chancePct);
String out = raw
                                    .replace("{player}", p.getName())
                                    .replace("{key}", def.getName())
                                    .replace("{item}", itemName).replace("{chance}", chanceStr);
                                Bukkit.getServer().broadcastMessage(Text.color(out));
                                // Optional broadcast sound to everyone
                                boolean sndEnabled = plugin.getConfig().getBoolean("broadcast.lowest_chance.sound.enabled", true);
                                if (sndEnabled) {
                                    String sndName = plugin.getConfig().getString("broadcast.lowest_chance.sound.name", "ENTITY_PLAYER_LEVELUP");
                                    double v = plugin.getConfig().getDouble("broadcast.lowest_chance.sound.volume", 1.0);
                                    double pch = plugin.getConfig().getDouble("broadcast.lowest_chance.sound.pitch", 1.2);
                                    org.bukkit.Sound snd;
                                    try { snd = org.bukkit.Sound.valueOf(sndName); } catch (IllegalArgumentException ex) { snd = org.bukkit.Sound.ENTITY_PLAYER_LEVELUP; }
                                    for (org.bukkit.entity.Player op : org.bukkit.Bukkit.getOnlinePlayers()) {
                                        op.playSound(op.getLocation(), snd, (float)v, (float)pch);
                                    }
                                }
                            }
                        }
                    } catch (Throwable ignored) {}

                    
                    // --- Lowest chance broadcast (1.16.5 compatible) ---
                    try {
                        boolean bcEnabled = plugin.getConfig().getBoolean("broadcast.lowest_chance.enabled", true);
                        if (bcEnabled && def != null && def.getRewards() != null && !def.getRewards().isEmpty()) {
                            int totalWgt = 0;
                            int minW = Integer.MAX_VALUE;
                            for (Reward rw : def.getRewards()) {
                                int w = Math.max(0, rw.getWeight());
                                totalWgt += w;
                                if (w > 0 && w < minW) minW = w;
                            }
                            int winW = (win != null ? Math.max(0, win.getWeight()) : 0);
                            if (minW != Integer.MAX_VALUE && winW == minW) {
                                double chancePct = (totalWgt > 0 ? (winW * 100.0 / totalWgt) : 0.0);
                                String chanceStr = String.format(java.util.Locale.US, "%.2f", chancePct);
                                String raw = plugin.getConfig().getString(
                                    "broadcast.lowest_chance.message",
                                    "&6[랜덤룰렛] &e{player}&7님이 &d{key}&7 랜덤 뽑기에서 &b{chance}%&7의 확률을 뚫고 &a{item}&7을 얻었습니다! &a축하드립니다!"
                                );
                                String keyName = (def.getName() != null ? def.getName() : "알 수 없음");
                                String itemName = (win != null && win.getItem() != null && win.getItem().hasItemMeta() && win.getItem().getItemMeta().hasDisplayName())
                                        ? win.getItem().getItemMeta().getDisplayName()
                                        : (win != null && win.getItem() != null ? win.getItem().getType().name() : "아이템");
                                String out = raw.replace("{player}", p.getName())
                                                .replace("{key}", keyName)
                                                .replace("{item}", itemName)
                                                .replace("{chance}", chanceStr);
                                Bukkit.getServer().broadcastMessage(Text.color(out));

                                boolean sndEnabled = plugin.getConfig().getBoolean("broadcast.lowest_chance.sound.enabled", true);
                                if (sndEnabled) {
                                    String sndName = plugin.getConfig().getString("broadcast.lowest_chance.sound.name", "ENTITY_PLAYER_LEVELUP");
                                    double v = plugin.getConfig().getDouble("broadcast.lowest_chance.sound.volume", 1.0);
                                    double pc = plugin.getConfig().getDouble("broadcast.lowest_chance.sound.pitch", 1.2);
                                    org.bukkit.Sound snd;
                                    try { snd = org.bukkit.Sound.valueOf(sndName); } catch (IllegalArgumentException ex) { snd = org.bukkit.Sound.ENTITY_PLAYER_LEVELUP; }
                                    for (org.bukkit.entity.Player op : org.bukkit.Bukkit.getOnlinePlayers()) {
                                        op.playSound(op.getLocation(), snd, (float)v, (float)pc);
                                    }
                                }
                            }
                        }
                    } catch (Throwable ignore) { }
                    // --- /Lowest chance broadcast ---
try { ((SpinGUIHolder) inv.getHolder()).setSpinning(false);} catch (Throwable ignored) {}
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                    p.sendMessage(Text.color("&a보상이 보관함에 지급되었습니다. &7(/랜덤 보관함 으로 확인)"));
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }


            
    
}
