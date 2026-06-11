/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.FontRenderer
 *  net.minecraft.client.gui.Gui
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiMultiplayer
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.gui.GuiSlot
 *  org.apache.commons.lang3.StringUtils
 *  org.lwjgl.input.Keyboard
 */
package me.ksyz.accountmanager.gui;

import java.io.IOException;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import me.ksyz.accountmanager.AccountManager;
import me.ksyz.accountmanager.auth.Account;
import me.ksyz.accountmanager.auth.AccountType;
import me.ksyz.accountmanager.auth.CrackedAuth;
import me.ksyz.accountmanager.auth.MicrosoftAuth;
import me.ksyz.accountmanager.auth.SessionManager;
import me.ksyz.accountmanager.gui.GuiAddAccount;
import me.ksyz.accountmanager.utils.Notification;
import me.ksyz.accountmanager.utils.TextFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;

public class GuiAccountManager
extends GuiScreen {
    protected final GuiScreen previousScreen;
    private GuiButton loginButton = null;
    private GuiButton deleteButton = null;
    private GuiButton cancelButton = null;
    private GuiAccountList guiAccountList = null;
    public static Notification notification = null;
    private int selectedAccount = -1;
    private ExecutorService executor = null;
    private CompletableFuture<Void> task = null;

    public GuiAccountManager(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
    }

    public GuiAccountManager(GuiScreen previousScreen, Notification notification) {
        this.previousScreen = previousScreen;
        GuiAccountManager.notification = notification;
    }

    public void func_73866_w_() {
        AccountManager.load();
        Keyboard.enableRepeatEvents((boolean)true);
        this.field_146292_n.clear();
        this.loginButton = new GuiButton(0, this.field_146294_l / 2 - 150 - 4, this.field_146295_m - 52, 150, 20, "Login");
        this.field_146292_n.add(this.loginButton);
        this.field_146292_n.add(new GuiButton(1, this.field_146294_l / 2 + 4, this.field_146295_m - 52, 150, 20, "Add"));
        this.deleteButton = new GuiButton(2, this.field_146294_l / 2 - 150 - 4, this.field_146295_m - 28, 150, 20, "Delete");
        this.field_146292_n.add(this.deleteButton);
        this.cancelButton = new GuiButton(3, this.field_146294_l / 2 + 4, this.field_146295_m - 28, 150, 20, "Cancel");
        this.field_146292_n.add(this.cancelButton);
        this.guiAccountList = new GuiAccountList(this.field_146297_k);
        this.guiAccountList.func_148134_d(11, 12);
        this.func_73876_c();
    }

    public void func_146281_b() {
        Keyboard.enableRepeatEvents((boolean)false);
        if (this.task != null && !this.task.isDone()) {
            this.task.cancel(true);
            this.executor.shutdownNow();
        }
    }

    public void func_73876_c() {
        if (this.loginButton != null && this.deleteButton != null) {
            this.deleteButton.field_146124_l = this.selectedAccount >= 0;
            this.loginButton.field_146124_l = this.deleteButton.field_146124_l;
            if (this.task != null && !this.task.isDone()) {
                this.loginButton.field_146124_l = false;
            }
        }
    }

    public void func_73863_a(int mouseX, int mouseY, float renderPartialTicks) {
        if (this.guiAccountList != null) {
            this.guiAccountList.func_148128_a(mouseX, mouseY, renderPartialTicks);
        }
        super.func_73863_a(mouseX, mouseY, renderPartialTicks);
        this.func_73732_a(this.field_146289_q, TextFormatting.translate(String.format("&rLumiere Account Manager &8(&7%s&8)&r", AccountManager.accounts.size())), this.field_146294_l / 2, 20, -1);
        String text = TextFormatting.translate(String.format("&7Username: &3%s&r", SessionManager.get().func_111285_a()));
        this.field_146297_k.field_71462_r.func_73731_b(this.field_146297_k.field_71466_p, text, 3, 3, -1);
        if (notification != null && !notification.isExpired()) {
            String notificationText = notification.getMessage();
            Gui.func_73734_a((int)(this.field_146297_k.field_71462_r.field_146294_l / 2 - this.field_146297_k.field_71466_p.func_78256_a(notificationText) / 2 - 3), (int)4, (int)(this.field_146297_k.field_71462_r.field_146294_l / 2 + this.field_146297_k.field_71466_p.func_78256_a(notificationText) / 2 + 3), (int)(7 + this.field_146297_k.field_71466_p.field_78288_b + 2), (int)0x64000000);
            this.field_146297_k.field_71462_r.func_73732_a(this.field_146297_k.field_71466_p, notification.getMessage(), this.field_146297_k.field_71462_r.field_146294_l / 2, 7, -1);
        }
    }

    public void func_146274_d() throws IOException {
        if (this.guiAccountList != null) {
            this.guiAccountList.func_178039_p();
        }
        super.func_146274_d();
    }

    protected void func_73869_a(char typedChar, int keyCode) {
        switch (keyCode) {
            case 200: {
                if (this.selectedAccount <= 0) break;
                --this.selectedAccount;
                if (!GuiAccountManager.func_146271_m()) break;
                Collections.swap(AccountManager.accounts, this.selectedAccount, this.selectedAccount + 1);
                AccountManager.save();
                break;
            }
            case 208: {
                if (this.selectedAccount >= AccountManager.accounts.size() - 1) break;
                ++this.selectedAccount;
                if (!GuiAccountManager.func_146271_m()) break;
                Collections.swap(AccountManager.accounts, this.selectedAccount, this.selectedAccount - 1);
                AccountManager.save();
                break;
            }
            case 28: {
                this.func_146284_a(this.loginButton);
                break;
            }
            case 211: {
                this.func_146284_a(this.deleteButton);
                break;
            }
            case 1: {
                this.func_146284_a(this.cancelButton);
            }
        }
        if (GuiAccountManager.func_175280_f((int)keyCode) && this.selectedAccount >= 0) {
            GuiAccountManager.func_146275_d((String)AccountManager.accounts.get(this.selectedAccount).getUsername());
        }
    }

    protected void func_146284_a(GuiButton button) {
        if (button == null) {
            return;
        }
        if (button.field_146124_l) {
            switch (button.field_146127_k) {
                case 0: {
                    Account account;
                    String username;
                    if (this.task != null && !this.task.isDone()) break;
                    if (this.executor == null) {
                        this.executor = Executors.newSingleThreadExecutor();
                    }
                    String string = username = StringUtils.isBlank((CharSequence)(account = AccountManager.accounts.get(this.selectedAccount)).getUsername()) ? "???" : account.getUsername();
                    if (account.getType() == AccountType.CRACKED) {
                        boolean loginSuccess = CrackedAuth.login(account.getUsername());
                        notification = loginSuccess ? new Notification(TextFormatting.translate(String.format("&aSuccessful login! (%s)&r", account.getUsername())), 5000L) : new Notification(TextFormatting.translate(String.format("&cFailed to log in! (%s)&r", account.getUsername())), 5000L);
                        return;
                    }
                    notification = new Notification(TextFormatting.translate(String.format("&7Fetching your Minecraft profile... (%s)&r", username)), -1L);
                    this.task = ((CompletableFuture)((CompletableFuture)((CompletableFuture)((CompletableFuture)((CompletableFuture)((CompletableFuture)((CompletableFuture)MicrosoftAuth.login(account.getAccessToken(), this.executor).handle((session, error) -> {
                        if (session != null) {
                            account.setUsername(session.func_111285_a());
                            AccountManager.save();
                            SessionManager.set(session);
                            notification = new Notification(TextFormatting.translate(String.format("&aSuccessful login! (%s)&r", account.getUsername())), 5000L);
                            return true;
                        }
                        return false;
                    })).thenComposeAsync(completed -> {
                        if (completed.booleanValue()) {
                            throw new NoSuchElementException();
                        }
                        notification = new Notification(TextFormatting.translate(String.format("&7Refreshing Microsoft access tokens... (%s)&r", username)), -1L);
                        return MicrosoftAuth.refreshMSAccessTokens(account.getRefreshToken(), this.executor);
                    })).thenComposeAsync(msAccessTokens -> {
                        notification = new Notification(TextFormatting.translate(String.format("&7Acquiring Xbox access token... (%s)&r", username)), -1L);
                        return MicrosoftAuth.acquireXboxAccessToken((String)msAccessTokens.get("access_token"), this.executor);
                    })).thenComposeAsync(xboxAccessToken -> {
                        notification = new Notification(TextFormatting.translate(String.format("&7Acquiring Xbox XSTS token... (%s)&r", username)), -1L);
                        return MicrosoftAuth.acquireXboxXstsToken(xboxAccessToken, this.executor);
                    })).thenComposeAsync(xboxXstsData -> {
                        notification = new Notification(TextFormatting.translate(String.format("&7Acquiring Minecraft access token... (%s)&r", username)), -1L);
                        return MicrosoftAuth.acquireMCAccessToken((String)xboxXstsData.get("Token"), (String)xboxXstsData.get("uhs"), this.executor);
                    })).thenComposeAsync(mcToken -> {
                        notification = new Notification(TextFormatting.translate(String.format("&7Fetching your Minecraft profile... (%s)&r", username)), -1L);
                        return MicrosoftAuth.login(mcToken, this.executor);
                    })).thenAccept(session -> {
                        account.setUsername(session.func_111285_a());
                        AccountManager.save();
                        SessionManager.set(session);
                        notification = new Notification(TextFormatting.translate(String.format("&aSuccessful login! (%s)&r", account.getUsername())), 5000L);
                    })).exceptionally(error -> {
                        if (!(error.getCause() instanceof NoSuchElementException)) {
                            notification = new Notification(TextFormatting.translate(String.format("&c%s (%s)&r", error.getMessage(), username)), 5000L);
                        }
                        return null;
                    });
                    break;
                }
                case 1: {
                    this.field_146297_k.func_147108_a((GuiScreen)new GuiAddAccount(this.previousScreen));
                    break;
                }
                case 2: {
                    if (this.selectedAccount <= -1 || this.selectedAccount >= AccountManager.accounts.size()) break;
                    AccountManager.accounts.remove(this.selectedAccount);
                    AccountManager.save();
                    this.selectedAccount = -1;
                    this.func_73876_c();
                    break;
                }
                case 3: {
                    this.field_146297_k.func_147108_a((GuiScreen)new GuiMultiplayer(this.previousScreen));
                    break;
                }
                default: {
                    this.guiAccountList.func_148147_a(button);
                }
            }
        }
    }

    class GuiAccountList
    extends GuiSlot {
        public GuiAccountList(Minecraft mc) {
            super(mc, GuiAccountManager.this.field_146294_l, GuiAccountManager.this.field_146295_m, 32, GuiAccountManager.this.field_146295_m - 64, 16);
        }

        protected int func_148127_b() {
            return AccountManager.accounts.size();
        }

        protected boolean func_148131_a(int slotIndex) {
            return slotIndex == GuiAccountManager.this.selectedAccount;
        }

        protected int func_148137_d() {
            return (this.field_148155_a + this.func_148139_c()) / 2 + 2;
        }

        public int func_148139_c() {
            return 308;
        }

        protected int func_148138_e() {
            return AccountManager.accounts.size() * 16;
        }

        protected void func_148144_a(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY) {
            GuiAccountManager.this.selectedAccount = slotIndex;
            GuiAccountManager.this.func_73876_c();
            if (isDoubleClick) {
                GuiAccountManager.this.func_146284_a(GuiAccountManager.this.loginButton);
            }
        }

        protected void func_148123_a() {
            GuiAccountManager.this.func_146276_q_();
        }

        protected void func_180791_a(int entryID, int x, int int_4, int k, int mouseXIn, int mouseYIn) {
            String unban;
            FontRenderer fr = GuiAccountManager.this.field_146289_q;
            Account account = AccountManager.accounts.get(entryID);
            String username = account.getUsername();
            if (StringUtils.isBlank((CharSequence)username)) {
                username = "&7&l?";
            }
            if (SessionManager.get() != null) {
                if (account.getType() == AccountType.CRACKED && username.equals(SessionManager.get().func_111285_a())) {
                    username = String.format("&a&l%s", username);
                } else if (account.getType() == AccountType.PREMIUM && account.getUsername().equals(SessionManager.get().func_111285_a())) {
                    username = String.format("&a&l%s", username);
                }
            }
            String accountTypeSuffix = account.getType() == AccountType.CRACKED ? " &7(Cracked)" : " &7(Premium)";
            String translatedUsername = TextFormatting.translate(String.format("&r%s", username));
            String translatedSuffix = TextFormatting.translate(accountTypeSuffix);
            GuiAccountManager.this.func_73731_b(fr, translatedUsername, x + 2, int_4 + 2, -1);
            GuiAccountManager.this.func_73731_b(fr, translatedSuffix, x + 2 + fr.func_78256_a(translatedUsername), int_4 + 2, -1);
            long currentTime = System.currentTimeMillis();
            long unbanTime = account.getUnban();
            if (unbanTime < 0L) {
                unban = "&4&l\u26a0";
            } else if (unbanTime <= currentTime) {
                unban = "&2&l\u2714";
            } else {
                long diff = unbanTime - currentTime;
                long s = diff / 1000L % 60L;
                long m = diff / 60000L % 60L;
                long h = diff / 3600000L % 24L;
                long d = diff / 86400000L;
                unban = String.format("%s%s%s%s", d > 0L ? String.format("%dd", d) : "", h > 0L ? String.format(" %dh", h) : "", m > 0L ? String.format(" %dm", m) : "", s > 0L ? String.format(" %ds", s) : "");
                unban = unban.trim();
                unban = String.format("%s &c&l\u26a0", unban);
            }
            unban = TextFormatting.translate(String.format("&r%s&r", unban));
            GuiAccountManager.this.func_73731_b(fr, unban, x + this.func_148139_c() - 5 - fr.func_78256_a(unban), int_4 + 2, -1);
        }
    }
}
