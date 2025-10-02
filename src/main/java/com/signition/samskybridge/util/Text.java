package com.signition.samskybridge.util;

import org.bukkit.ChatColor;

public final class Text {
    private Text() {}

    /** Remove color codes (& / §) and return plain text. */
    public static String stripColor(String input) {
        if (input == null) return "";
        String colored = ChatColor.translateAlternateColorCodes('&', input);
        return ChatColor.stripColor(colored);
    }

    /**
     * Colorize text using & codes (e.g., &a, &b, &l).
     */
    public static String color(String input) {
        if (input == null) return "";
        return ChatColor.translateAlternateColorCodes('&', input);
    }
    
}
