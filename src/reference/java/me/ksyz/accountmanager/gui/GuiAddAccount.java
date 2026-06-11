/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiScreen
 */
package me.ksyz.accountmanager.gui;

import me.ksyz.accountmanager.gui.GuiAccountManager;
import me.ksyz.accountmanager.gui.GuiCookieAuth;
import me.ksyz.accountmanager.gui.GuiCrackedAuth;
import me.ksyz.accountmanager.gui.GuiMicrosoftAuth;
import me.ksyz.accountmanager.gui.GuiTokenLogin;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public class GuiAddAccount
extends GuiScreen {
    private final GuiScreen previousScreen;

    public GuiAddAccount(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
    }

    public void func_73866_w_() {
        this.field_146292_n.add(new GuiButton(0, this.field_146294_l / 2 - 100, this.field_146295_m / 2 - 30, 200, 20, "Microsoft"));
        this.field_146292_n.add(new GuiButton(1, this.field_146294_l / 2 - 100, this.field_146295_m / 2, 200, 20, "Cookie"));
        this.field_146292_n.add(new GuiButton(2, this.field_146294_l / 2 - 100, this.field_146295_m / 2 + 30, 200, 20, "Cracked"));
        this.field_146292_n.add(new GuiButton(3, this.field_146294_l / 2 - 100, this.field_146295_m / 2 + 60, 200, 20, "AccessToken"));
        this.field_146292_n.add(new GuiButton(4, this.field_146294_l / 2 - 100, this.field_146295_m / 2 + 90, 200, 20, "Back"));
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        this.func_146276_q_();
        this.func_73732_a(this.field_146289_q, "Choose Account Type to Add", this.field_146294_l / 2, this.field_146295_m / 2 - 60, 0xFFFFFF);
        super.func_73863_a(mouseX, mouseY, partialTicks);
    }

    protected void func_146284_a(GuiButton button) {
        if (button == null) {
            return;
        }
        switch (button.field_146127_k) {
            case 0: {
                this.field_146297_k.func_147108_a((GuiScreen)new GuiMicrosoftAuth(this));
                break;
            }
            case 1: {
                this.field_146297_k.func_147108_a((GuiScreen)new GuiCookieAuth(this));
                break;
            }
            case 2: {
                this.field_146297_k.func_147108_a((GuiScreen)new GuiCrackedAuth(this));
                break;
            }
            case 3: {
                this.field_146297_k.func_147108_a((GuiScreen)new GuiTokenLogin(this));
                break;
            }
            case 4: {
                this.field_146297_k.func_147108_a((GuiScreen)new GuiAccountManager(this.previousScreen));
            }
        }
    }

    public void func_146281_b() {
    }
}
