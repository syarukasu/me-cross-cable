package com.syaru.mecrosscable.client;

import com.syaru.mecrosscable.CrossCableBlockEntity;
import com.syaru.mecrosscable.CrossCableMod;
import com.syaru.mecrosscable.PortRouting;
import appeng.api.util.AEColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid=CrossCableMod.ID, value=Dist.CLIENT, bus=EventBusSubscriber.Bus.MOD)
public final class CrossCableClient {
    private static final PortRouting DEFAULT_ROUTING=new PortRouting();
    @SubscribeEvent public static void setup(RegisterMenuScreensEvent event) {
        event.register(CrossCableMod.MENU.get(), CrossCableScreen::new);
    }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CrossCableMod.ENTITY.get(), CrossCableRenderer::new);
    }
    @SubscribeEvent public static void blockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state,level,pos,tint) -> colorForTint(
                level!=null&&pos!=null&&level.getBlockEntity(pos) instanceof CrossCableBlockEntity cable?cable:null,tint),
                CrossCableMod.GLASS.get(), CrossCableMod.SMART.get(), CrossCableMod.DENSE.get());
    }
    public static int colorForTint(CrossCableBlockEntity cable,int tint) {
        if(tint<0||tint>8)return 0xFFFFFF;
        if(cable==null)return 0x70777E;
        if(tint>=3&&!cable.connected(tint-3)&&cable.color(cable.routing().lane(tint-3))==AEColor.TRANSPARENT)
            return 0x70777E;
        return (tint<3?cable.lineColor(tint):cable.portColor(tint-3)).mediumVariant;
    }
    @SubscribeEvent public static void itemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack,tint) -> {
            if(tint>=0&&tint<=8) {
                var tag=stack.get(CrossCableMod.CONFIGURATION.get());
                int[] ports=tag==null?new int[0]:tag.getIntArray("ports");
                int lane=tint<3?tint:PortRouting.valid(ports)?ports[tint-3]:DEFAULT_ROUTING.lane(tint-3);
                int[] colors=tag==null?new int[0]:tag.getIntArray("colors");
                return lane<colors.length&&colors[lane]>=0&&colors[lane]<AEColor.values().length
                        &&AEColor.values()[colors[lane]]!=AEColor.TRANSPARENT?AEColor.values()[colors[lane]].mediumVariant:0x70777E;
            }
            return 0xFFFFFF;
        },
                CrossCableMod.GLASS_ITEM.get(), CrossCableMod.SMART_ITEM.get(), CrossCableMod.DENSE_ITEM.get());
    }
}
