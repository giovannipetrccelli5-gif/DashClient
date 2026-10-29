package com.dashclient;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Contextual settings for the selected working feature. */
public final class SettingsPanel {
    private SettingsPanel(){}
    public static void draw(GuiGraphicsExtractor g,Font font,Feature feature,
                            int x,int y,int w,int h,int mx,int my){
        DashTheme.rounded(g,x,y,w,h,DashTheme.PANEL);
        g.fill(x+12,y+39,x+w-12,y+40,DashTheme.LINE);
        g.text(font,"MODULE DETAILS",x+13,y+14,DashTheme.BRIGHT,true);
        if(feature==null){
            DashTheme.paragraph(g,font,"Select a module to configure it.",
                x+13,y+58,w-26,3,DashTheme.MUTED);
            return;
        }
        g.text(font,DashTheme.fit(font,feature.name,w-27),
            x+13,y+51,DashTheme.TEXT,true);
        g.text(font,feature.category.toUpperCase(),x+13,y+68,DashTheme.MUTED,false);
        DashTheme.paragraph(g,font,feature.description,x+13,y+92,w-26,3,DashTheme.MUTED);

        int switchY=y+Math.min(146,Math.max(120,h-94));
        DashTheme.line(g,x+13,switchY-14,w-26,DashTheme.LINE);
        g.text(font,"Enabled",x+13,switchY+5,DashTheme.TEXT,false);
        DashTheme.switcher(g,x+w-55,switchY,feature.enabled);

        if(h>=238){
            String tip=feature.category.equals("Visual")
                ?"Changes apply immediately."
                :"Enable then position it in the HUD Editor.";
            DashTheme.paragraph(g,font,tip,x+13,switchY+29,w-26,2,DashTheme.MUTED);
        }
        int by=y+h-38;
        boolean hover=contains(mx,my,x+12,by,w-24,26);
        DashTheme.button(g,x+12,by,w-24,26,
            DashClient.isHud(feature)?"OPEN HUD EDITOR":"OPEN VIDEO SETTINGS",
            font,hover,false);
    }
    public static boolean toggleHit(double mx,double my,int x,int y,int w,int h){
        int sy=y+Math.min(146,Math.max(120,h-94));
        return contains(mx,my,x+w-58,sy-4,44,27);
    }
    public static boolean actionHit(double mx,double my,int x,int y,int w,int h){
        return contains(mx,my,x+12,y+h-38,w-24,26);
    }
    private static boolean contains(double mx,double my,int x,int y,int w,int h){
        return mx>=x&&mx<x+w&&my>=y&&my<y+h;
    }
}
