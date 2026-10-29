package com.dashclient;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Working vanilla video presets with original DashClient controls. */
public final class VideoPresets extends Screen {
    private int x,y,w,h;
    private static int view,sim,cap;
    private static boolean saved;
    private String status="Video presets apply directly to Minecraft settings.";
    private static final String[] PRESETS={"BALANCED","COMPETITIVE","BATTERY","RESTORE"};

    public VideoPresets(){super(Component.literal("DashClient Performance"));}
    private void apply(String preset){
        var o=minecraft.options;
        if(o==null)return;
        if(!saved&&!preset.equals("RESTORE")){
            view=o.renderDistance().get();
            sim=o.simulationDistance().get();
            cap=o.framerateLimit().get();
            saved=true;
        }
        if(preset.equals("BALANCED")){
            o.renderDistance().set(Math.min(o.renderDistance().get(),8));
            o.simulationDistance().set(Math.min(o.simulationDistance().get(),6));
        }else if(preset.equals("COMPETITIVE")){
            o.renderDistance().set(Math.min(o.renderDistance().get(),5));
            o.simulationDistance().set(Math.min(o.simulationDistance().get(),5));
        }else if(preset.equals("BATTERY")){
            o.renderDistance().set(Math.min(o.renderDistance().get(),5));
            o.simulationDistance().set(Math.min(o.simulationDistance().get(),4));
            o.framerateLimit().set(Math.min(o.framerateLimit().get(),45));
        }else if(preset.equals("RESTORE")){
            if(!saved){status="No previous preset settings to restore.";return;}
            o.renderDistance().set(view);o.simulationDistance().set(sim);
            o.framerateLimit().set(cap);
            saved=false;
        }
        status=preset+" applied. Results depend on your device.";
    }
    @Override protected void init(){
        w=Math.min(width-12,565);h=Math.min(height-12,320);
        x=(width-w)/2;y=(height-h)/2;
    }
    private static boolean hit(double mx,double my,int bx,int by,int bw,int bh){
        return mx>=bx&&mx<bx+bw&&my>=by&&my<by+bh;
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubleClick){
        if(e.button()!=0)return super.mouseClicked(e,doubleClick);
        int gap=9,cw=(w-35)/2,ch=Math.max(32,Math.min(56,(h-140)/2));
        for(int i=0;i<PRESETS.length;i++){
            int bx=x+13+(i%2)*(cw+gap),by=y+70+(i/2)*(ch+8);
            if(hit(e.x(),e.y(),bx,by,cw,ch)){
                apply(PRESETS[i]);return true;
            }
        }
        if(hit(e.x(),e.y(),x+13,y+h-37,w-26,26)){
            minecraft.gui.setScreen(new ClientMenu());return true;
        }
        return super.mouseClicked(e,doubleClick);
    }
    @Override public void onClose(){minecraft.gui.setScreen(new ClientMenu());}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float tick){
        g.fill(0,0,width,height,DashTheme.BACKDROP);
        DashTheme.rounded(g,x,y,w,h,DashTheme.FRAME);
        DashTheme.rounded(g,x+4,y+4,w-8,35,DashTheme.PANEL);
        DashTheme.line(g,x+10,y+40,w-20,DashTheme.ACCENT);
        super.extractRenderState(g,mx,my,tick);
        g.text(font,"PERFORMANCE",x+16,y+16,DashTheme.TEXT,true);
        g.text(font,DashTheme.fit(font,status,w-26),x+13,y+48,DashTheme.MUTED,false);
        int gap=9,cw=(w-35)/2,ch=Math.max(32,Math.min(56,(h-140)/2));
        String[] desc={"8 chunks / 6 simulation","5 chunks / 5 simulation",
            "5 chunks / 45 FPS cap","Return to saved settings"};
        for(int i=0;i<PRESETS.length;i++){
            int bx=x+13+(i%2)*(cw+gap),by=y+70+(i/2)*(ch+8);
            boolean hover=hit(mx,my,bx,by,cw,ch);
            DashTheme.rounded(g,bx,by,cw,ch,hover?DashTheme.ACCENT:DashTheme.LINE);
            DashTheme.rounded(g,bx+1,by+1,cw-2,ch-2,hover?DashTheme.CARD_HOVER:DashTheme.CARD);
            g.text(font,PRESETS[i],bx+11,by+7,DashTheme.TEXT,true);
            if(ch>=40)g.text(font,DashTheme.fit(font,desc[i],cw-19),bx+11,by+23,
                DashTheme.MUTED,false);
        }
        if(h>265){
            g.text(font,"These settings affect vanilla graphics only.",x+13,
                y+h-61,DashTheme.MUTED,false);
        }
        DashTheme.button(g,x+13,y+h-37,w-26,26,"BACK TO MODS",font,
            hit(mx,my,x+13,y+h-37,w-26,26),false);
    }
}
