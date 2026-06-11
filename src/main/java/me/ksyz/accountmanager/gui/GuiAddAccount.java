package me.ksyz.accountmanager.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public class GuiAddAccount extends GuiScreen {
    private final GuiScreen previousScreen;

    public GuiAddAccount(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
    }

    @Override
    public void initGui() {
        this.buttonList.add(new GuiButton(0, this.width / 2 - 100, this.height / 2 - 45, 200, 20, "Microsoft"));
        this.buttonList.add(new GuiButton(1, this.width / 2 - 100, this.height / 2 - 20, 200, 20, "Cookie"));
        this.buttonList.add(new GuiButton(2, this.width / 2 - 100, this.height / 2 + 5, 200, 20, "Cracked"));
        this.buttonList.add(new GuiButton(3, this.width / 2 - 100, this.height / 2 + 30, 200, 20, "Access Token"));
        this.buttonList.add(new GuiButton(5, this.width / 2 - 100, this.height / 2 + 55, 200, 20, "Refresh Token"));
        this.buttonList.add(new GuiButton(4, this.width / 2 - 100, this.height / 2 + 80, 200, 20, "Back"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, "Choose Account Type to Add", this.width / 2, this.height / 2 - 70, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == null) {
            return;
        }

        switch (button.id) {
            case 0:
                this.mc.displayGuiScreen(new GuiMicrosoftAuth(this));
                break;
            case 1:
                this.mc.displayGuiScreen(new GuiCookieAuth(this));
                break;
            case 2:
                this.mc.displayGuiScreen(new GuiCrackedAuth(this));
                break;
            case 3:
                this.mc.displayGuiScreen(new GuiTokenLogin(this));
                break;
            case 5:
                this.mc.displayGuiScreen(new GuiRefreshTokenLogin(this));
                break;
            case 4:
                this.mc.displayGuiScreen(new GuiAccountManager(this.previousScreen));
                break;
        }
    }

    @Override
    public void onGuiClosed() {
    }
}
