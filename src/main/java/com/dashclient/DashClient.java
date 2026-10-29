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
    /** HUD-capable categories; visual options are switches, not readouts. */
    public static boolean isHud(Feature f) {
        return !f.category.equals("Visual");
    }
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
            case "time" -> "World Age  "+(mc.level==null?0:mc.level.getLevelData().getGameTime()%24000);
            case "memory" -> {
                Runtime rt=Runtime.getRuntime();
                yield "RAM  "+((rt.totalMemory()-rt.freeMemory())/(1024*1024))+" MB";
            }
            case "clock" -> "Clock  "+java.time.LocalTime.now().toString().substring(0,5);
            case "speed" -> "Speed  "+String.format(java.util.Locale.ROOT,"%.1f",mc.player.getDeltaMovement().horizontalDistance()*20)+" b/s";
            case "session" -> "Playtime  "+((System.currentTimeMillis()-START)/60000)+"m";
            case "position" -> "Yaw  "+Math.round(mc.player.getYRot())+"  Pitch  "+Math.round(mc.player.getXRot());
            case "dimension" -> "Dimension  "+mc.player.level().dimension().toString();
            default -> LiveMetrics.readout(f.id,mc);
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
        add("time","World age","HUD","World age in Minecraft ticks",false);
        add("memory","RAM usage","HUD","Minecraft Java heap usage",false);
        add("clock","Real clock","HUD","Local time",false);
        add("session","Playtime","HUD","Time since Minecraft opened",false);
        add("position","Rotation","HUD","Yaw and pitch readout",false);
        add("dimension","Dimension","HUD","Current dimension",false);
        add("cps","Left CPS","Combat","Mouse click presses per second",false);
        add("right_cps","Right CPS","Combat","Right mouse click presses per second",false);
        add("keys","Keystrokes","Combat","Live WASD key states",false);
        add("sprinting","Sprint indicator","Movement","Shows whether you are sprinting",false);
        add("sneaking","Sneak indicator","Movement","Shows whether you are sneaking",false);
        add("hunger","Hunger","HUD","Food level out of 20",false);
        add("armor","Armor points","Combat","Equipped armor protection points",false);
        add("xp","Experience level","Survival","Experience level",false);
        add("chunk","Chunk coordinates","World","Current chunk coordinates",false);
        add("altitude","Altitude","World","Current Y coordinate",false);
        add("block","Block below","World","Block standing beneath you",false);
        add("biome","Biome","World","Current biome name",false);
        add("weather","Weather","World","Current rain/clear conditions",false);
        add("light","Light level","World","Light at your current position",false);
        add("held","Held item","Survival","Main-hand item name",false);
        add("offhand","Offhand item","Survival","Offhand item name",false);
        add("held_count","Held stack size","Survival","Main-hand stack quantity",false);
        add("durability","Held durability","Combat","Main-hand remaining durability",false);
        add("totems","Totem counter","Combat","Totems in inventory",false);
        add("arrows","Arrow counter","Combat","Regular arrows in inventory",false);
        add("effects","Potion count","Combat","Number of active potion effects",false);
        add("server","Current server","World","Current server or singleplayer",false);
        add("playername","Username","HUD","Your Minecraft display name",false);
        add("fps_average","Average FPS","Performance","Rolling sampled FPS average",false);
        add("fps_low","Lowest FPS","Performance","Rolling sampled minimum FPS",false);
        add("fps_high","Highest FPS","Performance","Rolling sampled maximum FPS",false);
        add("memory_percent","Heap percentage","Performance","Minecraft memory utilization",false);
        add("nethercoords","Portal coordinates","World","Nether/Overworld coordinate conversion",false);
        add("waypoint","Nearest waypoint","World","Distance to nearest saved waypoint",false);
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
                Waypoints.load();
                configLoaded=true;
            }
            while(MENU_KEY.consumeClick()) {
                if(mc.gui.screen() instanceof ClientMenu)mc.gui.setScreen(null);
                else if(mc.gui.screen()==null)mc.gui.setScreen(new ClientMenu());
            }
            while(EDIT_KEY.consumeClick()) {
                if(mc.gui.screen()==null)mc.gui.setScreen(new HudEditor());
            }
            LiveMetrics.tick(mc);
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