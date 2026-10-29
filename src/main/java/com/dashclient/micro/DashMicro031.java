package com.dashclient.micro;

import com.dashclient.DashClient;
import com.dashclient.Feature;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Original optional DashClient addon: RAM Usage / Midnight.
 * One addon per JAR. Requires base DashClient and Fabric API.
 */
public final class DashMicro031 implements ClientModInitializer {
    @Override public void onInitializeClient() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("dashmicro031", "hud"), (graphics, tick) -> {
            Minecraft mc=Minecraft.getInstance();
            if(mc.player==null || mc.gui.screen()!=null)return;
            Feature metric=DashClient.feature("memory");
            if(metric==null)return;
            String value=DashClient.readout(metric,mc);
            if(value.isEmpty())return;
            int width=mc.getWindow().getGuiScaledWidth();
            int height=mc.getWindow().getGuiScaledHeight();
            int textWidth=mc.font.width(value);
            int x=12;
            int y=height-26 - 7*16;
            x=Math.max(4,Math.min(width-textWidth-5,x));
            y=Math.max(4,Math.min(height-15,y));
            graphics.fill(x-5,y-4,x+textWidth+6,y+12,0xD00D101E);
            
            graphics.text(mc.font,value,x,y,0xFFB6BDDB,true);
        });
    }
}
