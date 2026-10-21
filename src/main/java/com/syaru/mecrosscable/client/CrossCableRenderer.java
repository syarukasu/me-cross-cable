package com.syaru.mecrosscable.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.syaru.mecrosscable.CableGeometry;
import com.syaru.mecrosscable.CrossCableBlockEntity;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public final class CrossCableRenderer implements BlockEntityRenderer<CrossCableBlockEntity> {
    private record LampQuad(int port,int segment,float[] points) { }
    private static final List<List<LampQuad>> BANKS=List.of(bank(false),bank(true));
    private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath("minecraft","block/white_concrete");
    public CrossCableRenderer(BlockEntityRendererProvider.Context context) { }

    @Override public void render(CrossCableBlockEntity cable,float partialTick,PoseStack poses,
            MultiBufferSource buffers,int light,int overlay) {
        boolean dense=cable.cableBlock().dense();
        if(!cable.cableBlock().smart())return;
        int a=cable.litSegments(0),b=cable.litSegments(1),c=cable.litSegments(2);
        if(a+b+c==0)return;
        var sprite=Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(WHITE);
        // Block-entity vertices need an entity layer, not the chunk solid layer.
        var consumer=buffers.getBuffer(RenderType.entityTranslucentEmissive(TextureAtlas.LOCATION_BLOCKS));
        for(var lamp:BANKS.get(dense?1:0)) {
            if(!cable.connected(lamp.port()))continue;
            var side=Direction.from3DDataValue(lamp.port());
            int lane=cable.laneForSide(side);
            int limit=switch(lane){case 0->a;case 1->b;default->c;};
            if(lamp.segment()>=limit)continue;
            draw(lamp,cable.portColor(lamp.port()).whiteVariant,poses,consumer,sprite);
        }
    }

    private static void draw(LampQuad lamp,int rgb,PoseStack poses,
            com.mojang.blaze3d.vertex.VertexConsumer consumer,
            net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
        float[] points=lamp.points();
        var side=Direction.from3DDataValue(lamp.port());
        for(int i=0;i<4;i++)consumer.addVertex(poses.last().pose(),points[i*3],points[i*3+1],points[i*3+2])
                    .setColor((rgb>>16)&255,(rgb>>8)&255,rgb&255,255)
                    .setUv(i<2?sprite.getU0():sprite.getU1(),i==0||i==3?sprite.getV0():sprite.getV1())
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(poses.last(),side.getStepX(),side.getStepY(),side.getStepZ());
    }

    private static List<LampQuad> bank(boolean dense) {
        return CableGeometry.lamps(dense).stream().map(lamp->quad(lamp.port(),lamp.segment(),lamp.box())).toList();
    }

    private static LampQuad quad(int port,int segment,CableGeometry.Box box) {
            float x0=(float)box.x0()/16,x1=(float)box.x1()/16;
            float y0=(float)box.y0()/16,y1=(float)box.y1()/16;
            float z0=(float)box.z0()/16,z1=(float)box.z1()/16;
            // Move lit quads just outside the baked dark lenses to avoid z-fighting.
            switch(port) {
                case 0 -> y0-=0.0001F;case 1 -> y1+=0.0001F;
                case 2 -> z0-=0.0001F;case 3 -> z1+=0.0001F;
                case 4 -> x0-=0.0001F;case 5 -> x1+=0.0001F;
            }
            float[] points=switch(port) {
                case 0 -> new float[]{x0,y0,z0,x1,y0,z0,x1,y0,z1,x0,y0,z1};
                case 1 -> new float[]{x0,y1,z0,x0,y1,z1,x1,y1,z1,x1,y1,z0};
                case 2 -> new float[]{x0,y0,z0,x0,y1,z0,x1,y1,z0,x1,y0,z0};
                case 3 -> new float[]{x0,y0,z1,x1,y0,z1,x1,y1,z1,x0,y1,z1};
                case 4 -> new float[]{x0,y0,z0,x0,y0,z1,x0,y1,z1,x0,y1,z0};
                case 5 -> new float[]{x1,y0,z0,x1,y1,z0,x1,y1,z1,x1,y0,z1};
                default -> throw new IllegalArgumentException("Invalid lamp face");
            };
            return new LampQuad(port,segment,points);
    }
}
