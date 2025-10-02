package com.signition.samskybridge.rank;

import com.signition.samskybridge.Main;
import com.signition.samskybridge.data.DataStore;
import com.signition.samskybridge.data.IslandData;
import com.signition.samskybridge.level.LevelService;
import com.signition.samskybridge.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import java.util.UUID;
import java.util.Set;
import java.util.List;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class RankingService implements Listener {
    private final Main plugin;
    private final DataStore store;


    // TAB prefix support (scoreboard teams)
    private Scoreboard board;
    private Team tLeader, tMember, tNone;
    private final Map<UUID, Integer> rankCache = new HashMap<>();
    private long lastRankBuild = 0L;
    private static final long RANK_REBUILD_COOLDOWN_MS = 3000L;

    public RankingService(Main plugin, DataStore store) {
        this.plugin = plugin;
        this.store = store;
        initTabTeams();

    }
    // Overload for backward-compat: older Main passed LevelService, ignore it.
    public RankingService(Main plugin, DataStore store, LevelService ignored) {
        this(plugin, store);
    }

    /** 외부에서 주기적으로 호출 가능 */
    public void refreshRanking() {
        rankCache.clear();
        Map<UUID, IslandData> all = fetchAllIslands();
        List<UUID> sorted = all.values().stream()
                .sorted(Comparator.<IslandData>comparingInt(this::levelOf).reversed()
                        .thenComparingLong(this::xpOf).reversed())
                .map(this::ownerOf)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        int idx = 1;
        for (UUID u : sorted) rankCache.put(u, idx++);
    }

    public Integer getRank(UUID owner) {
        if (!rankCache.containsKey(owner)) refreshRanking();
        return rankCache.getOrDefault(owner, -1);
    }

    public void sendTop(Player p, int count) {
        if (count <= 0) count = 10;
        refreshRanking();

        Map<UUID, IslandData> map = fetchAllIslands();
        List<IslandData> sorted = map.values().stream()
                .sorted(Comparator.<IslandData>comparingInt(this::levelOf).reversed()
                        .thenComparingLong(this::xpOf).reversed())
                .limit(count)
                .collect(Collectors.toList());

        String header = plugin.getConfig().getString("ranking.header", "&b섬 랭킹 Top <count>").replace("<count>", String.valueOf(count));
        p.sendMessage(Text.color(header));

        int rank = 1;
        for (IslandData is : sorted) {
            String leader = nameOf(ownerOf(is));
            String members = memberNames(is);
            int sizeUp = readInt(is, 0, "sizeUpgrades", "sizeUpgradeCount", "sizeLevel");
            int memberUp = readInt(is, 0, "memberUpgrades", "memberUpgradeCount", "memberLevel");
            String fmt = plugin.getConfig().getString(
                    "ranking.line",
                    "&7[ 섬 랭킹 <rank>위 ] &b<leader> &7(Lv.<level>) &8| &7업글: 크기 <size_up> / 인원 <member_up> &8| &7인원: <members>"
            );
            String line = Text.color(fmt
                    .replace("<rank>", String.valueOf(rank++))
                    .replace("<leader>", leader)
                    .replace("<level>", String.valueOf(levelOf(is)))
                    .replace("<size_up>", String.valueOf(sizeUp))
                    .replace("<member_up>", String.valueOf(memberUp))
                    .replace("<members>", members));
            p.sendMessage(line);
        }

        p.sendMessage(Text.color(plugin.getConfig().getString("ranking.footer", "&8------------------------------------")));
    }

    /* ===== internals ===== */

    private Map<UUID, IslandData> fetchAllIslands() {
        // 우선 DataStore API 시도
        try {
            Method m = DataStore.class.getMethod("getAllIslands");
            Object r = m.invoke(store);
            if (r instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<UUID, IslandData> map = (Map<UUID, IslandData>) r;
                return map;
            }
        } catch (Throwable ignore) {}
        // 폴백: private 필드 접근
        try {
            Field f = DataStore.class.getDeclaredField("islandMap");
            f.setAccessible(true);
            Object obj = f.get(store);
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<UUID, IslandData> map = (Map<UUID, IslandData>) obj;
                return map;
            }
        } catch (Throwable ignore) {}
        return new HashMap<>();
    }

    private UUID ownerOf(IslandData is) {
        try {
            try { return (UUID) IslandData.class.getMethod("getOwner").invoke(is); }
            catch (NoSuchMethodException ignore) {}
            Field f = IslandData.class.getDeclaredField("owner");
            f.setAccessible(true);
            Object v = f.get(is);
            if (v instanceof UUID) return (UUID) v;
        } catch (Throwable ignored) {}
        return null;
    }

    private int levelOf(IslandData is) {
        return readInt(is, 0, "level");
    }
    private long xpOf(IslandData is) {
        for (String fn : new String[]{"xp", "experience", "islandXp"}) {
            try {
                Method m = IslandData.class.getMethod("get" + up(fn));
                Object v = m.invoke(is);
                if (v instanceof Number) return ((Number) v).longValue();
            } catch (Throwable ignore) {
                try {
                    Field f = IslandData.class.getDeclaredField(fn);
                    f.setAccessible(true);
                    Object v = f.get(is);
                    if (v instanceof Number) return ((Number) v).longValue();
                } catch (Throwable ignored) {}
            }
        }
        return 0L;
    }

    private String nameOf(UUID uuid) {
        if (uuid == null) return "알수없음";
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return op != null && op.getName() != null ? op.getName() : uuid.toString().substring(0, 8);
    }

    @SuppressWarnings("unchecked")
    private Set<UUID> membersOf(IslandData is) {
        try {
            try { return (Set<UUID>) IslandData.class.getMethod("getMembers").invoke(is); }
            catch (NoSuchMethodException ignore) {}
            Field f = IslandData.class.getDeclaredField("members");
            f.setAccessible(true);
            Object v = f.get(is);
            if (v instanceof Set) return (Set<UUID>) v;
            if (v instanceof Collection) return new HashSet<>((Collection<UUID>) v);
        } catch (Throwable ignored) {}
        return Collections.emptySet();
    }

    private String memberNames(IslandData is) {
        Set<UUID> members = membersOf(is);
        if (members.isEmpty()) return "없음";
        return members.stream().map(this::nameOf).collect(Collectors.joining(", "));
    }

    private int readInt(IslandData is, int def, String... fields) {
        for (String fn : fields) {
            try {
                Method m = IslandData.class.getMethod("get" + up(fn));
                Object v = m.invoke(is);
                if (v instanceof Number) return ((Number) v).intValue();
            } catch (Throwable ignore) {
                try {
                    Field f = IslandData.class.getDeclaredField(fn);
                    f.setAccessible(true);
                    Object v = f.get(is);
                    if (v instanceof Number) return ((Number) v).intValue();
                } catch (Throwable ignored) {}
            }
        }
        return def;
    }
    private String up(String s) { return s.length()==0? s : Character.toUpperCase(s.charAt(0))+s.substring(1); }

    /* ===== TAB: dynamic rank/level per role ===== */
    private void initTab(){
        try {
            this.board = org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard();
        } catch (Throwable t){
            plugin.getLogger().warning("[RankingService] scoreboard init failed: " + t.getMessage());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e){
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> applyTab(e.getPlayer()), 10L);
    }

    public void refreshTabAll(){
        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()){
            applyTab(p);
        }
    }

    private String cut16(String s){
        if (s == null) return "";
        return s.length() > 16 ? s.substring(0,16) : s;
    }

    public void applyTab(Player p){
        if (p == null) return;
        if (board == null){
            try { this.board = org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard(); }
            catch (Throwable ignore){ return; }
        }
        String teamName = "SSB_" + p.getName();
        if (teamName.length() > 16) teamName = teamName.substring(0,16);
        Team t = board.getTeam(teamName);
        if (t == null) t = board.registerNewTeam(teamName);

        // role + island
        UUID uid = p.getUniqueId();
        int role = resolveRole(p); // 2 leader, 1 member, 0 none

        com.signition.samskybridge.data.IslandData island = null;
        UUID ownerId = null;
        int level = 0;
        String islandName = "";
        String leaderName = "";

        boolean usedBento = false;
        if (plugin.getConfig().getBoolean("integration.bentobox.enabled", true)){
            Object bIs = bentoGetIsland(p.getWorld(), uid);
            if (bIs != null){
                usedBento = true;
                ownerId = bentoOwner(bIs);
                islandName = bentoIslandName(bIs);
                // level은 우리 레벨서비스 기반이면 DataStore에서 가져오므로 0 유지 허용
            }
        }
        if (!usedBento){
            try {
                java.util.List<com.signition.samskybridge.data.IslandData> list = store.all();
                for (com.signition.samskybridge.data.IslandData is : list){
                    UUID own = is.getOwner();
                    java.util.Set<UUID> mem = is.getMembers();
                    if ((own != null && own.equals(uid)) || (mem != null && mem.contains(uid))) {
                        island = is;
                        ownerId = own;
                        break;
                    }
                }
            } catch (Throwable ignore){}
            if (island != null) {
                try { level = island.getLevel(); } catch (Throwable ignore){}
                try { islandName = String.valueOf(island.getName()); } catch (Throwable ignore){}
            }
        }

        if (ownerId != null){
            org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(ownerId);
            if (op != null && op.getName() != null) leaderName = op.getName();
        }
        String leaderName = "";
        if (island != null){
            try { islandName = String.valueOf(island.getName()); } catch (Throwable ignore){}
            try {
                UUID own = island.getOwner();
                if (own != null){
                    OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(own);
                    leaderName = (op != null && op.getName() != null) ? op.getName() : own.toString();
                }
            } catch (Throwable ignore){}
        }

        String pref, suf;
        if (role == 2){ // leader
            String pFmt = plugin.getConfig().getString("tab.dynamic.leader.prefix", "&7[ &a섬 랭킹 &f<rank>위 &7] ");
            String sFmt = plugin.getConfig().getString("tab.dynamic.leader.suffix", "&7[ &bLv.<level> &7]");
            pref = pFmt; suf = sFmt;
        } else if (role == 1){ // member
            String pFmt = plugin.getConfig().getString("tab.dynamic.member.prefix", "&7[ &a<island> 섬 &7| &f<rank>위 ] ");
            String sFmt = plugin.getConfig().getString("tab.dynamic.member.suffix", "&7[ &3Lv.<level> &7]");
            pref = pFmt; suf = sFmt;
        } else {
            String pFmt = plugin.getConfig().getString("tab.dynamic.none.prefix", "&7[등록안됨]");
            String sFmt = plugin.getConfig().getString("tab.dynamic.none.suffix", "");
            pref = pFmt; suf = sFmt;
        }

        pref = pref.replace("<rank>", (rank<=0?"-":String.valueOf(rank)))
                   .replace("<level>", String.valueOf(level))
                   .replace("<island>", islandName)
                   .replace("<leader>", leaderName);
        suf  = suf.replace("<rank>", (rank<=0?"-":String.valueOf(rank)))
                   .replace("<level>", String.valueOf(level))
                   .replace("<island>", islandName)
                   .replace("<leader>", leaderName);

        pref = com.signition.samskybridge.util.Text.color(pref);
        suf  = com.signition.samskybridge.util.Text.color(suf);
        if (pref.length() > 16) pref = cut16(pref);
        if (suf.length() > 16)  suf  = cut16(suf);

        t.setPrefix(pref);
        t.setSuffix(suf);
        if (!t.hasEntry(p.getName())) t.addEntry(p.getName());
        try { p.setScoreboard(board); } catch (Throwable ignore){}
    }

    /** 2=leader,1=member,0=none */
    private int resolveRole(Player p){
        UUID uid = p.getUniqueId();
        if (plugin.getConfig().getBoolean("integration.bentobox.enabled", true)){
            Object island = bentoGetIsland(p.getWorld(), uid);
            if (island != null){
                UUID owner = bentoOwner(island);
                if (owner != null && owner.equals(uid)) return 2;
                java.util.Set<java.util.UUID> mem = bentoMembers(island);
                if (mem != null && mem.contains(uid)) return 1;
                return 1;
            }
        }
        // Fallback DataStore
        try {
            java.util.List<com.signition.samskybridge.data.IslandData> list = store.all();
            for (com.signition.samskybridge.data.IslandData is : list){
                java.util.UUID owner = is.getOwner();
                if (owner != null && owner.equals(uid)) return 2;
                java.util.Set<java.util.UUID> mem = is.getMembers();
                if (mem != null && mem.contains(uid)) return 1;
            }
        } catch (Throwable ignore){}
        return 0;
    }



    /* ===== Bento helpers ===== */
    private Object bentoGetIsland(org.bukkit.World w, java.util.UUID uid){
        if (!plugin.getConfig().getBoolean("integration.bentobox.enabled", true)) return null;
        try {
            Class<?> bb = Class.forName("world.bentobox.bentobox.BentoBox");
            Object bbx = bb.getMethod("getInstance").invoke(null);
            Object islands = bb.getMethod("getIslands").invoke(bbx);
            java.lang.reflect.Method mGetIsland = islands.getClass().getMethod("getIsland", org.bukkit.World.class, java.util.UUID.class);
            return mGetIsland.invoke(islands, w, uid);
        } catch (Throwable t){ return null; }
    }
    private java.util.UUID bentoOwner(Object island){
        if (island == null) return null;
        try { return (java.util.UUID) island.getClass().getMethod("getOwner").invoke(island); } catch (Throwable t){ return null; }
    }
    @SuppressWarnings("unchecked")
    private java.util.Set<java.util.UUID> bentoMembers(Object island){
        if (island == null) return java.util.Collections.emptySet();
        try { return (java.util.Set<java.util.UUID>) island.getClass().getMethod("getMemberSet").invoke(island); } catch (Throwable t){ return java.util.Collections.emptySet(); }
    }
    private String bentoIslandName(Object island){
        if (island == null) return "";
        try {
            Object name = null;
            try { name = island.getClass().getMethod("getName").invoke(island); } catch (Throwable ignore){}
            if (name != null) return String.valueOf(name);
            java.util.UUID owner = bentoOwner(island);
            if (owner != null){
                org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(owner);
                if (op != null && op.getName() != null) return op.getName();
            }
            return "";
        } catch (Throwable t){ return ""; }
    }


    public void removeFromTeams(org.bukkit.entity.Player p){
        if (board == null || p == null) return;
        String teamName = "SSB_" + p.getName();
        if (teamName.length() > 16) teamName = teamName.substring(0,16);
        org.bukkit.scoreboard.Team t = board.getTeam(teamName);
        if (t != null) {
            try { t.removeEntry(p.getName()); } catch (Throwable ignore){}
            try { if (t.getEntries().isEmpty()) t.unregister(); } catch (Throwable ignore){}
        }
    }

    /** Rebuild rank cache from island levels in descending order. */
    public synchronized void rebuildRanks(){
        try {
            java.util.List<com.signition.samskybridge.data.IslandData> list = store.all();
            list.sort((a,b) -> Integer.compare(
                b != null ? b.getLevel() : 0,
                a != null ? a.getLevel() : 0
            ));
            this.rankCache.clear();
            int r = 1;
            for (com.signition.samskybridge.data.IslandData is : list){
                if (is == null) continue;
                java.util.UUID owner = is.getOwner();
                if (owner != null) {
                    this.rankCache.put(owner, r++);
                }
            }
            this.lastRankBuild = System.currentTimeMillis();
        } catch (Throwable t){
            plugin.getLogger().warning("[RankingService] rebuildRanks failed: " + t.getMessage());
        }
    }
}
