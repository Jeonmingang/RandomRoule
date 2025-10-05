package com.minkang.ultimateroulette.gui;

import com.minkang.ultimateroulette.UltimateRoulette;
import com.minkang.ultimateroulette.data.KeyDef;
import com.minkang.ultimateroulette.util.Text;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class KeyUseListener implements Listener {
    private final UltimateRoulette plugin;
    public KeyUseListener(UltimateRoulette plugin) { this.plugin = plugin; }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack it = e.getItem();
        if (it == null) return;
        if (!it.hasItemMeta()) return;
        ItemMeta meta = it.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // Check roulette key first
        String keyName = pdc.getOrDefault(plugin.keyTag(), PersistentDataType.STRING, null);
        if (keyName != null && !keyName.isEmpty()) {
            KeyDef def = plugin.keys().get(keyName);
            if (def != null) {
                e.setCancelled(true);
                new PreviewGUI(plugin, def).open((Player) e.getPlayer());
            }
            return;
        }

        // Check package key
        String pkgName = pdc.getOrDefault(plugin.packageTag(), PersistentDataType.STRING, null);
        if (pkgName != null && !pkgName.isEmpty()) {
            com.minkang.ultimateroulette.pkg.PackageDef def = plugin.packages().get(pkgName);
            if (def != null) {
                e.setCancelled(true);
                new com.minkang.ultimateroulette.pkg.gui.PackageGUI(plugin, def).open((Player) e.getPlayer());
            }
        }
    }
}