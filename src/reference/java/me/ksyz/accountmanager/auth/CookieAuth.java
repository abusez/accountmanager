/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  net.minecraft.util.Session
 */
package me.ksyz.accountmanager.auth;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javax.net.ssl.HttpsURLConnection;
import me.ksyz.accountmanager.AccountManager;
import me.ksyz.accountmanager.auth.Account;
import me.ksyz.accountmanager.auth.AccountType;
import me.ksyz.accountmanager.auth.SessionManager;
import me.ksyz.accountmanager.gui.GuiAccountManager;
import me.ksyz.accountmanager.gui.GuiCookieAuth;
import me.ksyz.accountmanager.utils.Notification;
import me.ksyz.accountmanager.utils.TextFormatting;
import net.minecraft.util.Session;

public class CookieAuth {
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final Gson gson = new Gson();

    public static CompletableFuture<Boolean> addAccountFromCookieFile(File cookieFile, GuiCookieAuth gui) {
        CompletableFuture<Boolean> future = new CompletableFuture<Boolean>();
        executor.execute(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader((InputStream)new FileInputStream(cookieFile), StandardCharsets.UTF_8));){
                gui.status = "&fReading cookie file...&r";
                Map<String, String> cookieMap = CookieAuth.parseCookies(reader);
                if (cookieMap.isEmpty()) {
                    gui.status = "&cNo valid Microsoft cookies found&r";
                    future.complete(false);
                    return;
                }
                gui.status = "&fBuilding cookie string...&r";
                CookieAuth.authenticateWithCookies(cookieMap, gui).whenComplete((result, ex) -> {
                    if (ex != null) {
                        System.err.println("[CookieAuth] Authentication failed: " + ex.getMessage());
                        ex.printStackTrace();
                        gui.status = "&cAuthentication failed: " + ex.getMessage() + "&r";
                        future.complete(false);
                    } else {
                        future.complete((Boolean)result);
                    }
                });
            }
            catch (Exception e) {
                gui.status = "&cError processing cookie file&r";
                e.printStackTrace();
                future.complete(false);
            }
        });
        return future;
    }

    private static Map<String, String> parseCookies(BufferedReader reader) throws IOException {
        String line;
        LinkedHashMap<String, String> cookies = new LinkedHashMap<String, String>();
        while ((line = reader.readLine()) != null) {
            String value;
            String[] parts;
            if ((line = line.trim()).isEmpty() || line.startsWith("#") || !line.contains("\t") || (parts = line.split("\t")).length < 7) continue;
            String domain = parts[0].toLowerCase();
            String name = parts[5].trim();
            String string = value = parts.length > 6 ? parts[6].trim() : "";
            if (!domain.contains("login.live.com") && !domain.equals(".live.com") && !domain.equals("login.live.com") && !domain.equals(".account.microsoft.com")) continue;
            if (value.equals("Disabled") || value.isEmpty()) {
                value = "Disabled";
            }
            if (cookies.containsKey(name)) continue;
            cookies.put(name, value);
        }
        return cookies;
    }

    private static String buildCookieHeader(Map<String, String> cookies, boolean useJSH) {
        ArrayList<String> cookieOrder = new ArrayList<String>(Arrays.asList(useJSH ? "JSH" : "JSHP", "MSPAuth", "MSPBack", "MSPProf", "MSPRequ", "MSPSoftVis", "NAP", "OParams", "PPLState", "WLSSC"));
        ArrayList<String> extraCookies = new ArrayList<String>();
        for (String name : cookies.keySet()) {
            if (cookieOrder.contains(name) || !name.equals("__Host-MSAAUTH") && !name.startsWith("__Host-") && !name.equals("uaid")) continue;
            extraCookies.add(name);
        }
        cookieOrder.addAll(extraCookies);
        StringBuilder cookieHeader = new StringBuilder();
        for (String cookieName : cookieOrder) {
            if (!cookies.containsKey(cookieName)) continue;
            if (cookieHeader.length() > 0) {
                cookieHeader.append("; ");
            }
            cookieHeader.append(cookieName).append("=").append(cookies.get(cookieName));
        }
        return cookieHeader.toString();
    }

    private static String getAccessTokenFromCookie(Map<String, String> cookies, boolean useJSH) throws Exception {
        String cookieHeader = CookieAuth.buildCookieHeader(cookies, useJSH);
        String oauthUrl = "https://login.live.com/oauth20_authorize.srf?redirect_uri=https://sisu.xboxlive.com/connect/oauth/XboxLive&response_type=token&client_id=000000004420578E&scope=XboxLive.Signin%20XboxLive.offline_access";
        HttpsURLConnection conn = (HttpsURLConnection)new URL(oauthUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Host", "login.live.com");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36");
        conn.setRequestProperty("Cookie", cookieHeader);
        conn.setRequestProperty("Accept-Encoding", "gzip");
        conn.setRequestProperty("Connection", "keep-alive");
        conn.setRequestProperty("Accept", "*/*");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
        conn.setInstanceFollowRedirects(false);
        conn.connect();
        int responseCode = conn.getResponseCode();
        String location = conn.getHeaderField("Location");
        conn.disconnect();
        if (responseCode == 302 && location != null && location.contains("#access_token=")) {
            String fragment = location.split("#", 2)[1];
            HashMap<String, String> params = new HashMap<String, String>();
            for (String param : fragment.split("&")) {
                if (!param.contains("=")) continue;
                String[] kv = param.split("=", 2);
                params.put(kv[0], kv[1]);
            }
            String accessToken = (String)params.get("access_token");
            if (accessToken != null) {
                return URLDecoder.decode(accessToken, StandardCharsets.UTF_8.name());
            }
        }
        return null;
    }

    private static CompletableFuture<Boolean> authenticateWithCookies(Map<String, String> cookies, GuiCookieAuth gui) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                gui.status = "&fStarting authentication process...&r";
                boolean hasJSHP = cookies.containsKey("JSHP");
                boolean hasJSH = cookies.containsKey("JSH");
                String accessToken = null;
                if (hasJSHP) {
                    gui.status = "&fTrying authentication with JSHP...&r";
                    try {
                        accessToken = CookieAuth.getAccessTokenFromCookie(cookies, false);
                    }
                    catch (Exception e) {
                        System.err.println("[CookieAuth] JSHP attempt failed: " + e.getMessage());
                    }
                }
                if (accessToken == null && hasJSH) {
                    gui.status = "&fTrying authentication with JSH...&r";
                    try {
                        accessToken = CookieAuth.getAccessTokenFromCookie(cookies, true);
                    }
                    catch (Exception e) {
                        System.err.println("[CookieAuth] JSH attempt failed: " + e.getMessage());
                    }
                }
                if (accessToken == null) {
                    gui.status = "&cFailed to get access token&r";
                    return false;
                }
                gui.status = "&fAuthenticating with Xbox Live...&r";
                JsonObject xboxPayload = new JsonObject();
                JsonObject xboxProps = new JsonObject();
                xboxProps.addProperty("AuthMethod", "RPS");
                xboxProps.addProperty("SiteName", "user.auth.xboxlive.com");
                xboxProps.addProperty("RpsTicket", "d=" + accessToken);
                xboxPayload.add("Properties", (JsonElement)xboxProps);
                xboxPayload.addProperty("RelyingParty", "http://auth.xboxlive.com");
                xboxPayload.addProperty("TokenType", "JWT");
                HttpsURLConnection xboxConn = (HttpsURLConnection)new URL("https://user.auth.xboxlive.com/user/authenticate").openConnection();
                xboxConn.setRequestMethod("POST");
                xboxConn.setRequestProperty("Content-Type", "application/json");
                xboxConn.setRequestProperty("User-Agent", "Go-http-client/1.1");
                xboxConn.setRequestProperty("X-Xbl-Contract-Version", "0");
                xboxConn.setDoOutput(true);
                try (OutputStream os = xboxConn.getOutputStream();){
                    os.write(gson.toJson((JsonElement)xboxPayload).getBytes(StandardCharsets.UTF_8));
                }
                if (xboxConn.getResponseCode() != 200) {
                    gui.status = "&cXbox Live authentication failed&r";
                    return false;
                }
                JsonObject xboxData = (JsonObject)gson.fromJson((Reader)new InputStreamReader(xboxConn.getInputStream(), StandardCharsets.UTF_8), JsonObject.class);
                xboxConn.disconnect();
                String xboxToken = xboxData.get("Token").getAsString();
                JsonObject displayClaims = xboxData.getAsJsonObject("DisplayClaims");
                String uhs = displayClaims.getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();
                gui.status = "&fGetting XSTS token...&r";
                JsonObject xstsPayload = new JsonObject();
                JsonObject xstsProps = new JsonObject();
                xstsProps.addProperty("SandboxId", "RETAIL");
                xstsProps.add("UserTokens", gson.toJsonTree(Collections.singletonList(xboxToken)));
                xstsPayload.add("Properties", (JsonElement)xstsProps);
                xstsPayload.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
                xstsPayload.addProperty("TokenType", "JWT");
                HttpsURLConnection xstsConn = (HttpsURLConnection)new URL("https://xsts.auth.xboxlive.com/xsts/authorize").openConnection();
                xstsConn.setRequestMethod("POST");
                xstsConn.setRequestProperty("Content-Type", "application/json");
                xstsConn.setRequestProperty("User-Agent", "Go-http-client/1.1");
                xstsConn.setRequestProperty("X-Xbl-Contract-Version", "0");
                xstsConn.setDoOutput(true);
                try (OutputStream os = xstsConn.getOutputStream();){
                    os.write(gson.toJson((JsonElement)xstsPayload).getBytes(StandardCharsets.UTF_8));
                }
                if (xstsConn.getResponseCode() != 200) {
                    gui.status = "&cXSTS authentication failed&r";
                    return false;
                }
                JsonObject xstsData = (JsonObject)gson.fromJson((Reader)new InputStreamReader(xstsConn.getInputStream(), StandardCharsets.UTF_8), JsonObject.class);
                xstsConn.disconnect();
                String xstsToken = xstsData.get("Token").getAsString();
                String xblToken = "XBL3.0 x=" + uhs + ";" + xstsToken;
                gui.status = "&fAuthenticating with Minecraft...&r";
                McResponse mcRes = CookieAuth.postMinecraftLogin(xblToken);
                if (mcRes == null || mcRes.access_token == null) {
                    System.err.println("[AuthFlow] Failed to get Minecraft access token");
                    gui.status = "&cFailed to get Minecraft access token&r";
                    return false;
                }
                gui.status = "&fRetrieving Minecraft profile...&r";
                ProfileResponse profileRes = CookieAuth.getMinecraftProfile(mcRes.access_token);
                if (profileRes == null || profileRes.name == null) {
                    System.err.println("[AuthFlow] Failed to get Minecraft profile");
                    gui.status = "&cFailed to get Minecraft profile&r";
                    return false;
                }
                gui.status = "&aCreating Minecraft session...&r";
                Session session = new Session(profileRes.name, profileRes.id, mcRes.access_token, "mojang");
                gui.status = "&aSaving account details...&r";
                AccountManager.accounts.add(new Account("", mcRes.access_token, profileRes.name, profileRes.id, 0L, AccountType.PREMIUM));
                AccountManager.save();
                SessionManager.set(session);
                System.out.println("[AuthFlow] Successfully logged in as " + session.func_111285_a());
                gui.status = "&aSuccessfully logged in as " + session.func_111285_a() + "&r";
                return true;
            }
            catch (Exception e) {
                System.err.println("[AuthFlow] Authentication failed: " + e.getMessage());
                e.printStackTrace();
                gui.status = "&cAuthentication failed: " + e.getMessage() + "&r";
                return false;
            }
        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static McResponse postMinecraftLogin(String xblToken) throws Exception {
        GuiAccountManager.notification = new Notification(TextFormatting.translate("&7Logging into Minecraft services..."), 5000L);
        String url = "https://api.minecraftservices.com/authentication/login_with_xbox";
        String payload = "{\"identityToken\":\"" + xblToken + "\",\"ensureLegacyEnabled\":true}";
        HttpsURLConnection conn = (HttpsURLConnection)new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream();){
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            String errorResponse = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
            throw new IOException("HTTP error code: " + responseCode + ", Response: " + errorResponse);
        }
        StringBuilder response = new StringBuilder();
        try (InputStream is = conn.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));){
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        finally {
            conn.disconnect();
        }
        return (McResponse)gson.fromJson(response.toString(), McResponse.class);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static ProfileResponse getMinecraftProfile(String accessToken) throws Exception {
        GuiAccountManager.notification = new Notification(TextFormatting.translate("&7Fetching Minecraft profile..."), 5000L);
        String url = "https://api.minecraftservices.com/minecraft/profile";
        HttpsURLConnection conn = (HttpsURLConnection)new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Accept", "application/json");
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            String errorResponse = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
            throw new IOException("HTTP error code: " + responseCode + ", Response: " + errorResponse);
        }
        StringBuilder response = new StringBuilder();
        try (InputStream is = conn.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));){
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        finally {
            conn.disconnect();
        }
        return (ProfileResponse)gson.fromJson(response.toString(), ProfileResponse.class);
    }

    public static void shutdown() {
        executor.shutdown();
    }

    public static class ProfileResponse {
        public String name;
        public String id;
    }

    public static class McResponse {
        public String access_token;
    }
}
