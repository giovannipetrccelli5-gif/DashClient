package com.dashclient.modules;

import com.dashclient.DashClient;
import com.dashclient.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.item.Items;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** Practical navigation, combat, survival and presentation tools. Opt-in. */
public final class QualityOfLifePack {
    private QualityOfLifePack() {}
    private static void info(List<Module> out, String name, Function<Minecraft,String> read) {
        out.add(new Module(name) {
            @Override public String hudText(Minecraft c) { return read.apply(c); }
        });
    }
    private static String decimal(double number) {
        return String.format(Locale.ROOT,"%.1f",number);
    }
    private static LivingEntity crosshair(Minecraft c) {
        return c.hitResult instanceof EntityHitResult result &&
            result.getEntity() instanceof LivingEntity living && living.isAlive() ?
            living : null;
    }

    public static void register(List<Module> out) {
        out.add(new ModuleListHud());
        out.add(new Module("Clean Screenshot HUD") {});
        out.add(new Module("Streamer Privacy HUD") {});

        info(out,"Info: Session Runtime",c -> c.player == null ? null :
            "Session uptime: " + (System.nanoTime() - SessionClock.START)/60_000_000_000L + " min");
        info(out,"Info: Direction Cardinal",c -> {
            if(c.player==null)return null;
            double rotation = ((c.player.getYRot()%360)+360)%360;
            String[] dirs={"South","West","North","East"};
            return "Facing: "+dirs[((int)Math.floor((rotation+45)/90))%4];
        });
        info(out,"Info: Movement Horizontal",c->c.player==null?null:
            "Horizontal movement: "+decimal(c.player.getDeltaMovement().horizontalDistance()*20)+" blocks/s");
        info(out,"Info: Movement Heading",c -> {
            if(c.player==null)return null;
            var vec = c.player.getDeltaMovement();
            if(vec.horizontalDistanceSqr()<0.0001)return "Heading: Standing still";
            return "Movement angle: "+decimal((Math.toDegrees(Math.atan2(-vec.x,vec.z))+360)%360)+" deg";
        });
        info(out,"Info: Current Screen Scale",c->c.options==null?null:
            "GUI scale: "+c.options.guiScale().get());
        info(out,"Info: FPS Versus 60",c->c.player==null?null:
            "60 FPS target: "+(c.getFps()>=60?"Met":"Below")+" ("+c.getFps()+" FPS)");
        info(out,"Info: Chunk Center Offset",c->c.player==null?null:
            "Chunk center offset: "+decimal(Math.abs((c.player.getX()%16+16)%16-8))+", "+
                decimal(Math.abs((c.player.getZ()%16+16)%16-8)));
        info(out,"Info: Block Grid Position",c->c.player==null?null:
            "Local grid: "+(c.player.blockPosition().getX()&15)+", "+
                (c.player.blockPosition().getZ()&15));
        info(out,"Info: Dimension Friendly",c->c.level==null?null:
            "Current dimension: "+c.level.dimension().identifier().getPath());
        info(out,"Info: Low Air Alert",c->c.player==null||!c.player.isInWater()?
            null:c.player.getAirSupply()<60?"Warning: low air":null);
        info(out,"Info: Inventory Full Alert",c->{
            if(c.player==null)return null;
            for(int slot=0;slot<36;slot++)
                if(c.player.getInventory().getItem(slot).isEmpty())return null;
            return "Inventory full!";
        });
        info(out,"Info: Totem Not Equipped Alert",c->c.player==null?null:
            c.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING) ?
                null:"No totem equipped");
        info(out,"Info: Missing Chest Armor Alert",c->c.player==null?null:
            c.player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()?
                "No chest armor equipped":null);
        info(out,"Info: Slow FPS Alert",c->c.player!=null&&c.getFps()>0&&c.getFps()<35?
            "Performance alert: FPS under 35":null);
        info(out,"Info: Crosshair Target Health",c->{
            LivingEntity target=crosshair(c);
            return target==null?null:"Crosshair HP: "+decimal(target.getHealth());
        });
        info(out,"Info: Crosshair Target Distance",c->{
            LivingEntity target=crosshair(c);
            return target==null||c.player==null?null:
                "Crosshair distance: "+decimal(c.player.distanceTo(target))+" blocks";
        });
        info(out,"Info: Crosshair Target Armor",c->{
            LivingEntity target=crosshair(c);
            return target==null?null:"Crosshair armor: "+target.getArmorValue();
        });
        info(out,"Info: Combat Cooldown",c->c.player==null?null:
            "Melee cooldown: "+Math.round(c.player.getAttackStrengthScale(0)*100)+"%");
        info(out,"Info: Active Module Count",c->{
            int active=0;
            for(Module m:DashClient.MODULES)if(m.enabled())active++;
            return "Enabled modules: "+active;
        });
    }

    private static final class SessionClock {
        static final long START=System.nanoTime();
    }

    /**
     * Conventional draggable, compact active-modules list. No extra render
     * mixins and no allocations of other modules' HUD strings.
     */
    private static final class ModuleListHud extends Module {
        private static final int MAX_LINES=12;
        private java.util.List<String> cachedNames=java.util.List.of();
        private int cachedWidth=70;
        private int ticks=5;
        ModuleListHud(){super("Module List HUD");}
        @Override public boolean customHud(){return true;}
        @Override public void onEnable(){ticks=5;}
        @Override public void onDisable(){cachedNames=java.util.List.of();}

        // Cache enabled module labels on the game tick, never rebuild the
        // entire module list from the render thread for every video frame.
        @Override public void onTick(Minecraft c){
            if(++ticks<5)return;
            ticks=0;
            java.util.ArrayList<String> names=new java.util.ArrayList<>();
            int width=70;
            for(Module module:DashClient.MODULES){
                if(module==this||!module.enabled()||
                        module.name().startsWith("Info:"))continue;
                names.add(module.name());
                width=Math.max(width,c.font.width(module.name())+10);
                if(names.size()>=MAX_LINES)break;
            }
            cachedWidth=width;
            cachedNames=java.util.List.copyOf(names);
        }
        @Override public int hudWidth(Minecraft c){return cachedWidth;}
        @Override public int hudHeight(Minecraft c){
            return Math.max(14,cachedNames.size()*12+4);
        }
        @Override public void renderHud(Minecraft c,GuiGraphicsExtractor g,int x,int y){
            var list=cachedNames;
            int height=Math.max(14,list.size()*12+4);
            g.fill(x,y,x+cachedWidth,y+height,0xAA101826);
            g.fill(x,y,x+3,y+height,0xFF6BDBFF);
            int row=0;
            for(String name:list){
                g.text(c.font,name,x+7,y+4+row*12,0xFFEAF5FF,true);
                row++;
            }
            if(list.isEmpty())g.text(c.font,"No modules",x+7,y+4,0xFFEAF5FF,true);
        }
    }
}
