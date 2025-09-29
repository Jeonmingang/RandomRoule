package com.minkang.ultimate.random.gui;
import com.minkang.ultimate.random.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
public class PreviewGUI implements Listener{

public static void open(Main plugin, Player p, Roulette r){
    open(plugin, p, r, 0);
}

public static void open(Main plugin, Player p, Roulette r, int page){
    java.util.List<RouletteEntry> entries=new java.util.ArrayList<>(r.getEntries());
    int total = entries.size();
    int perPage = 45;
    int pages = Math.max(1, (int)Math.ceil(total / (double)perPage));
    int current = Math.max(0, Math.min(page, pages-1));

    String baseTitle = plugin.getConfig().getString("titles.preview","룰렛 미리보기: %key%").replace("%key%", r.getKey());
    String titled = baseTitle + " (" + (current+1) + "/" + pages + ")";
    org.bukkit.inventory.Inventory inv=org.bukkit.Bukkit.createInventory(p,54, org.bukkit.ChatColor.translateAlternateColorCodes('&', titled));

    boolean showWeight=plugin.getConfig().getBoolean("preview.show-weight", true);
    boolean showChance=plugin.getConfig().getBoolean("preview.show-chance", true);
    String weightFmt=org.bukkit.ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("preview.weight-format","&7가중치: &e%weight%"));
    String chanceFmt=org.bukkit.ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("preview.chance-format","&7확률: &e%chance%%"));
    java.util.List<String> extraTop=new java.util.ArrayList<>();
    for(String s: plugin.getConfig().getStringList("preview.extra-lore-top")) extraTop.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', s));
    java.util.List<String> extraBottom=new java.util.ArrayList<>();
    for(String s: plugin.getConfig().getStringList("preview.extra-lore-bottom")) extraBottom.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', s));
    java.util.List<String> removeTags=plugin.getConfig().getStringList("preview.remove-tags");

    int totalWeight = 0;
    for (RouletteEntry e : entries) totalWeight += Math.max(1, e.getWeight());
    java.text.DecimalFormat df=new java.text.DecimalFormat("#.##");

    int start = current * perPage;
    int end = Math.min(total, start + perPage);
    int slot = 0;
    for(int i=start; i<end; i++){
        RouletteEntry e = entries.get(i);
        org.bukkit.inventory.ItemStack it=e.getItem().clone(); org.bukkit.inventory.meta.ItemMeta m=it.getItemMeta();
        if(m!=null){
            java.util.List<String> lore=m.hasLore()?m.getLore():new java.util.ArrayList<>();
            // remove tags
            if(removeTags!=null && !removeTags.isEmpty()){
                java.util.Iterator<String> itl=lore.iterator();
                while(itl.hasNext()){
                    String line=org.bukkit.ChatColor.stripColor(itl.next());
                    if(line==null) continue;
                    for(String tag: removeTags){
                        if(line.replace(" ","").startsWith(tag)) { itl.remove(); break; }
                    }
                }
            }
            // extra top
            if(extraTop!=null && !extraTop.isEmpty()){
                java.util.List<String> top=new java.util.ArrayList<>(extraTop);
                top.addAll(lore); lore=top;
            }
            // weight/chance
            if(showWeight){
                lore.add(weightFmt.replace("%weight%", String.valueOf(e.getWeight())));
            }
            if(showChance && totalWeight>0){
                double pct = (Math.max(1,e.getWeight())*100.0)/totalWeight;
                lore.add(chanceFmt.replace("%chance%", df.format(pct)));
            }
            // extra bottom
            if(extraBottom!=null && !extraBottom.isEmpty()){
                lore.addAll(extraBottom);
            }
            m.setLore(lore); it.setItemMeta(m);
        }
        inv.setItem(slot++, it);
    }

    // Bottom controls
    org.bukkit.inventory.ItemStack prev = new org.bukkit.inventory.ItemStack(org.bukkit.Material.ARROW);
    org.bukkit.inventory.meta.ItemMeta pm = prev.getItemMeta(); if(pm!=null){ pm.setDisplayName(org.bukkit.ChatColor.YELLOW + "이전 페이지"); prev.setItemMeta(pm); }
    org.bukkit.inventory.ItemStack next = new org.bukkit.inventory.ItemStack(org.bukkit.Material.ARROW);
    org.bukkit.inventory.meta.ItemMeta nm = next.getItemMeta(); if(nm!=null){ nm.setDisplayName(org.bukkit.ChatColor.YELLOW + "다음 페이지"); next.setItemMeta(nm); }
    org.bukkit.inventory.ItemStack close = new org.bukkit.inventory.ItemStack(org.bukkit.Material.BARRIER);
    org.bukkit.inventory.meta.ItemMeta cm = close.getItemMeta(); if(cm!=null){ cm.setDisplayName(org.bukkit.ChatColor.RED + "닫기"); close.setItemMeta(cm); }

    // place controls
    inv.setItem(45, current>0 ? prev : new org.bukkit.inventory.ItemStack(org.bukkit.Material.BLACK_STAINED_GLASS_PANE));
    inv.setItem(49, close);
    inv.setItem(53, current<pages-1 ? next : new org.bukkit.inventory.ItemStack(org.bukkit.Material.BLACK_STAINED_GLASS_PANE));

    p.openInventory(inv);
}

@org.bukkit.event.EventHandler
public void onPreviewClick(org.bukkit.event.inventory.InventoryClickEvent e){
    String title = e.getView().getTitle();
    if(title==null) return;
    String plain = org.bukkit.ChatColor.stripColor(title);
    if(plain==null || !plain.startsWith("룰렛 미리보기:")) return;

    // block taking/moving from top inventory
    if(e.getRawSlot() < e.getView().getTopInventory().getSize()){
        e.setCancelled(true);
    }

    org.bukkit.entity.HumanEntity he = e.getWhoClicked();
    if(!(he instanceof org.bukkit.entity.Player)) return;
    org.bukkit.entity.Player p = (org.bukkit.entity.Player) he;

    // parse key and page info: "룰렛 미리보기: KEY (x/y)"
    String rest = plain.substring("룰렛 미리보기:".length()).trim();
    String key = rest;
    int paren = rest.lastIndexOf('(');
    int page = 0, pages = 1;
    if(paren != -1 && rest.endsWith(")")){
        key = rest.substring(0, paren).trim();
        String inside = rest.substring(paren+1, rest.length()-1); // x/y
        String[] sp = inside.split("/");
        try { page = Math.max(0, Integer.parseInt(sp[0].trim()) - 1); } catch(Exception ignore){}
        try { pages = Math.max(1, Integer.parseInt(sp[1].trim())); } catch(Exception ignore){}
    }
    com.minkang.ultimate.random.Roulette r = ((com.minkang.ultimate.random.Main) org.bukkit.Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).getManager().get(key);
    if(r==null) return;

    int slot = e.getRawSlot();
    if(slot == 45 && page > 0){
        e.setCancelled(true);
        open(((com.minkang.ultimate.random.Main) org.bukkit.Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")), p, r, page-1);
        return;
    }
    if(slot == 53 && page < pages-1){
        e.setCancelled(true);
        open(((com.minkang.ultimate.random.Main) org.bukkit.Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")), p, r, page+1);
        return;
    }
    if(slot == 49){
        e.setCancelled(true);
        p.closeInventory();
    }
}


  private final Main plugin; public PreviewGUI(Main p){ this.plugin=p; }
  public static void openInternal_Legacy(Main plugin, Player p, Roulette r){
    String title=plugin.getConfig().getString("titles.preview","룰렛 미리보기: %key%").replace("%key%", r.getKey());
    Inventory inv=Bukkit.createInventory(p,54, ChatColor.translateAlternateColorCodes('&', title));
    boolean showWeight=plugin.getConfig().getBoolean("preview.show-weight", true);
    boolean showChance=plugin.getConfig().getBoolean("preview.show-chance", true);
    String weightFmt=ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("preview.weight-format","&7가중치: &e%weight%"));
    String chanceFmt=ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("preview.chance-format","&7확률: &e%chance%%"));
    java.util.List<String> extraTop=new java.util.ArrayList<>();
    for(String s: plugin.getConfig().getStringList("preview.extra-lore-top")) extraTop.add(ChatColor.translateAlternateColorCodes('&', s));
    java.util.List<String> extraBottom=new java.util.ArrayList<>();
    for(String s: plugin.getConfig().getStringList("preview.extra-lore-bottom")) extraBottom.add(ChatColor.translateAlternateColorCodes('&', s));
    java.util.List<String> removeTags=plugin.getConfig().getStringList("preview.remove-tags");
    int total=r.getTotalWeight(); java.text.DecimalFormat df=new java.text.DecimalFormat("#.##");
    int slot=0; for(RouletteEntry e:r.getEntries()){
      if(slot>=45) break;
      ItemStack it=e.getItem().clone(); ItemMeta m=it.getItemMeta();
      if(m!=null){
        java.util.List<String> lore=m.hasLore()?m.getLore():new java.util.ArrayList<String>(); if(lore==null) lore=new java.util.ArrayList<String>();
        if(removeTags!=null && !removeTags.isEmpty()){
          java.util.Iterator<String> itLore=lore.iterator();
          while(itLore.hasNext()){
            String line=ChatColor.stripColor(itLore.next()).trim();
            String norm=line.replace(" ","").toLowerCase();
            boolean removed=false;
            for(String tag: removeTags){
              if(tag==null) continue;
              String t=tag.trim(); if(t.isEmpty()) continue;
              String tnorm=t.replace(" ","").toLowerCase();
              if(norm.startsWith(tnorm)){ itLore.remove(); removed=true; break; }
            }
          }
        }
        lore.addAll(0, extraTop);
        if(showChance){ double chance=100.0*e.getWeight()/Math.max(1,total); lore.add(chanceFmt.replace("%chance%", df.format(chance))); }
        if(showWeight){ lore.add(weightFmt.replace("%weight%", String.valueOf(e.getWeight()))); }
        lore.addAll(extraBottom);
        m.setLore(lore); it.setItemMeta(m);
      }
      inv.setItem(slot++, it);
    }
    ItemStack draw=new ItemStack(Material.EMERALD_BLOCK);
    ItemMeta dm=draw.getItemMeta();
    if(dm!=null){
      dm.setDisplayName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("gui.preview_draw_button_name","&a[ 뽑기 시작 ]")));
      java.util.List<String> lore=new java.util.ArrayList<String>();
      for(String s: plugin.getConfig().getStringList("gui.preview_draw_button_lore")) lore.add(ChatColor.translateAlternateColorCodes('&', s));
      dm.setLore(lore); draw.setItemMeta(dm);
    }
    inv.setItem(49, draw);
    p.openInventory(inv);
    p.sendMessage(plugin.msg("open_preview").replace("%key%", r.getKey()));
  }
  private boolean hasKeyTag(Main plugin, ItemStack it, String key){
    if(it==null||it.getType()==Material.AIR) return false;
    if(it.getItemMeta()==null) return false;
    String tag=it.getItemMeta().getPersistentDataContainer().get(plugin.getPdcKey(), PersistentDataType.STRING);
    return key.equalsIgnoreCase(tag);
  }
  private boolean consumeOneKey(Main plugin, Player p, String key){
    PlayerInventory pinv=p.getInventory();
    ItemStack main=pinv.getItemInMainHand();
    if(hasKeyTag(plugin, main, key)){
      if(main.getAmount()<=1) pinv.setItemInMainHand(new ItemStack(Material.AIR));
      else{ main.setAmount(main.getAmount()-1); pinv.setItemInMainHand(main); }
      return true;
    }
    try{
      ItemStack off=pinv.getItemInOffHand();
      if(hasKeyTag(plugin, off, key)){
        if(off.getAmount()<=1) pinv.setItemInOffHand(new ItemStack(Material.AIR));
        else{ off.setAmount(off.getAmount()-1); pinv.setItemInOffHand(off); }
        return true;
      }
    }catch(Throwable ignored){}
    for(int i=0;i<pinv.getSize();i++){
      ItemStack it=pinv.getItem(i);
      if(hasKeyTag(plugin, it, key)){
        if(it.getAmount()<=1) pinv.setItem(i, new ItemStack(Material.AIR));
        else{ it.setAmount(it.getAmount()-1); pinv.setItem(i, it); }
        return true;
      }
    }
    return false;
  }
  @EventHandler public void onClick(InventoryClickEvent e){
    if(!(e.getWhoClicked() instanceof Player)) return;
    String title=e.getView().getTitle(); if(title==null) return;
    if(!ChatColor.stripColor(title).startsWith("룰렛 미리보기:")) return;
    e.setCancelled(true);
    if(e.getRawSlot()==49){
      String plain=ChatColor.stripColor(title);
      String key=plain.replace("룰렛 미리보기:","").trim();
      int idx=key.indexOf(" "); if(idx!=-1) key=key.substring(0,idx).trim();
      Roulette r=((com.minkang.ultimate.random.Main)org.bukkit.Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).getManager().get(key);
      Player pl=(Player)e.getWhoClicked();
      if(r==null){ pl.sendMessage(((com.minkang.ultimate.random.Main)org.bukkit.Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).msg("not_found").replace("%key%", key)); pl.closeInventory(); return; }
      if(r.isEmpty()){ pl.sendMessage(((com.minkang.ultimate.random.Main)org.bukkit.Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).msg("no_items")); pl.closeInventory(); return; }
      boolean need=((com.minkang.ultimate.random.Main)org.bukkit.Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).getConfig().getBoolean("gui.draw_consumes_key", true);
      if(need && !consumeOneKey(((com.minkang.ultimate.random.Main)org.bukkit.Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")), pl, r.getKey())){
        pl.sendMessage(((com.minkang.ultimate.random.Main)org.bukkit.Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")).msg("need_key").replace("%key%", r.getKey()));
        pl.playSound(pl.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 0.8f);
        return;
      }
      pl.closeInventory();
      pl.playSound(pl.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
      com.minkang.ultimate.random.gui.SpinnerGUI.start(((com.minkang.ultimate.random.Main)org.bukkit.Bukkit.getPluginManager().getPlugin("UltimateRandomRoulette")), pl, r);
    }
  }
}