/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 */
package me.ksyz.accountmanager.auth;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Optional;
import me.ksyz.accountmanager.auth.AccountType;

public class Account {
    private String refreshToken;
    private String accessToken;
    private String username;
    private String uuid;
    private long unban;
    private AccountType type;

    public Account(String refreshToken, String accessToken, String username, String uuid) {
        this(refreshToken, accessToken, username, uuid, 0L, AccountType.PREMIUM);
    }

    public Account(String refreshToken, String accessToken, String username, String uuid, long unban) {
        this(refreshToken, accessToken, username, uuid, unban, AccountType.PREMIUM);
    }

    public Account(String refreshToken, String accessToken, String username, String uuid, long unban, AccountType type) {
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
        this.username = username;
        this.uuid = uuid;
        this.unban = unban;
        this.type = type;
    }

    public Account(String username, String accessToken, String uuid) {
        this("", accessToken, username, uuid, 0L, AccountType.PREMIUM);
    }

    public String getRefreshToken() {
        return this.refreshToken;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public String getUsername() {
        return this.username;
    }

    public String getUuid() {
        return this.uuid;
    }

    public long getUnban() {
        return this.unban;
    }

    public AccountType getType() {
        return this.type;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setUnban(long unban) {
        this.unban = unban;
    }

    public void setType(AccountType type) {
        this.type = type;
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("refreshToken", this.refreshToken);
        jsonObject.addProperty("accessToken", this.accessToken);
        jsonObject.addProperty("username", this.username);
        jsonObject.addProperty("uuid", this.uuid);
        jsonObject.addProperty("unban", (Number)this.unban);
        jsonObject.addProperty("type", this.type.toString());
        return jsonObject;
    }

    public static Account fromJson(JsonObject jsonObject) {
        return new Account(Optional.ofNullable(jsonObject.get("refreshToken")).map(JsonElement::getAsString).orElse(""), Optional.ofNullable(jsonObject.get("accessToken")).map(JsonElement::getAsString).orElse(""), Optional.ofNullable(jsonObject.get("username")).map(JsonElement::getAsString).orElse(""), Optional.ofNullable(jsonObject.get("uuid")).map(JsonElement::getAsString).orElse(""), Optional.ofNullable(jsonObject.get("unban")).map(JsonElement::getAsLong).orElse(0L), Optional.ofNullable(jsonObject.get("type")).map(JsonElement::getAsString).map(AccountType::valueOf).orElse(AccountType.PREMIUM));
    }

    public String toString() {
        return "Account{refreshToken='" + this.refreshToken + '\'' + ", accessToken='" + this.accessToken + '\'' + ", username='" + this.username + '\'' + ", uuid='" + this.uuid + '\'' + ", unban=" + this.unban + ", type=" + (Object)((Object)this.type) + '}';
    }
}
