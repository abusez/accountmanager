/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.util.Session
 */
package me.ksyz.accountmanager.auth;

import java.lang.reflect.Field;
import java.util.UUID;
import me.ksyz.accountmanager.AccountManager;
import me.ksyz.accountmanager.auth.SessionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;

public class CrackedAuth {
    public static boolean login(String username) {
        if (username == null || username.trim().isEmpty()) {
            System.err.println("username cannot be null or empty!");
            return false;
        }
        AccountManager.addCrackedAccount(username);
        String uuid = CrackedAuth.getUUID(username);
        Session session = new Session(username, uuid, "accessToken", "legacy");
        CrackedAuth.setMinecraftSession(session);
        SessionManager.set(session);
        System.out.println("successfully logged in as: " + username);
        return true;
    }

    private static String getUUID(String name) {
        String offlinePlayerString = "offlinePlayer:" + name;
        return UUID.nameUUIDFromBytes(offlinePlayerString.getBytes()).toString().replace("-", "");
    }

    private static void setMinecraftSession(Session session) {
        try {
            Minecraft mc = Minecraft.func_71410_x();
            Field sessionField = Minecraft.class.getDeclaredField("session");
            sessionField.setAccessible(true);
            sessionField.set(mc, session);
        }
        catch (Exception e) {
            System.err.println("failed to set Minecraft session: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
