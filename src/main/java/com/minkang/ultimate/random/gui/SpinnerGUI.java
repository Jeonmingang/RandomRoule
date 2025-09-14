package com.minkang.ultimate.random.gui;

import com.minkang.ultimate.random.Main;
import com.minkang.ultimate.random.Roulette;
import com.minkang.ultimate.random.RouletteEntry;
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
import java.util.Random;

public class SpinnerGUI {

    public static void start(final Main plugin, final Player p, final Roulette r) {
        final String title = plugin.getConfig().getString("titles.spinner", "룰렛 뽑기: %key%").replace("%key%", r.getKey());
        final Inventory inv = Bukkit.createInventory(p, 27, ChatColor.translateAlternateColorCodes('&', title));

        // 위/아래 하얀 유리 채우기
        final ItemStack glass = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        ItemMeta gm = glass.getItemMeta();
        if (gm != null) {
            gm.setDisplayName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("spinner.glass-name", "&f")));
            glass.setItemMeta(gm);
        }
        for (int i = 0; i < 9; i++) inv.setItem(i, glass);
        for (int i = 18; i < 27; i++) inv.setItem(i, glass);

        p.openInventory(inv);
        p.sendMessage(plugin.msg("draw_start"));

        // 내부 클래스에서 캡처될 변수들은 final/effectively final 로 유지
        final List<RouletteEntry> entries = new ArrayList<RouletteEntry>(r.getEntries());
        if (entries.isEmpty()) { p.sendMessage(plugin.msg("no_items")); p.closeInventory(); return; }
        final RouletteEntry win = r.pickByWeight();
        if (win == null) { p.sendMessage(plugin.msg("no_items")); p.closeInventory(); return; }

        // 당첨 인덱스
        int winIndexTmp = 0;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i) == win) { winIndexTmp = i; break; }
        }
        final int winIndex = winIndexTmp;

        // 최종 스텝 수 계산(당첨 인덱스에 정확히 멈추도록 보정)
        final int size = entries.size();
        final int baseCycles = Math.max(20, size * 3);
        final int extra = plugin.getConfig().getInt("spinner.extra-steps-random", 35);
        final int provisionalSteps = baseCycles + new Random().nextInt(Math.max(1, extra));
        int deltaToWin = (winIndex - (provisionalSteps % size));
        while (deltaToWin < 0) deltaToWin += size;
        final int totalSteps = provisionalSteps + deltaToWin;
        final int startDelay = Math.max(1, plugin.getConfig().getInt("spinner.ticks-per-step-start", 2));

        new BukkitRunnable() {
            int pointer = 0;
            int stepsLeft = totalSteps;
            int delay = startDelay;

            void paintRow(int centerIndex) {
                for (int offset = -4; offset <= 4; offset++) {
                    int slot = 13 + offset;
                    int idx = (centerIndex + offset) % size;
                    if (idx < 0) idx += size;
                    inv.setItem(slot, entries.get(idx).getItem());
                }
            }

            void giveReward() {
                ItemStack reward = win.getItem().clone();
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                String itemName = reward.hasItemMeta() && reward.getItemMeta().hasDisplayName()
                        ? reward.getItemMeta().getDisplayName()
                        : reward.getType().name();
                p.sendMessage(plugin.msg("draw_win").replace("%item%", ChatColor.stripColor(itemName)));

                if (p.getInventory().firstEmpty() == -1) p.getWorld().dropItemNaturally(p.getLocation(), reward);
                else p.getInventory().addItem(reward);

                // 최저 가중치 당첨 브로드캐스트
                int minWeight = Integer.MAX_VALUE;
                for (RouletteEntry re : entries) if (re.getWeight() < minWeight) minWeight = re.getWeight();
                if (win.getWeight() == minWeight) {
                    String msg = plugin.getConfig().getString("messages.rare_broadcast",
                            "&d&l[대박]&r %player% 이(가) %key% 에서 가장 낮은 확률의 아이템 [%item%] 을 뽑았습니다!");
                    msg = msg.replace("%player%", p.getName())
                             .replace("%key%", r.getKey())
                             .replace("%item%", ChatColor.stripColor(itemName));
                    Bukkit.broadcastMessage(plugin.color(msg));
                }
            }

            @Override
            public void run() {
                if (stepsLeft <= 0) {
                    paintRow(pointer);
                    giveReward();
                    return;
                }
                paintRow(pointer);

                // 감속 효과 사운드
                p.playSound(
                        p.getLocation(),
                        Sound.UI_BUTTON_CLICK,
                        0.6f,
                        1.5f - Math.min(0.8f, (float)(totalSteps - stepsLeft) / (float) totalSteps)
                );

                pointer = (pointer + 1) % size;
                stepsLeft--;

                // 단계적으로 딜레이 증가 (감속)
                if ((totalSteps - stepsLeft) % 12 == 0) delay++;
                if ((totalSteps - stepsLeft) % 24 == 0) delay++;
                if ((totalSteps - stepsLeft) % 36 == 0) delay++;

                // 같은 Runnable을 지연 재실행 (중첩 Runnable 생성 X)
                this.runTaskLater(plugin, delay);
            }
        }.runTask(plugin);
    }
}
