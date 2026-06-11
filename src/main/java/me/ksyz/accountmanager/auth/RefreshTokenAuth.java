package me.ksyz.accountmanager.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import me.ksyz.accountmanager.gui.GuiAccountManager;
import me.ksyz.accountmanager.utils.Notification;
import me.ksyz.accountmanager.utils.SSLUtil;
import me.ksyz.accountmanager.utils.TextFormatting;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
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

import javax.net.ssl.SSLSocketFactory;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * Microsoft OAuth refresh-token flow based on
 * https://github.com/ravioli-a/refresh-token-authentication
 */
public final class RefreshTokenAuth {
    private static final String CLIENT_ID = "00000000402b5328";
    private static final String REDIRECT_URI = "https://login.live.com/oauth20_desktop.srf";
    private static final String SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";

    private static final Map<Long, String> XSTS_ERRORS = new HashMap<Long, String>();

    static {
        XSTS_ERRORS.put(2148916227L, "The account is banned from Xbox");
        XSTS_ERRORS.put(2148916233L, "The account doesn't have an Xbox account (never signed in)");
        XSTS_ERRORS.put(2148916235L, "The account is from a country where Xbox Live is not available/banned");
        XSTS_ERRORS.put(2148916236L, "The account needs adult verification on Xbox page. (South Korea)");
        XSTS_ERRORS.put(2148916237L, "The account needs adult verification on Xbox page. (South Korea)");
        XSTS_ERRORS.put(2148916238L, "The account is a child (under 18) and cannot proceed unless the account is added to a Family by an adult");
        XSTS_ERRORS.put(2148916262L, "Unknown error");
    }

    private RefreshTokenAuth() {
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
            return HttpClientBuilder.create().setSSLSocketFactory((LayeredConnectionSocketFactory) sslsf).build();
        } catch (Exception e) {
            return HttpClients.createDefault();
        }
    }

    public static CompletableFuture<Account> authenticate(String refreshToken, Executor executor) {
        return refreshMicrosoftToken(refreshToken, executor)
                .thenComposeAsync(tokens -> acquireXboxLiveToken(tokens.get("access_token"), executor)
                        .thenComposeAsync(xbl -> acquireXstsToken(xbl.get("Token"), executor)
                                .thenComposeAsync(xsts -> acquireMinecraftAccessToken(
                                        xsts.get("Token"),
                                        xbl.get("uhs"),
                                        executor
                                ).thenComposeAsync(mcAccessToken -> MicrosoftAuth.login(mcAccessToken, executor)
                                        .thenApply(session -> new Account(
                                                tokens.get("refresh_token"),
                                                mcAccessToken,
                                                session.getUsername(),
                                                session.getPlayerID(),
                                                0L,
                                                AccountType.PREMIUM
                                        ))))), executor);
    }

    private static CompletableFuture<Map<String, String>> refreshMicrosoftToken(String refreshToken, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = createHttpClient()) {
                HttpPost request = new HttpPost(URI.create("https://login.live.com/oauth20_token.srf"));
                request.setConfig(MicrosoftAuth.REQUEST_CONFIG);
                request.setHeader("Content-Type", "application/x-www-form-urlencoded");
                request.setEntity(new UrlEncodedFormEntity(Arrays.asList(
                        new BasicNameValuePair("client_id", CLIENT_ID),
                        new BasicNameValuePair("grant_type", "refresh_token"),
                        new BasicNameValuePair("redirect_uri", REDIRECT_URI),
                        new BasicNameValuePair("refresh_token", refreshToken),
                        new BasicNameValuePair("scope", SCOPE)
                ), "UTF-8"));

                CloseableHttpResponse response = client.execute(request);
                JsonObject json = new JsonParser().parse(EntityUtils.toString(response.getEntity())).getAsJsonObject();

                if (json.has("error")) {
                    String error = json.get("error").getAsString();
                    String description = json.has("error_description") ? json.get("error_description").getAsString() : error;
                    throw new Exception(error + ": " + description);
                }

                String accessToken = Optional.ofNullable(json.get("access_token"))
                        .map(JsonElement::getAsString)
                        .filter(token -> !StringUtils.isBlank(token))
                        .orElseThrow(() -> new Exception("Microsoft access token missing from refresh response."));
                String newRefreshToken = Optional.ofNullable(json.get("refresh_token"))
                        .map(JsonElement::getAsString)
                        .filter(token -> !StringUtils.isBlank(token))
                        .orElse(refreshToken);

                Map<String, String> result = new HashMap<String, String>();
                result.put("access_token", accessToken);
                result.put("refresh_token", newRefreshToken);
                return result;
            } catch (InterruptedException e) {
                throw new CancellationException("Refresh token exchange was cancelled.");
            } catch (Exception e) {
                throw new CompletionException("Unable to refresh Microsoft OAuth token.", e);
            }
        }, executor);
    }

    private static CompletableFuture<Map<String, String>> acquireXboxLiveToken(String accessToken, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = createHttpClient()) {
                GuiAccountManager.notification = new Notification(
                        TextFormatting.translate("&7Acquiring Xbox access token..."),
                        5000L
                );

                JsonObject entity = new JsonObject();
                JsonObject properties = new JsonObject();
                properties.addProperty("AuthMethod", "RPS");
                properties.addProperty("SiteName", "user.auth.xboxlive.com");
                properties.addProperty("RpsTicket", "t=" + accessToken);
                entity.add("Properties", properties);
                entity.addProperty("RelyingParty", "http://auth.xboxlive.com");
                entity.addProperty("TokenType", "JWT");

                HttpPost request = new HttpPost(URI.create("https://user.auth.xboxlive.com/user/authenticate"));
                request.setConfig(MicrosoftAuth.REQUEST_CONFIG);
                request.setHeader("Content-Type", "application/json");
                request.setEntity(new StringEntity(entity.toString()));

                CloseableHttpResponse response = client.execute(request);
                JsonObject json = new JsonParser().parse(EntityUtils.toString(response.getEntity())).getAsJsonObject();

                String token = Optional.ofNullable(json.get("Token"))
                        .map(JsonElement::getAsString)
                        .filter(value -> !StringUtils.isBlank(value))
                        .orElseThrow(() -> new Exception("Xbox Live token missing from response."));

                String uhs = json.get("DisplayClaims").getAsJsonObject()
                        .get("xui").getAsJsonArray()
                        .get(0).getAsJsonObject()
                        .get("uhs").getAsString();

                Map<String, String> result = new HashMap<String, String>();
                result.put("Token", token);
                result.put("uhs", uhs);
                return result;
            } catch (InterruptedException e) {
                throw new CancellationException("Xbox Live token acquisition was cancelled.");
            } catch (Exception e) {
                throw new CompletionException("Unable to acquire Xbox Live token.", e);
            }
        }, executor);
    }

    private static CompletableFuture<Map<String, String>> acquireXstsToken(String xboxToken, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = createHttpClient()) {
                GuiAccountManager.notification = new Notification(
                        TextFormatting.translate("&7Acquiring Xbox XSTS token..."),
                        5000L
                );

                JsonObject entity = new JsonObject();
                JsonObject properties = new JsonObject();
                JsonArray userTokens = new JsonArray();
                userTokens.add(new JsonPrimitive(xboxToken));
                properties.addProperty("SandboxId", "RETAIL");
                properties.add("UserTokens", userTokens);
                entity.add("Properties", properties);
                entity.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
                entity.addProperty("TokenType", "JWT");

                HttpPost request = new HttpPost(URI.create("https://xsts.auth.xboxlive.com/xsts/authorize"));
                request.setConfig(MicrosoftAuth.REQUEST_CONFIG);
                request.setHeader("Content-Type", "application/json");
                request.setEntity(new StringEntity(entity.toString()));

                CloseableHttpResponse response = client.execute(request);
                JsonObject json = new JsonParser().parse(EntityUtils.toString(response.getEntity())).getAsJsonObject();

                if (json.has("XErr")) {
                    long errorCode = json.get("XErr").getAsLong();
                    String message = XSTS_ERRORS.containsKey(errorCode)
                            ? XSTS_ERRORS.get(errorCode)
                            : "Unknown Xbox error (" + errorCode + ")";
                    throw new Exception(message);
                }

                String token = Optional.ofNullable(json.get("Token"))
                        .map(JsonElement::getAsString)
                        .filter(value -> !StringUtils.isBlank(value))
                        .orElseThrow(() -> new Exception("XSTS token missing from response."));

                Map<String, String> result = new HashMap<String, String>();
                result.put("Token", token);
                return result;
            } catch (InterruptedException e) {
                throw new CancellationException("XSTS token acquisition was cancelled.");
            } catch (Exception e) {
                throw new CompletionException("Unable to acquire XSTS token.", e);
            }
        }, executor);
    }

    private static CompletableFuture<String> acquireMinecraftAccessToken(String xstsToken, String userHash, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = createHttpClient()) {
                GuiAccountManager.notification = new Notification(
                        TextFormatting.translate("&7Acquiring Minecraft access token..."),
                        5000L
                );

                HttpPost request = new HttpPost(URI.create("https://api.minecraftservices.com/authentication/login_with_xbox"));
                request.setConfig(MicrosoftAuth.REQUEST_CONFIG);
                request.setHeader("Content-Type", "application/json");
                request.setEntity(new StringEntity(String.format(
                        "{\"identityToken\":\"XBL3.0 x=%s;%s\"}",
                        userHash,
                        xstsToken
                )));

                CloseableHttpResponse response = client.execute(request);
                JsonObject json = new JsonParser().parse(EntityUtils.toString(response.getEntity())).getAsJsonObject();

                return Optional.ofNullable(json.get("access_token"))
                        .map(JsonElement::getAsString)
                        .filter(token -> !StringUtils.isBlank(token))
                        .orElseThrow(() -> new Exception(json.has("error")
                                ? json.get("error").getAsString() + ": " + json.get("errorMessage").getAsString()
                                : "Minecraft access token missing from response."));
            } catch (InterruptedException e) {
                throw new CancellationException("Minecraft access token acquisition was cancelled.");
            } catch (Exception e) {
                throw new CompletionException("Unable to acquire Minecraft access token.", e);
            }
        }, executor);
    }
}
