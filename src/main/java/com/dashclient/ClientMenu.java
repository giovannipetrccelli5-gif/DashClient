package com.dashclient;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * DashClient v3 click GUI, drawn independently using Minecraft primitives.
 * This deliberately replaces the old wall of vanilla menu buttons.
 */
public final class ClientMenu extends Screen {
    private static final String[] CATEGORIES={
        "All","HUD","Combat","Survival","World","Movement","Performance","Visual"
    };
    private static final String[] ACTIONS={"HUD","WAYPOINTS","PROFILES","VIDEO"};
    private int x,y,w,h,sidebar,detailsWidth,bodyX,bodyWidth,bodyY,footerY;
    private int columnCount,cardHeight,rows,page,pages=1,tab=0;
    private String query="",status="Select a module to see its details.";
    private Feature selected;
    private EditBox search;
    private final List<Feature> matches=new ArrayList<>();
    private final List<ModuleCard> shownCards=new ArrayList<>();

    public ClientMenu(){super(Component.literal("DashClient v3"));}
    @Override protected void init(){
        w=Math.min(width-10,1060);
        h=Math.min(height-10,620);
        x=(width-w)/2;
        y=(height-h)/2;
        sidebar=w<515?84:136;
        detailsWidth=w>=765?217:0;
        bodyX=x+sidebar+11;
        bodyWidth=w-sidebar-25-(detailsWidth>0?detailsWidth+10:0);
        bodyY=y+79;
        footerY=y+h-33;
        columnCount=bodyWidth>=385?2:1;
        cardHeight=h<335?53:65;
        rows=Math.max(1,(footerY-bodyY-4)/(cardHeight+8));
        if(selected==null&&!DashClient.FEATURES.isEmpty())
            selected=DashClient.FEATURES.get(0);

        search=new EditBox(font,bodyX+10,y+46,Math.max(45,bodyWidth-20),19,
            Component.literal("Search client mods"));
        search.setValue(query);
        search.setHint(Component.literal("Search features..."));
        search.setResponder(text->{query=text;page=0;rebuildCards();});
        addRenderableWidget(search);
        rebuildCards();
    }
    private int navStep(){
        return Math.max(15,Math.min(33,(h-112)/CATEGORIES.length));
    }
    private int actionWidth(){
        return Math.max(22,Math.min(77,(bodyWidth-78)/4));
    }
    private int actionStart(){
        return bodyX+bodyWidth-4*actionWidth()-3*4;
    }
    private int detailsX(){return bodyX+bodyWidth+10;}
    private int detailsY(){return y+43;}
    private int detailsHeight(){return h-55;}

    private void rebuildCards(){
        matches.clear();
        shownCards.clear();
        String phrase=query.trim().toLowerCase(Locale.ROOT);
        for(Feature f:DashClient.FEATURES){
            if(tab>0&&!f.category.equals(CATEGORIES[tab]))continue;
            if(!phrase.isEmpty()
                &&!f.name.toLowerCase(Locale.ROOT).contains(phrase)
                &&!f.description.toLowerCase(Locale.ROOT).contains(phrase)
                &&!f.category.toLowerCase(Locale.ROOT).contains(phrase))continue;
            matches.add(f);
        }
        int perPage=Math.max(1,columnCount*rows);
        pages=Math.max(1,(matches.size()+perPage-1)/perPage);
        page=Math.min(page,pages-1);
        int gap=8,cardW=(bodyWidth-gap*(columnCount-1))/columnCount;
        for(int i=page*perPage;i<Math.min(matches.size(),(page+1)*perPage);i++){
            int local=i-page*perPage;
            int bx=bodyX+(local%columnCount)*(cardW+gap);
            int by=bodyY+(local/columnCount)*(cardHeight+gap);
            shownCards.add(new ModuleCard(matches.get(i),bx,by,cardW,cardHeight));
        }
    }
    private static boolean hit(double mx,double my,int bx,int by,int bw,int bh){
        return mx>=bx&&mx<bx+bw&&my>=by&&my<by+bh;
    }
    private void openAction(int action){
        switch(action){
            case 0 -> minecraft.gui.setScreen(new HudEditor());
            case 1 -> minecraft.gui.setScreen(new WaypointsScreen());
            case 2 -> minecraft.gui.setScreen(new ProfileMenu());
            case 3 -> minecraft.gui.setScreen(new VideoPresets());
            default -> {}
        }
    }
    private void setTab(int next){
        if(next<0||next>=CATEGORIES.length)return;
        tab=next;page=0;rebuildCards();
        status="Viewing "+CATEGORIES[tab]+" modules";
    }
    private void toggle(Feature feature){
        DashClient.toggle(feature);
        selected=feature;
        status=feature.name+(feature.enabled?" enabled":" disabled")+". Saved to "+ClientConfig.active+".";
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubleClick){
        if(super.mouseClicked(e,doubleClick))return true;
        double mx=e.x(),my=e.y();
        if(e.button()!=0 && e.button()!=1)return false;

        int step=navStep();
        for(int i=0;i<CATEGORIES.length;i++){
            if(hit(mx,my,x+10,y+76+i*step,sidebar-19,Math.max(14,step-2))){
                setTab(i);return true;
            }
        }
        if(e.button()==0){
            int aw=actionWidth(),start=actionStart();
            for(int i=0;i<ACTIONS.length;i++){
                if(hit(mx,my,start+i*(aw+4),footerY,aw,23)){
                    openAction(i);return true;
                }
            }
            if(hit(mx,my,bodyX,footerY,20,23)){
                if(page>0){page--;rebuildCards();}
                return true;
            }
            if(hit(mx,my,bodyX+39,footerY,20,23)){
                if(page+1<pages){page++;rebuildCards();}
                return true;
            }
            if(detailsWidth>0&&selected!=null){
                int dx=detailsX(),dy=detailsY(),dh=detailsHeight();
                if(SettingsPanel.toggleHit(mx,my,dx,dy,detailsWidth,dh)){
                    toggle(selected);return true;
                }
                if(SettingsPanel.actionHit(mx,my,dx,dy,detailsWidth,dh)){
                    if(DashClient.isHud(selected))openAction(0);
                    else openAction(3);
                    return true;
                }
            }
        }
        for(ModuleCard card:shownCards){
            if(!card.contains(mx,my))continue;
            selected=card.feature;
            if(e.button()==0&&card.containsToggle(mx,my))
                toggle(card.feature);
            else status=card.feature.description;
            return true;
        }
        return false;
    }
    @Override public void onClose(){minecraft.gui.setScreen(null);}

    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float frame){
        rebuildCards();
        g.fill(0,0,width,height,DashTheme.BACKDROP);

        // Frame: shadow, panel, top stripe, subtle layered sidebar.
        DashTheme.rounded(g,x-4,y-2,w+8,h+8,0x67000000);
        DashTheme.rounded(g,x,y,w,h,DashTheme.FRAME);
        DashTheme.rounded(g,x+3,y+3,sidebar-3,h-6,DashTheme.SIDEBAR);
        g.fill(x+sidebar,y+37,x+sidebar+1,y+h-9,DashTheme.LINE);
        DashTheme.line(g,x+sidebar+1,y+36,w-sidebar-6,DashTheme.LINE);
        DashTheme.rounded(g,x+8,y+8,23,23,DashTheme.ACCENT);
        g.fill(x+8,y+35,x+w-8,y+37,0xFF354869);

        // The search is the only native widget, preserving text editing, focus
        // handling and keyboard accessibility. Everything else is custom.
        DashTheme.rounded(g,bodyX,y+42,bodyWidth,27,0xFF202C40);
        super.extractRenderState(g,mx,my,frame);

        g.centeredText(font,"D",x+19,y+16,0xFF0C1730);
        g.text(font,sidebar<100?"DASH":"DASHCLIENT",x+38,y+15,DashTheme.TEXT,true);
        if(sidebar>=100)
            g.text(font,"CLIENT INTERFACE",x+13,y+58,DashTheme.MUTED,false);
        else g.text(font,"MODS",x+13,y+58,DashTheme.MUTED,false);

        String headline="YOUR MODS";
        g.text(font,headline,bodyX,y+16,DashTheme.TEXT,true);
        String stat=matches.size()+" / "+DashClient.FEATURES.size()+" MODULES";
        g.text(font,DashTheme.fit(font,stat,Math.max(40,bodyWidth-100)),
            bodyX+Math.min(98,bodyWidth/3),y+16,DashTheme.BRIGHT,false);

        int step=navStep();
        for(int i=0;i<CATEGORIES.length;i++){
            int nx=x+10,ny=y+76+i*step,nw=sidebar-19,nh=Math.max(14,step-2);
            boolean active=i==tab,hover=hit(mx,my,nx,ny,nw,nh);
            if(active||hover)
                DashTheme.rounded(g,nx,ny,nw,nh,active?0xFF324971:0xFF202D43);
            if(active)g.fill(nx+1,ny+3,nx+3,ny+nh-3,DashTheme.BRIGHT);
            String label=sidebar<100?shortCategory(i):CATEGORIES[i];
            g.text(font,DashTheme.fit(font,label,nw-11),nx+7,ny+(nh-8)/2,
                active?DashTheme.TEXT:DashTheme.MUTED,active);
        }

        if(shownCards.isEmpty()){
            DashTheme.rounded(g,bodyX,bodyY,bodyWidth,56,DashTheme.PANEL);
            g.centeredText(font,"No matching modules",bodyX+bodyWidth/2,bodyY+12,DashTheme.TEXT);
            g.centeredText(font,"Try another search or category",bodyX+bodyWidth/2,
                bodyY+28,DashTheme.MUTED);
        }else{
            for(ModuleCard card:shownCards)
                card.draw(g,font,mx,my,card.feature==selected);
        }

        // Functional page navigation and the four shortcut destinations.
        DashTheme.button(g,bodyX,footerY,20,23,"<",font,
            hit(mx,my,bodyX,footerY,20,23),false);
        g.centeredText(font,(page+1)+"/"+pages,bodyX+30,footerY+8,DashTheme.MUTED);
        DashTheme.button(g,bodyX+39,footerY,20,23,">",font,
            hit(mx,my,bodyX+39,footerY,20,23),false);
        int aw=actionWidth(),start=actionStart();
        for(int i=0;i<ACTIONS.length;i++){
            int ax=start+i*(aw+4);
            DashTheme.button(g,ax,footerY,aw,23,
                DashTheme.fit(font,ACTIONS[i],aw-6),font,
                hit(mx,my,ax,footerY,aw,23),false);
        }

        if(detailsWidth>0)
            SettingsPanel.draw(g,font,selected,detailsX(),detailsY(),detailsWidth,
                detailsHeight(),mx,my);

        // Mini status bar at the bottom, visible in wide mode only.
        if(w>=760){
            String state=ClientConfig.active.toUpperCase(Locale.ROOT)+" PROFILE  /  "
                +DashTheme.fit(font,status,190);
            g.text(font,state,x+sidebar+12,y+h-44,DashTheme.MUTED,false);
        }
    }
    private static String shortCategory(int index){
        return switch(index){
            case 5 -> "Move";
            case 6 -> "Perf";
            case 7 -> "Visual";
            default -> CATEGORIES[index];
        };
    }
}
