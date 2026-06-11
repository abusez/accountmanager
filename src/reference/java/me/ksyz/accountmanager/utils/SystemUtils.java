/*
 * Decompiled with CFR 0.152.
 */
package me.ksyz.accountmanager.utils;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.net.URI;

public class SystemUtils {
    public static void openWebLink(URI url) {
        try {
            Class<?> desktop = Class.forName("java.awt.Desktop");
            Object object = desktop.getMethod("getDesktop", new Class[0]).invoke(null, new Object[0]);
            desktop.getMethod("browse", URI.class).invoke(object, url);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static void setClipboard(String text) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }
}
