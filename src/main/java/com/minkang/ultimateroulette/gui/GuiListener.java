package com.minkang.ultimateroulette.gui;

import com.minkang.ultimateroulette.UltimateRoulette;
import com.minkang.ultimateroulette.data.KeyDef;
import com.minkang.ultimateroulette.data.Reward;
import com.minkang.ultimateroulette.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import com.minkang.ultimateroulette.pkg.PackageDef;
import com.minkang.ultimateroulette.pkg.gui.PackageGUI;
import java.util.concurrent.ConcurrentHashMap;

public class GuiListener implements Listener {
    private final Map<UUID, Long> cooldown = new ConcurrentHashMap<>();
    private final UltimateRoulette plugin;
    private final Set<UUID> spinning = new HashSet<>(); // to prevent double payout

    public GuiListener(UltimateRoulette plugin) { this.plugin = plugin; }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        long cd = plugin.getConfig().getLong("cooldown_ms", 700L);
        long now = System.currentTimeMillis();
        Long last = cooldown.get(e.getPlayer().getUniqueId());
        if (last != null && now - last < cd) return;
        cooldown.put(e.getPlayer().getUniqueId(), now);
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack it = e.getItem();
        if (it == null || !it.hasItemMeta()) return;
        String key = it.getItemMeta().getPersistentDataContainer().get(plugin.keyTag(), PersistentDataType.STRING);
        if (key == null) {
            String pkg = it.getItemMeta().getPersistentDataContainer().get(plugin.packageTag(), org.bukkit.persistence.PersistentDataType.STRING);
            if (pkg == null) return;
            com.minkang.ultimateroulette.pkg.PackageDef pdef = plugin.packages().get(pkg);
            if (pdef == null) return;
            e.setCancelled(true);
            new PackageGUI(plugin, pdef).open(e.getPlayer());
            return;
        }
        KeyDef def = plugin.keys().get(key);
        if (def == null) return;

        e.setCancelled(true);
        new PreviewGUI(plugin, def).open(e.getPlayer());
    }

    @EventHandler public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTitle().contains("룰렛") || e.getView().getTitle().contains("미리보기") || e.getView().getTitle().contains("설정") || e.getView().getTitle().contains("패키지 설정"))
            e.setCancelled(true);
    }

    @EventHandler public void onClick(InventoryClickEvent e) {
        if (e.getView().getTitle().contains("룰렛") || e.getView().getTitle().contains("미리보기") || e.getView().getTitle().contains("패키지:")) {
            e.setCancelled(true);
            Player p = (Player) e.getWhoClicked();
            if (e.getClickedInventory() == null) return;
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null) return;
        if (e.getView().getTitle().contains("패키지:")) {
            e.setCancelled(true);
            if (!(e.getWhoClicked() instanceof Player)) return;
            Player p = (Player) e.getWhoClicked();
            if (clicked.getType() == Material.LIME_WOOL && clicked.hasItemMeta() && clicked.getItemMeta().getDisplayName().contains("받기")) {
                // find package key in inventory & consume 1
                java.util.Optional<org.bukkit.inventory.ItemStack> keyStack = java.util.Arrays.stream(p.getInventory().getContents())
                        .filter(is -> is != null && is.hasItemMeta()
                                && is.getItemMeta().getPersistentDataContainer().has(plugin.packageTag(), org.bukkit.persistence.PersistentDataType.STRING))
                        .findFirst();
                if (!keyStack.isPresent()) { p.sendMessage(Text.color("&c전용 패키지 아이템이 없습니다.")); return; }
                String pkg = keyStack.get().getItemMeta().getPersistentDataContainer().get(plugin.packageTag(), org.bukkit.persistence.PersistentDataType.STRING);
                PackageDef defp = plugin.packages().get(pkg);
                if (defp == null) { p.sendMessage(Text.color("&c패키지 구성이 없습니다.")); return; }
                keyStack.get().setAmount(keyStack.get().getAmount()-1);
                if (keyStack.get().getAmount() <= 0) p.getInventory().removeItem(keyStack.get());
                // give items; overflow -> claim
                for (org.bukkit.inventory.ItemStack it2 : defp.getItems()) {
                    java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> left = p.getInventory().addItem(it2.clone());
                    left.values().forEach(rem -> plugin.storage().addClaim(p.getUniqueId(), rem));
                }
                p.sendMessage(Text.color("&a패키지 아이템을 지급했습니다. 남는 아이템은 보관함에 저장됩니다."));
                try { org.bukkit.Sound s = org.bukkit.Sound.valueOf(plugin.getConfig().getString("sounds.claim","ENTITY_ITEM_PICKUP")); p.playSound(p.getLocation(), s, 1f, 1f);} catch (Exception ex) { p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);}
                p.closeInventory();
            }
            return;
        }

            if (clicked.getType() == Material.ARROW && clicked.hasItemMeta()) {
                String t = e.getView().getTitle();
                int pIndex = 0;
                if (t.contains("(p")) { try { pIndex = Math.max(0, Integer.parseInt(t.substring(t.indexOf("(p")+2, t.indexOf(")"))) -1);} catch (Exception ignore) {} }
                boolean next = clicked.getItemMeta().getDisplayName().contains("다음");
                int newPage = next ? pIndex+1 : Math.max(0, pIndex-1);
                java.util.Optional<org.bukkit.inventory.ItemStack> keyStack = java.util.Arrays.stream(p.getInventory().getContents())
                    .filter(is -> is != null && is.hasItemMeta() &&
                            is.getItemMeta().getPersistentDataContainer().has(plugin.keyTag(), org.bukkit.persistence.PersistentDataType.STRING))
                    .findFirst();
                if (keyStack.isPresent()) {
                    String key = keyStack.get().getItemMeta().getPersistentDataContainer().get(plugin.keyTag(), org.bukkit.persistence.PersistentDataType.STRING);
                    if (key != null && plugin.keys().get(key) != null) new PreviewGUI(plugin, plugin.keys().get(key), newPage).open(p);
                }
            } else if (clicked.getType() == Material.LIME_WOOL && clicked.hasItemMeta()) {
                String dn = clicked.getItemMeta().getDisplayName();
                if (dn != null && dn.contains("뽑기 시작")) {
                    // consume one key item
                    Optional<ItemStack> keyStack = Arrays.stream(p.getInventory().getContents())
                            .filter(is -> is != null && is.hasItemMeta()
                                    && is.getItemMeta().getPersistentDataContainer().has(plugin.keyTag(), org.bukkit.persistence.PersistentDataType.STRING)
                                    && plugin.keys().get(is.getItemMeta().getPersistentDataContainer().get(plugin.keyTag(), org.bukkit.persistence.PersistentDataType.STRING)) != null)
                            .findFirst();
                    if (!keyStack.isPresent()) { p.sendMessage(Text.color("&c전용아이템이 인벤토리에 없습니다.")); return; }
                    String key = keyStack.get().getItemMeta().getPersistentDataContainer().get(plugin.keyTag(), org.bukkit.persistence.PersistentDataType.STRING);
                    KeyDef def = plugin.keys().get(key);
                    if (def == null || def.getRewards().isEmpty()) { p.sendMessage(Text.color("&c보상 풀이 비어있습니다.")); return; }
                    if (spinning.contains(p.getUniqueId())) { p.sendMessage(Text.color("&c이미 스핀 중입니다.")); return; }
                    // remove 1
                    keyStack.get().setAmount(keyStack.get().getAmount() - 1);
                    if (keyStack.get().getAmount() <= 0) p.getInventory().removeItem(keyStack.get());

                    spinning.add(p.getUniqueId());
                    new SpinGUI(plugin, def, reward -> {
                        // payout to claim once
                        plugin.storage().addClaim(p.getUniqueId(), reward.clone());
                        p.sendMessage(Text.color("&a보관함에 아이템이 1회 지급되었습니다. &e/랜덤 보관함"));
                        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                        spinning.remove(p.getUniqueId());
                    }).open(p);
                }
            }
        } else if (e.getView().getTitle().contains("설정") || e.getView().getTitle().contains("패키지 설정")) {
            e.setCancelled(true);
            if (!(e.getWhoClicked() instanceof Player)) return;
            Player p = (Player) e.getWhoClicked();
            if (e.getView().getTitle().contains("패키지 설정")) com.minkang.ultimateroulette.pkg.gui.PackageEditGUI.handleClick(p, e); else EditGUI.handleClick(p, e);
        } else if (e.getView().getTitle().contains("보관함")) {
            // Claim GUI handles internally; block taking structure items etc.
            if (!(e.getWhoClicked() instanceof Player)) return;
            ClaimGUI.handleClick((Player) e.getWhoClicked(), e);
        }
    }

    @EventHandler public void onClose(InventoryCloseEvent e) {
        // No special handling now
    }
}
