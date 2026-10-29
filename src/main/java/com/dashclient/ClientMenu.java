package com.dashclient;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** New DashClient interface. Drawn from scratch; no Lunar Client code or assets. */
public final class ClientMenu extends Screen {
    private static final String[] CATEGORIES={"All","HUD","Combat","Survival","World","Movement","Performance","Visual"};
    private int tab=0, page=0, pages=1;
    private String query="", status="Change mods, then drag HUD elements into place.";
    private int x,y,w,h,contentX,contentW,top;
    private EditBox search;
    private boolean refreshPending;
    private final List<Button> cards=new ArrayList<>();
    private Button prev,next;
    private final List<Feature> displayed=new ArrayList<>();

    public ClientMenu(){super(Component.literal("DashClient"));}
    @Override protected void init(){
        w=Math.min(width-12,760); h=Math.min(height-12,440);
        x=(width-w)/2;y=(height-h)/2;
        int sidebar=w<380?74:120;
        contentX=x+sidebar+10;
        contentW=w-sidebar-21;
        top=y+76;

        int tabGap=Math.max(18,Math.min(32,(h-145)/CATEGORIES.length));
        for(int i=0;i<CATEGORIES.length;i++){
            final int idx=i;
            addRenderableWidget(Button.builder(Component.literal(
                (i==tab?"[ ":"  ")+CATEGORIES[i]+(i==tab?" ]":"")),b->{
                tab=idx;page=0;refreshCards();
            }).bounds(x+8,y+72+i*tabGap,sidebar-10,Math.max(16,tabGap-3)).build());
        }
        addRenderableWidget(Button.builder(Component.literal("WAYPOINTS"),b->
            minecraft.gui.setScreen(new WaypointsScreen())
        ).bounds(x+8,y+h-62,sidebar-10,23).build());
        int actionW=Math.max(37,Math.min(82,(contentW-12)/3));
        addRenderableWidget(Button.builder(Component.literal("HUD EDIT"),b->
            minecraft.gui.setScreen(new HudEditor())
        ).bounds(x+w-8-actionW,y+7,actionW,23).build());
        addRenderableWidget(Button.builder(Component.literal("VIDEO"),b->
            minecraft.gui.setScreen(new VideoPresets())
        ).bounds(x+w-12-2*actionW,y+7,actionW,23).build());
        addRenderableWidget(Button.builder(Component.literal("PROFILES"),b->
            minecraft.gui.setScreen(new ProfileMenu())
        ).bounds(x+w-16-3*actionW,y+7,actionW,23).build());
        search=new EditBox(font,contentX,y+42,contentW,22,Component.literal("Module search"));
        search.setValue(query);
        search.setHint(Component.literal("Search mods..."));
        search.setResponder(s->{
            if(!query.equals(s)){query=s;page=0;refreshPending=true;}
        });
        addRenderableWidget(search);
        int foot=y+h-31;
        int fW=Math.max(36,(contentW-8)/3);
        prev=addRenderableWidget(Button.builder(Component.literal("< Prev"),b->{
            if(page>0){page--;refreshCards();}
        }).bounds(contentX,foot,fW,23).build());
        next=addRenderableWidget(Button.builder(Component.literal("Next >"),b->{
            if(page<pages-1){page++;refreshCards();}
        }).bounds(contentX+fW+4,foot,fW,23).build());
        addRenderableWidget(Button.builder(Component.literal("Done"),b->
            minecraft.gui.setScreen(null)
        ).bounds(contentX+2*(fW+4),foot,fW,23).build());
        refreshCards();
    }
    private void refreshCards(){
        for(Button b:cards)removeWidget(b);
        cards.clear();
        displayed.clear();
        for(Feature f:DashClient.FEATURES){
            if(tab>0&&!f.category.equals(CATEGORIES[tab]))continue;
            if(!f.name.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))
                && !f.description.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))continue;
            displayed.add(f);
        }
        int columns=contentW>290?2:1;
        int countRows=Math.max(1,(y+h-76-top)/45);
        int perPage=columns*countRows;
        pages=Math.max(1,(displayed.size()+perPage-1)/perPage);
        page=Math.min(page,pages-1);
        int bw=(contentW-7*(columns-1))/columns;
        for(int i=page*perPage;i<Math.min(displayed.size(),(page+1)*perPage);i++){
            Feature f=displayed.get(i);
            int local=i-page*perPage;
            int bx=contentX+(local%columns)*(bw+7);
            int by=top+(local/columns)*45;
            String title=f.name;
            String label=title+(f.enabled?"  [ON]":"  [OFF]");
            while(!title.isEmpty()&&font.width(label)>bw-15){
                title=title.substring(0,title.length()-1);
                label=title+(f.enabled?"  [ON]":"  [OFF]");
            }
            cards.add(addRenderableWidget(Button.builder(Component.literal(label),b->{
                DashClient.toggle(f);
                status=f.name+": "+(f.enabled?"enabled":"disabled")+" - "+f.description;
                refreshCards();
            }).bounds(bx,by,bw,37).build()));
        }
        if(prev!=null)prev.active=page>0;
        if(next!=null)next.active=page+1<pages;
    }
    @Override public void tick(){
        super.tick();
        if(refreshPending){refreshPending=false;refreshCards();}
    }
    @Override public void onClose(){minecraft.gui.setScreen(null);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float tick){
        g.fill(0,0,width,height,0xC5080A10);
        g.fill(x-2,y-2,x+w+2,y+h+2,0xFF6479A4);
        g.fill(x,y,x+w,y+h,0xF0131721);
        g.fill(x+3,y+3,x+w-3,y+35,0xFF1B2436);
        g.fill(x+3,y+35,x+w-3,y+37,0xFF698EF0);
        g.fill(x+5,y+40,contentX-7,y+h-8,0xFF1A2231);
        super.extractRenderState(g,mx,my,tick);
        g.text(font,w<370?"DC":"DASHCLIENT",x+12,y+15,0xFFC0CFFF,true);
        g.text(font,"MODS",contentX,y+15,0xFFE8EDFF,true);
        if(w>550)g.text(font,ClientConfig.active.toUpperCase(Locale.ROOT)+" PROFILE",
            contentX+75,y+15,0xFF9BACCD,true);
        String hint=status+"  •  Page "+(page+1)+"/"+pages;
        while(!hint.isEmpty()&&font.width(hint)>contentW-2)hint=hint.substring(0,hint.length()-1);
        g.centeredText(font,hint,contentX+contentW/2,y+h-48,0xFFB2BED4);
    }
}