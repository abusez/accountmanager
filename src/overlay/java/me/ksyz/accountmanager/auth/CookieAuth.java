package me.ksyz.accountmanager.auth;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import me.ksyz.accountmanager.AccountManager;
import me.ksyz.accountmanager.gui.GuiAccountManager;
import me.ksyz.accountmanager.gui.GuiCookieAuth;
import me.ksyz.accountmanager.utils.Notification;
import me.ksyz.accountmanager.utils.SSLUtil;
import me.ksyz.accountmanager.utils.TextFormatting;
import net.minecraft.util.Session;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CookieAuth {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4);
    private static final Gson GSON = new Gson();
    private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom()
            .setConnectionRequestTimeout(30000)
            .setConnectTimeout(30000)
            .setSocketTimeout(30000)
            .build();

    private static final List<String> COOKIE_ORDER_JSHP = Arrays.asList(
            "__Host-MSAAUTH", "__Host-MSAAUTHP",
            "JSHP", "JSH",
            "MSPAuth", "MSPBack", "MSPProf", "MSPRequ", "MSPSoftVis", "MSPOK",
            "MSPShared", "MSPPre", "MSPCID", "MSPOAuthVis",
            "AMCSecAuth", "NAP", "ANON", "OParams", "PPLState", "WLSSC", "uaid", "pres", "LOpt"
    );

    private static final List<String> COOKIE_ORDER_JSH = Arrays.asList(
            "__Host-MSAAUTH", "__Host-MSAAUTHP",
            "JSH", "JSHP",
            "MSPAuth", "MSPBack", "MSPProf", "MSPRequ", "MSPSoftVis", "MSPOK",
            "MSPShared", "MSPPre", "MSPCID", "MSPOAuthVis",
            "AMCSecAuth", "NAP", "ANON", "OParams", "PPLState", "WLSSC", "uaid", "pres", "LOpt"
    );

    private static final String OAUTH_URL =
            "https://login.live.com/oauth20_authorize.srf"
                    + "?redirect_uri=https://sisu.xboxlive.com/connect/oauth/XboxLive"
                    + "&response_type=token"
                    + "&client_id=000000004420578E"
                    + "&scope=XboxLive.Signin%20XboxLive.offline_access"
                    + "&prompt=none";

    public static CompletableFuture<Boolean> addAccountFromCookieFile(File cookieFile, GuiCookieAuth gui) {
        CompletableFuture<Boolean> future = new CompletableFuture<Boolean>();
        EXECUTOR.execute(() -> {
            try {
                gui.setStatus("&fReading cookie file...&r");
                Map<String, String> cookieMap = parseCookieFile(cookieFile);
                if (cookieMap.isEmpty()) {
                    gui.setStatus("&cNo valid Microsoft cookies found in file&r");
                    future.complete(false);
                    return;
                }
                if (!hasRequiredCookies(cookieMap)) {
                    gui.setStatus("&cMissing auth cookies (need __Host-MSAAUTH, JSH, or JSHP)&r");
                    future.complete(false);
                    return;
                }
                gui.setStatus("&fAuthenticating with Microsoft...&r");
                authenticateWithCookies(cookieMap, gui).whenComplete((result, ex) -> {
                    if (ex != null) {
                        System.err.println("[CookieAuth] Authentication failed: " + ex.getMessage());
                        ex.printStackTrace();
                        gui.setStatus("&cAuthentication failed: " + ex.getMessage() + "&r");
                        future.complete(false);
                    } else {
                        future.complete(result);
                    }
                });
            } catch (Exception e) {
                gui.setStatus("&cError processing cookie file: " + e.getMessage() + "&r");
                e.printStackTrace();
                future.complete(false);
            }
        });
        return future;
    }

    private static boolean hasRequiredCookies(Map<String, String> cookies) {
        return cookies.containsKey("__Host-MSAAUTH")
                || cookies.containsKey("__Host-MSAAUTHP")
                || cookies.containsKey("JSH")
                || cookies.containsKey("JSHP");
    }

    private static Map<String, String> parseCookieFile(File cookieFile) throws IOException {
        String content = readFile(cookieFile);
        if (content.trim().isEmpty()) {
            return Collections.emptyMap();
        }

        if (content.trim().startsWith("[")) {
            return parseJsonCookies(content);
        }

        Map<String, String> cookies = parseNetscapeCookies(content);
        if (!cookies.isEmpty()) {
            return cookies;
        }

        return parseLooseCookies(content);
    }

    private static String readFile(File cookieFile) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(cookieFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
        }
        return builder.toString();
    }

    private static Map<String, String> parseJsonCookies(String content) {
        LinkedHashMap<String, String> cookies = new LinkedHashMap<String, String>();
        try {
            JsonElement root = new JsonParser().parse(content);
            JsonArray array;
            if (root.isJsonArray()) {
                array = root.getAsJsonArray();
            } else if (root.isJsonObject() && root.getAsJsonObject().has("cookies")) {
                array = root.getAsJsonObject().getAsJsonArray("cookies");
            } else {
                return cookies;
            }

            for (JsonElement element : array) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject obj = element.getAsJsonObject();
                if (!obj.has("name") || !obj.has("value")) {
                    continue;
                }
                if (obj.has("expirationDate")) {
                    double expiration = obj.get("expirationDate").getAsDouble();
                    if (expiration > 0 && expiration < System.currentTimeMillis() / 1000.0) {
                        continue;
                    }
                }
                String domain = "";
                if (obj.has("domain")) {
                    domain = obj.get("domain").getAsString();
                } else if (obj.has("host")) {
                    domain = obj.get("host").getAsString();
                }
                String name = obj.get("name").getAsString().trim();
                String value = obj.get("value").getAsString().trim();
                if (!isMicrosoftAuthDomain(domain) || !isAuthCookieName(name) || value.isEmpty()) {
                    continue;
                }
                putCookie(cookies, name, value);
            }
        } catch (Exception e) {
            System.err.println("[CookieAuth] Failed to parse JSON cookies: " + e.getMessage());
        }
        return cookies;
    }

    private static Map<String, String> parseNetscapeCookies(String content) {
        LinkedHashMap<String, String> cookies = new LinkedHashMap<String, String>();
        for (String line : content.split("\\r?\\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split("\t", 7);
            if (parts.length < 7) {
                continue;
            }

            String domain = parts[0].trim().toLowerCase();
            String name = parts[5].trim();
            String value = parts[6].trim();
            if (!isMicrosoftAuthDomain(domain) || !isAuthCookieName(name) || value.isEmpty()) {
                continue;
            }
            putCookie(cookies, name, value);
        }
        return cookies;
    }

    private static Map<String, String> parseLooseCookies(String content) {
        LinkedHashMap<String, String> cookies = new LinkedHashMap<String, String>();
        String normalized = content.replace("\n", "").replace("\r", "");

        for (String segment : normalized.split(";")) {
            segment = segment.trim();
            if (!segment.contains("=")) {
                continue;
            }
            int equals = segment.indexOf('=');
            String name = segment.substring(0, equals).trim();
            String value = segment.substring(equals + 1).trim();
            if (!isAuthCookieName(name) || value.isEmpty()) {
                continue;
            }
            putCookie(cookies, name, value);
        }

        if (cookies.isEmpty()) {
            for (String line : content.split("\\r?\\n")) {
                line = line.trim();
                if (!line.contains("=")) {
                    continue;
                }
                int equals = line.indexOf('=');
                String name = line.substring(0, equals).trim();
                String value = line.substring(equals + 1).trim();
                if (!isAuthCookieName(name) || value.isEmpty()) {
                    continue;
                }
                putCookie(cookies, name, value);
            }
        }

        return cookies;
    }

    private static void putCookie(Map<String, String> cookies, String name, String value) {
        if ("Disabled".equalsIgnoreCase(value)) {
            return;
        }
        cookies.put(name, value);
    }

    private static boolean isMicrosoftAuthDomain(String domain) {
        if (domain == null || domain.isEmpty()) {
            return true;
        }
        domain = domain.toLowerCase();
        return domain.contains("live.com")
                || domain.contains("microsoftonline.com")
                || domain.contains("microsoft.com")
                || domain.contains("xboxlive.com");
    }

    private static boolean isAuthCookieName(String name) {
        return name.startsWith("__Host-")
                || name.startsWith("MSP")
                || name.equals("JSH")
                || name.equals("JSHP")
                || name.equals("NAP")
                || name.equals("OParams")
                || name.equals("PPLState")
                || name.equals("WLSSC")
                || name.equals("uaid")
                || name.equals("AMCSecAuth")
                || name.equals("ESTSAUTH")
                || name.equals("ESTSAUTHPERSISTENT")
                || name.equals("MSPOK")
                || name.equals("MSPShared")
                || name.equals("MSPPre")
                || name.equals("MSPCID")
                || name.equals("ANON")
                || name.equals("pres")
                || name.equals("LOpt")
                || name.equals("MSPOAuthVis");
    }

    private static String buildCookieHeader(Map<String, String> cookies, List<String> preferredOrder) {
        ArrayList<String> orderedNames = new ArrayList<String>(preferredOrder);
        for (String name : cookies.keySet()) {
            if (!orderedNames.contains(name)) {
                orderedNames.add(name);
            }
        }

        StringBuilder header = new StringBuilder();
        for (String name : orderedNames) {
            if (!cookies.containsKey(name)) {
                continue;
            }
            if (header.length() > 0) {
                header.append("; ");
            }
            header.append(name).append('=').append(cookies.get(name));
        }
        return header.toString();
    }

    private static String getAccessTokenFromCookies(Map<String, String> cookies) throws Exception {
        Exception lastError = null;
        List<List<String>> orderings = new ArrayList<List<String>>();
        orderings.add(COOKIE_ORDER_JSHP);
        orderings.add(COOKIE_ORDER_JSH);

        for (List<String> ordering : orderings) {
            String cookieHeader = buildCookieHeader(cookies, ordering);
            if (StringUtils.isBlank(cookieHeader)) {
                continue;
            }
            try {
                String token = requestOAuthToken(cookieHeader);
                if (token != null) {
                    return token;
                }
            } catch (Exception e) {
                lastError = e;
            }
        }

        if (lastError != null) {
            throw lastError;
        }
        return null;
    }

    private static String requestOAuthToken(String cookieHeader) throws Exception {
        try (CloseableHttpClient client = createHttpClient()) {
            String currentUrl = OAUTH_URL;

            for (int hop = 0; hop < 12; hop++) {
                HttpGet request = new HttpGet(URI.create(currentUrl));
                request.setConfig(REQUEST_CONFIG);
                request.setHeader("Host", URI.create(currentUrl).getHost());
                request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36");
                request.setHeader("Cookie", cookieHeader);
                request.setHeader("Accept", "*/*");
                request.setHeader("Accept-Language", "en-US,en;q=0.9");
                request.setHeader("Connection", "keep-alive");

                CloseableHttpResponse response = client.execute(request);
                int responseCode = response.getStatusLine().getStatusCode();
                String location = response.getFirstHeader("Location") != null
                        ? response.getFirstHeader("Location").getValue()
                        : null;
                EntityUtils.consume(response.getEntity());
                response.close();

                if (location != null) {
                    String oauthError = extractOAuthError(location);
                    if (oauthError != null) {
                        throw new IOException(oauthError);
                    }

                    String token = extractAccessToken(location);
                    if (token != null) {
                        return token;
                    }

                    if (responseCode == 302 || responseCode == 303 || responseCode == 301 || responseCode == 307) {
                        currentUrl = resolveRedirectUrl(currentUrl, location);
                        continue;
                    }
                }

                if (responseCode == 200) {
                    return null;
                }
                break;
            }
        }
        return null;
    }

    private static String resolveRedirectUrl(String currentUrl, String location) throws Exception {
        URI base = URI.create(currentUrl);
        URI target = base.resolve(location);
        return target.toString();
    }

    private static String extractOAuthError(String location) {
        String query = location;
        if (location.contains("#")) {
            query = location.split("#", 2)[1];
        } else if (location.contains("?")) {
            query = location.split("\\?", 2)[1];
        }
        String error = null;
        String description = null;
        for (String param : query.split("&")) {
            if (param.startsWith("error=")) {
                error = param.substring("error=".length());
            } else if (param.startsWith("error_description=")) {
                description = param.substring("error_description=".length());
            }
        }
        if (error == null) {
            return null;
        }
        try {
            error = URLDecoder.decode(error, "UTF-8");
            if (description != null) {
                description = URLDecoder.decode(description, "UTF-8");
            }
        } catch (Exception ignored) {
        }
        return description != null ? error + ": " + description : error;
    }

    private static String extractAccessToken(String location) throws Exception {
        if (location.contains("#")) {
            String fragment = location.split("#", 2)[1];
            for (String param : fragment.split("&")) {
                if (param.startsWith("access_token=")) {
                    return URLDecoder.decode(param.substring("access_token=".length()), "UTF-8");
                }
            }
        }
        if (location.contains("access_token=")) {
            int start = location.indexOf("access_token=") + "access_token=".length();
            int end = location.indexOf('&', start);
            String token = end == -1 ? location.substring(start) : location.substring(start, end);
            return URLDecoder.decode(token, "UTF-8");
        }
        return null;
    }

    private static CompletableFuture<Boolean> authenticateWithCookies(Map<String, String> cookies, GuiCookieAuth gui) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                gui.setStatus("&fRequesting Microsoft access token...&r");
                String accessToken = getAccessTokenFromCookies(cookies);
                if (accessToken == null) {
                    gui.setStatus("&cFailed to get access token (cookies may be expired)&r");
                    return false;
                }

                gui.setStatus("&fAuthenticating with Xbox Live...&r");
                Map<String, String> xbl = acquireXboxLiveToken(accessToken);
                gui.setStatus("&fGetting XSTS token...&r");
                String xstsToken = acquireXstsToken(xbl.get("Token"));
                String xblToken = "XBL3.0 x=" + xbl.get("uhs") + ";" + xstsToken;

                gui.setStatus("&fAuthenticating with Minecraft...&r");
                McResponse mcResponse = postMinecraftLogin(xblToken);
                if (mcResponse == null || mcResponse.access_token == null) {
                    gui.setStatus("&cFailed to get Minecraft access token&r");
                    return false;
                }

                gui.setStatus("&fRetrieving Minecraft profile...&r");
                ProfileResponse profile = getMinecraftProfile(mcResponse.access_token);
                if (profile == null || profile.name == null) {
                    gui.setStatus("&cFailed to get Minecraft profile&r");
                    return false;
                }

                Session session = new Session(profile.name, profile.id, mcResponse.access_token, "mojang");
                AccountManager.accounts.add(new Account("", mcResponse.access_token, profile.name, profile.id, 0L, AccountType.PREMIUM));
                AccountManager.save();
                SessionManager.set(session);
                gui.setStatus("&aSuccessfully logged in as " + session.getUsername() + "&r");
                return true;
            } catch (Exception e) {
                System.err.println("[CookieAuth] Authentication failed: " + e.getMessage());
                e.printStackTrace();
                gui.setStatus("&cAuthentication failed: " + e.getMessage() + "&r");
                return false;
            }
        });
    }

    private static Map<String, String> acquireXboxLiveToken(String accessToken) throws Exception {
        Exception lastError = null;
        for (String ticketPrefix : new String[]{"t=", "d="}) {
            try {
                JsonObject entity = new JsonObject();
                JsonObject properties = new JsonObject();
                properties.addProperty("AuthMethod", "RPS");
                properties.addProperty("SiteName", "user.auth.xboxlive.com");
                properties.addProperty("RpsTicket", ticketPrefix + accessToken);
                entity.add("Properties", properties);
                entity.addProperty("RelyingParty", "http://auth.xboxlive.com");
                entity.addProperty("TokenType", "JWT");

                try (CloseableHttpClient client = createHttpClient()) {
                    HttpPost request = new HttpPost(URI.create("https://user.auth.xboxlive.com/user/authenticate"));
                    request.setConfig(REQUEST_CONFIG);
                    request.setHeader("Content-Type", "application/json");
                    request.setHeader("User-Agent", "Go-http-client/1.1");
                    request.setHeader("X-Xbl-Contract-Version", "0");
                    request.setEntity(new StringEntity(entity.toString(), StandardCharsets.UTF_8));

                    CloseableHttpResponse response = client.execute(request);
                    int code = response.getStatusLine().getStatusCode();
                    String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    response.close();

                    if (code != 200) {
                        throw new IOException("Xbox Live authentication failed (" + code + "): " + body);
                    }

                    JsonObject json = new JsonParser().parse(body).getAsJsonObject();
                    Map<String, String> result = new LinkedHashMap<String, String>();
                    result.put("Token", json.get("Token").getAsString());
                    result.put("uhs", json.getAsJsonObject("DisplayClaims")
                            .getAsJsonArray("xui").get(0).getAsJsonObject()
                            .get("uhs").getAsString());
                    return result;
                }
            } catch (Exception e) {
                lastError = e;
            }
        }
        throw lastError != null ? lastError : new IOException("Xbox Live authentication failed");
    }

    private static String acquireXstsToken(String xboxToken) throws Exception {
        JsonObject entity = new JsonObject();
        JsonObject properties = new JsonObject();
        JsonArray userTokens = new JsonArray();
        userTokens.add(new JsonPrimitive(xboxToken));
        properties.addProperty("SandboxId", "RETAIL");
        properties.add("UserTokens", userTokens);
        entity.add("Properties", properties);
        entity.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        entity.addProperty("TokenType", "JWT");

        try (CloseableHttpClient client = createHttpClient()) {
            HttpPost request = new HttpPost(URI.create("https://xsts.auth.xboxlive.com/xsts/authorize"));
            request.setConfig(REQUEST_CONFIG);
            request.setHeader("Content-Type", "application/json");
            request.setHeader("User-Agent", "Go-http-client/1.1");
            request.setHeader("X-Xbl-Contract-Version", "0");
            request.setEntity(new StringEntity(entity.toString(), StandardCharsets.UTF_8));

            CloseableHttpResponse response = client.execute(request);
            int code = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            response.close();

            if (code != 200) {
                throw new IOException("XSTS authentication failed (" + code + "): " + body);
            }

            JsonObject json = new JsonParser().parse(body).getAsJsonObject();
            if (json.has("XErr")) {
                throw new IOException("XSTS error: " + json.get("XErr").getAsString());
            }
            return json.get("Token").getAsString();
        }
    }

    public static McResponse postMinecraftLogin(String xblToken) throws Exception {
        GuiAccountManager.notification = new Notification(TextFormatting.translate("&7Logging into Minecraft services..."), 5000L);
        String payload = "{\"identityToken\":\"" + xblToken + "\",\"ensureLegacyEnabled\":true}";

        try (CloseableHttpClient client = createHttpClient()) {
            HttpPost request = new HttpPost(URI.create("https://api.minecraftservices.com/authentication/login_with_xbox"));
            request.setConfig(REQUEST_CONFIG);
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Accept", "application/json");
            request.setEntity(new StringEntity(payload, StandardCharsets.UTF_8));

            CloseableHttpResponse response = client.execute(request);
            int code = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            response.close();

            if (code != 200) {
                throw new IOException("Minecraft login failed (" + code + "): " + body);
            }
            return GSON.fromJson(body, McResponse.class);
        }
    }

    public static ProfileResponse getMinecraftProfile(String accessToken) throws Exception {
        GuiAccountManager.notification = new Notification(TextFormatting.translate("&7Fetching Minecraft profile..."), 5000L);

        try (CloseableHttpClient client = createHttpClient()) {
            HttpGet request = new HttpGet(URI.create("https://api.minecraftservices.com/minecraft/profile"));
            request.setConfig(REQUEST_CONFIG);
            request.setHeader("Authorization", "Bearer " + accessToken);
            request.setHeader("Accept", "application/json");

            CloseableHttpResponse response = client.execute(request);
            int code = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            response.close();

            if (code != 200) {
                throw new IOException("Minecraft profile request failed (" + code + "): " + body);
            }
            return GSON.fromJson(body, ProfileResponse.class);
        }
    }

    private static CloseableHttpClient createHttpClient() {
        try {
            SSLSocketFactory socketFactory = SSLUtil.getSSLContext().getSocketFactory();
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                    socketFactory,
                    new String[]{"TLSv1.2"},
                    null,
                    (X509HostnameVerifier) new BrowserCompatHostnameVerifier()
            );
            return HttpClientBuilder.create()
                    .setSSLSocketFactory((LayeredConnectionSocketFactory) sslsf)
                    .disableRedirectHandling()
                    .build();
        } catch (Exception e) {
            return HttpClients.custom().disableRedirectHandling().build();
        }
    }

    public static void shutdown() {
        EXECUTOR.shutdown();
    }

    public static class ProfileResponse {
        public String name;
        public String id;
    }

    public static class McResponse {
        public String access_token;
    }
}
