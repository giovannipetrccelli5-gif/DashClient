package com.dashclient;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Compact, touch-friendly presets and HUD options, separate from module search. */
public final class DashSettingsScreen extends Screen {
    private String status = "Choose a graphics profile or adjust your HUD.";
    private boolean confirmHudReset;
    private int x, y, w, h;

    public DashSettingsScreen() { super(Component.literal("DashClient Settings")); }

    private Module find(String name) {
        for (Module m : DashClient.MODULES) if (m.name().equals(name)) return m;
        return null;
    }

    private void profile(String name) {
        for (String profile : new String[]{"Performance Boost", "Extreme FPS", "Battery Saver"}) {
            Module m = find(profile);
            if (m != null && m.enabled()) m.setEnabled(false);
        }
        if (!name.equals("Vanilla")) {
            Module m = find(name);
            if (m != null) m.setEnabled(true);
        }
        Config.save();
        status = "Selected: " + name + " (settings saved)";
        refresh();
    }

    private void toggle(String name) {
        Module m = find(name);
        if (m == null) return;
        m.setEnabled(!m.enabled());
        Config.save();
        status = name + ": " + (m.enabled() ? "ON" : "OFF");
        refresh();
    }

    private String label(String name) {
        Module m = find(name);
        return (m != null && m.enabled() ? "[ON] " : "[OFF] ") + name;
    }

    private void refresh() {
        clearWidgets();
        int margin = 14;
        int inner = Math.max(100, w - margin*2);
        int gap = 6;
        int col = Math.max(46,(inner-gap)/2);
        addRenderableWidget(Button.builder(Component.literal("Back"), b ->
            minecraft.gui.setScreen(new DashScreen()))
            .bounds(x + margin, y + 9, Math.min(66,col), 22).build());
        addRenderableWidget(Button.builder(Component.literal("FILES"), b ->
            minecraft.gui.setScreen(new ClientImportsScreen()))
            .bounds(x+w-margin-Math.min(66,col), y+9, Math.min(66,col), 22).build());

        String[] presets={"Performance Boost","Extreme FPS","Battery Saver","Vanilla"};
        int start=y+62;
        for(int i=0;i<presets.length;i++){
            final String name=presets[i];
            addRenderableWidget(Button.builder(Component.literal(name), b -> profile(name))
                .bounds(x+margin+(i%2)*(col+gap),start+(i/2)*27,col,23).build());
        }
        String[] toggles={"Adaptive FPS","HUD Eco Refresh","HUD Minimal Mode",
                "HUD Gameplay Only","HUD Transparent","HUD Compact"};
        int toggleStart=start+72;
        for(int i=0;i<toggles.length;i++){
            final String name=toggles[i];
            addRenderableWidget(Button.builder(Component.literal(label(name)), b -> toggle(name))
                .bounds(x+margin+(i%2)*(col+gap),
                    toggleStart+(i/2)*26,col,22).build());
        }
        int foot=y+h-39;
        addRenderableWidget(Button.builder(Component.literal(confirmHudReset ?
                "Confirm reset?" : "Reset HUD positions"), b -> {
            if (!confirmHudReset) {
                confirmHudReset=true;
                status="Press reset again to clear your saved HUD positions.";
            } else {
                HudLayout.reset();
                confirmHudReset=false;
                status="HUD positions reset. Other settings are unchanged.";
            }
            refresh();
        }).bounds(x+margin,foot,inner,23).build());
    }

    @Override protected void init() {
        w=Math.min(width-10, 540);
        h=Math.min(height-10, 325);
        x=(width-w)/2;
        y=(height-h)/2;
        refresh();
    }

    @Override public void onClose() {
        minecraft.gui.setScreen(new DashScreen());
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g,
                                            int mx,int my,float tick) {
        g.fill(0,0,width,height,0xBB050A16);
        g.fill(x-1,y-1,x+w+1,y+h+1,0xFF4088B6);
        g.fill(x,y,x+w,y+h,0xF0142134);
        g.fill(x+2,y+2,x+w-2,y+33,0xFF18354F);
        super.extractRenderState(g,mx,my,tick);
        g.centeredText(font,"DASHCLIENT  /  SETTINGS",x+w/2,y+17,0xFF83E4FF);
        g.centeredText(font,"GRAPHICS PROFILES",x+w/2,y+48,0xFFA6CADB);
        g.centeredText(font,"HUD & PERFORMANCE",x+w/2,y+126,0xFFA6CADB);
        if (minecraft.options != null) {
            var o = minecraft.options;
            String details = "FPS " + minecraft.getFps()
                + "  |  Cap " + o.framerateLimit().get()
                + "  |  VSync " + (o.enableVsync().get() ? "ON" : "OFF")
                + "  |  View " + o.renderDistance().get();
            g.centeredText(font, details, x+w/2,y+h-70,0xFF91E9B8);
        }
        String line=status;
        while(!line.isEmpty() && font.width(line)>w-22)
            line=line.substring(0,line.length()-1);
        g.centeredText(font,line,x+w/2,y+h-51,0xFFF7DEAB);
    }
}
