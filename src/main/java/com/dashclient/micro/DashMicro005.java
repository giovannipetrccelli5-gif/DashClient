package com.dashclient.micro;

import com.dashclient.DashClient;
import com.dashclient.Feature;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Original optional DashClient addon: Coordinates / Compact.
 * One addon per JAR. Requires base DashClient and Fabric API.
 */
public final class DashMicro005 implements ClientModInitializer {
    @Override public void onInitializeClient() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("dashmicro005", "hud"), (graphics, tick) -> {
            Minecraft mc=Minecraft.getInstance();
            if(mc.player==null || mc.gui.screen()!=null)return;
            Feature metric=DashClient.feature("coords");
            if(metric==null)return;
            String value=DashClient.readout(metric,mc);
            if(value.isEmpty())return;
            int width=mc.getWindow().getGuiScaledWidth();
            int height=mc.getWindow().getGuiScaledHeight();
            int textWidth=mc.font.width(value);
            int x=12;
            int y=18 + 1*16;
            x=Math.max(4,Math.min(width-textWidth-5,x));
            y=Math.max(4,Math.min(height-15,y));
            
            
            graphics.text(mc.font,value,x,y,0xFFF7FAFF,true);
        });
    }
}
