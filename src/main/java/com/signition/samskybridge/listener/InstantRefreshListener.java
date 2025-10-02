package com.signition.samskybridge.listener;

import com.signition.samskybridge.Main;
import com.signition.samskybridge.rank.RankingService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class InstantRefreshListener implements Listener {
    private final Main plugin;
    private final RankingService ranking;

    public InstantRefreshListener(Main plugin, RankingService ranking) {
        this.plugin = plugin;
        this.ranking = ranking;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        // already handled inside RankingService, but ensure double safety
        Bukkit.getScheduler().runTaskLater(plugin, () -> ranking.applyTab(e.getPlayer()), 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        // Optional: nothing needed; scoreboard teams auto-GC names
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> ranking.applyTab(e.getPlayer()), 5L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        // After upgrade GUI or any island GUI close, recalc quickly
        if (e.getPlayer() instanceof Player) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> ranking.applyTab((Player)e.getPlayer()), 2L);
        }
    }

    @EventHandler
    public void onIslandCommands(PlayerCommandPreprocessEvent e) {
        String msg = e.getMessage().toLowerCase();
        // common aliases: /섬, /sky, /island, /is
        if (msg.startsWith("/섬") || msg.startsWith("/sky") || msg.startsWith("/island") || msg.startsWith("/is ")) {
            // Light weight approach: refresh all soon (covers invite/accept/kick/promote etc without parsing)
            Bukkit.getScheduler().runTaskLater(plugin, () -> ranking.rebuildRanks(); ranking.refreshTabAll(), 4L);
        }
    
    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> ranking.applyTab(e.getPlayer()), 5L);
    
    @EventHandler
    public void onKick(PlayerKickEvent e) {
        ranking.removeFromTeams(e.getPlayer());
    }
}
