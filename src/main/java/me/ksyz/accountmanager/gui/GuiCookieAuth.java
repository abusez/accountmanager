package me.ksyz.accountmanager.gui;

import me.ksyz.accountmanager.auth.CookieAuth;
import me.ksyz.accountmanager.utils.TextFormatting;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GuiCookieAuth extends GuiScreen {
    private final GuiScreen previousScreen;
    private ExecutorService executor;
    private CompletableFuture<Boolean> task;
    private GuiButton openButton;
    private boolean openButtonEnabled = true;
    private GuiButton cancelButton;
    private String status;
    private String cause;

    public GuiCookieAuth(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int buttonWidth = 200;
        int buttonHeight = 20;
        int spacing = 5;
        int centerX = this.width / 2;
        int startX = centerX - buttonWidth / 2;
        int baseY = this.height / 2 + this.fontRendererObj.FONT_HEIGHT / 2 + this.fontRendererObj.FONT_HEIGHT * 2;

        this.openButton = new GuiButton(0, startX, baseY, buttonWidth, buttonHeight, "Open Cookie File");
        this.buttonList.add(this.openButton);
        this.cancelButton = new GuiButton(1, startX, baseY + buttonHeight + spacing, buttonWidth, buttonHeight, "Cancel");
        this.buttonList.add(this.cancelButton);
        this.status = "&fSelect a cookie file to authenticate&r";

        if (this.executor == null || this.executor.isShutdown()) {
            this.executor = Executors.newSingleThreadExecutor();
        }
    }

    @Override
    public void onGuiClosed() {
        if (this.task != null && !this.task.isDone()) {
            this.task.cancel(true);
        }
        if (this.executor != null && !this.executor.isShutdown()) {
            this.executor.shutdownNow();
        }
    }

    public void setStatus(String status) {
        if (this.mc != null) {
            this.mc.addScheduledTask(() -> this.status = status);
        } else {
            this.status = status;
        }
    }

    private void runOnMainThread(Runnable runnable) {
        this.mc.addScheduledTask(runnable);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (this.openButton != null) {
            this.openButton.enabled = this.openButtonEnabled;
        }

        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.drawCenteredString(
                this.fontRendererObj,
                "Cookie Authentication",
                this.width / 2,
                this.height / 2 - this.fontRendererObj.FONT_HEIGHT / 2 - this.fontRendererObj.FONT_HEIGHT * 2,
                0xAAAAAA
        );

        if (this.status != null) {
            this.drawCenteredString(
                    this.fontRendererObj,
                    TextFormatting.translate(this.status),
                    this.width / 2,
                    this.height / 2 - this.fontRendererObj.FONT_HEIGHT / 2,
                    -1
            );
        }

        if (this.cause != null) {
            String causeText = TextFormatting.translate(this.cause);
            Gui.drawRect(
                    0,
                    this.height - 2 - this.fontRendererObj.FONT_HEIGHT - 3,
                    3 + this.mc.fontRendererObj.getStringWidth(causeText) + 3,
                    this.height,
                    0x64000000
            );
            this.drawString(this.fontRendererObj, causeText, 3, this.height - 2 - this.fontRendererObj.FONT_HEIGHT, -1);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 1) {
            this.actionPerformed(this.cancelButton);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == null || !button.enabled) {
            return;
        }

        switch (button.id) {
            case 0: {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                this.setStatus("&aOpening file picker...&r");
                SwingUtilities.invokeLater(() -> {
                    JFileChooser chooser = new JFileChooser(System.getProperty("user.home") + File.separator + "Downloads");
                    chooser.setDialogTitle("Select Cookie File");
                    chooser.setFileFilter(new FileNameExtensionFilter("Cookie/Text Files", "txt", "json", "cookies"));
                    int result = chooser.showOpenDialog(null);

                    if (result != JFileChooser.APPROVE_OPTION) {
                        this.runOnMainThread(() -> this.setStatus("&eFile selection canceled.&r"));
                        return;
                    }

                    File selectedFile = chooser.getSelectedFile();
                    if (selectedFile == null || !selectedFile.exists()) {
                        this.runOnMainThread(() -> this.setStatus("&cSelected file does not exist!&r"));
                        return;
                    }

                    this.runOnMainThread(() -> {
                        this.openButtonEnabled = false;
                        this.setStatus("&fReading cookie file...&r");
                    });

                    this.task = CookieAuth.addAccountFromCookieFile(selectedFile, this);
                    this.task.whenComplete((success, error) -> this.runOnMainThread(() -> {
                        this.openButtonEnabled = true;
                        if (Boolean.TRUE.equals(success)) {
                            this.mc.displayGuiScreen(new GuiAccountManager(this.previousScreen));
                        } else if (error != null) {
                            this.setStatus("&cAuthentication failed: " + error.getMessage() + "&r");
                        }
                    }));
                });
                break;
            }
            case 1: {
                this.mc.displayGuiScreen(this.previousScreen);
                break;
            }
            default: {
                System.err.println("Unknown button ID: " + button.id);
            }
        }
    }
}
