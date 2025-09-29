package com.minkang.ultimate.random;

import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 간단한 디바운서: 500ms 이내 중복 지급 차단 */
public final class GrantService {
    private static final Map<UUID, Long> last = new HashMap<>();
    private GrantService(){}

    public static boolean shouldGive(Player p) {
        long now = System.currentTimeMillis();
        Long prev = last.get(p.getUniqueId());
        if (prev != null && now - prev < 500) {
            return false;
        }
        last.put(p.getUniqueId(), now);
        return true;
    }
}