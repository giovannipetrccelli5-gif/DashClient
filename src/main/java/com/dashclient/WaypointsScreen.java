package com.dashclient;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.List;

/** Locally saved waypoint manager, built in the DashClient v3 visual language. */
public final class WaypointsScreen extends Screen {
    private int x,y,w,h,page;
    private String status="Your waypoint coordinates are stored locally.";
    private EditBox nameBox;
    public WaypointsScreen(){super(Component.literal("DashClient Waypoints"));}

    @Override protected void init(){
        w=Math.min(width-12,650);h=Math.min(height-12,400);
        x=(width-w)/2;y=(height-h)/2;
        nameBox=new EditBox(font,x+18,y+50,Math.max(32,w-123),20,
            Component.literal("New waypoint name"));
        nameBox.setHint(Component.literal("Enter waypoint name..."));
        addRenderableWidget(nameBox);
    }
    private int rowCount(){return Math.max(1,(h-151)/32);}
    private int pageCount(){
        return Math.max(1,(Waypoints.list().size()+rowCount()-1)/rowCount());
    }
    private static boolean hit(double mx,double my,int bx,int by,int bw,int bh){
        return mx>=bx&&mx<bx+bw&&my>=by&&my<by+bh;
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubleClick){
        if(super.mouseClicked(e,doubleClick))return true;
        if(e.button()!=0)return false;
        double mx=e.x(),my=e.y();
        if(hit(mx,my,x+w-97,y+48,84,25)){
            if(Waypoints.add(minecraft,nameBox.getValue())){
                nameBox.setValue("");
                page=Math.max(0,pageCount()-1);
                status="Waypoint saved in your current dimension.";
            }else status="Join a world or delete some of the 24 saved waypoints.";
            return true;
        }
        List<Waypoints.Point> points=Waypoints.list();
        int perPage=rowCount();
        page=Math.min(page,pageCount()-1);
        for(int i=page*perPage;i<Math.min(points.size(),(page+1)*perPage);i++){
            int ry=y+85+(i-page*perPage)*32;
            if(hit(mx,my,x+w-88,ry,74,26)){
                Waypoints.remove(i);
                page=Math.min(page,pageCount()-1);
                status="Waypoint removed.";
                return true;
            }
            if(hit(mx,my,x+14,ry,w-110,26)){
                var point=points.get(i);
                status=point.name()+" - "+point.dimension();
                return true;
            }
        }
        int fy=y+h-67;
        if(hit(mx,my,x+14,fy,46,23)){
            page=Math.max(0,page-1);return true;
        }
        if(hit(mx,my,x+66,fy,46,23)){
            page=Math.min(pageCount()-1,page+1);return true;
        }
        if(hit(mx,my,x+14,y+h-36,w-28,25)){
            minecraft.gui.setScreen(new ClientMenu());return true;
        }
        return false;
    }
    @Override public void onClose(){minecraft.gui.setScreen(new ClientMenu());}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float tick){
        g.fill(0,0,width,height,DashTheme.BACKDROP);
        DashTheme.rounded(g,x,y,w,h,DashTheme.FRAME);
        DashTheme.rounded(g,x+4,y+4,w-8,35,DashTheme.PANEL);
        DashTheme.line(g,x+10,y+40,w-20,DashTheme.ACCENT);
        DashTheme.rounded(g,x+13,y+46,w-26,29,0xFF202C40);
        super.extractRenderState(g,mx,my,tick);
        g.text(font,"WAYPOINTS",x+16,y+16,DashTheme.TEXT,true);
        int total=Waypoints.list().size();
        g.text(font,total+" / 24 SAVED",x+w-100,y+17,DashTheme.BRIGHT,false);
        DashTheme.button(g,x+w-97,y+48,84,25,"ADD HERE",font,
            hit(mx,my,x+w-97,y+48,84,25),true);
        int perPage=rowCount();
        page=Math.min(page,pageCount()-1);
        List<Waypoints.Point> all=Waypoints.list();
        for(int i=page*perPage;i<Math.min(total,(page+1)*perPage);i++){
            Waypoints.Point p=all.get(i);
            int ry=y+85+(i-page*perPage)*32;
            DashTheme.rounded(g,x+14,ry,w-28,26,DashTheme.CARD);
            String coords=p.x()+", "+p.y()+", "+p.z();
            String label=DashTheme.fit(font,p.name(),Math.max(30,w-210));
            g.text(font,label,x+25,ry+9,DashTheme.TEXT,true);
            if(w>=340)
                g.text(font,DashTheme.fit(font,coords,125),x+w-205,ry+9,
                    DashTheme.MUTED,false);
            DashTheme.button(g,x+w-88,ry,74,26,"REMOVE",font,
                hit(mx,my,x+w-88,ry,74,26),false);
        }
        if(total==0)
            g.centeredText(font,"No waypoints yet. Add one at your current location.",
                x+w/2,y+101,DashTheme.MUTED);
        int fy=y+h-67;
        DashTheme.button(g,x+14,fy,46,23,"< PREV",font,
            hit(mx,my,x+14,fy,46,23),false);
        DashTheme.button(g,x+66,fy,46,23,"NEXT >",font,
            hit(mx,my,x+66,fy,46,23),false);
        g.text(font,(page+1)+" / "+pageCount(),x+122,fy+8,DashTheme.MUTED,false);
        String line=DashTheme.fit(font,status,w-28);
        g.text(font,line,x+14,y+h-84,DashTheme.MUTED,false);
        DashTheme.button(g,x+14,y+h-36,w-28,25,"BACK TO MODS",font,
            hit(mx,my,x+14,y+h-36,w-28,25),false);
    }
}
