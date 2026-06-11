/*
 * Decompiled with CFR 0.152.
 */
package me.ksyz.accountmanager.utils;

import java.io.InputStream;
import java.security.KeyStore;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class SSLUtil {
    private static SSLContext ctx;

    public static SSLContext getSSLContext() {
        return ctx;
    }

    static {
        try {
            KeyStore myKeyStore = KeyStore.getInstance("JKS");
            InputStream keystoreStream = SSLUtil.class.getResourceAsStream("/accountmanager-ssl.jks");
            if (keystoreStream == null) {
                throw new RuntimeException("Could not find accountmanager-ssl.jks in resources");
            }
            myKeyStore.load(keystoreStream, "changeit".toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(myKeyStore);
            ctx = SSLContext.getInstance("TLS");
            ctx.init(null, tmf.getTrustManagers(), null);
            HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to initialize custom SSLContext", e);
        }
    }
}
