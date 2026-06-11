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

    public void func_73866_w_() {
        this.usernameField = new GuiTextField(0, this.field_146289_q, this.field_146294_l / 2 - 100, this.field_146295_m / 2 - 30, 200, 20);
        this.usernameField.func_146203_f(16);
        this.usernameField.func_146195_b(true);
        this.loginButton = new GuiButton(0, this.field_146294_l / 2 - 100, this.field_146295_m / 2, 200, 20, "Login");
        this.field_146292_n.add(this.loginButton);
        this.generateRandomButton = new GuiButton(1, this.field_146294_l / 2 - 100, this.field_146295_m / 2 + 25, 200, 20, "Generate Random");
        this.field_146292_n.add(this.generateRandomButton);
        this.cancelButton = new GuiButton(2, this.field_146294_l / 2 - 100, this.field_146295_m / 2 + 50, 200, 20, "Cancel");
        this.field_146292_n.add(this.cancelButton);
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        this.func_146276_q_();
        this.func_73732_a(this.field_146289_q, "Cracked Authentication", this.field_146294_l / 2, this.field_146295_m / 2 - 60, 0xFFFFFF);
        this.usernameField.func_146194_f();
        super.func_73863_a(mouseX, mouseY, partialTicks);
    }

    protected void func_73869_a(char typedChar, int keyCode) throws IOException {
        this.usernameField.func_146201_a(typedChar, keyCode);
        if (keyCode == 1) {
            this.field_146297_k.func_147108_a(this.previousScreen);
        }
    }

    protected void func_73864_a(int mouseX, int mouseY, int mouseButton) throws IOException {
        this.usernameField.func_146192_a(mouseX, mouseY, mouseButton);
        super.func_73864_a(mouseX, mouseY, mouseButton);
    }

    protected void func_146284_a(GuiButton button) {
        if (button == null || !button.field_146124_l) {
            return;
        }
        switch (button.field_146127_k) {
            case 0: {
                this.handleLogin();
                break;
            }
            case 1: {
                this.handleGenerateRandom();
                break;
            }
            case 2: {
                this.field_146297_k.func_147108_a(this.previousScreen);
                break;
            }
            default: {
                System.err.println("Unknown button ID: " + button.field_146127_k);
            }
        }
    }

    private void handleLogin() {
        String username = this.usernameField.func_146179_b().trim();
        if (username.isEmpty()) {
            System.out.println("Username cannot be empty!");
            return;
        }
        boolean loginSuccess = CrackedAuth.login(username);
        if (loginSuccess) {
            System.out.println("Logged in successfully as: " + username);
            this.field_146297_k.func_147108_a((GuiScreen)new GuiAccountManager(this.previousScreen));
        } else {
            System.out.println("Failed to log in!");
        }
    }

    private void handleGenerateRandom() {
        CompletableFuture.runAsync(() -> {
            String randomUsername = UsernameGenerator.generate();
            SwingUtilities.invokeLater(() -> this.usernameField.func_146180_a(randomUsername));
        });
    }

    public void func_73876_c() {
        this.usernameField.func_146178_a();
    }
}
