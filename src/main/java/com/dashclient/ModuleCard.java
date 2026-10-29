package com.dashclient;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** A custom-drawn module component. Card selection and toggling are separate actions. */
public final class ModuleCard {
    public final Feature feature;
    public final int x,y,w,h;
    public ModuleCard(Feature feature,int x,int y,int w,int h){
        this.feature=feature;this.x=x;this.y=y;this.w=w;this.h=h;
    }
    public boolean contains(double mx,double my){
        return mx>=x&&mx<x+w&&my>=y&&my<y+h;
    }
    public boolean containsToggle(double mx,double my){
        return mx>=x+w-48&&mx<x+w-6&&my>=y+6&&my<y+29;
    }
    public void draw(GuiGraphicsExtractor g,Font font,int mx,int my,boolean selected){
        boolean hovered=contains(mx,my);
        DashTheme.rounded(g,x,y,w,h,selected?DashTheme.ACCENT:DashTheme.LINE);
        DashTheme.rounded(g,x+1,y+1,w-2,h-2,
            hovered?DashTheme.CARD_HOVER:DashTheme.CARD);
        if(selected)g.fill(x+2,y+6,x+4,y+h-6,DashTheme.BRIGHT);

        // The category badge gives every card a visually consistent icon.
        DashTheme.rounded(g,x+11,y+10,25,25,
            feature.enabled?0xFF344E72:0xFF29364B);
        String initial=feature.category.substring(0,1).toUpperCase();
        g.centeredText(font,initial,x+23,y+18,
            feature.enabled?DashTheme.BRIGHT:DashTheme.MUTED);

        String label=DashTheme.fit(font,feature.name,Math.max(26,w-104));
        g.text(font,label,x+43,y+10,DashTheme.TEXT,true);
        String description=DashTheme.fit(font,feature.description,Math.max(24,w-55));
        g.text(font,description,x+43,y+25,DashTheme.MUTED,false);

        DashTheme.switcher(g,x+w-46,y+10,feature.enabled);
        if(h>=58){
            String status=feature.enabled?"ENABLED":"DISABLED";
            g.text(font,status,x+12,y+h-15,
                feature.enabled?DashTheme.GREEN:DashTheme.MUTED,false);
            g.text(font,selected?"SELECTED":"DETAILS  >",x+w-70,y+h-15,
                selected?DashTheme.BRIGHT:DashTheme.MUTED,false);
        }
    }
}
