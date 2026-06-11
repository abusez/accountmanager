package me.ksyz.accountmanager.gui;

import me.ksyz.accountmanager.AccountManager;
import me.ksyz.accountmanager.auth.Account;
import me.ksyz.accountmanager.auth.RefreshTokenAuth;
import me.ksyz.accountmanager.utils.Notification;
import me.ksyz.accountmanager.utils.TextFormatting;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuiRefreshTokenLogin extends GuiScreen {
    private static final Pattern MICROSOFT_REFRESH_TOKEN = Pattern.compile("M\\.C[A-Za-z0-9._!*$\\-]+");

    private final GuiScreen previousScreen;
    private GuiTextArea tokenField;
    private GuiButton loginButton;
    private GuiButton cancelButton;
    private String status = "\u00a77Enter Microsoft OAuth refresh token(s)\u00a7r";
    private ExecutorService executor;
    private CompletableFuture<Void> task;

    public GuiRefreshTokenLogin(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.loginButton = new GuiButton(0, this.width / 2 - 100, this.height / 2 + 30, 200, 20, "Login Account(s)");
        this.buttonList.add(this.loginButton);
        this.cancelButton = new GuiButton(1, this.width / 2 - 100, this.height / 2 + 55, 200, 20, "Cancel");
        this.buttonList.add(this.cancelButton);
        this.tokenField = new GuiTextArea(2, this.fontRendererObj, this.width / 2 - 100, this.height / 2 - 60, 200, 80);
        this.tokenField.setMaxStringLength(50000);
        this.tokenField.setFocused(true);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        if (this.task != null && !this.task.isDone()) {
            this.task.cancel(true);
            if (this.executor != null && !this.executor.isShutdown()) {
                this.executor.shutdownNow();
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (!button.enabled) {
            return;
        }

        switch (button.id) {
            case 0: {
                String input = this.tokenField.getText().trim();
                if (input.isEmpty()) {
                    this.status = "\u00a7cPlease enter at least one refresh token.\u00a7r";
                    return;
                }
                this.processInputAndLogin(input);
                break;
            }
            case 1: {
                this.mc.displayGuiScreen(this.previousScreen);
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 1) {
            this.actionPerformed(this.cancelButton);
            return;
        }

        this.tokenField.textboxKeyTyped(typedChar, keyCode);

        if (keyCode == 28 && isCtrlKeyDown() && !this.tokenField.getText().trim().isEmpty()) {
            this.actionPerformed(this.loginButton);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.tokenField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        this.tokenField.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, "\u00a7fLogin with Refresh Token(s)", this.width / 2, this.height / 2 - 90, 0xFFFFFF);
        this.drawCenteredString(this.fontRendererObj, this.status, this.width / 2, this.height / 2 - 75, 0xAAAAAA);
        this.tokenField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void processInputAndLogin(String fullInput) {
        if (this.executor == null || this.executor.isShutdown()) {
            this.executor = Executors.newFixedThreadPool(3);
        }

        List<String> refreshTokens = extractRefreshTokens(fullInput);
        if (refreshTokens.isEmpty()) {
            this.status = "\u00a7cNo valid refresh tokens found.\u00a7r";
            return;
        }

        this.status = "\u00a77Processing accounts...\u00a7r";
        this.loginButton.enabled = false;

        List<CompletableFuture<Void>> loginTasks = new ArrayList<CompletableFuture<Void>>();
        List<String> failedAccounts = new ArrayList<String>();
        List<String> successfulAccounts = new ArrayList<String>();

        for (final String refreshToken : refreshTokens) {
            CompletableFuture<Void> task = RefreshTokenAuth.authenticate(refreshToken, this.executor)
                    .thenAcceptAsync(account -> {
                        Optional<Account> existing = AccountManager.accounts.stream()
                                .filter(stored -> stored.getRefreshToken().equals(refreshToken)
                                        || stored.getAccessToken().equals(account.getAccessToken()))
                                .findFirst();

                        if (existing.isPresent()) {
                            Account stored = existing.get();
                            stored.setRefreshToken(account.getRefreshToken());
                            stored.setAccessToken(account.getAccessToken());
                            stored.setUsername(account.getUsername());
                            stored.setUuid(account.getUuid());
                        } else {
                            AccountManager.accounts.add(account);
                        }

                        successfulAccounts.add(account.getUsername());
                    }, (Executor) this.executor)
                    .exceptionally(error -> {
                        String errorMessage = "Login failed!";
                        if (error != null) {
                            Throwable cause = error.getCause();
                            errorMessage = cause != null ? cause.getMessage() : error.getMessage();
                        }

                        String preview = refreshToken.length() > 30
                                ? refreshToken.substring(0, 30) + "..."
                                : refreshToken;
                        failedAccounts.add("\u00a7cFailed (" + errorMessage + ") for token: " + preview + "\u00a7r");
                        System.err.println("Error processing refresh token: " + preview + " - " + errorMessage);
                        return null;
                    });

            loginTasks.add(task);
        }

        this.task = CompletableFuture.allOf(loginTasks.toArray(new CompletableFuture[0]))
                .thenRunAsync(() -> {
                    AccountManager.save();
                    this.mc.addScheduledTask(() -> {
                        String finalMessage;
                        if (!successfulAccounts.isEmpty() && failedAccounts.isEmpty()) {
                            finalMessage = String.format(
                                    "\u00a7aSuccessfully logged in %d account(s)!\u00a7r",
                                    successfulAccounts.size()
                            );
                        } else if (successfulAccounts.isEmpty() && !failedAccounts.isEmpty()) {
                            finalMessage = String.format(
                                    "\u00a7cFailed to log in %d account(s).\u00a7r",
                                    failedAccounts.size()
                            );
                        } else {
                            finalMessage = String.format(
                                    "\u00a7aLogged in %d, \u00a7cfailed %d account(s).\u00a7r",
                                    successfulAccounts.size(),
                                    failedAccounts.size()
                            );
                        }

                        this.mc.displayGuiScreen(new GuiAccountManager(
                                this.previousScreen,
                                new Notification(TextFormatting.translate(finalMessage), 5000L)
                        ));

                        if (!failedAccounts.isEmpty()) {
                            System.err.println("Failed refresh token details:");
                            for (String failure : failedAccounts) {
                                System.err.println(failure);
                            }
                        }
                    });
                }, this.executor)
                .exceptionally(totalError -> {
                    this.mc.addScheduledTask(() -> {
                        this.status = "\u00a7cAn unexpected error occurred during batch processing.\u00a7r";
                        this.loginButton.enabled = true;
                    });
                    return null;
                });
    }

    private static List<String> extractRefreshTokens(String input) {
        List<String> tokens = new ArrayList<String>();
        String normalized = input.trim();

        if (normalized.toLowerCase().startsWith("refreshtoken:")) {
            normalized = normalized.substring(normalized.indexOf(':') + 1).trim();
        }

        // Microsoft refresh tokens are one continuous string; users often paste them with line breaks.
        String collapsed = normalized.replaceAll("\\s+", "");
        Matcher matcher = MICROSOFT_REFRESH_TOKEN.matcher(collapsed);
        while (matcher.find()) {
            String token = matcher.group();
            if (token.length() >= 50 && !tokens.contains(token)) {
                tokens.add(token);
            }
        }

        if (!tokens.isEmpty()) {
            return tokens;
        }

        for (String line : input.split("[\\r\\n]+")) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.toLowerCase().contains("refreshtoken:")) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    line = parts[1].trim();
                }
            }

            line = line.replaceAll("\\s+", "");
            matcher = MICROSOFT_REFRESH_TOKEN.matcher(line);
            if (matcher.matches() && matcher.group().length() >= 50 && !tokens.contains(matcher.group())) {
                tokens.add(matcher.group());
            }
        }

        return tokens;
    }
}
