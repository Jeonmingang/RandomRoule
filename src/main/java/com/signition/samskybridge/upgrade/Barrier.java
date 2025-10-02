package com.signition.samskybridge.upgrade;

import com.signition.samskybridge.Main;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

public class Barrier {
    public static void showWhiteBarrier(Player p, int radius){
        int seconds = Main.get().getConfig().getInt("barrier.show-seconds", 6);
        double step = Main.get().getConfig().getDouble("barrier.step", 2.0);
        Location c = p.getLocation().getBlock().getLocation().add(0.5, 0.0, 0.5);
        long end = System.currentTimeMillis() + seconds*1000L;
        Main.get().getServer().getScheduler().runTaskTimer(Main.get(), task -> {
            if (System.currentTimeMillis() > end){ task.cancel(); return; }
            for (double x=-radius; x<=radius; x+=step){
                draw(c.clone().add(x, 0, -radius), p);
                draw(c.clone().add(x, 0, radius), p);
            }
            for (double z=-radius; z<=radius; z+=step){
                draw(c.clone().add(-radius, 0, z), p);
                draw(c.clone().add(radius, 0, z), p);
            }
        }, 1L, 5L);
    }

    private static void draw(Location l, Player p){
        p.spawnParticle(Particle.REDSTONE, l.add(0,1.2,0), 1,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(255,255,255), 1.0f));
    }
}