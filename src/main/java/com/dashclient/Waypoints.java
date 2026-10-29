package com.dashclient;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Local waypoints. Locations never leave the player's game directory. */
public final class Waypoints {
    public record Point(String name,String dimension,int x,int y,int z) {}
    private static final Path PATH=FabricLoader.getInstance().getConfigDir()
        .resolve("dashclient-waypoints.properties");
    private static final List<Point> points=new ArrayList<>();
    private Waypoints() {}
    public static List<Point> list(){return List.copyOf(points);}
    public static void load(){
        points.clear();
        Properties p=new Properties();
        if(!Files.isRegularFile(PATH))return;
        try(InputStream in=Files.newInputStream(PATH)){
            p.load(in);
            int n=Math.max(0,Math.min(24,Integer.parseInt(p.getProperty("count","0"))));
            for(int i=0;i<n;i++){
                String raw=p.getProperty("point."+i);
                if(raw==null)continue;
                String[] data=raw.split("\\|",-1);
                if(data.length!=5)continue;
                points.add(new Point(data[0],data[1],
                    Integer.parseInt(data[2]),Integer.parseInt(data[3]),
                    Integer.parseInt(data[4])));
            }
        }catch(Exception ex){DashClient.LOGGER.warn("Waypoint load failed",ex);}
    }
    public static boolean add(Minecraft mc,String name){
        if(mc.player==null||mc.level==null||points.size()>=24)return false;
        name=name.trim().replace("|","-");
        if(name.isEmpty())name="Waypoint "+(points.size()+1);
        if(name.length()>28)name=name.substring(0,28);
        var p=mc.player.blockPosition();
        points.add(new Point(name,mc.level.dimension().toString(),p.getX(),p.getY(),p.getZ()));
        save();
        return true;
    }
    public static void remove(int index){
        if(index<0||index>=points.size())return;
        points.remove(index);save();
    }
    public static String nearest(Minecraft mc){
        if(mc.player==null||mc.level==null)return "";
        var pos=mc.player.blockPosition();
        Point best=null; double shortest=Double.MAX_VALUE;
        for(Point p:points){
            if(!p.dimension.equals(mc.level.dimension().toString()))continue;
            double dx=p.x-pos.getX(),dy=p.y-pos.getY(),dz=p.z-pos.getZ();
            double d=dx*dx+dy*dy+dz*dz;
            if(d<shortest){best=p;shortest=d;}
        }
        if(best==null)return "Waypoint  none in this dimension";
        return best.name+"  "+Math.round(Math.sqrt(shortest))+"m";
    }
    private static void save(){
        Properties p=new Properties();
        p.setProperty("count",Integer.toString(points.size()));
        for(int i=0;i<points.size();i++){
            Point a=points.get(i);
            p.setProperty("point."+i,a.name+"|"+a.dimension+"|"
                +a.x+"|"+a.y+"|"+a.z);
        }
        try{
            Files.createDirectories(PATH.getParent());
            Path tmp=PATH.resolveSibling(PATH.getFileName()+".tmp");
            try(OutputStream out=Files.newOutputStream(tmp)){
                p.store(out,"DashClient local-only waypoints");
            }
            Files.move(tmp,PATH,StandardCopyOption.REPLACE_EXISTING);
        }catch(Exception ex){DashClient.LOGGER.warn("Waypoint save failed",ex);}
    }
}
