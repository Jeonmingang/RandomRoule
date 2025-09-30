package com.signition.samskybridge.data;
import java.util.UUID;
public class IslandData {
    private final UUID owner;
    private String name;
    private int level = 1;
    private long xp = 0L;
    private int sizeTier = 0;
    private int memberCapTier = 0;
    public IslandData(UUID owner, String name){
        this.owner = owner;
        this.name = name;
    }
    public UUID getOwner(){ return owner; }
    public String getName(){ return name; }
    public void setName(String n){ this.name = n; }
    public int getLevel(){ return level; }
    public void setLevel(int l){ this.level = Math.max(1, l); }
    public long getXp(){ return xp; }
    public void setXp(long x){ this.xp = Math.max(0L, x); }
    public void addXp(long add){ this.xp += Math.max(0L, add); }
    public int getSizeTier(){ return sizeTier; }
    public void setSizeTier(int t){ this.sizeTier = Math.max(0, t); }
    public int getMemberCapTier(){ return memberCapTier; }
    public void setMemberCapTier(int t){ this.memberCapTier = Math.max(0, t); }
}
