package com.dashclient;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * DashClient 2.0, a fully new Fabric client implementation.
 * Not affiliated with or copied from Lunar Client.
 */
public final class DashClient implements ClientModInitializer {
    public static final Logger LOGGER=LoggerFactory.getLogger("DashClient");
    public static final List<Feature> FEATURES=new ArrayList<>();
    public static KeyMapping MENU_KEY, ZOOM_KEY, EDIT_KEY;
    private static boolean configLoaded;
    private static Integer originalFov;
    private static final long START=System.currentTimeMillis();

    private static InputConstants.Type keyboardType(){
        try{return InputConstants.Type.valueOf("KEYBOARD");}
        catch(IllegalArgumentException ignored){return InputConstants.Type.valueOf("KEYSYM");}
    }
    private static void add(String id,String title,String category,String description,boolean on){
        FEATURES.add(new Feature(id,title,category,description,on));
    }
    public static Feature feature(String id) {
        for(Feature f:FEATURES) if(f.id.equals(id))return f;
        return null;
    }
    public static boolean enabled(String id){
        Feature f=feature(id);
        return f!=null&&f.enabled;
    }
    public static void toggle(Feature f){
        f.enabled=!f.enabled;
        ClientConfig.save();
    }
    public static boolean isHud(Feature f) { return f.category.equals("HUD"); }
    public static String readout(Feature f,Minecraft mc) {
        if(mc.player==null)return "";
        return switch(f.id) {
            case "fps" -> "FPS  "+mc.getFps();
            case "coords" -> "XYZ  "+mc.player.blockPosition().getX()+"  "
                +mc.player.blockPosition().getY()+"  "+mc.player.blockPosition().getZ();
            case "direction" -> {
                int quarter=Math.floorMod(Math.round(mc.player.getYRot()/90),4);
                yield "Facing  "+new String[]{"South","West","North","East"}[quarter];
            }
            case "health" -> "Health  "+Math.round(mc.player.getHealth())+" HP";
            case "ping" -> {
                var connection=mc.getConnection();
                var info=connection==null?null:connection.getPlayerInfo(mc.player.getUUID());
                yield "Ping  "+(info==null?"--":info.getLatency()+" ms");
            }
            case "time" -> "World Time  "+(mc.level==null?0:mc.level.getDayTime()%24000);
            case "memory" -> {
                Runtime rt=Runtime.getRuntime();
                yield "RAM  "+((rt.totalMemory()-rt.freeMemory())/(1024*1024))+" MB";
            }
            case "clock" -> "Clock  "+java.time.LocalTime.now().toString().substring(0,5);
            case "speed" -> "Speed  "+String.format(java.util.Locale.ROOT,"%.1f",mc.player.getDeltaMovement().horizontalDistance()*20)+" b/s";
            case "session" -> "Playtime  "+((System.currentTimeMillis()-START)/60000)+"m";
            case "position" -> "Yaw  "+Math.round(mc.player.getYRot())+"  Pitch  "+Math.round(mc.player.getXRot());
            case "dimension" -> "Dimension  "+mc.player.level().dimension().location().getPath();
            default -> f.name;
        };
    }
    public static List<Feature> hudFeatures(){
        List<Feature> result=new ArrayList<>();
        for(Feature f:FEATURES) if(isHud(f)&&f.enabled) result.add(f);
        return result;
    }
    private static void setZoom(Minecraft mc){
        if(mc.options==null)return;
        boolean zoom=enabled("zoom")&&mc.gui.screen()==null&&ZOOM_KEY.isDown();
        if(zoom){
            if(originalFov==null)originalFov=mc.options.fov().get();
            mc.options.fov().set(30);
        }else if(originalFov!=null){
            mc.options.fov().set(originalFov);
            originalFov=null;
        }
    }
    @Override public void onInitializeClient() {
        // Readable modules only; legacy exploit/automation features are not bundled.
        add("fps","FPS","HUD","Live frames per second",true);
        add("coords","Coordinates","HUD","Your current block coordinates",true);
        add("ping","Ping","HUD","Connection latency",false);
        add("direction","Direction","HUD","Cardinal facing direction",false);
        add("health","Health","HUD","Current player health",false);
        add("speed","Movement speed","HUD","Approximate blocks per second",false);
        add("time","World time","HUD","Current Minecraft world time",false);
        add("memory","RAM usage","HUD","Minecraft Java heap usage",false);
        add("clock","Real clock","HUD","Local time",false);
        add("session","Playtime","HUD","Time since Minecraft opened",false);
        add("position","Rotation","HUD","Yaw and pitch readout",false);
        add("dimension","Dimension","HUD","Current dimension",false);
        add("zoom","Zoom","Visual","Hold C to zoom your view",true);
        add("background","HUD backgrounds","Visual","Show translucent HUD backgrounds",true);
        add("compact","Compact HUD","Visual","Use tighter HUD spacing",false);
        add("right","Right-side HUD","Visual","Move default HUD alignment to the right",false);
        KeyMapping.Category cat=KeyMapping.Category.register(Identifier.fromNamespaceAndPath("dashclient","controls"));
        MENU_KEY=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.dashclient.menu",
            keyboardType(),InputConstants.KEY_RSHIFT,cat));
        ZOOM_KEY=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.dashclient.zoom",
            keyboardType(),InputConstants.KEY_C,cat));
        EDIT_KEY=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.dashclient.hud",
            keyboardType(),InputConstants.KEY_J,cat));
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            if(!configLoaded&&mc.options!=null){
                ClientConfig.load();
                configLoaded=true;
            }
            while(MENU_KEY.consumeClick()) {
                if(mc.gui.screen() instanceof ClientMenu)mc.gui.setScreen(null);
                else if(mc.gui.screen()==null)mc.gui.setScreen(new ClientMenu());
            }
            while(EDIT_KEY.consumeClick()) {
                if(mc.gui.screen()==null)mc.gui.setScreen(new HudEditor());
            }
            setZoom(mc);
        });
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("dashclient","hud"),
            (g,delta)->drawHud(Minecraft.getInstance(),g));
        LOGGER.info("DashClient 2 initialized: {} original features",FEATURES.size());
    }
    private static void drawHud(Minecraft mc,GuiGraphicsExtractor g){
        if(mc.player==null||mc.gui.screen() instanceof HudEditor)return;
        int index=0, sw=mc.getWindow().getGuiScaledWidth(),sh=mc.getWindow().getGuiScaledHeight();
        for(Feature f:hudFeatures()){
            String line=readout(f,mc);
            if(line.isEmpty())continue;
            int tw=mc.font.width(line);
            int fallbackX=enabled("right")?sw-tw-12:12;
            int fallbackY=18+index*(enabled("compact")?11:17);
            int x=Math.min(sw-tw-2,ClientConfig.x(f,Math.max(2,fallbackX)));
            int y=Math.min(sh-13,ClientConfig.y(f,fallbackY));
            if(enabled("background"))g.fill(x-4,y-3,x+tw+5,y+12,0xA10C101B);
            g.text(mc.font,line,x,y,0xFFF1F5FF,true);
            index++;
        }
    }
}