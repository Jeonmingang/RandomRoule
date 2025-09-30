package com.signition.samskybridge.gui;
import com.signition.samskybridge.Main;
import com.signition.samskybridge.data.IslandData;
import com.signition.samskybridge.level.LevelService;
import com.signition.samskybridge.upgrade.UpgradeService;
import com.signition.samskybridge.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import java.util.ArrayList;
import java.util.List;
public class UpgradeGUI implements Listener {
    private final Main plugin;
    private final LevelService level;
    private final UpgradeService upgrade;
    public UpgradeGUI(Main plugin, LevelService level, UpgradeService upgrade){
        this.plugin = plugin;
        this.level = level;
        this.upgrade = upgrade;
    }
    public void open(Player player){
        Inventory inv = Bukkit.createInventory(player, 54, Text.color("&b섬 업그레이드"));
        fillUpgradeButtons(player, inv);
        player.openInventory(inv);
    }
    private void fillUpgradeButtons(Player player, Inventory inv){
        final int SLOT_SIZE = 13;
        final int SLOT_MEMBERS = 15;
        {
            ItemStack it = new ItemStack(Material.GRASS_BLOCK);
            ItemMeta im = it.getItemMeta();
            im.setDisplayName(Text.color("&e섬 &l크기 &f업그레이드"));
            List<String> lore = new ArrayList<>();
            IslandData is = level.getIslandOf(player);
            int sizeNow = upgrade.getProtectedSize(is);
            int sizeNext = upgrade.getNextSize(is);
            int needLv  = upgrade.getRequiredLevelForSize(is);
            long price  = upgrade.getPriceForSize(is);
            lore.add(Text.color("&7현재 보호반경: &f" + sizeNow + " 블럭"));
            lore.add(Text.color("&7다음 단계: &a" + sizeNext + " 블럭"));
            lore.add(Text.color("&7요구 레벨: &bLv." + needLv));
            lore.add(Text.color("&7필요 금액: &d" + String.format("%,d", price)));
            lore.add(Text.color("&7클릭: 업그레이드"));
            im.setLore(lore);
            it.setItemMeta(im);
            inv.setItem(SLOT_SIZE, it);
        }
        {
            ItemStack it = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta sm = (SkullMeta) it.getItemMeta();
            OfflinePlayer skull = Bukkit.getOfflinePlayer("MHF_Steve");
            try { sm.setOwningPlayer(skull); } catch (Throwable ignored) {}
            sm.setDisplayName(Text.color("&e섬 &l인원수 &f업그레이드"));
            List<String> lore = new ArrayList<>();
            IslandData is = level.getIslandOf(player);
            int capNow = upgrade.getMemberCap(is);
            int capNext = upgrade.getNextMemberCap(is);
            int needLv  = upgrade.getRequiredLevelForMemberCap(is);
            long price  = upgrade.getPriceForMemberCap(is);
            lore.add(Text.color("&7현재 인원수: &f" + capNow + " 명"));
            lore.add(Text.color("&7다음 단계: &a" + capNext + " 명"));
            lore.add(Text.color("&7요구 레벨: &bLv." + needLv));
            lore.add(Text.color("&7필요 금액: &d" + String.format("%,d", price)));
            lore.add(Text.color("&7클릭: 업그레이드"));
            sm.setLore(lore);
            it.setItemMeta(sm);
            inv.setItem(SLOT_MEMBERS, it);
        }
    }
    @EventHandler
    public void onClick(InventoryClickEvent e){
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (e.getView().getTitle() == null) return;
        if (!Text.color("&b섬 업그레이드").equals(e.getView().getTitle())) return;
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        int raw = e.getRawSlot();
        if (raw == 13) {
            upgrade.tryUpgradeSize(p);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            Bukkit.getScheduler().runTask(plugin, () -> open(p));
        } else if (raw == 15) {
            upgrade.tryUpgradeMemberCap(p);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            Bukkit.getScheduler().runTask(plugin, () -> open(p));
        }
    }
}
