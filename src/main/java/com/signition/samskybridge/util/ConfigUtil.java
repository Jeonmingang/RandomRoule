
package com.signition.samskybridge.util;

import com.signition.samskybridge.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;

public class ConfigUtil {
    public static FileConfiguration loadBlocks(Main plugin){
        return YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "removed_blocks.yml"));
    }
    public static boolean saveBlocks(Main plugin, FileConfiguration cfg){
        try { cfg.save(new File(plugin.getDataFolder(), "removed_blocks.yml")); return true; }
        catch (IOException e){ plugin.getLogger().severe("save failed: "+e.getMessage()); return false; }
    }
}
