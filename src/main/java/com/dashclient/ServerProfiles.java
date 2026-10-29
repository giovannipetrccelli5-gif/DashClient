package com.dashclient;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/** Optional server-specific profile selection, stored locally only. */
public final class ServerProfiles {
    private static final Path PATH=FabricLoader.getInstance().getConfigDir()
        .resolve("dashclient-server-profiles.properties");
    private static final Map<String,String> bindings=new HashMap<>();
    private static String lastAddress="";
    private ServerProfiles() {}
    public static void load() {
        bindings.clear();
        Properties p=new Properties();
        if(Files.isRegularFile(PATH))try(InputStream in=Files.newInputStream(PATH)){
            p.load(in);
            for(String address:p.stringPropertyNames()){
                String profile=p.getProperty(address);
                for(String valid:ClientConfig.PROFILES)
                    if(valid.equals(profile))bindings.put(address,profile);
            }
        }catch(Exception ex){DashClient.LOGGER.warn("Server profiles read failed",ex);}
    }
    private static String address(Minecraft mc) {
        if(mc==null||mc.getCurrentServer()==null)return "";
        return mc.getCurrentServer().ip.trim().toLowerCase(Locale.ROOT);
    }
    public static String assigned(Minecraft mc){
        return bindings.getOrDefault(address(mc),"No automatic profile");
    }
    public static boolean bindCurrent(Minecraft mc) {
        String address=address(mc);
        if(address.isEmpty())return false;
        bindings.put(address,ClientConfig.active);
        save();
        return true;
    }
    public static boolean unbindCurrent(Minecraft mc) {
        String address=address(mc);
        if(address.isEmpty())return false;
        bindings.remove(address);
        save();
        return true;
    }
    public static void tick(Minecraft mc) {
        String now=address(mc);
        if(now.equals(lastAddress))return;
        lastAddress=now;
        String desired=bindings.get(now);
        if(desired!=null&&!now.isEmpty())ClientConfig.switchProfile(desired);
    }
    private static void save() {
        Properties p=new Properties();
        p.putAll(bindings);
        try {
            Files.createDirectories(PATH.getParent());
            Path tmp=PATH.resolveSibling(PATH.getFileName()+".tmp");
            try(OutputStream out=Files.newOutputStream(tmp)){
                p.store(out,"DashClient server-specific profiles (local only)");
            }
            Files.move(tmp,PATH,StandardCopyOption.REPLACE_EXISTING);
        }catch(Exception ex){DashClient.LOGGER.warn("Server profiles save failed",ex);}
    }
}
