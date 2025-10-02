package com.signition.samskybridge.data;

import java.util.UUID;

public class IslandData {
    private java.util.UUID owner; // island owner (nullable for legacy)
    private java.util.Set<java.util.UUID> members = new java.util.HashSet<>();

    private final UUID id;
    private String name;
    private int level;
    private long xp;
    private int size;
    private int teamMax;

    public IslandData(UUID id, String name, int level, long xp, int size, int teamMax){
        this.id = id;
        this.name = name;
        this.level = level;
        this.xp = xp;
        this.size = size;
        this.teamMax = teamMax;
    }
    public UUID getId(){ return id; }
    public String getName(){ return name; }
    public int getLevel(){ return level; }
    public long getXp(){ return xp; }
    public int getSize(){ return size; }
    public int getTeamMax(){ return teamMax; }

    public void setName(String name){ this.name = name; }
    public void setLevel(int level){ this.level = level; }
    public void setXp(long xp){ this.xp = xp; }
    public void addXp(long add){ this.xp += add; }
    public void setSize(int size){ this.size = size; }
    public void setTeamMax(int teamMax){ this.teamMax = teamMax; }


public java.util.UUID getOwner() {
    // fallback: if null, use this island's id as owner for legacy data
    if (owner == null) owner = id;
    return owner;
}
public void setOwner(java.util.UUID owner) {
    this.owner = owner;
}

public java.util.Set<java.util.UUID> getMembers() {
    return members;
}
public void setMembers(java.util.Set<java.util.UUID> members) {
    this.members = (members == null) ? new java.util.HashSet<>() : members;
}

}
