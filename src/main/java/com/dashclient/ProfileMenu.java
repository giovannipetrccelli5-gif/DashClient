package com.dashclient;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** DashClient profiles screen, styled to match the new click GUI. */
public final class ProfileMenu extends Screen {
    private int x,y,w,h;
    private String note="Each profile remembers its own enabled modules.";
    public ProfileMenu(){super(Component.literal("DashClient Profiles"));}

    @Override protected void init(){
        w=Math.min(width-12,580);h=Math.min(height-12,340);
        x=(width-w)/2;y=(height-h)/2;
    }
    private static boolean hit(double mx,double my,int bx,int by,int bw,int bh){
        return mx>=bx&&mx<bx+bw&&my>=by&&my<by+bh;
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubleClick){
        if(e.button()!=0)return super.mouseClicked(e,doubleClick);
        double mx=e.x(),my=e.y();
        int gap=9,cardW=(w-35)/2,cardH=Math.min(46,Math.max(29,(h-171)/2));
        for(int i=0;i<ClientConfig.PROFILES.length;i++){
            int bx=x+13+(i%2)*(cardW+gap),by=y+65+(i/2)*(cardH+7);
            if(hit(mx,my,bx,by,cardW,cardH)){
                ClientConfig.switchProfile(ClientConfig.PROFILES[i]);
                note="Switched to "+ClientConfig.active+". Changes save automatically.";
                return true;
            }
        }
        int actionsY=y+h-71,half=(w-35)/2;
        if(hit(mx,my,x+13,actionsY,half,25)){
            note=ServerProfiles.bindCurrent(minecraft)
                ?"Current server bound to "+ClientConfig.active+"."
                :"Join a multiplayer server to bind a profile.";
            return true;
        }
        if(hit(mx,my,x+22+half,actionsY,half,25)){
            note=ServerProfiles.unbindCurrent(minecraft)
                ?"Server profile binding removed.":"No server binding to remove.";
            return true;
        }
        if(hit(mx,my,x+13,y+h-37,w-26,25)){
            minecraft.gui.setScreen(new ClientMenu());return true;
        }
        return super.mouseClicked(e,doubleClick);
    }
    @Override public void onClose(){minecraft.gui.setScreen(new ClientMenu());}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        g.fill(0,0,width,height,DashTheme.BACKDROP);
        DashTheme.rounded(g,x,y,w,h,DashTheme.FRAME);
        DashTheme.rounded(g,x+4,y+4,w-8,35,DashTheme.PANEL);
        DashTheme.line(g,x+10,y+40,w-20,DashTheme.ACCENT);
        super.extractRenderState(g,mx,my,delta);
        g.text(font,"PROFILES",x+16,y+16,DashTheme.TEXT,true);
        g.text(font,DashTheme.fit(font,note,w-28),x+14,y+47,DashTheme.MUTED,false);
        int gap=9,cardW=(w-35)/2,cardH=Math.min(46,Math.max(29,(h-171)/2));
        for(int i=0;i<ClientConfig.PROFILES.length;i++){
            String profile=ClientConfig.PROFILES[i];
            boolean active=ClientConfig.active.equals(profile);
            int bx=x+13+(i%2)*(cardW+gap),by=y+65+(i/2)*(cardH+7);
            boolean hover=hit(mx,my,bx,by,cardW,cardH);
            DashTheme.rounded(g,bx,by,cardW,cardH,active?DashTheme.ACCENT:DashTheme.LINE);
            DashTheme.rounded(g,bx+1,by+1,cardW-2,cardH-2,
                hover?DashTheme.CARD_HOVER:DashTheme.CARD);
            if(active)g.fill(bx+3,by+4,bx+5,by+cardH-4,DashTheme.GREEN);
            g.text(font,DashTheme.fit(font,profile,cardW-24),bx+13,by+8,
                DashTheme.TEXT,true);
            if(cardH>35)
                g.text(font,active?"ACTIVE":"SELECT",bx+13,by+23,
                    active?DashTheme.GREEN:DashTheme.MUTED,false);
        }
        int actionsY=y+h-71,half=(w-35)/2;
        DashTheme.button(g,x+13,actionsY,half,25,"BIND TO SERVER",font,
            hit(mx,my,x+13,actionsY,half,25),false);
        DashTheme.button(g,x+22+half,actionsY,half,25,"UNBIND",font,
            hit(mx,my,x+22+half,actionsY,half,25),false);
        DashTheme.button(g,x+13,y+h-37,w-26,25,"BACK TO MODS",font,
            hit(mx,my,x+13,y+h-37,w-26,25),false);
        String binding=ServerProfiles.assigned(minecraft);
        String label="SERVER PROFILE: "+binding;
        if(h>285)
            g.text(font,DashTheme.fit(font,label,w-30),x+13,actionsY-17,DashTheme.MUTED,false);
    }
}
