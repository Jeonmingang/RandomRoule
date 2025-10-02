
package com.signition.samskybridge.listener;

import com.signition.samskybridge.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.PortalCreateEvent;

public class PortalBlocker implements Listener {
    private final Main plugin;
    public PortalBlocker(Main plugin){ this.plugin = plugin; }

    private boolean isBlockedWorld(String name){
        if (name == null) return false;
        name = name.toLowerCase();
        return name.contains("bskyblock_world_nether") || name.contains("bskyblock_world_the_end");
    }

    @EventHandler
    public void onPortal(PlayerPortalEvent e){
        if (e.getTo() != null && isBlockedWorld(e.getTo().getWorld().getName())){
            e.setCancelled(true);
            e.getPlayer().sendMessage("§c이 섬에서는 지옥/엔더로 이동할 수 없습니다.");
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e){
        PlayerTeleportEvent.TeleportCause c = e.getCause();
        if (c == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL || c == PlayerTeleportEvent.TeleportCause.END_PORTAL){
            if (e.getTo() != null && isBlockedWorld(e.getTo().getWorld().getName())){
                e.setCancelled(true);
                e.getPlayer().sendMessage("§c이 섬에서는 지옥/엔더로 이동할 수 없습니다.");
            }
        }
    }

    @EventHandler
    public void onPortalCreate(PortalCreateEvent e){
        if (e.getWorld() != null && isBlockedWorld(e.getWorld().getName())){
            e.setCancelled(true);
        }
    }
}
