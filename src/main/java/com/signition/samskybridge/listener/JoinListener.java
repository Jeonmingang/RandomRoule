
package com.signition.samskybridge.listener;

import com.signition.samskybridge.Main;
import com.signition.samskybridge.data.DataStore;
import com.signition.samskybridge.data.IslandData;
import com.signition.samskybridge.integration.BentoSync;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {
    private final DataStore store;
    private final BentoSync bento;
    public JoinListener(Main plugin, DataStore store, BentoSync bento){
        this.store = store;
        this.bento = bento;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e){
        if (bento == null || !bento.isEnabled()) return;
        IslandData is = store.getOrCreate(e.getPlayer().getUniqueId(), e.getPlayer().getName());
        bento.reapplyOnJoin(e.getPlayer(), is.getTeamMax());
    }
}
