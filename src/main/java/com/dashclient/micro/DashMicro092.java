package com.dashclient.micro;

import com.dashclient.DashClient;
import com.dashclient.Feature;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Original optional DashClient addon: Durability / Neon.
 * One addon per JAR. Requires base DashClient and Fabric API.
 */
public final class DashMicro092 implements ClientModInitializer {
    @Override public void onInitializeClient() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("dashmicro092", "hud"), (graphics, tick) -> {
            Minecraft mc=Minecraft.getInstance();
            if(mc.player==null || mc.gui.screen()!=null)return;
            Feature metric=DashClient.feature("durability");
            if(metric==null)return;
            String value=DashClient.readout(metric,mc);
            if(value.isEmpty())return;
            int width=mc.getWindow().getGuiScaledWidth();
            int height=mc.getWindow().getGuiScaledHeight();
            int textWidth=mc.font.width(value);
            int x=width-textWidth-14;
            int y=height-26 - 10*16;
            x=Math.max(4,Math.min(width-textWidth-5,x));
            y=Math.max(4,Math.min(height-15,y));
            graphics.fill(x-5,y-4,x+textWidth+6,y+12,0xB13A245E);
            graphics.fill(x-5,y-4,x-2,y+12,0xFF8FA7FF);
            graphics.text(mc.font,value,x,y,0xFFD5B7FF,true);
        });
    }
}
