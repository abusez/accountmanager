/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiScreen
 *  org.apache.commons.lang3.RandomStringUtils
 */
package me.ksyz.accountmanager.gui;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import me.ksyz.accountmanager.AccountManager;
import me.ksyz.accountmanager.auth.Account;
import me.ksyz.accountmanager.auth.MicrosoftAuth;
import me.ksyz.accountmanager.auth.SessionManager;
import me.ksyz.accountmanager.gui.GuiAccountManager;
import me.ksyz.accountmanager.utils.Notification;
import me.ksyz.accountmanager.utils.SystemUtils;
import me.ksyz.accountmanager.utils.TextFormatting;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.apache.commons.lang3.RandomStringUtils;

public class GuiMicrosoftAuth
extends GuiScreen {
    private final GuiScreen previousScreen;
    private final String state;
    private GuiButton openButton = null;
    private GuiButton copyButton = null;
    private GuiButton cancelButton = null;
    private boolean openButtonEnabled = true;
    private String status = null;
    private String cause = null;
    private ExecutorService executor = null;
    private CompletableFuture<Void> task = null;
    private boolean success = false;
    private long lastDotUpdateTime;
    private int dotCount;
    private static final long DOT_ANIMATION_INTERVAL = 200L;

    public GuiMicrosoftAuth(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
        this.state = RandomStringUtils.randomAlphanumeric((int)8);
        this.lastDotUpdateTime = System.currentTimeMillis();
        this.dotCount = 0;
    }

    public void initGui() {
        this.buttonList.clear();
        int buttonWidth = 200;
        int buttonHeight = 20;
        int spacing = 5;
        int centerX = this.width / 2;
        int startX = centerX - buttonWidth / 2;
        int baseY = this.height / 2 + this.fontRendererObj.FONT_HEIGHT / 2 + this.fontRendererObj.FONT_HEIGHT * 2;
        this.openButton = new GuiButton(0, startX, baseY, buttonWidth, buttonHeight, "Open Link");
        this.buttonList.add(this.openButton);
        this.copyButton = new GuiButton(1, startX, baseY + buttonHeight + spacing, buttonWidth, buttonHeight, "Copy Link");
        this.buttonList.add(this.copyButton);
        this.cancelButton = new GuiButton(2, startX, baseY + (buttonHeight + spacing) * 2, buttonWidth, buttonHeight, "Cancel");
        this.buttonList.add(this.cancelButton);
        if (this.task == null) {
            this.status = "&fWaiting for login&r";
            if (this.executor == null) {
                this.executor = Executors.newSingleThreadExecutor();
            }
            AtomicReference<String> refreshTokenRef = new AtomicReference<String>("");
            AtomicReference<String> accessTokenRef = new AtomicReference<String>("");
            this.task = MicrosoftAuth.acquireMSAuthCode(this.state, this.executor)
                    .thenComposeAsync(msAuthCode -> {
                        this.openButtonEnabled = false;
                        this.status = "&fAcquiring Microsoft access tokens&r";
                        return MicrosoftAuth.acquireMSAccessTokens(msAuthCode, this.executor);
                    }, this.executor)
                    .thenComposeAsync(msAccessTokens -> {
                        this.status = "&fAcquiring Xbox access token.&r";
                        refreshTokenRef.set(msAccessTokens.get("refresh_token"));
                        return MicrosoftAuth.acquireXboxAccessToken(msAccessTokens.get("access_token"), this.executor);
                    }, this.executor)
                    .thenComposeAsync(xboxAccessToken -> {
                        this.status = "&fAcquiring Xbox XSTS token&r";
                        return MicrosoftAuth.acquireXboxXstsToken(xboxAccessToken, this.executor);
                    }, this.executor)
                    .thenComposeAsync(xboxXstsData -> {
                        this.status = "&fAcquiring Minecraft access token&r";
                        return MicrosoftAuth.acquireMCAccessToken(xboxXstsData.get("Token"), xboxXstsData.get("uhs"), this.executor);
                    }, this.executor)
                    .thenComposeAsync(mcToken -> {
                        this.status = "&fFetching your Minecraft profile&r";
                        accessTokenRef.set(mcToken);
                        return MicrosoftAuth.login(mcToken, this.executor);
                    }, this.executor)
                    .thenAccept(session -> {
                        this.status = null;
                        this.cause = null;
                        Account acc = new Account(refreshTokenRef.get(), accessTokenRef.get(), session.getUsername(), session.getPlayerID());
                        for (Account account : AccountManager.accounts) {
                            if (acc.getUsername().equals(account.getUsername())) {
                                acc.setUnban(account.getUnban());
                                break;
                            }
                        }
                        AccountManager.accounts.add(acc);
                        AccountManager.save();
                        SessionManager.set(session);
                        this.success = true;
                    })
                    .exceptionally(error -> {
                        this.openButtonEnabled = true;
                        this.status = "&cLogin failed!&r";
                        Throwable cause = error.getCause();
                        this.cause = cause != null && cause.getMessage() != null
                                ? String.format("&cReason: %s&r", cause.getMessage())
                                : "&cUnknown error occurred.&r";
                        return null;
                    });
        }
    }

    public void onGuiClosed() {
        if (this.task != null && !this.task.isDone()) {
            this.task.cancel(true);
            this.executor.shutdownNow();
        }
    }

    public void updateScreen() {
        if (this.success) {
            this.mc.displayGuiScreen((GuiScreen)new GuiAccountManager(this.previousScreen, new Notification(TextFormatting.translate(String.format("&aSuccessful login! (%s)&r", SessionManager.get().getUsername())), 5000L)));
            this.success = false;
        }
        if (this.status != null && !this.success && this.task != null && !this.task.isDone()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - this.lastDotUpdateTime >= 200L) {
                this.dotCount = (this.dotCount + 1) % 4;
                this.lastDotUpdateTime = currentTime;
            }
        } else {
            this.dotCount = 0;
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (this.openButton != null) {
            this.openButton.enabled = this.openButtonEnabled;
        }
        if (this.copyButton != null) {
            this.copyButton.enabled = this.openButtonEnabled;
        }
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.drawCenteredString(this.fontRendererObj, "Microsoft Authentication", this.width / 2, this.height / 2 - this.fontRendererObj.FONT_HEIGHT / 2 - this.fontRendererObj.FONT_HEIGHT * 2, 0xAAAAAA);
        if (this.status != null) {
            String displayedStatus = this.status;
            if (this.task != null && !this.task.isDone() && this.cause == null) {
                for (int i = 0; i < this.dotCount; ++i) {
                    displayedStatus = displayedStatus + ".";
                }
            }
            this.drawCenteredString(this.fontRendererObj, TextFormatting.translate(displayedStatus), this.width / 2, this.height / 2 - this.fontRendererObj.FONT_HEIGHT / 2, -1);
        }
        if (this.cause != null) {
            this.drawCenteredString(this.fontRendererObj, TextFormatting.translate(this.cause), this.width / 2, this.height / 2 + this.fontRendererObj.FONT_HEIGHT / 2 + this.fontRendererObj.FONT_HEIGHT, 0xFFAAAA);
        }
    }

    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 1) {
            this.actionPerformed(this.cancelButton);
        }
    }

    protected void actionPerformed(GuiButton button) {
        if (button == null) {
            return;
        }
        if (button.enabled) {
            switch (button.id) {
                case 0: {
                    SystemUtils.openWebLink(MicrosoftAuth.getMSAuthLink(this.state));
                    this.status = "&fPlease complete the login in your browser&r";
                    this.cause = null;
                    this.lastDotUpdateTime = System.currentTimeMillis();
                    this.dotCount = 0;
                    break;
                }
                case 1: {
                    URI url = MicrosoftAuth.getMSAuthLink(this.state);
                    if (url != null) {
                        SystemUtils.setClipboard(url.toString());
                        this.status = "&aLogin link copied!&r";
                        this.cause = null;
                        this.dotCount = 0;
                        break;
                    }
                    this.status = "&cFailed to get login link.&r";
                    this.cause = "&cPlease try again.&r";
                    this.dotCount = 0;
                    break;
                }
                case 2: {
                    this.mc.displayGuiScreen(this.previousScreen);
                }
            }
        }
    }
}
