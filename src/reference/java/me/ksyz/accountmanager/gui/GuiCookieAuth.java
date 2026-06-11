/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.Gui
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiScreen
 */
package me.ksyz.accountmanager.gui;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import me.ksyz.accountmanager.auth.CookieAuth;
import me.ksyz.accountmanager.gui.GuiAccountManager;
import me.ksyz.accountmanager.utils.TextFormatting;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public class GuiCookieAuth
extends GuiScreen {
    private final GuiScreen previousScreen;
    private ExecutorService executor = null;
    private CompletableFuture<Void> task = null;
    private GuiButton openButton = null;
    private boolean openButtonEnabled = true;
    private GuiButton cancelButton = null;
    public String status = null;
    private String cause = null;

    public GuiCookieAuth(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
    }

    public void func_73866_w_() {
        this.field_146292_n.clear();
        int buttonWidth = 200;
        int buttonHeight = 20;
        int spacing = 5;
        int centerX = this.field_146294_l / 2;
        int startX = centerX - buttonWidth / 2;
        int baseY = this.field_146295_m / 2 + this.field_146289_q.field_78288_b / 2 + this.field_146289_q.field_78288_b * 2;
        this.openButton = new GuiButton(0, startX, baseY, buttonWidth, buttonHeight, "Open Cookie File");
        this.field_146292_n.add(this.openButton);
        this.cancelButton = new GuiButton(1, startX, baseY + buttonHeight + spacing, buttonWidth, buttonHeight, "Cancel");
        this.field_146292_n.add(this.cancelButton);
        this.status = "&fSelect a cookie file to authenticate&r";
        if (this.executor == null) {
            this.executor = Executors.newSingleThreadExecutor();
        }
    }

    public void func_146281_b() {
        if (this.task != null && !this.task.isDone()) {
            this.task.cancel(true);
            this.executor.shutdownNow();
        }
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        if (this.openButton != null) {
            this.openButton.field_146124_l = this.openButtonEnabled;
        }
        this.func_146276_q_();
        super.func_73863_a(mouseX, mouseY, partialTicks);
        this.func_73732_a(this.field_146289_q, "Cookie Authentication", this.field_146294_l / 2, this.field_146295_m / 2 - this.field_146289_q.field_78288_b / 2 - this.field_146289_q.field_78288_b * 2, 0xAAAAAA);
        if (this.status != null) {
            this.func_73732_a(this.field_146289_q, TextFormatting.translate(this.status), this.field_146294_l / 2, this.field_146295_m / 2 - this.field_146289_q.field_78288_b / 2, -1);
        }
        if (this.cause != null) {
            String causeText = TextFormatting.translate(this.cause);
            Gui.func_73734_a((int)0, (int)(this.field_146295_m - 2 - this.field_146289_q.field_78288_b - 3), (int)(3 + this.field_146297_k.field_71466_p.func_78256_a(causeText) + 3), (int)this.field_146295_m, (int)0x64000000);
            this.func_73731_b(this.field_146289_q, TextFormatting.translate(this.cause), 3, this.field_146295_m - 2 - this.field_146289_q.field_78288_b, -1);
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
                    try {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    SwingUtilities.invokeLater(() -> {
                        FileDialog fileDialog = new FileDialog((Frame)null, "Select Cookie File", 0);
                        fileDialog.setDirectory(System.getProperty("user.home") + File.separator + "Downloads");
                        this.status = "&aFile Picker has been opened in the Background!&r";
                        fileDialog.setFile("*.txt");
                        fileDialog.setModal(true);
                        fileDialog.setVisible(true);
                        String selectedFileName = fileDialog.getFile();
                        if (selectedFileName != null) {
                            File selectedFile = new File(fileDialog.getDirectory(), selectedFileName);
                            if (selectedFile.exists()) {
                                this.openButtonEnabled = false;
                                this.status = "&fReading cookie file...&r";
                                CompletableFuture<Boolean> authTask = CookieAuth.addAccountFromCookieFile(selectedFile, this);
                                authTask.thenAccept(success -> {
                                    if (success.booleanValue()) {
                                        this.field_146297_k.func_147108_a((GuiScreen)new GuiAccountManager(this.previousScreen));
                                    }
                                });
                            } else {
                                this.status = "&cSelected file does not exist!&r";
                            }
                        } else {
                            this.status = "&eFile selection canceled.&r";
                        }
                    });
                    break;
                }
                case 1: {
                    this.field_146297_k.func_147108_a(this.previousScreen);
                    break;
                }
                default: {
                    System.err.println("Unknown button ID: " + button.field_146127_k);
                }
            }
        }
    }
}
