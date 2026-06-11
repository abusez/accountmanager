/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiDisconnected
 *  net.minecraft.client.gui.GuiMultiplayer
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.gui.GuiSelectWorld
 *  net.minecraft.client.multiplayer.ServerData
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.util.IChatComponent
 *  net.minecraftforge.client.event.GuiScreenEvent$ActionPerformedEvent
 *  net.minecraftforge.client.event.GuiScreenEvent$InitGuiEvent$Post
 *  net.minecraftforge.event.world.WorldEvent$Load
 *  net.minecraftforge.fml.common.eventhandler.SubscribeEvent
 *  net.minecraftforge.fml.common.gameevent.TickEvent$Phase
 *  net.minecraftforge.fml.common.gameevent.TickEvent$RenderTickEvent
 *  net.minecraftforge.fml.relauncher.ReflectionHelper
 *  org.apache.commons.lang3.StringUtils
 */
package me.ksyz.accountmanager;

import java.lang.reflect.Field;
import me.ksyz.accountmanager.AccountManager;
import me.ksyz.accountmanager.auth.Account;
import me.ksyz.accountmanager.auth.SessionManager;
import me.ksyz.accountmanager.gui.GuiAccountManager;
import me.ksyz.accountmanager.utils.TextFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.commons.lang3.StringUtils;

public class Events {
    private static final Minecraft mc = Minecraft.func_71410_x();

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || Events.mc.field_71462_r == null) {
            return;
        }
        if (Events.mc.field_71462_r instanceof GuiSelectWorld || Events.mc.field_71462_r instanceof GuiMultiplayer) {
            String text = TextFormatting.translate(String.format("&7Username: &3%s&r", SessionManager.get().func_111285_a()));
            GlStateManager.func_179140_f();
            Events.mc.field_71462_r.func_73731_b(Events.mc.field_71466_p, text, 3, 3, -1);
            GlStateManager.func_179145_e();
        }
    }

    @SubscribeEvent
    public void initGuiEvent(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.gui instanceof GuiSelectWorld || event.gui instanceof GuiMultiplayer) {
            event.buttonList.add(new GuiButton(69, event.gui.field_146294_l - 106, 6, 100, 20, "Accounts"));
        }
        if (event.gui instanceof GuiDisconnected) {
            try {
                String unban;
                Field f = ReflectionHelper.findField(GuiDisconnected.class, (String[])new String[]{"message", "field_146304_f"});
                IChatComponent message = (IChatComponent)f.get(event.gui);
                String text = message.func_150254_d().split("\n\n")[0];
                if (text.equals("\u00a7r\u00a7cYou are permanently banned from this server!") || text.equals("\u00a7r\u00a7cYour account has been blocked.")) {
                    AccountManager.load();
                    for (Account account : AccountManager.accounts) {
                        if (!mc.func_110432_I().func_111285_a().equals(account.getUsername())) continue;
                        account.setUnban(-1L);
                    }
                    AccountManager.save();
                    return;
                }
                if ((text.matches("\u00a7r\u00a7cYou are temporarily banned for \u00a7r\u00a7f.*\u00a7r\u00a7c from this server!") || text.matches("\u00a7r\u00a7cYour account is temporarily blocked for \u00a7r\u00a7f.*\u00a7r\u00a7c from this server!")) && (unban = StringUtils.substringBetween((String)text, (String)"\u00a7r\u00a7f", (String)"\u00a7r\u00a7c")) != null) {
                    long time = System.currentTimeMillis();
                    block15: for (String duration : unban.split(" ")) {
                        String type = duration.substring(duration.length() - 1);
                        long value = Long.parseLong(duration.substring(0, duration.length() - 1));
                        switch (type) {
                            case "d": {
                                time += value * 86400000L;
                                continue block15;
                            }
                            case "h": {
                                time += value * 3600000L;
                                continue block15;
                            }
                            case "m": {
                                time += value * 60000L;
                                continue block15;
                            }
                            case "s": {
                                time += value * 1000L;
                            }
                        }
                    }
                    AccountManager.load();
                    for (Account account : AccountManager.accounts) {
                        if (!mc.func_110432_I().func_111285_a().equals(account.getUsername())) continue;
                        account.setUnban(time);
                    }
                    AccountManager.save();
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    @SubscribeEvent
    public void onClick(GuiScreenEvent.ActionPerformedEvent event) {
        if ((event.gui instanceof GuiSelectWorld || event.gui instanceof GuiMultiplayer) && event.button.field_146127_k == 69) {
            mc.func_147108_a((GuiScreen)new GuiAccountManager(event.gui));
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        String serverIP;
        ServerData serverData = mc.func_147104_D();
        if (serverData != null && ((serverIP = serverData.field_78845_b).endsWith("hypixel.net") || serverIP.endsWith("hypixel.io"))) {
            AccountManager.load();
            for (Account account : AccountManager.accounts) {
                if (!mc.func_110432_I().func_111285_a().equals(account.getUsername())) continue;
                account.setUnban(0L);
            }
            AccountManager.save();
        }
    }
}
