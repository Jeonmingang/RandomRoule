package com.minkang.ultimateroulette.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class ItemIO {
    public static String toBase64(ItemStack item) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BukkitObjectOutputStream oos = new BukkitObjectOutputStream(baos);
            oos.writeObject(item);
            oos.close();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize item: " + e.getMessage(), e);
        }
    }
    public static ItemStack fromBase64(String data) {
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            BukkitObjectInputStream ois = new BukkitObjectInputStream(new ByteArrayInputStream(bytes));
            Object obj = ois.readObject();
            ois.close();
            return (ItemStack) obj;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize item: " + e.getMessage(), e);
        }
    }
}
