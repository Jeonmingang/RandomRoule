package com.minkang.ultimate.random;
import com.minkang.ultimate.random.gui.PreviewGUI;
import com.minkang.ultimate.random.gui.SettingsGUI;
import com.minkang.ultimate.random.gui.SpinnerGUI;
import com.minkang.ultimate.random.gui.SpinnerProtector;
import com.minkang.ultimate.random.listener.InteractListener;
import com.minkang.ultimate.random.pack.PackageCommand;
import com.minkang.ultimate.random.pack.PackageManager;
import com.minkang.ultimate.random.pack.PackageSettingsGUI;
import com.minkang.ultimate.random.pack.PackageClaimGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
public class Main extends JavaPlugin {
  private final java.util.Set<java.util.UUID> spinning = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
  public boolean tryBeginSpin(org.bukkit.entity.Player p){ return spinning.add(p.getUniqueId()); }
  public void endSpin(org.bukkit.entity.Player p){ spinning.remove(p.getUniqueId()); }
  public boolean tryEndSpinOnce(org.bukkit.entity.Player p){ return spinning.remove(p.getUniqueId()); }

  private RouletteManager manager; private PackageManager packageManager;
  private NamespacedKey pdcKey; private NamespacedKey pkgPdcKey;
  public void onEnable(){
    saveDefaultConfig();
    manager=new RouletteManager(this);
    packageManager=new PackageManager(this);
    pdcKey=new NamespacedKey(this,"ultimate_random_key");
    pkgPdcKey=new NamespacedKey(this,"ultimate_package_key");
    RandomCommand cmd=new RandomCommand(this); PluginCommand pc=getCommand("random"); if(pc!=null){ pc.setExecutor(cmd); pc.setTabCompleter(cmd); }
    PackageCommand pkg=new PackageCommand(this); PluginCommand pcp=getCommand("package"); if(pcp!=null){ pcp.setExecutor(pkg); pcp.setTabCompleter(pkg); }
    Bukkit.getPluginManager().registerEvents(new SettingsGUI(this),this);
    Bukkit.getPluginManager().registerEvents(new PreviewGUI(this),this);
    Bukkit.getPluginManager().registerEvents(new SpinnerProtector(this),this);
    Bukkit.getPluginManager().registerEvents(new InteractListener(this),this);
    Bukkit.getPluginManager().registerEvents(new PackageSettingsGUI(this),this);
    Bukkit.getPluginManager().registerEvents(new PackageClaimGUI(this),this);
    getLogger().info("UltimateRandomRoulette v"+getDescription().getVersion()+" enabled.");
  }
  public void onDisable(){ manager.save(); packageManager.save(); }
  public RouletteManager getManager(){ return manager; }
  public PackageManager getPackageManager(){ return packageManager; }
  public NamespacedKey getPdcKey(){ return pdcKey; }
  public NamespacedKey getPkgPdcKey(){ return pkgPdcKey; }
  
public String msg(String path){ String s=getConfig().getString("messages."+path,""); String prefix=getConfig().getString("messages.prefix","&b[룰렛]&r "); boolean use=getConfig().getBoolean("messages.use-prefix", true); return org.bukkit.ChatColor.translateAlternateColorCodes('&', (use?prefix:"")+ (s==null? "": s)); }

  public String color(String s){ return ChatColor.translateAlternateColorCodes('&', s); }
}