/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.gui.GuiTextField
 */
package me.ksyz.accountmanager.gui;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import javax.swing.SwingUtilities;
import me.ksyz.accountmanager.auth.CrackedAuth;
import me.ksyz.accountmanager.gui.GuiAccountManager;
import me.ksyz.accountmanager.utils.UsernameGenerator;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

public class GuiCrackedAuth
extends GuiScreen {
    private final GuiScreen previousScreen;
    private GuiTextField usernameField;
    private GuiButton loginButton;
    private GuiButton generateRandomButton;
    private GuiButton cancelButton = null;

    public GuiCrackedAuth(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
    }

    public void initGui() {
        this.usernameField = new GuiTextField(0, this.fontRendererObj, this.width / 2 - 100, this.height / 2 - 30, 200, 20);
        this.usernameField.setMaxStringLength(16);
        this.usernameField.setFocused(true);
        this.loginButton = new GuiButton(0, this.width / 2 - 100, this.height / 2, 200, 20, "Login");
        this.buttonList.add(this.loginButton);
        this.generateRandomButton = new GuiButton(1, this.width / 2 - 100, this.height / 2 + 25, 200, 20, "Generate Random");
        this.buttonList.add(this.generateRandomButton);
        this.cancelButton = new GuiButton(2, this.width / 2 - 100, this.height / 2 + 50, 200, 20, "Cancel");
        this.buttonList.add(this.cancelButton);
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, "Cracked Authentication", this.width / 2, this.height / 2 - 60, 0xFFFFFF);
        this.usernameField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        this.usernameField.textboxKeyTyped(typedChar, keyCode);
        if (keyCode == 1) {
            this.mc.displayGuiScreen(this.previousScreen);
        }
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        this.usernameField.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    protected void actionPerformed(GuiButton button) {
        if (button == null || !button.enabled) {
            return;
        }
        switch (button.id) {
            case 0: {
                this.handleLogin();
                break;
            }
            case 1: {
                this.handleGenerateRandom();
                break;
            }
            case 2: {
                this.mc.displayGuiScreen(this.previousScreen);
                break;
            }
            default: {
                System.err.println("Unknown button ID: " + button.id);
            }
        }
    }

    private void handleLogin() {
        String username = this.usernameField.getText().trim();
        if (username.isEmpty()) {
            System.out.println("Username cannot be empty!");
            return;
        }
        boolean loginSuccess = CrackedAuth.login(username);
        if (loginSuccess) {
            System.out.println("Logged in successfully as: " + username);
            this.mc.displayGuiScreen((GuiScreen)new GuiAccountManager(this.previousScreen));
        } else {
            System.out.println("Failed to log in!");
        }
    }

    private void handleGenerateRandom() {
        CompletableFuture.runAsync(() -> {
            String randomUsername = UsernameGenerator.generate();
            SwingUtilities.invokeLater(() -> this.usernameField.setText(randomUsername));
        });
    }

    public void updateScreen() {
        this.usernameField.updateCursorCounter();
    }
}
