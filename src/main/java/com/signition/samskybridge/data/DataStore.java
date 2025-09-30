package com.signition.samskybridge.data;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
public class DataStore {
    private final Map<UUID, IslandData> islands = new ConcurrentHashMap<>();
    public IslandData getOrCreate(UUID owner, String name) {
        if (owner == null) return null;
        return islands.computeIfAbsent(owner, u -> new IslandData(u, name == null ? "unknown" : name));
    }
    public Collection<IslandData> all(){ return islands.values(); }
    public void save(){}
}
