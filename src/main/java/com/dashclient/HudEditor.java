package com.dashclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Drag actual HUD readouts. Per-widget positions persist between sessions. */
public final class HudEditor extends Screen {
    private record Box(Feature feature,int x,int y,int w,int h){}
    private final Map<String,Box> boxes=new HashMap<>();
    private Feature dragging;
    private int grabX,grabY,dropX,dropY;
    private boolean dirty;
    public HudEditor(){super(Component.literal("DashClient HUD Editor"));}
    @Override protected void init(){
        int w=Math.max(50,(width-32)/2);
        addRenderableWidget(Button.builder(Component.literal("Reset Layout"),b->{
            ClientConfig.resetHud();dirty=true;
        }).bounds(8,height-30,w,23).build());
        addRenderableWidget(Button.builder(Component.literal("Save / Done"),b->{
            minecraft.gui.setScreen(new ClientMenu());
        }).bounds(16+w,height-30,w,23).build());
    }
    private void layout(){
        boxes.clear();
        Minecraft mc=Minecraft.getInstance();
        List<Feature> hud=DashClient.hudFeatures();
        int i=0;
        for(Feature f:hud){
            String label=DashClient.readout(f,mc);
            int bw=Math.max(40,mc.font.width(label)+16);
            int x=ClientConfig.x(f,12);
            int y=ClientConfig.y(f,24+i*20);
            if(dragging==f){x=dropX;y=dropY;}
            x=Math.max(0,Math.min(width-bw,x));
            y=Math.max(0,Math.min(height-45,y));
            boxes.put(f.id,new Box(f,x,y,bw,17));
            i++;
        }
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubleClick){
        if(e.button()==0){
            layout();
            for(Box box:boxes.values()){
                if(e.x()>=box.x&&e.x()<=box.x+box.w&&e.y()>=box.y&&e.y()<=box.y+box.h){
                    dragging=box.feature;grabX=(int)e.x()-box.x;grabY=(int)e.y()-box.y;
                    dropX=box.x;dropY=box.y;
                    return true;
                }
            }
        }
        return super.mouseClicked(e,doubleClick);
    }
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){
        if(dragging!=null&&e.button()==0){
            dropX=Math.max(0,Math.min(width-40,(int)e.x()-grabX));
            dropY=Math.max(0,Math.min(height-46,(int)e.y()-grabY));
            dirty=true;
            return true;
        }
        return super.mouseDragged(e,dx,dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent e){
        if(dragging!=null){
            ClientConfig.hud(dragging,dropX,dropY);
            dragging=null;dirty=false;
            return true;
        }
        return super.mouseReleased(e);
    }
    @Override public void onClose(){minecraft.gui.setScreen(new ClientMenu());}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float tick){
        g.fill(0,0,width,height,0xDA0B1018);
        g.fill(0,0,width,36,0xFF1B2A40);
        super.extractRenderState(g,mx,my,tick);
        g.centeredText(font,"DASHCLIENT / DRAG HUD ELEMENTS",width/2,11,0xFFC5D2FF);
        layout();
        for(Box b:boxes.values()){
            g.fill(b.x-1,b.y-1,b.x+b.w+1,b.y+b.h+1,0xFF708FD9);
            g.fill(b.x,b.y,b.x+b.w,b.y+b.h,0xDB1D2637);
            g.text(font,DashClient.readout(b.feature,minecraft),b.x+7,b.y+4,0xFFEDF2FF,true);
        }
        if(dirty)g.centeredText(font,"Release to save",width/2,height-44,0xFF9CE3C2);
    }
}