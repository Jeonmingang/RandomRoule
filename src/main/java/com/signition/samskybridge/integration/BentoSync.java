
package com.signition.samskybridge.integration;

import com.signition.samskybridge.Main;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Safe BentoBox/BSkyBlock integration without compile-time API dependency.
 * - Range sync: uses admin command "/bsbadmin range set <owner> <radius>"
 * - Team size sync: uses permission "[gamemode].team.maxsize.X" on owner
 *
 * Notes:
 * - Range via permission requires relog, so we use the admin command to apply instantly.
 * - Team size via permission is applied with a runtime attachment and re-applied on join.
 */
public class BentoSync {

    private final Main plugin;
    private final String gamemodeId; // e.g. "bskyblock"
    private final boolean enabled;
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    public BentoSync(Main plugin){
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("integration.bentobox.enabled", true);
        this.gamemodeId = plugin.getConfig().getString("integration.bentobox.gamemode-id","bskyblock");
    }

    public boolean isEnabled(){
        if (!enabled) return false;
        return Bukkit.getPluginManager().getPlugin("BentoBox") != null;
    }

    /**
     * Apply island protected range instantly by dispatching admin command.
     * Owner name is used as target to comply with BSkyBlock's admin command.
     */
    public void applyRangeInstant(OfflinePlayer owner, int radius){
        if (!isEnabled()) return;
        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
        String cmd = "bsbadmin range set " + owner.getName() + " " + radius;
        Bukkit.dispatchCommand(console, cmd);
        plugin.getLogger().info("[BentoSync] Ran: " + cmd);
    }

    /**
     * Apply team max size to island owner via numbered permission.
     * [gamemode].team.maxsize.X (docs)
     */
    public void applyTeamMax(Player ownerOnline, int max){
        if (!isEnabled() || ownerOnline == null) return;
        // Remove prior attachment if any
        PermissionAttachment old = attachments.remove(ownerOnline.getUniqueId());
        if (old != null) {
            try { ownerOnline.removeAttachment(old); } catch (Throwable ignored) {}
        }
        PermissionAttachment att = ownerOnline.addAttachment(plugin);
        String node = gamemodeId + ".team.maxsize." + max;
        att.setPermission(node, true);
        attachments.put(ownerOnline.getUniqueId(), att);
        plugin.getLogger().info("[BentoSync] Granted runtime permission: " + node + " to " + ownerOnline.getName());
    }

    public void reapplyOnJoin(Player player, int teamMax){
        if (!isEnabled()) return;
        applyTeamMax(player, teamMax);
    }
}
