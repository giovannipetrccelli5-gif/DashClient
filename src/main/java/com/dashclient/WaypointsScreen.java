package com.dashclient;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

/** Add, browse and delete local waypoints with a GUI, including on touch devices. */
public final class WaypointsScreen extends Screen {
    private int x,y,w,h,page;
    private String message="Save locations to see your closest waypoint on the HUD.";
    private EditBox nameBox;
    public WaypointsScreen(){super(Component.literal("DashClient Waypoints"));}
    @Override protected void init(){
        w=Math.min(width-12,540);h=Math.min(height-12,390);
        x=(width-w)/2;y=(height-h)/2;
        int pad=12;
        nameBox=new EditBox(font,x+pad,y+48,w-pad*2-94,23,Component.literal("Waypoint name"));
        nameBox.setHint(Component.literal("New waypoint name..."));
        addRenderableWidget(nameBox);
        addRenderableWidget(Button.builder(Component.literal("ADD HERE"),b->{
            if(minecraft.player==null){message="Join a world to create a waypoint.";return;}
            String name=nameBox.getValue();
            if(Waypoints.add(minecraft,name)){
                message="Waypoint saved in current dimension.";
                minecraft.gui.setScreen(new WaypointsScreen());
            }else message="Maximum 24 waypoints, or world not loaded.";
        }).bounds(x+w-88,y+48,76,23).build());

        List<Waypoints.Point> all=Waypoints.list();
        int maxRows=Math.max(1,(h-150)/29);
        int pages=Math.max(1,(all.size()+maxRows-1)/maxRows);
        page=Math.min(page,pages-1);
        for(int i=page*maxRows;i<Math.min(all.size(),(page+1)*maxRows);i++){
            final int idx=i;
            var p=all.get(i);
            String name=p.name()+"  "+p.x()+", "+p.y()+", "+p.z();
            while(!name.isEmpty()&&font.width(name)>w-130)name=name.substring(0,name.length()-1);
            addRenderableWidget(Button.builder(Component.literal(name),b->{
                message="Saved location: "+p.dimension();
            }).bounds(x+pad,y+86+(i-page*maxRows)*29,w-114,24).build());
            addRenderableWidget(Button.builder(Component.literal("DELETE"),b->{
                Waypoints.remove(idx);
                minecraft.gui.setScreen(new WaypointsScreen());
            }).bounds(x+w-96,y+86+(i-page*maxRows)*29,84,24).build());
        }
        addRenderableWidget(Button.builder(Component.literal("<"),b->{
            if(page>0){page--;minecraft.gui.setScreen(this);}
        }).bounds(x+12,y+h-65,45,23).build());
        addRenderableWidget(Button.builder(Component.literal(">"),b->{
            if(page+1<pages){page++;minecraft.gui.setScreen(this);}
        }).bounds(x+63,y+h-65,45,23).build());
        addRenderableWidget(Button.builder(Component.literal("Back to mods"),b->
            minecraft.gui.setScreen(new ClientMenu())
        ).bounds(x+12,y+h-35,w-24,24).build());
    }
    @Override public void onClose(){minecraft.gui.setScreen(new ClientMenu());}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float tick){
        g.fill(0,0,width,height,0xC4080C15);
        g.fill(x-1,y-1,x+w+1,y+h+1,0xFF748FDA);
        g.fill(x,y,x+w,y+h,0xF0151D2C);
        g.fill(x+2,y+2,x+w-2,y+34,0xFF253653);
        super.extractRenderState(g,mx,my,tick);
        g.centeredText(font,"DASHCLIENT / WAYPOINTS",x+w/2,y+15,0xFFBFD0FF);
        String line=message;
        while(!line.isEmpty()&&font.width(line)>w-25)line=line.substring(0,line.length()-1);
        g.centeredText(font,line,x+w/2,y+h-78,0xFFA7B6D4);
    }
}
