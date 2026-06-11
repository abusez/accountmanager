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

    public void func_73866_w_() {
        this.field_146292_n.clear();
        int buttonWidth = 200;
        int buttonHeight = 20;
        int spacing = 5;
        int centerX = this.field_146294_l / 2;
        int startX = centerX - buttonWidth / 2;
        int baseY = this.field_146295_m / 2 + this.field_146289_q.field_78288_b / 2 + this.field_146289_q.field_78288_b * 2;
        this.openButton = new GuiButton(0, startX, baseY, buttonWidth, buttonHeight, "Open Link");
        this.field_146292_n.add(this.openButton);
        this.copyButton = new GuiButton(1, startX, baseY + buttonHeight + spacing, buttonWidth, buttonHeight, "Copy Link");
        this.field_146292_n.add(this.copyButton);
        this.cancelButton = new GuiButton(2, startX, baseY + (buttonHeight + spacing) * 2, buttonWidth, buttonHeight, "Cancel");
        this.field_146292_n.add(this.cancelButton);
        if (this.task == null) {
            this.status = "&fWaiting for login&r";
            if (this.executor == null) {
                this.executor = Executors.newSingleThreadExecutor();
            }
            AtomicReference<String> refreshTokenRef = new AtomicReference<String>("");
            AtomicReference<String> accessTokenRef = new AtomicReference<String>("");
            AtomicReference<String> uuidRef = new AtomicReference<String>("");
            this.task = ((CompletableFuture)((CompletableFuture)((CompletableFuture)((CompletableFuture)((CompletableFuture)((CompletableFuture)MicrosoftAuth.acquireMSAuthCode(this.state, this.executor).thenComposeAsync(msAuthCode -> {
                this.openButtonEnabled = false;
                this.status = "&fAcquiring Microsoft access tokens&r";
                return MicrosoftAuth.acquireMSAccessTokens(msAuthCode, this.executor);
            })).thenComposeAsync(msAccessTokens -> {
                this.status = "&fAcquiring Xbox access token.&r";
                refreshTokenRef.set((String)msAccessTokens.get("refresh_token"));
                return MicrosoftAuth.acquireXboxAccessToken((String)msAccessTokens.get("access_token"), this.executor);
            })).thenComposeAsync(xboxAccessToken -> {
                this.status = "&fAcquiring Xbox XSTS token&r";
                return MicrosoftAuth.acquireXboxXstsToken(xboxAccessToken, this.executor);
            })).thenComposeAsync(xboxXstsData -> {
                this.status = "&fAcquiring Minecraft access token&r";
                return MicrosoftAuth.acquireMCAccessToken((String)xboxXstsData.get("Token"), (String)xboxXstsData.get("uhs"), this.executor);
            })).thenComposeAsync(mcToken -> {
                this.status = "&fFetching your Minecraft profile&r";
                accessTokenRef.set((String)mcToken);
                return MicrosoftAuth.login(mcToken, this.executor);
            })).thenAccept(session -> {
                this.status = null;
                this.cause = null;
                Account acc = new Account((String)refreshTokenRef.get(), (String)accessTokenRef.get(), session.func_111285_a(), session.func_148255_b());
                for (Account account : AccountManager.accounts) {
                    if (!acc.getUsername().equals(account.getUsername())) continue;
                    acc.setUnban(account.getUnban());
                    break;
                }
                AccountManager.accounts.add(acc);
                AccountManager.save();
                SessionManager.set(session);
                this.success = true;
            })).exceptionally(error -> {
                this.openButtonEnabled = true;
                this.status = String.format("&cLogin failed!&r", new Object[0]);
                this.cause = error.getCause() != null && error.getCause().getMessage() != null ? String.format("&cReason: %s&r", error.getCause().getMessage()) : String.format("&cUnknown error occurred.&r", new Object[0]);
                return null;
            });
        }
    }

    public void func_146281_b() {
        if (this.task != null && !this.task.isDone()) {
            this.task.cancel(true);
            this.executor.shutdownNow();
        }
    }

    public void func_73876_c() {
        if (this.success) {
            this.field_146297_k.func_147108_a((GuiScreen)new GuiAccountManager(this.previousScreen, new Notification(TextFormatting.translate(String.format("&aSuccessful login! (%s)&r", SessionManager.get().func_111285_a())), 5000L)));
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

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        if (this.openButton != null) {
            this.openButton.field_146124_l = this.openButtonEnabled;
        }
        if (this.copyButton != null) {
            this.copyButton.field_146124_l = this.openButtonEnabled;
        }
        this.func_146276_q_();
        super.func_73863_a(mouseX, mouseY, partialTicks);
        this.func_73732_a(this.field_146289_q, "Microsoft Authentication", this.field_146294_l / 2, this.field_146295_m / 2 - this.field_146289_q.field_78288_b / 2 - this.field_146289_q.field_78288_b * 2, 0xAAAAAA);
        if (this.status != null) {
            String displayedStatus = this.status;
            if (this.task != null && !this.task.isDone() && this.cause == null) {
                for (int i = 0; i < this.dotCount; ++i) {
                    displayedStatus = displayedStatus + ".";
                }
            }
            this.func_73732_a(this.field_146289_q, TextFormatting.translate(displayedStatus), this.field_146294_l / 2, this.field_146295_m / 2 - this.field_146289_q.field_78288_b / 2, -1);
        }
        if (this.cause != null) {
            this.func_73732_a(this.field_146289_q, TextFormatting.translate(this.cause), this.field_146294_l / 2, this.field_146295_m / 2 + this.field_146289_q.field_78288_b / 2 + this.field_146289_q.field_78288_b, 0xFFAAAA);
        }
    }

    protected void func_73869_a(char typedChar, int keyCode) {
        if (keyCode == 1) {
            this.func_146284_a(this.cancelButton);
        }
    }

    protected void func_146284_a(GuiButton button) {
        if (button == null) {
            return;
        }
        if (button.field_146124_l) {
            switch (button.field_146127_k) {
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
                    this.field_146297_k.func_147108_a(this.previousScreen);
                }
            }
        }
    }
}
