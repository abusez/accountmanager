/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.util.Session
 */
package me.ksyz.accountmanager.auth;

import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;

public class SessionManager {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static Field field = null;

    private static Field getField() {
        if (field == null) {
            try {
                for (Field f : Minecraft.class.getDeclaredFields()) {
                    if (!f.getType().isAssignableFrom(Session.class)) continue;
                    field = f;
                    field.setAccessible(true);
                    break;
                }
            }
            catch (Exception e) {
                field = null;
            }
        }
        return field;
    }

    public static Session get() {
        return mc.func_110432_I();
    }

    public static void set(Session session) {
        try {
            SessionManager.getField().set(mc, session);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }
}
