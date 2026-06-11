/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.google.gson.JsonPrimitive
 *  net.minecraft.util.Session
 *  net.minecraft.util.Session$Type
 *  org.apache.commons.io.IOUtils
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.http.HttpEntity
 *  org.apache.http.NameValuePair
 *  org.apache.http.client.config.RequestConfig
 *  org.apache.http.client.entity.UrlEncodedFormEntity
 *  org.apache.http.client.methods.CloseableHttpResponse
 *  org.apache.http.client.methods.HttpGet
 *  org.apache.http.client.methods.HttpPost
 *  org.apache.http.client.methods.HttpUriRequest
 *  org.apache.http.client.utils.URIBuilder
 *  org.apache.http.client.utils.URLEncodedUtils
 *  org.apache.http.conn.socket.LayeredConnectionSocketFactory
 *  org.apache.http.conn.ssl.BrowserCompatHostnameVerifier
 *  org.apache.http.conn.ssl.SSLConnectionSocketFactory
 *  org.apache.http.conn.ssl.X509HostnameVerifier
 *  org.apache.http.entity.StringEntity
 *  org.apache.http.impl.client.CloseableHttpClient
 *  org.apache.http.impl.client.HttpClientBuilder
 *  org.apache.http.impl.client.HttpClients
 *  org.apache.http.message.BasicNameValuePair
 *  org.apache.http.util.EntityUtils
 */
package me.ksyz.accountmanager.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.sun.net.httpserver.HttpServer;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.net.ssl.SSLSocketFactory;
import me.ksyz.accountmanager.utils.SSLUtil;
import net.minecraft.util.Session;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public final class MicrosoftAuth {
    public static final RequestConfig REQUEST_CONFIG = RequestConfig.custom().setConnectionRequestTimeout(30000).setConnectTimeout(30000).setSocketTimeout(30000).build();
    public static final String CLIENT_ID = "42a60a84-599d-44b2-a7c6-b00cdef1d6a2";
    public static final int PORT = 25575;

    public static URI getMSAuthLink(String state) {
        try {
            URIBuilder uriBuilder = new URIBuilder("https://login.live.com/oauth20_authorize.srf").addParameter("client_id", CLIENT_ID).addParameter("response_type", "code").addParameter("redirect_uri", String.format("http://localhost:%d/callback", 25575)).addParameter("scope", "XboxLive.signin XboxLive.offline_access").addParameter("state", state).addParameter("prompt", "select_account");
            return uriBuilder.build();
        }
        catch (Exception e) {
            return null;
        }
    }

    public static CompletableFuture<String> acquireMSAuthCode(String state, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            HttpServer server = HttpServer.create(new InetSocketAddress(25575), 0);
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Object> authCode = new AtomicReference<Object>(null);
            AtomicReference<Object> errorMsg = new AtomicReference<Object>(null);
            server.createContext("/callback", exchange -> {
                Map<String, String> query = URLEncodedUtils.parse((String)exchange.getRequestURI().toString().replaceAll("/callback\\?", ""), (Charset)StandardCharsets.UTF_8).stream().collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
                if (!state.equals(query.get("state"))) {
                    errorMsg.set(String.format("State mismatch! Expected '%s' but got '%s'.", state, query.get("state")));
                } else if (query.containsKey("code")) {
                    authCode.set(query.get("code"));
                } else if (query.containsKey("error")) {
                    errorMsg.set(String.format("%s: %s", query.get("error"), query.get("error_description")));
                }
                InputStream stream = MicrosoftAuth.class.getResourceAsStream("/callback.html");
                byte[] response = stream != null ? IOUtils.toByteArray((InputStream)stream) : new byte[]{};
                exchange.getResponseHeaders().add("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.getResponseBody().close();
                latch.countDown();
            });
            try {
                server.start();
                latch.await();
                String string = Optional.ofNullable(authCode.get()).filter(code -> !StringUtils.isBlank((CharSequence)code)).orElseThrow(() -> new Exception(Optional.ofNullable(errorMsg.get()).orElse("There was no auth code or error description present.")));
                server.stop(2);
                return string;
            }
            catch (Throwable throwable) {
                try {
                    server.stop(2);
                    throw throwable;
                }
                catch (InterruptedException e) {
                    throw new CancellationException("Microsoft auth code acquisition was cancelled!");
                }
                catch (Exception e) {
                    throw new CompletionException("Unable to acquire Microsoft auth code!", e);
                }
            }
        }, executor);
    }

    private static CloseableHttpClient createTrustedHttpClient() {
        try {
            SSLSocketFactory socketFactory = SSLUtil.getSSLContext().getSocketFactory();
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(socketFactory, new String[]{"TLSv1.2"}, null, (X509HostnameVerifier)new BrowserCompatHostnameVerifier());
            return HttpClientBuilder.create().setSSLSocketFactory((LayeredConnectionSocketFactory)sslsf).build();
        }
        catch (Exception e) {
            e.printStackTrace();
            return HttpClients.createDefault();
        }
    }

    public static CompletableFuture<Map<String, String>> acquireMSAccessTokens(String authCode, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = MicrosoftAuth.createTrustedHttpClient();){
                HttpPost request = new HttpPost(URI.create("https://login.live.com/oauth20_token.srf"));
                request.setConfig(REQUEST_CONFIG);
                request.setHeader("Content-Type", "application/x-www-form-urlencoded");
                request.setEntity((HttpEntity)new UrlEncodedFormEntity(Arrays.asList(new BasicNameValuePair("client_id", CLIENT_ID), new BasicNameValuePair("grant_type", "authorization_code"), new BasicNameValuePair("code", authCode), new BasicNameValuePair("redirect_uri", String.format("http://localhost:%d/callback", 25575))), "UTF-8"));
                CloseableHttpResponse res = client.execute((HttpUriRequest)request);
                JsonObject json = new JsonParser().parse(EntityUtils.toString((HttpEntity)res.getEntity())).getAsJsonObject();
                String accessToken = Optional.ofNullable(json.get("access_token")).map(JsonElement::getAsString).filter(token -> !StringUtils.isBlank((CharSequence)token)).orElseThrow(() -> new Exception(json.has("error") ? String.format("%s: %s", json.get("error").getAsString(), json.get("error_description").getAsString()) : "There was no Microsoft access token or error description present."));
                String refreshToken = Optional.ofNullable(json.get("refresh_token")).map(JsonElement::getAsString).filter(token -> !StringUtils.isBlank((CharSequence)token)).orElseThrow(() -> new Exception(json.has("error") ? String.format("%s: %s", json.get("error").getAsString(), json.get("error_description").getAsString()) : "There was no Microsoft refresh token or error description present."));
                HashMap<String, String> result = new HashMap<String, String>();
                result.put("access_token", accessToken);
                result.put("refresh_token", refreshToken);
                HashMap<String, String> hashMap = result;
                return hashMap;
            }
            catch (InterruptedException e) {
                throw new CancellationException("Microsoft access tokens acquisition was cancelled!");
            }
            catch (Exception e) {
                throw new CompletionException("Unable to acquire Microsoft access tokens!", e);
            }
        }, executor);
    }

    public static CompletableFuture<Map<String, String>> refreshMSAccessTokens(String msToken, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = MicrosoftAuth.createTrustedHttpClient();){
                HttpPost request = new HttpPost(URI.create("https://login.live.com/oauth20_token.srf"));
                request.setConfig(REQUEST_CONFIG);
                request.setHeader("Content-Type", "application/x-www-form-urlencoded");
                request.setEntity((HttpEntity)new UrlEncodedFormEntity(Arrays.asList(new BasicNameValuePair("client_id", CLIENT_ID), new BasicNameValuePair("grant_type", "refresh_token"), new BasicNameValuePair("refresh_token", msToken), new BasicNameValuePair("redirect_uri", String.format("http://localhost:%d/callback", 25575))), "UTF-8"));
                CloseableHttpResponse res = client.execute((HttpUriRequest)request);
                JsonObject json = new JsonParser().parse(EntityUtils.toString((HttpEntity)res.getEntity())).getAsJsonObject();
                String accessToken = Optional.ofNullable(json.get("access_token")).map(JsonElement::getAsString).filter(token -> !StringUtils.isBlank((CharSequence)token)).orElseThrow(() -> new Exception(json.has("error") ? String.format("%s: %s", json.get("error").getAsString(), json.get("error_description").getAsString()) : "There was no Microsoft access token or error description present."));
                String refreshToken = Optional.ofNullable(json.get("refresh_token")).map(JsonElement::getAsString).filter(token -> !StringUtils.isBlank((CharSequence)token)).orElseThrow(() -> new Exception(json.has("error") ? String.format("%s: %s", json.get("error").getAsString(), json.get("error_description").getAsString()) : "There was no Microsoft refresh token or error description present."));
                HashMap<String, String> result = new HashMap<String, String>();
                result.put("access_token", accessToken);
                result.put("refresh_token", refreshToken);
                HashMap<String, String> hashMap = result;
                return hashMap;
            }
            catch (InterruptedException e) {
                throw new CancellationException("Microsoft access tokens acquisition was cancelled!");
            }
            catch (Exception e) {
                throw new CompletionException("Unable to acquire Microsoft access tokens!", e);
            }
        }, executor);
    }

    public static CompletableFuture<String> acquireXboxAccessToken(String accessToken, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = MicrosoftAuth.createTrustedHttpClient();){
                HttpPost request = new HttpPost(URI.create("https://user.auth.xboxlive.com/user/authenticate"));
                JsonObject entity = new JsonObject();
                JsonObject properties = new JsonObject();
                properties.addProperty("AuthMethod", "RPS");
                properties.addProperty("SiteName", "user.auth.xboxlive.com");
                properties.addProperty("RpsTicket", String.format("d=%s", accessToken));
                entity.add("Properties", (JsonElement)properties);
                entity.addProperty("RelyingParty", "http://auth.xboxlive.com");
                entity.addProperty("TokenType", "JWT");
                request.setConfig(REQUEST_CONFIG);
                request.setHeader("Content-Type", "application/json");
                request.setEntity((HttpEntity)new StringEntity(entity.toString()));
                CloseableHttpResponse res = client.execute((HttpUriRequest)request);
                JsonObject json = res.getStatusLine().getStatusCode() == 200 ? new JsonParser().parse(EntityUtils.toString((HttpEntity)res.getEntity())).getAsJsonObject() : new JsonObject();
                String string = Optional.ofNullable(json.get("Token")).map(JsonElement::getAsString).filter(token -> !StringUtils.isBlank((CharSequence)token)).orElseThrow(() -> new Exception(json.has("XErr") ? String.format("%s: %s", json.get("XErr").getAsString(), json.get("Message").getAsString()) : "There was no access token or error description present."));
                return string;
            }
            catch (InterruptedException e) {
                throw new CancellationException("Xbox Live access token acquisition was cancelled!");
            }
            catch (Exception e) {
                throw new CompletionException("Unable to acquire Xbox Live access token!", e);
            }
        }, executor);
    }

    public static CompletableFuture<Map<String, String>> acquireXboxXstsToken(String accessToken, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = MicrosoftAuth.createTrustedHttpClient();){
                HttpPost request = new HttpPost("https://xsts.auth.xboxlive.com/xsts/authorize");
                JsonObject entity = new JsonObject();
                JsonObject properties = new JsonObject();
                JsonArray userTokens = new JsonArray();
                userTokens.add((JsonElement)new JsonPrimitive(accessToken));
                properties.addProperty("SandboxId", "RETAIL");
                properties.add("UserTokens", (JsonElement)userTokens);
                entity.add("Properties", (JsonElement)properties);
                entity.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
                entity.addProperty("TokenType", "JWT");
                request.setConfig(REQUEST_CONFIG);
                request.setHeader("Content-Type", "application/json");
                request.setEntity((HttpEntity)new StringEntity(entity.toString()));
                CloseableHttpResponse res = client.execute((HttpUriRequest)request);
                JsonObject json = res.getStatusLine().getStatusCode() == 200 ? new JsonParser().parse(EntityUtils.toString((HttpEntity)res.getEntity())).getAsJsonObject() : new JsonObject();
                Map map = Optional.ofNullable(json.get("Token")).map(JsonElement::getAsString).filter(token -> !StringUtils.isBlank((CharSequence)token)).map(token -> {
                    String uhs = json.get("DisplayClaims").getAsJsonObject().get("xui").getAsJsonArray().get(0).getAsJsonObject().get("uhs").getAsString();
                    HashMap<String, String> result = new HashMap<String, String>();
                    result.put("Token", (String)token);
                    result.put("uhs", uhs);
                    return result;
                }).orElseThrow(() -> new Exception(json.has("XErr") ? String.format("%s: %s", json.get("XErr").getAsString(), json.get("Message").getAsString()) : "There was no access token or error description present."));
                return map;
            }
            catch (InterruptedException e) {
                throw new CancellationException("Xbox Live XSTS token acquisition was cancelled!");
            }
            catch (Exception e) {
                throw new CompletionException("Unable to acquire Xbox Live XSTS token!", e);
            }
        }, executor);
    }

    public static CompletableFuture<String> acquireMCAccessToken(String xstsToken, String userHash, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = MicrosoftAuth.createTrustedHttpClient();){
                HttpPost request = new HttpPost(URI.create("https://api.minecraftservices.com/authentication/login_with_xbox"));
                request.setConfig(REQUEST_CONFIG);
                request.setHeader("Content-Type", "application/json");
                request.setEntity((HttpEntity)new StringEntity(String.format("{\"identityToken\": \"XBL3.0 x=%s;%s\"}", userHash, xstsToken)));
                CloseableHttpResponse res = client.execute((HttpUriRequest)request);
                JsonObject json = new JsonParser().parse(EntityUtils.toString((HttpEntity)res.getEntity())).getAsJsonObject();
                String string = Optional.ofNullable(json.get("access_token")).map(JsonElement::getAsString).filter(token -> !StringUtils.isBlank((CharSequence)token)).orElseThrow(() -> new Exception(json.has("error") ? String.format("%s: %s", json.get("error").getAsString(), json.get("errorMessage").getAsString()) : "There was no access token or error description present."));
                return string;
            }
            catch (InterruptedException e) {
                throw new CancellationException("Minecraft access token acquisition was cancelled!");
            }
            catch (Exception e) {
                throw new CompletionException("Unable to acquire Minecraft access token!", e);
            }
        }, executor);
    }

    public static CompletableFuture<Session> login(String mcToken, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = MicrosoftAuth.createTrustedHttpClient();){
                HttpGet request = new HttpGet(URI.create("https://api.minecraftservices.com/minecraft/profile"));
                request.setConfig(REQUEST_CONFIG);
                request.setHeader("Authorization", "Bearer " + mcToken);
                CloseableHttpResponse res = client.execute((HttpUriRequest)request);
                JsonObject json = new JsonParser().parse(EntityUtils.toString((HttpEntity)res.getEntity())).getAsJsonObject();
                if (json.has("error")) {
                    throw new Exception(String.format("%s: %s", json.get("error").getAsString(), json.get("errorMessage").getAsString()));
                }
                Session session = Optional.ofNullable(json.get("id")).map(JsonElement::getAsString).filter(uuid -> !StringUtils.isBlank((CharSequence)uuid)).map(uuid -> new Session(json.get("name").getAsString(), uuid, mcToken, Session.Type.MOJANG.toString())).orElseThrow(() -> new Exception("Minecraft profile ID (UUID) was missing from the response."));
                return session;
            }
            catch (InterruptedException e) {
                throw new CancellationException("Minecraft profile fetching was cancelled!");
            }
            catch (Exception e) {
                throw new CompletionException("Unable to fetch Minecraft profile!", e);
            }
        }, executor);
    }

    public static CompletableFuture<Session> login(String accessToken, String username, String uuid, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            if (StringUtils.isBlank((CharSequence)accessToken) || StringUtils.isBlank((CharSequence)username) || StringUtils.isBlank((CharSequence)uuid)) {
                throw new IllegalArgumentException("Access Token, Username, and UUID cannot be empty for direct login.");
            }
            return new Session(username, uuid, accessToken, Session.Type.MOJANG.toString());
        }, executor);
    }
}
