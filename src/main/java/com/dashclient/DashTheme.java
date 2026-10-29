package com.dashclient;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;

/**
 * Original DashClient visual identity. Lightweight 2D drawing only:
 * no imported client assets, shaders, fonts, or third-party UI libraries.
 */
public final class DashTheme {
    public static final int BACKDROP=0xD9050811;
    public static final int FRAME=0xFF111829;
    public static final int SIDEBAR=0xFF101728;
    public static final int PANEL=0xFF171F31;
    public static final int CARD=0xFF1B263A;
    public static final int CARD_HOVER=0xFF24334B;
    public static final int LINE=0xFF33415B;
    public static final int ACCENT=0xFF7795FF;
    public static final int BRIGHT=0xFF9BE2F9;
    public static final int GREEN=0xFF77E0B6;
    public static final int TEXT=0xFFF2F6FF;
    public static final int MUTED=0xFF9EADC5;

    private DashTheme(){}

    /** Pixel-corner cards look rounded without needing custom OpenGL state. */
    public static void rounded(GuiGraphicsExtractor g,int x,int y,int w,int h,int color){
        if(w<4||h<4)return;
        g.fill(x+3,y,x+w-3,y+h,color);
        g.fill(x+1,y+1,x+w-1,y+h-1,color);
        g.fill(x,y+3,x+w,y+h-3,color);
    }
    public static void outline(GuiGraphicsExtractor g,int x,int y,int w,int h,int color){
        rounded(g,x,y,w,h,color);
        rounded(g,x+1,y+1,w-2,h-2,PANEL);
    }
    public static void line(GuiGraphicsExtractor g,int x,int y,int w,int color){
        if(w>0)g.fill(x,y,x+w,y+1,color);
    }
    public static void button(GuiGraphicsExtractor g,int x,int y,int w,int h,
                              String label,Font font,boolean hover,boolean selected){
        int color=selected?ACCENT:(hover?0xFF314561:0xFF202C40);
        rounded(g,x,y,w,h,color);
        int text=selected?0xFF0B1324:TEXT;
        String actual=fit(font,label,w-8);
        g.centeredText(font,actual,x+w/2,y+(h-8)/2,text);
    }
    public static void switcher(GuiGraphicsExtractor g,int x,int y,boolean enabled){
        switcher(g,x,y,enabled?1f:0f);
    }
    /** Toggle thumb interpolates across the track for a subtle native animation. */
    public static void switcher(GuiGraphicsExtractor g,int x,int y,float progress){
        float t=Math.max(0f,Math.min(1f,progress));
        rounded(g,x,y,37,18,mix(0xFF3A465B,0xFF477D83,t));
        rounded(g,x+2,y+2,33,14,mix(0xFF273244,0xFF295763,t));
        rounded(g,x+3+Math.round(18*t),y+3,13,12,
            mix(0xFF95A5BC,GREEN,t));
    }
    public static int mix(int left,int right,float t){
        t=Math.max(0f,Math.min(1f,t));
        int a=Math.round(((left>>>24)&255)*(1-t)+((right>>>24)&255)*t);
        int r=Math.round(((left>>>16)&255)*(1-t)+((right>>>16)&255)*t);
        int g=Math.round(((left>>>8)&255)*(1-t)+((right>>>8)&255)*t);
        int b=Math.round((left&255)*(1-t)+(right&255)*t);
        return (a<<24)|(r<<16)|(g<<8)|b;
    }
    public static String fit(Font font,String str,int pixels){
        if(str==null||pixels<=0)return "";
        if(font.width(str)<=pixels)return str;
        String suffix="..";
        while(!str.isEmpty()&&font.width(str+suffix)>pixels)
            str=str.substring(0,str.length()-1);
        return str.isEmpty()?"":str+suffix;
    }
    public static void paragraph(GuiGraphicsExtractor g,Font font,String value,
                                 int x,int y,int maxWidth,int maxLines,int color){
        if(value==null||maxLines<1)return;
        String line="";
        int lineIndex=0;
        for(String word:value.split(" ")){
            String trial=line.isEmpty()?word:line+" "+word;
            if(font.width(trial)>maxWidth&&!line.isEmpty()){
                g.text(font,fit(font,line,maxWidth),x,y+lineIndex*13,color,false);
                if(++lineIndex>=maxLines)return;
                line=word;
            }else line=trial;
        }
        if(!line.isEmpty()&&lineIndex<maxLines)
            g.text(font,fit(font,line,maxWidth),x,y+lineIndex*13,color,false);
    }
}
