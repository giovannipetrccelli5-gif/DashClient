package com.dashclient;

import net.fabricmc.loader.api.FabricLoader;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** Clean configuration namespace. Never reads legacy DashClient preferences. */
public final class ClientConfig {
    private static final Path PATH=FabricLoader.getInstance().getConfigDir().resolve("dashclient-v2.properties");
    public static final String[] PROFILES={"Default","PvP","Survival","Low FPS"};
    public static String active="Default";
    private static final Properties store=new Properties();

    private ClientConfig(){}
    public static void load(){
        store.clear();
        if(Files.isRegularFile(PATH)) try(InputStream in=Files.newInputStream(PATH)){
            store.load(in);
        }catch(Exception ex){DashClient.LOGGER.warn("Unable to read client config",ex);}
        String name=store.getProperty("profile.active","Default");
        for(String p:PROFILES) if(p.equals(name)) active=p;
        for(Feature f:DashClient.FEATURES){
            f.enabled=Boolean.parseBoolean(store.getProperty("feature."+active+"."+f.id,Boolean.toString(f.initial)));
        }
    }
    public static void save(){
        store.setProperty("profile.active",active);
        for(Feature f:DashClient.FEATURES)
            store.setProperty("feature."+active+"."+f.id,Boolean.toString(f.enabled));
        try{
            Files.createDirectories(PATH.getParent());
            Path tmp=PATH.resolveSibling(PATH.getFileName()+".tmp");
            try(OutputStream out=Files.newOutputStream(tmp)){
                store.store(out,"DashClient 2 independent settings");
            }
            Files.move(tmp,PATH,StandardCopyOption.REPLACE_EXISTING);
        }catch(Exception ex){DashClient.LOGGER.warn("Unable to save config",ex);}
    }
    public static void switchProfile(String profile){
        boolean known=false;
        for(String p:PROFILES) if(p.equals(profile)) known=true;
        if(!known||active.equals(profile))return;
        save();
        active=profile;
        for(Feature f:DashClient.FEATURES) {
            // Defaults are deliberately the same for every new profile.
            f.enabled=Boolean.parseBoolean(store.getProperty("feature."+active+"."+f.id,Boolean.toString(f.initial)));
        }
        save();
    }
    public static int x(Feature f,int fallback){
        return bound(store.getProperty("hud."+f.id+".x"),fallback);
    }
    public static int y(Feature f,int fallback){
        return bound(store.getProperty("hud."+f.id+".y"),fallback);
    }
    private static int bound(String value,int fallback){
        if(value==null)return fallback;
        try{return Math.max(0,Math.min(10000,Integer.parseInt(value)));}catch(Exception ex){return fallback;}
    }
    public static void hud(Feature f,int x,int y){
        store.setProperty("hud."+f.id+".x",Integer.toString(Math.max(0,x)));
        store.setProperty("hud."+f.id+".y",Integer.toString(Math.max(0,y)));
        save();
    }
    public static void resetHud(){
        for(Feature f:DashClient.FEATURES) {
            store.remove("hud."+f.id+".x");
            store.remove("hud."+f.id+".y");
        }
        save();
    }
}