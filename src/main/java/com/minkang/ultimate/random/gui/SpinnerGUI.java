package com.minkang.ultimate.random.gui;
import com.minkang.ultimate.random.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.enchantments.Enchantment;
import com.minkang.ultimate.random.LoreSanitizer;
import com.minkang.ultimate.random.GrantService;
public class SpinnerGUI {

  private static org.bukkit.inventory.ItemStack sanitized(org.bukkit.inventory.ItemStack src){
    if(src==null) return null;
    try { return src.clone(); } catch(Throwable t){ return src; }
  }

  public static void start(final Main plugin, final Player p, final Roulette r){
    if(!plugin.tryBeginSpin(p)){ p.sendMessage(plugin.msg("already_spinning")); p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 0.7f); return; }

    final String title=plugin.getConfig().getString("titles.spinner","룰렛 뽑기: %key%").replace("%key%", r.getKey());
    final boolean[] done = {false};
    final Inventory inv=Bukkit.createInventory(p,27, ChatColor.translateAlternateColorCodes('&', title));
    final Material glassMat;
    try{ glassMat=Material.valueOf(plugin.getConfig().getString("spinner.glass-material","WHITE_STAINED_GLASS_PANE")); }catch(Exception ex){ throw new IllegalArgumentException("Invalid glass material in config."); }
    final ItemStack glass=new ItemStack(glassMat);
    ItemMeta gm=glass.getItemMeta(); if(gm!=null){ gm.setDisplayName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("spinner.glass-name","&f"))); glass.setItemMeta(gm); }
    for(int i=0;i<9;i++) inv.setItem(i,glass); for(int i=18;i<27;i++) inv.setItem(i,glass);
    if(plugin.getConfig().getBoolean("spinner.use-pointer-panes", true)){
      ItemStack pointer=new ItemStack(Material.HOPPER);
      ItemMeta pmeta=pointer.getItemMeta(); if(pmeta!=null){ pmeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("spinner.pointer-name","&6▲ 중앙 슬롯 ▼"))); pointer.setItemMeta(pmeta); }
      inv.setItem(4, pointer); inv.setItem(22, pointer);
    }
    p.openInventory(inv); p.sendMessage(plugin.msg("draw_start"));
    final java.util.List<RouletteEntry> entries=new java.util.ArrayList<>(r.getEntries()); if(entries.isEmpty()){ p.sendMessage(plugin.msg("no_items")); p.closeInventory(); return; }
    final RouletteEntry win=r.pickByWeight(); if(win==null){ p.sendMessage(plugin.msg("no_items")); p.closeInventory(); return; }
    int winIndexTmp=0; for(int i=0;i<entries.size();i++){ if(entries.get(i)==win){ winIndexTmp=i; break; } }
    final int winIndex=winIndexTmp, size=entries.size();
    final int totalWeight=r.getTotalWeight();
    final boolean chanceOnly=plugin.getConfig().getBoolean("spinner.show-chance-only", true);
    final boolean centerGlow=plugin.getConfig().getBoolean("spinner.center-glow", true);
    final String chanceFmt=ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("spinner.chance-format","&7확률: &e%chance%%"));
    final java.text.DecimalFormat df=new java.text.DecimalFormat("#.##");
    final int perSize=plugin.getConfig().getInt("spinner.base-cycles-per-size",2);
    final int minBase=plugin.getConfig().getInt("spinner.min-base-cycles",12);
    final int baseCycles=Math.max(minBase,size*perSize);
    final int extra=plugin.getConfig().getInt("spinner.extra-steps-random",15);
    final int provisional=baseCycles+new java.util.Random().nextInt(Math.max(1,extra));
    int delta=(winIndex-(provisional%size)); while(delta<0) delta+=size;
    final int totalSteps=provisional+delta;
    final int startDelay=Math.max(1, plugin.getConfig().getInt("spinner.ticks-per-step-start",1));
    new org.bukkit.scheduler.BukkitRunnable(){
      int pointer=0; int stepsLeft=totalSteps; int delay=startDelay; int cooldown=0;
      
ItemStack withChanceLore(RouletteEntry re, boolean glow){
        ItemStack it=re.getItem().clone();
        // Sanitize display for book-like items to avoid massive JSON page preview
        if (it.getItemMeta() instanceof org.bukkit.inventory.meta.BookMeta) {
          ItemStack display = new ItemStack(org.bukkit.Material.PAPER);
          org.bukkit.inventory.meta.ItemMeta dm = display.getItemMeta();
          org.bukkit.inventory.meta.ItemMeta src = it.getItemMeta();
          if (dm != null && src != null) {
            if (src.hasDisplayName()) dm.setDisplayName(src.getDisplayName());
            java.util.List<String> lore = new java.util.ArrayList<>();
            if (src != null && src.hasLore()) {
              java.util.List<String> base = new java.util.ArrayList<>(src.getLore());
              java.util.Iterator<String> itl = base.iterator();
              while(itl.hasNext()){
                String plain = org.bukkit.ChatColor.stripColor(String.valueOf(itl.next()));
                String low = plain.toLowerCase();
                if(low.startsWith("가중치") || low.startsWith("weight")) itl.remove();
              }
              lore.addAll(base);
            }
            double chance=100.0*re.getWeight()/Math.max(1,totalWeight);
            lore.add(chanceFmt.replace("%chance%", df.format(chance)));
            dm.setLore(new java.util.ArrayList<>(new java.util.LinkedHashSet<>(lore)));
if(glow){
              try{ dm.addEnchant(Enchantment.DURABILITY, 1, true); dm.addItemFlags(ItemFlag.HIDE_ENCHANTS); }catch(Throwable ignored){}
            }
            display.setItemMeta(dm);
          }
          return display;
        }
        org.bukkit.inventory.meta.ItemMeta m=it.getItemMeta();
        if(m!=null){
          java.util.List<String> lore = new java.util.ArrayList<String>();
          ItemStack srcClean2 = sanitized(it);
          if (m.hasLore()) lore.addAll(m.getLore());
          if (srcClean2 != null && srcClean2.hasItemMeta() && srcClean2.getItemMeta().hasLore())
            lore.addAll(srcClean2.getItemMeta().getLore());
double chance=100.0*re.getWeight()/Math.max(1,totalWeight);
          lore.add(chanceFmt.replace("%chance%", df.format(chance)));
          m.setLore(new java.util.ArrayList<>(new java.util.LinkedHashSet<>(lore)));
          if(glow){
            try{ m.addEnchant(Enchantment.DURABILITY, 1, true); m.addItemFlags(ItemFlag.HIDE_ENCHANTS); }catch(Throwable ignored){}
          }
          it.setItemMeta(m);
        }
        return it;
      }

      void paintRow(int center){
        for(int offset=-4; offset<=4; offset++){
          int slot=13+offset;
          int idx=(center+offset)%size; if(idx<0) idx+=size;
          boolean glow=centerGlow && offset==0;
          ItemStack display = chanceOnly ? withChanceLore(entries.get(idx), glow) : entries.get(idx).getItem();
          if(glow && !chanceOnly){
            try{
              ItemMeta m=display.getItemMeta(); if(m!=null){ m.addEnchant(Enchantment.DURABILITY,1,true); m.addItemFlags(ItemFlag.HIDE_ENCHANTS); display.setItemMeta(m); }
            }catch(Throwable ignored){}
          }
          inv.setItem(slot, display);
        }
      }
      void giveReward(){
        if(done[0]) return;
        done[0] = true;
        Integer _sd = p.getPersistentDataContainer().get(new org.bukkit.NamespacedKey(plugin, "spin-done"), org.bukkit.persistence.PersistentDataType.INTEGER);
        if(_sd!=null && _sd==1) return;
        p.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "spin-done"), org.bukkit.persistence.PersistentDataType.INTEGER, 1);
        if(!plugin.tryEndSpinOnce(p)) return;

        ItemStack reward=win.getItem().clone();
        /* keep original item meta (display name and lore) when giving reward */
if(reward.hasItemMeta()){
  /* ensure meta exists but DO NOT remove lore */
}
        if(p.isOnline()){
          p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
          String itemName=(reward.hasItemMeta() && reward.getItemMeta().hasDisplayName())? reward.getItemMeta().getDisplayName() : reward.getType().name();
          p.sendMessage(plugin.msg("draw_win").replace("%item%", ChatColor.stripColor(itemName)));
          ItemStack clean = com.minkang.ultimate.random.LoreSanitizer.strip(reward);
          com.minkang.ultimate.random.RewardGiver.giveClean(p, clean);
          int min=Integer.MAX_VALUE; for(RouletteEntry re: entries) if(re.getWeight()<min) min=re.getWeight();
          if(win.getWeight()==min){
            String msg=plugin.getConfig().getString("messages.rare_broadcast","&d&l[대박]&r %player% 이(가) %key% 에서 가장 낮은 확률의 아이템 [%item%] 을 뽑았습니다!");
            msg=msg.replace("%player%", p.getName()).replace("%key%", r.getKey()).replace("%item%", ChatColor.stripColor(itemName));
            Bukkit.broadcastMessage(plugin.color(msg));
          }
        }
      }
      public void run(){
        if(!p.isOnline()){ plugin.endSpin(p); cancel(); return; }
        if(stepsLeft<=0){ paintRow(pointer); giveReward(); plugin.endSpin(p); cancel(); return; }
        if(cooldown>0){ cooldown--; return; }
        paintRow(pointer);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.7f - Math.min(1.0f, (float)(totalSteps-stepsLeft)/(float)totalSteps));
        pointer=(pointer+1)%size; stepsLeft--;
        if((totalSteps-stepsLeft)%10==0) delay++;
        if((totalSteps-stepsLeft)%20==0) delay++;
        cooldown=Math.max(1, delay);
      }
    }.runTaskTimer(plugin, 0L, 1L);
  }

}
