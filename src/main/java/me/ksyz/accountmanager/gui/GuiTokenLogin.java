/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.util.Session
 *  org.apache.commons.lang3.StringUtils
 *  org.lwjgl.input.Keyboard
 */
package me.ksyz.accountmanager.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.ksyz.accountmanager.AccountManager;
import me.ksyz.accountmanager.auth.Account;
import me.ksyz.accountmanager.auth.MicrosoftAuth;
import me.ksyz.accountmanager.gui.GuiAccountManager;
import me.ksyz.accountmanager.gui.GuiTextArea;
import me.ksyz.accountmanager.utils.Notification;
import me.ksyz.accountmanager.utils.TextFormatting;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.Session;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;

public class GuiTokenLogin
extends GuiScreen {
    private final GuiScreen previousScreen;
    private GuiTextArea tokenField;
    private GuiButton loginButton;
    private GuiButton cancelButton;
    private String status = "\u00a77Enter your Minecraft Access Token(s)\u00a7r";
    private ExecutorService executor;
    private CompletableFuture<Void> task;
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern CORE_INFO_EXTRACTION_PATTERN = Pattern.compile("(?:(?:.*?[:|\\s])?(?:Accesstoken|accesstoken|Mctoken|mctoken):([a-zA-Z0-9\\-_\\.]+))|([a-zA-Z0-9\\-_\\.]+)(?:\\s*\\|McName:([a-zA-Z0-9_]+))?(?:\\s*\\|([a-zA-Z0-9_]+))?(?:\\s*\\|([0-9a-fA-F-]{36}))?");
    private static final Pattern ACCOUNT_FULL_EXTRACTION_PATTERN = Pattern.compile("(?:.*?)?(?:Accesstoken|accesstoken|Mctoken|mctoken):([a-zA-Z0-9\\-_\\.]+)(?:\\s*\\|McName:([a-zA-Z0-9_]+))?(?:\\s*\\|([a-zA-Z0-9_]+))?(?:\\s*\\|([0-9a-fA-F-]{36}))?|([a-zA-Z0-9\\-_\\.]+)\\|([a-zA-Z0-9_]+)\\|?([0-9a-fA-F-]{36})?", 32);
    private static final Pattern JWT_TOKEN_PATTERN = Pattern.compile("eyJ[a-zA-Z0-9_-]*\\.eyJ[a-zA-Z0-9_-]*\\.[a-zA-Z0-9_-]*");
    private static final Pattern TOKEN_LIKE_PATTERN = Pattern.compile("[a-zA-Z0-9\\-_]{20,}(?:\\.[a-zA-Z0-9\\-_]+){2,}|[a-zA-Z0-9\\-_]{100,}");

    public GuiTokenLogin(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
    }

    public void initGui() {
        Keyboard.enableRepeatEvents((boolean)true);
        this.buttonList.clear();
        this.loginButton = new GuiButton(0, this.width / 2 - 100, this.height / 2 + 30, 200, 20, "Login Account(s)");
        this.buttonList.add(this.loginButton);
        this.cancelButton = new GuiButton(1, this.width / 2 - 100, this.height / 2 + 55, 200, 20, "Cancel");
        this.buttonList.add(this.cancelButton);
        this.tokenField = new GuiTextArea(2, this.fontRendererObj, this.width / 2 - 100, this.height / 2 - 60, 200, 80);
        this.tokenField.setMaxStringLength(50000);
        this.tokenField.setFocused(true);
    }

    public void onGuiClosed() {
        Keyboard.enableRepeatEvents((boolean)false);
        if (this.task != null && !this.task.isDone()) {
            this.task.cancel(true);
            if (this.executor != null && !this.executor.isShutdown()) {
                this.executor.shutdownNow();
            }
        }
    }

    protected void actionPerformed(GuiButton button) {
        if (button.enabled) {
            switch (button.id) {
                case 0: {
                    String input = this.tokenField.getText().trim();
                    if (!input.isEmpty()) {
                        this.processInputAndLogin(input);
                        break;
                    }
                    this.status = "\u00a7cPlease enter at least one account.\u00a7r";
                    break;
                }
                case 1: {
                    this.mc.displayGuiScreen(this.previousScreen);
                }
            }
        }
    }

    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 1) {
            this.actionPerformed(this.cancelButton);
            return;
        }
        this.tokenField.textboxKeyTyped(typedChar, keyCode);
        if (keyCode == 28 && GuiTokenLogin.isCtrlKeyDown() && !this.tokenField.getText().trim().isEmpty()) {
            this.actionPerformed(this.loginButton);
        }
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.tokenField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    public void updateScreen() {
        this.tokenField.updateCursorCounter();
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, "\u00a7fLogin with Access Token(s)", this.width / 2, this.height / 2 - 90, 0xFFFFFF);
        this.drawCenteredString(this.fontRendererObj, this.status, this.width / 2, this.height / 2 - 75, 0xAAAAAA);
        this.tokenField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void processInputAndLogin(String fullInput) {
        if (this.executor == null || this.executor.isShutdown()) {
            this.executor = Executors.newFixedThreadPool(5);
        }
        this.status = "\u00a77Processing accounts...\u00a7r";
        this.loginButton.enabled = false;
        ArrayList<CompletableFuture<Void>> loginTasks = new ArrayList<CompletableFuture<Void>>();
        ArrayList<String> failedAccounts = new ArrayList<String>();
        ArrayList<String> successfulAccounts = new ArrayList<String>();
        ArrayList<String> processedTokens = new ArrayList<String>();
        Matcher jwtMatcher = JWT_TOKEN_PATTERN.matcher(fullInput);
        while (jwtMatcher.find()) {
            String token = jwtMatcher.group();
            if (processedTokens.contains(token)) continue;
            processedTokens.add(token);
            this.processSimpleToken(token, loginTasks, failedAccounts, successfulAccounts);
        }
        if (!processedTokens.isEmpty()) {
            this.completeLoginProcess(loginTasks, failedAccounts, successfulAccounts);
            return;
        }
        ArrayList<String> extractedAccountBlocks = new ArrayList<String>();
        Matcher fullExtractorMatcher = ACCOUNT_FULL_EXTRACTION_PATTERN.matcher(fullInput);
        while (fullExtractorMatcher.find()) {
            if (fullExtractorMatcher.group(1) == null && fullExtractorMatcher.group(5) == null) continue;
            extractedAccountBlocks.add(fullExtractorMatcher.group(0));
        }
        String[] lines = fullInput.split("[\\r\\n]+");
        for (String line : lines) {
            boolean hasStructuredFormat;
            if ((line = line.trim()).isEmpty()) continue;
            Matcher lineJwtMatcher = JWT_TOKEN_PATTERN.matcher(line);
            while (lineJwtMatcher.find()) {
                String token = lineJwtMatcher.group();
                if (processedTokens.contains(token)) continue;
                processedTokens.add(token);
                this.processSimpleToken(token, loginTasks, failedAccounts, successfulAccounts);
            }
            if (lineJwtMatcher.find()) {
                lineJwtMatcher.reset();
                continue;
            }
            boolean bl = hasStructuredFormat = line.contains("McName:") || line.contains("Accesstoken:") || line.contains("accesstoken:") || line.contains("Mctoken:") || line.contains("mctoken:");
            if (hasStructuredFormat) {
                this.processAccountEntry(line, loginTasks, failedAccounts, successfulAccounts);
                continue;
            }
            if (line.contains("|")) {
                String[] parts;
                for (String part : parts = line.split("\\|")) {
                    if ((part = part.trim()).isEmpty()) continue;
                    if (JWT_TOKEN_PATTERN.matcher(part).matches()) {
                        if (processedTokens.contains(part)) continue;
                        processedTokens.add(part);
                        this.processSimpleToken(part, loginTasks, failedAccounts, successfulAccounts);
                        continue;
                    }
                    if (!TOKEN_LIKE_PATTERN.matcher(part).matches() || processedTokens.contains(part)) continue;
                    processedTokens.add(part);
                    this.processSimpleToken(part, loginTasks, failedAccounts, successfulAccounts);
                }
                continue;
            }
            if (!TOKEN_LIKE_PATTERN.matcher(line).matches() || processedTokens.contains(line)) continue;
            processedTokens.add(line);
            this.processSimpleToken(line, loginTasks, failedAccounts, successfulAccounts);
        }
        for (String rawEntry : extractedAccountBlocks) {
            this.processAccountEntry(rawEntry.trim(), loginTasks, failedAccounts, successfulAccounts);
        }
        if (loginTasks.isEmpty() && !fullInput.trim().isEmpty()) {
            String trimmed = fullInput.trim().replaceAll("[\\r\\n]+", "");
            if (TOKEN_LIKE_PATTERN.matcher(trimmed).matches()) {
                this.processSimpleToken(trimmed, loginTasks, failedAccounts, successfulAccounts);
            } else {
                this.processAccountEntry(fullInput.trim(), loginTasks, failedAccounts, successfulAccounts);
            }
        }
        this.completeLoginProcess(loginTasks, failedAccounts, successfulAccounts);
    }

    private void processSimpleToken(String token, List<CompletableFuture<Void>> loginTasks, List<String> failedAccounts, List<String> successfulAccounts) {
        if ((token = token.trim()).isEmpty() || token.length() < 20) {
            return;
        }
        String finalToken = token;
        CompletableFuture<Void> currentTask = MicrosoftAuth.login(finalToken, this.executor)
                .thenAcceptAsync(session -> {
                    String finalUsername = session.getUsername();
                    String finalUuid = session.getPlayerID();
                    Optional<Account> existingAccountOptional = AccountManager.accounts.stream()
                            .filter(acc -> acc.getAccessToken().equals(finalToken))
                            .findFirst();
                    if (existingAccountOptional.isPresent()) {
                        Account accountToSave = existingAccountOptional.get();
                        accountToSave.setUsername(finalUsername);
                        accountToSave.setUuid(finalUuid);
                    } else {
                        AccountManager.accounts.add(new Account(finalUsername, finalToken, finalUuid));
                    }
                    successfulAccounts.add(finalUsername);
                }, this.executor)
                .exceptionally(error -> {
                    String errorMessage = "Login failed!";
                    if (error != null) {
                        Throwable cause = error.getCause();
                        errorMessage = cause != null ? cause.getMessage() : error.getMessage();
                    }
                    String tokenPreview = finalToken.length() > 30 ? finalToken.substring(0, 30) + "..." : finalToken;
                    failedAccounts.add("\u00a7cFailed (" + errorMessage + ") for token: " + tokenPreview + "\u00a7r");
                    System.err.println("Error processing token: " + tokenPreview + " - " + errorMessage);
                    return null;
                });
        loginTasks.add(currentTask);
    }

    private void processAccountEntry(String trimmedEntry, List<CompletableFuture<Void>> loginTasks, List<String> failedAccounts, List<String> successfulAccounts) {
        if (trimmedEntry.isEmpty()) {
            return;
        }
        String token = null;
        String usernameFromInput = null;
        String uuidFromInput = null;
        Matcher jwtMatcher = JWT_TOKEN_PATTERN.matcher(trimmedEntry);
        if (jwtMatcher.find()) {
            token = jwtMatcher.group();
        }
        if (token == null) {
            Matcher coreInfoMatcher = CORE_INFO_EXTRACTION_PATTERN.matcher(trimmedEntry);
            while (coreInfoMatcher.find()) {
                String potentialUuid;
                if (coreInfoMatcher.group(1) != null) {
                    token = coreInfoMatcher.group(1);
                }
                if (token == null && coreInfoMatcher.group(2) != null) {
                    token = coreInfoMatcher.group(2);
                }
                if (coreInfoMatcher.group(3) != null) {
                    usernameFromInput = coreInfoMatcher.group(3);
                } else if (coreInfoMatcher.group(4) != null) {
                    usernameFromInput = coreInfoMatcher.group(4);
                }
                if (coreInfoMatcher.group(5) != null && (potentialUuid = coreInfoMatcher.group(5)) != null && UUID_PATTERN.matcher(potentialUuid).matches()) {
                    uuidFromInput = potentialUuid;
                }
                if (token == null) continue;
            }
        }
        if (token != null && token.length() < 20 && !token.contains(".")) {
            token = null;
        }
        if (token == null || token.isEmpty()) {
            failedAccounts.add("\u00a7cInvalid format or missing token for: " + (trimmedEntry.length() > 50 ? trimmedEntry.substring(0, 50) + "..." : trimmedEntry) + "\u00a7r");
            return;
        }
        String finalToken = token;
        String finalUsernameFromInput = usernameFromInput;
        String finalUuidFromInput = uuidFromInput;
        CompletableFuture<Session> loginFuture = !StringUtils.isBlank(finalUsernameFromInput) && !StringUtils.isBlank(finalUuidFromInput)
                ? MicrosoftAuth.login(finalToken, finalUsernameFromInput, finalUuidFromInput, this.executor)
                : MicrosoftAuth.login(finalToken, this.executor);
        CompletableFuture<Void> currentTask = loginFuture
                .thenAcceptAsync(session -> {
                    String finalUsername = session.getUsername();
                    String finalUuid = session.getPlayerID();
                    Optional<Account> existingAccountOptional = AccountManager.accounts.stream()
                            .filter(acc -> acc.getAccessToken().equals(finalToken))
                            .findFirst();
                    if (existingAccountOptional.isPresent()) {
                        Account accountToSave = existingAccountOptional.get();
                        accountToSave.setUsername(finalUsername);
                        accountToSave.setUuid(finalUuid);
                    } else {
                        AccountManager.accounts.add(new Account(finalUsername, finalToken, finalUuid));
                    }
                    successfulAccounts.add(finalUsername);
                }, this.executor)
                .exceptionally(error -> {
                    String errorMessage = "Login failed!";
                    if (error != null) {
                        Throwable cause = error.getCause();
                        errorMessage = cause != null ? cause.getMessage() : error.getMessage();
                    }
                    failedAccounts.add("\u00a7cFailed (" + errorMessage + ") for: " + (finalUsernameFromInput != null ? finalUsernameFromInput : "Unknown Username/Invalid Token") + "\u00a7r");
                    System.err.println("Error processing account: " + trimmedEntry + " - " + errorMessage);
                    return null;
                });
        loginTasks.add(currentTask);
    }

    private void completeLoginProcess(List<CompletableFuture<Void>> loginTasks, List<String> failedAccounts, List<String> successfulAccounts) {
        this.task = CompletableFuture.allOf(loginTasks.toArray(new CompletableFuture[0]))
                .thenRunAsync(() -> {
                    AccountManager.save();
                    this.mc.addScheduledTask(() -> {
                        String finalMessage = !successfulAccounts.isEmpty() && failedAccounts.isEmpty()
                                ? String.format("\u00a7aSuccessfully logged in %d account(s)!\u00a7r", successfulAccounts.size())
                                : (successfulAccounts.isEmpty() && !failedAccounts.isEmpty()
                                ? String.format("\u00a7cFailed to log in %d account(s).\u00a7r", failedAccounts.size())
                                : String.format("\u00a7aLogged in %d, \u00a7cfailed %d account(s).\u00a7r", successfulAccounts.size(), failedAccounts.size()));
                        this.mc.displayGuiScreen(new GuiAccountManager(this.previousScreen, new Notification(TextFormatting.translate(finalMessage), 5000L)));
                        if (!failedAccounts.isEmpty()) {
                            System.err.println("Failed account details:");
                            failedAccounts.forEach(System.err::println);
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
}
