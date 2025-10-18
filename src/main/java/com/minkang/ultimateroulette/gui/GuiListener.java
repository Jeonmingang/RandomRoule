
package com.minkang.ultimateroulette.gui;

import com.minkang.ultimateroulette.UltimateRoulette;
import com.minkang.ultimateroulette.data.KeyDef;
import com.minkang.ultimateroulette.data.Reward;
import com.minkang.ultimateroulette.pkg.PackageDef;
import com.minkang.ultimateroulette.util.Text;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class GuiListener implements Listener {
    private final UltimateRoulette plugin;
    public GuiListener(UltimateRoulette plugin) { this.plugin = plugin; }

    // ---------------- helpers ----------------
    private boolean isEdit(String title) {
        if (title == null) return false;
        String plain = ChatColor.stripColor(title);
        return plain != null && plain.startsWith("설정:");
    }
    private boolean isPreview(String title) {
        if (title == null) return false;
        String plain = ChatColor.stripColor(title);
        return plain != null && plain.startsWith("미리보기:");
    }
    private boolean isSpin(String title) {
        if (title == null) return false;
        String plain = ChatColor.stripColor(title);
        return plain != null && plain.startsWith("룰렛 스핀:");
    }
    private boolean isPackageEdit(String title) {
        if (title == null) return false;
        String plain = ChatColor.stripColor(title);
        return plain != null && plain.startsWith("패키지 설정:");
    }
    private boolean isPackagePreview(String title) {
        if (title == null) return false;
        String plain = ChatColor.stripColor(title);
        return plain != null && plain.startsWith("패키지:") && !plain.contains("설정");
    }
    private int currentPageFromTitle(String title) {
        try {
            String plain = ChatColor.stripColor(title);
            int pIdx = plain.lastIndexOf("(p");
            if (pIdx >= 0) {
                int end = plain.indexOf(')', pIdx);
                String num = plain.substring(pIdx + 2, end);
                int slash = num.indexOf('/');
                if (slash >= 0) num = num.substring(0, slash);
                return Math.max(0, Integer.parseInt(num) - 1);
            }
        } catch (Exception ignored) {}
        return 0;
    }
    private String keyFromTitle(String title) {
        try {
            String plain = ChatColor.stripColor(title);
            int s = plain.indexOf("설정:") + 3;
            if (s > 0 && s < plain.length() && plain.charAt(s) == ' ') s++;
            int e = plain.indexOf(" (p");
            if (e < 0) e = plain.length();
            if (s >= 0 && e > s) return plain.substring(s, e).trim();
        } catch (Exception ignored) {}
        return null;
    }
    private String packageNameFromTitle(String title) {
        try {
            String plain = ChatColor.stripColor(title);
            int s = plain.indexOf("패키지:") + 4;
            if (s > 0 && s < plain.length() && plain.charAt(s) == ' ') s++;
            int e = plain.indexOf(" (");
            if (e < 0) e = plain.length();
            if (s >= 0 && e > s) return plain.substring(s, e).trim();
        } catch (Exception ignored) {}
        return null;
    }

    // ---------------- Edit GUI ----------------
    @EventHandler
    public void onEditClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();
        if (!isEdit(title)) return;

        Inventory clicked = e.getClickedInventory();
        Inventory top = e.getView().getTopInventory();
        Inventory bottom = e.getView().getBottomInventory();
        boolean isTopClick = (clicked != null && clicked.equals(top));
        boolean isBottomClick = (clicked != null && clicked.equals(bottom));

        // Interactions in player's own inventory (bottom)
        if (isBottomClick) {
            // SHIFT-click quick-add 1 item
            if (e.isShiftClick()) {
                ItemStack from = e.getCurrentItem();
                String key = keyFromTitle(title);
                if (from != null && from.getType() != Material.AIR && key != null) {
                    KeyDef def = plugin.keys().get(key);
                    if (def != null) {
                        def.getRewards().add(new Reward(from.clone(), 1));
                        plugin.keys().save();
                        e.setCancelled(true);
                        int page = currentPageFromTitle(title);
                        new EditGUI(plugin, def, page).open(p);
                        return;
                    }
                }
            }
            // Otherwise allow normal actions in player inv
            return;
        }

        // Top inventory logic (actual edit grid)
        e.setCancelled(true);
        String key = keyFromTitle(title);
        if (key == null) return;
        KeyDef def = plugin.keys().get(key);
        if (def == null) return;
        int page = currentPageFromTitle(title);

        // Reward grid is 0..44
        int slot = e.getRawSlot();
        if (slot >= 0 && slot < 45) {
            // existing reward -> adjust
            if (slot < def.getRewards().size()) {
                Reward r = def.getRewards().get(slot);
                if (e.getClick() == ClickType.DROP) { // Q delete
                    def.getRewards().remove(slot);
                } else if (e.getClick() == ClickType.LEFT) {
                    r.setWeight(Math.max(1, r.getWeight() + (e.isShiftClick() ? 10 : 1)));
                } else if (e.getClick() == ClickType.RIGHT) {
                    r.setWeight(Math.max(1, r.getWeight() - (e.isShiftClick() ? 10 : 1)));
                } else if (e.getClick() == ClickType.NUMBER_KEY) {
                    int hotbar = e.getHotbarButton();
                    if (hotbar >= 0) {
                        ItemStack hb = p.getInventory().getItem(hotbar);
                        if (hb != null && hb.getType() != Material.AIR) {
                            def.getRewards().add(new Reward(hb.clone(), 1));
                        }
                    }
                } else if (e.getClick() == ClickType.SWAP_OFFHAND) {
                    ItemStack off = p.getInventory().getItemInOffHand();
                    if (off != null && off.getType() != Material.AIR) {
                        def.getRewards().add(new Reward(off.clone(), 1));
                    }
                }
                plugin.keys().save();
                new EditGUI(plugin, def, page).open(p);
                return;
            }

            // empty reward slot + cursor item -> add 1 and consume 1 from cursor
            ItemStack cursor = e.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                def.getRewards().add(new Reward(cursor.clone(), 1));
                // consume one from cursor
                ItemStack cur = cursor.clone();
                cur.setAmount(Math.max(0, cursor.getAmount() - 1));
                e.setCursor(cur.getAmount() <= 0 ? null : cur);
                plugin.keys().save();
                new EditGUI(plugin, def, page).open(p);
                return;
            }
        }
    }

    @EventHandler
    public void onEditDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        String title = e.getView().getTitle();
        if (!isEdit(title)) return;
        e.setCancelled(true);
    }

    // ---------------- Preview GUI ----------------
    @EventHandler
    public void onPreviewClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player)e.getWhoClicked();
        String title = e.getView().getTitle();
        if (!isPreview(title)) return;
        e.setCancelled(true);

        String plain = ChatColor.stripColor(title);
        String key = null;
        try {
            int s = plain.indexOf("미리보기:") + 5;
            if (s > 0 && s < plain.length() && plain.charAt(s) == ' ') s++;
            int eIdx = plain.indexOf(" (p");
            if (eIdx < 0) eIdx = plain.length();
            key = plain.substring(s, eIdx).trim();
        } catch (Exception ignored) {}
        if (key == null || key.isEmpty()) return;

        KeyDef def = plugin.keys().get(key);
        if (def == null) return;
        int page = currentPageFromTitle(title);

        if (e.getRawSlot() == 45) { // prev
            if (page > 0) new PreviewGUI(def).open(p, page - 1);
            return;
        }
        if (e.getRawSlot() == 53) { // next
            int total = Math.max(1, (int)Math.ceil(def.getRewards().size() / 45.0));
            if (page + 1 < total) new PreviewGUI(def).open(p, page + 1);
            return;
        }
        if (e.getRawSlot() == 49) { // spin start
            new SpinGUI(plugin, def).open(p);
            return;
        }
    }

    // ---------------- Spin GUI ----------------
    @EventHandler public void onSpinClose(InventoryCloseEvent e) {
        String title = e.getView().getTitle();
        if (!isSpin(title)) return;
        Inventory inv = e.getInventory();
        if (inv == null) return;
        org.bukkit.inventory.InventoryHolder holder = inv.getHolder();
        if (holder instanceof SpinGUIHolder) {
            SpinGUIHolder h = (SpinGUIHolder) holder;
            if (h.isSpinning() && e.getPlayer() instanceof Player) {
                Player p = (Player) e.getPlayer();
                Bukkit.getScheduler().runTask(plugin, () -> p.openInventory(inv));
            }
        } else {
            if (e.getPlayer() instanceof Player) {
                Player p = (Player) e.getPlayer();
                Bukkit.getScheduler().runTask(plugin, () -> p.openInventory(inv));
            }
        }
    }

    // ---------------- Claim GUI ----------------
    @EventHandler
    public void onClaimClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ClaimGUI.handleClick(plugin, p, e);
    }
    @EventHandler
    public void onClaimDrag(InventoryDragEvent e) {
        if (ClaimGUI.isClaimTitle(e.getView().getTitle())) e.setCancelled(true);
    }

    // ---------------- Package Edit GUI ----------------
    @EventHandler
    public void onPackageEditClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();
        if (!isPackageEdit(title)) return;

        Inventory clicked = e.getClickedInventory();
        Inventory top = e.getView().getTopInventory();
        Inventory bottom = e.getView().getBottomInventory();
        boolean isTop = (clicked != null && clicked.equals(top));
        boolean isBottom = (clicked != null && clicked.equals(bottom));

        String pkg = packageNameFromTitle(title);
        if (pkg == null || pkg.isEmpty()) return;
        PackageDef def = plugin.packages().get(pkg);
        if (def == null) return;

        if (isBottom) {
            if (e.isShiftClick()) {
                ItemStack from = e.getCurrentItem();
                if (from != null && from.getType() != Material.AIR) {
                    ItemStack add = from.clone();
                    add.setAmount(1);
                    def.getItems().add(add);
                    plugin.packages().save();
                    e.setCancelled(true);
                    new com.minkang.ultimateroulette.pkg.gui.PackageEditGUI(plugin, def).open(p);
                }
            }
            return;
        }

        e.setCancelled(true);
        int slot = e.getRawSlot();
        if (slot >= 0 && slot < 45) {
            if (slot < def.getItems().size()) {
                if (e.getClick() == ClickType.DROP) {
                    def.getItems().remove(slot);
                    plugin.packages().save();
                    new com.minkang.ultimateroulette.pkg.gui.PackageEditGUI(plugin, def).open(p);
                    return;
                }
            } else {
                ItemStack cursor = e.getCursor();
                if (cursor != null && cursor.getType() != Material.AIR) {
                    ItemStack add = cursor.clone();
                    add.setAmount(1);
                    def.getItems().add(add);
                    plugin.packages().save();
                    ItemStack cur = cursor.clone();
                    cur.setAmount(Math.max(0, cursor.getAmount()-1));
                    e.setCursor(cur.getAmount()<=0?null:cur);
                    new com.minkang.ultimateroulette.pkg.gui.PackageEditGUI(plugin, def).open(p);
                    return;
                }
            }
        }
    }

    // ---------------- Helpers for keys ----------------
    private boolean isKeyItem(ItemStack it, KeyDef def) {
        if (it == null || it.getType() == Material.AIR || !it.hasItemMeta()) return false;
        String tag = it.getItemMeta().getPersistentDataContainer()
                .getOrDefault(plugin.keyTag(), PersistentDataType.STRING, null);
        return tag != null && tag.equals(def.getName());
    }
    private boolean isPackageKeyItem(ItemStack it, PackageDef def) {
        if (it == null || it.getType() == Material.AIR || !it.hasItemMeta()) return false;
        String tag = it.getItemMeta().getPersistentDataContainer()
                .getOrDefault(plugin.packageTag(), PersistentDataType.STRING, null);
        return tag != null && tag.equals(def.getName());
    }
    private boolean consumePackageKey(Player p, PackageDef def) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (isPackageKeyItem(hand, def)) {
            int amt = hand.getAmount();
            if (amt <= 1) p.getInventory().setItemInMainHand(null);
            else hand.setAmount(amt - 1);
            return true;
        }
        for (int i=0;i<p.getInventory().getSize();i++) {
            ItemStack it = p.getInventory().getItem(i);
            if (isPackageKeyItem(it, def)) {
                int amt = it.getAmount();
                if (amt <= 1) p.getInventory().setItem(i, null);
                else it.setAmount(amt - 1);
                return true;
            }
        }
        return false;
    }

    // ---------------- Package Preview/Take ----------------
    @EventHandler
    public void onPackageClick(InventoryClickEvent e) {
            if (!(e.getWhoClicked() instanceof Player)) return;
            Player p = (Player) e.getWhoClicked();
            String title = e.getView().getTitle();
            if (!isPackagePreview(title)) return;

            String name = packageNameFromTitle(title);
            if (name == null || name.isEmpty()) return;
            com.minkang.ultimateroulette.pkg.PackageDef def = plugin.packages().get(name);
            if (def == null) return;

            e.setCancelled(true);
            int slot = e.getRawSlot();
            if (slot == 49) { // 수령
                if (!consumePackageKey(p, def)) {
                    p.sendMessage(com.minkang.ultimateroulette.util.Text.color("&c해당 패키지 키가 필요합니다."));
                    return;
                }
                for (org.bukkit.inventory.ItemStack it : def.getItems()) {
                    if (it != null && it.getType() != org.bukkit.Material.AIR) {
                        p.getInventory().addItem(it.clone());
                    }
                }
                p.sendMessage(com.minkang.ultimateroulette.util.Text.color("&a패키지 아이템을 수령했습니다."));
                p.closeInventory();
            }

        }


        // ---------------- Package Edit GUI (delegate to GUI class) ----------------
        @org.bukkit.event.EventHandler
        public void onPackageEditClick(org.bukkit.event.inventory.InventoryClickEvent e) {
            if (!(e.getWhoClicked() instanceof org.bukkit.entity.Player)) return;
            String title = e.getView().getTitle();
            if (!isPackageEdit(title)) return;
            org.bukkit.entity.Player p = (org.bukkit.entity.Player) e.getWhoClicked();
            com.minkang.ultimateroulette.pkg.gui.PackageEditGUI.handleClick(p, e);
        }

        @org.bukkit.event.EventHandler
        public void onPackageEditDrag(org.bukkit.event.inventory.InventoryDragEvent e) {
            String title = e.getView().getTitle();
            if (isPackageEdit(title)) e.setCancelled(true);
        }
}
