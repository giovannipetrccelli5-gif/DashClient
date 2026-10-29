package com.dashclient;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Transparent vanilla-setting presets; no unverifiable FPS claims. */
public final class VideoPresets extends Screen {
    private int x,y,w,h,view,sim,cap;
    private boolean saved;
    private String status="Choose a preset or restore your own settings.";
    public VideoPresets(){super(Component.literal("DashClient Video Settings"));}
    private void use(String preset){
        var o=minecraft.options;
        if(o==null)return;
        if(!saved){view=o.renderDistance().get();sim=o.simulationDistance().get();
            cap=o.framerateLimit().get();saved=true;}
        if(preset.equals("Balanced")){
            o.renderDistance().set(Math.min(o.renderDistance().get(),8));
            o.simulationDistance().set(Math.min(o.simulationDistance().get(),6));
        }else if(preset.equals("Competitive")){
            o.renderDistance().set(Math.min(o.renderDistance().get(),5));
            o.simulationDistance().set(Math.min(o.simulationDistance().get(),5));
        }else if(preset.equals("Battery")){
            o.renderDistance().set(Math.min(o.renderDistance().get(),5));
            o.simulationDistance().set(Math.min(o.simulationDistance().get(),4));
            o.framerateLimit().set(Math.min(o.framerateLimit().get(),45));
        }else if(preset.equals("Restore")&&saved){
            o.renderDistance().set(view);o.simulationDistance().set(sim);
            o.framerateLimit().set(cap);
            saved=false;
        }
        status=preset+" applied. Performance varies by device.";
    }
    @Override protected void init(){
        w=Math.min(width-12,460);h=Math.min(height-12,275);
        x=(width-w)/2;y=(height-h)/2;
        String[] presets={"Balanced","Competitive","Battery","Restore"};
        for(int i=0;i<presets.length;i++){
            String preset=presets[i];
            addRenderableWidget(Button.builder(Component.literal(preset),b->use(preset))
                .bounds(x+12,y+65+i*33,w-24,27).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Back to Mods"),b->
            minecraft.gui.setScreen(new ClientMenu())
        ).bounds(x+12,y+h-34,w-24,23).build());
    }
    @Override public void onClose(){minecraft.gui.setScreen(new ClientMenu());}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float tick){
        g.fill(0,0,width,height,0xB5080A12);
        g.fill(x-1,y-1,x+w+1,y+h+1,0xFF698EF0);
        g.fill(x,y,x+w,y+h,0xF0141925);
        g.fill(x+2,y+2,x+w-2,y+34,0xFF1D2940);
        super.extractRenderState(g,mx,my,tick);
        g.centeredText(font,"DASHCLIENT / PERFORMANCE",x+w/2,y+16,0xFFC1D0FF);
        g.centeredText(font,"Adjust vanilla graphics settings",x+w/2,y+51,0xFFE1E6F0);
        String display=status;
        while(!display.isEmpty()&&font.width(display)>w-20)display=display.substring(0,display.length()-1);
        g.centeredText(font,display,x+w/2,y+h-48,0xFFB5C7DB);
    }
}