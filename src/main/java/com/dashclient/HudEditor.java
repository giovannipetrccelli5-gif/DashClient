package com.dashclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Custom themed, draggable HUD editor. Positions persist in DashClient config. */
public final class HudEditor extends Screen {
    private record Box(Feature feature,int x,int y,int w,int h){}
    private final Map<String,Box> boxes=new HashMap<>();
    private Feature dragging;
    private int grabX,grabY,dropX,dropY;
    private boolean dirty;
    private String message="Drag a highlighted module to reposition it.";

    public HudEditor(){super(Component.literal("DashClient HUD Editor"));}
    private boolean hit(double mx,double my,int x,int y,int w,int h){
        return mx>=x&&mx<x+w&&my>=y&&my<y+h;
    }
    private String readout(Feature f,Minecraft mc){
        String readout=DashClient.readout(f,mc);
        return readout.isEmpty()?f.name+" / PREVIEW":readout;
    }
    private void layout(){
        boxes.clear();
        Minecraft mc=Minecraft.getInstance();
        List<Feature> hud=DashClient.hudFeatures();
        int i=0;
        for(Feature f:hud){
            String label=readout(f,mc);
            int bw=Math.max(55,mc.font.width(label)+21);
            int x=ClientConfig.x(f,15);
            int y=ClientConfig.y(f,57+i*24);
            if(dragging==f){x=dropX;y=dropY;}
            x=Math.max(3,Math.min(width-bw-3,x));
            y=Math.max(38,Math.min(height-53,y));
            boxes.put(f.id,new Box(f,x,y,bw,22));
            i++;
        }
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubleClick){
        if(e.button()!=0)return super.mouseClicked(e,doubleClick);
        if(hit(e.x(),e.y(),10,height-37,Math.max(90,width/4),25)){
            ClientConfig.resetHud();
            message="HUD positions reset to defaults.";
            dirty=false;
            return true;
        }
        int doneW=Math.max(90,width/4);
        if(hit(e.x(),e.y(),width-doneW-10,height-37,doneW,25)){
            minecraft.gui.setScreen(new ClientMenu());
            return true;
        }
        layout();
        for(Box b:boxes.values()){
            if(hit(e.x(),e.y(),b.x,b.y,b.w,b.h)){
                dragging=b.feature;
                grabX=(int)e.x()-b.x;
                grabY=(int)e.y()-b.y;
                dropX=b.x;
                dropY=b.y;
                dirty=false;
                return true;
            }
        }
        return super.mouseClicked(e,doubleClick);
    }
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){
        if(dragging!=null&&e.button()==0){
            dropX=Math.max(3,Math.min(width-45,(int)e.x()-grabX));
            dropY=Math.max(38,Math.min(height-53,(int)e.y()-grabY));
            dirty=true;
            return true;
        }
        return super.mouseDragged(e,dx,dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent e){
        if(dragging!=null){
            ClientConfig.hud(dragging,dropX,dropY);
            message=dragging.name+" position saved.";
            dragging=null;
            dirty=false;
            return true;
        }
        return super.mouseReleased(e);
    }
    @Override public void onClose(){minecraft.gui.setScreen(new ClientMenu());}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float tick){
        g.fill(0,0,width,height,0xEE090E1A);
        // Subtle alignment grid. No shaders or third-party textures are required.
        for(int gx=0;gx<width;gx+=24)
            g.fill(gx,35,gx+1,height-44,0x1E7394C4);
        for(int gy=35;gy<height-44;gy+=24)
            g.fill(0,gy,width,gy+1,0x1E7394C4);
        DashTheme.rounded(g,4,4,width-8,33,DashTheme.PANEL);
        DashTheme.line(g,9,36,width-18,DashTheme.ACCENT);
        DashTheme.rounded(g,4,height-44,width-8,40,DashTheme.FRAME);
        super.extractRenderState(g,mx,my,tick);
        g.text(font,"HUD EDITOR",14,15,DashTheme.TEXT,true);
        String stat=DashClient.hudFeatures().size()+" ACTIVE WIDGETS";
        g.text(font,stat,width-Math.min(130,font.width(stat)+15),15,DashTheme.BRIGHT,false);
        layout();
        if(boxes.isEmpty()){
            g.centeredText(font,"Enable some HUD modules in the main GUI first.",
                width/2,height/2,DashTheme.MUTED);
        }
        for(Box b:boxes.values()){
            boolean hovered=hit(mx,my,b.x,b.y,b.w,b.h);
            boolean active=dragging==b.feature;
            DashTheme.rounded(g,b.x,b.y,b.w,b.h,
                active?DashTheme.BRIGHT:(hovered?DashTheme.ACCENT:DashTheme.LINE));
            DashTheme.rounded(g,b.x+1,b.y+1,b.w-2,b.h-2,
                active?DashTheme.CARD_HOVER:DashTheme.CARD);
            g.fill(b.x+3,b.y+5,b.x+5,b.y+b.h-5,
                active?DashTheme.GREEN:DashTheme.ACCENT);
            g.text(font,readout(b.feature,minecraft),b.x+12,b.y+7,DashTheme.TEXT,true);
        }
        int resetW=Math.max(90,width/4),doneW=Math.max(90,width/4);
        DashTheme.button(g,10,height-37,resetW,25,"RESET LAYOUT",font,
            hit(mx,my,10,height-37,resetW,25),false);
        DashTheme.button(g,width-doneW-10,height-37,doneW,25,"SAVE / DONE",font,
            hit(mx,my,width-doneW-10,height-37,doneW,25),true);
        if(width>380)
            g.centeredText(font,DashTheme.fit(font,dirty?"Release to save position":message,
                width-resetW-doneW-45),width/2,height-28,DashTheme.MUTED);
    }
}
