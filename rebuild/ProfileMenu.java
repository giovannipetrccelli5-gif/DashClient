package com.dashclient;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Four independent module toggle profiles. */
public final class ProfileMenu extends Screen {
    private int x,y,w,h;
    public ProfileMenu(){super(Component.literal("DashClient Profiles"));}
    @Override protected void init(){
        w=Math.min(width-12,480);h=Math.min(height-12,275);
        x=(width-w)/2;y=(height-h)/2;
        int col=(w-34)/2;
        for(int i=0;i<ClientConfig.PROFILES.length;i++){
            final String profile=ClientConfig.PROFILES[i];
            addRenderableWidget(Button.builder(Component.literal(
                (ClientConfig.active.equals(profile)?"[ACTIVE] ":"")+profile),b->{
                ClientConfig.switchProfile(profile);
                minecraft.gui.setScreen(new ProfileMenu());
            }).bounds(x+12+(i%2)*(col+8),y+72+(i/2)*39,col,32).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Back to Mods"),b->
            minecraft.gui.setScreen(new ClientMenu())
        ).bounds(x+12,y+h-37,w-24,24).build());
    }
    @Override public void onClose(){minecraft.gui.setScreen(new ClientMenu());}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        g.fill(0,0,width,height,0xB3080A12);
        g.fill(x-1,y-1,x+w+1,y+h+1,0xFF698EF0);
        g.fill(x,y,x+w,y+h,0xF0141925);
        g.fill(x+2,y+2,x+w-2,y+35,0xFF1D2940);
        super.extractRenderState(g,mx,my,delta);
        g.centeredText(font,"DASHCLIENT / PROFILES",x+w/2,y+16,0xFFC1D0FF);
        g.centeredText(font,"Switch independent module setups",x+w/2,y+52,0xFFE1E6F0);
        g.centeredText(font,"Changes are saved automatically",x+w/2,y+h-65,0xFF99B3D1);
    }
}