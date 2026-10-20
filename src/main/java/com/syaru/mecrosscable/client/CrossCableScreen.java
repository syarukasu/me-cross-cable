package com.syaru.mecrosscable.client;

import com.syaru.mecrosscable.CrossCableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CrossCableScreen extends AbstractContainerScreen<CrossCableMenu> {
    private final Button[] ports = new Button[6];
    public CrossCableScreen(CrossCableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth=240;imageHeight=224;
    }
    @Override protected void init() {
        super.init();
        int[][] positions={{67,92},{67,36},{18,64},{116,64},{18,92},{116,92}};
        for(int i=0;i<6;i++) {
            int port=i;
            ports[i]=addRenderableWidget(Button.builder(Component.translatable("port.me_cross_cable."+i),
                    button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId,port))
                    .bounds(leftPos+positions[i][0],topPos+positions[i][1],44,24).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("menu.me_cross_cable.done"),button->onClose())
                .bounds(leftPos+imageWidth-64,topPos+imageHeight-29,52,20).build());
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY) {
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xFF282E34);
        g.fill(leftPos+2,topPos+2,leftPos+imageWidth-2,topPos+imageHeight-2,0xFFC6C6C6);
        g.fill(leftPos+10,topPos+28,leftPos+165,topPos+123,0xFF454D55);
        for(int i=0;i<6;i++) {
            int lane=menu.lane(i); int color=0xFF000000|menu.portColor(i).mediumVariant;
            var button=ports[i];
            g.fill(button.getX(),button.getY()+button.getHeight()+1,button.getX()+button.getWidth(),button.getY()+button.getHeight()+3,color);
            if(menu.selected()==i) g.renderOutline(button.getX()-1,button.getY()-1,button.getWidth()+2,button.getHeight()+2,0xFFFFFF66);
            button.setTooltip(Tooltip.create(Component.translatable("axis.me_cross_cable."+lane)));
        }
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY) {
        g.drawString(font,title,10,10,0xFF20252A,false);
        for(int lane=0;lane<3;lane++) {
            int y=135+lane*18; int color=0xFF000000|menu.lineColor(lane).mediumVariant;
            g.fill(12,y,16,y+12,color);
            int first=-1,second=-1;
            for(int port=0;port<6;port++)if(menu.lane(port)==lane){if(first<0)first=port;else second=port;}
            var pair=Component.translatable("port.me_cross_cable."+first).append(" \u2194 ")
                    .append(Component.translatable("port.me_cross_cable."+second));
            g.drawString(font,pair,22,y+2,0xFF20252A,false);
            String capacity=menu.infinite(lane)?"*":Integer.toString(menu.capacity(lane));
            String amount=menu.used(lane)+" / "+capacity;
            g.drawString(font,amount,imageWidth-12-font.width(amount),y+2,0xFF20252A,false);
        }
        Component prompt=menu.selected()<0?Component.translatable("menu.me_cross_cable.pair"):
                Component.translatable("menu.me_cross_cable.selected",Component.translatable("port.me_cross_cable."+menu.selected()));
        g.drawWordWrap(font,prompt,170,34,60,0xFF20252A);
        g.drawWordWrap(font,Component.translatable("menu.me_cross_cable.reroute"),12,196,150,0xFF454D55);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial) {
        renderBackground(g);super.render(g,mouseX,mouseY,partial);renderTooltip(g,mouseX,mouseY);
    }
}
