package com.syaru.mecrosscable;

import appeng.api.util.AEColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public final class CrossCableMenu extends AbstractContainerMenu {
    private final CrossCableBlockEntity cable;
    private final BlockPos pos;
    private final ContainerData data;
    private int selected = -1;

    public CrossCableMenu(int id, Inventory inventory, BlockPos pos) {
        super(CrossCableMod.MENU.get(), id);
        this.pos = pos;
        cable = inventory.player.level().getBlockEntity(pos) instanceof CrossCableBlockEntity c ? c : null;
        data = inventory.player.level().isClientSide ? new SimpleContainerData(34) : new ContainerData() {
            @Override public int getCount() { return 34; }
            @Override public void set(int index, int value) { }
            @Override public int get(int index) {
                if (index == 6) return selected;
                if (cable == null) return 0;
                if (index < 6) return cable.routing().lane(index);
                if (index < 13) { int value = cable.used((index-7)/2); return (index%2 == 1 ? value : value >>> 16) & 0xffff; }
                if (index < 19) { int value = cable.capacity((index-13)/2); return (index%2 == 1 ? value : value >>> 16) & 0xffff; }
                if (index < 22) return cable.lineColor(index-19).ordinal();
                if (index < 25) return cable.powered(index-22) ? 1 : 0;
                if (index < 28) return cable.infinite(index-25) ? 1 : 0;
                return cable.portColor(index-28).ordinal();
            }
        };
        addDataSlots(data);
    }
    public int lane(int port) { return data.get(port); }
    public int selected() { return data.get(6); }
    public int used(int lane) { return (data.get(7+lane*2)&0xffff) | data.get(8+lane*2)<<16; }
    public int capacity(int lane) { return (data.get(13+lane*2)&0xffff) | data.get(14+lane*2)<<16; }
    public boolean infinite(int lane) { return data.get(25+lane) != 0; }
    public AEColor lineColor(int lane) { return AEColor.values()[data.get(19+lane)]; }
    public AEColor portColor(int port) { return AEColor.values()[data.get(28+port)]; }
    @Override public boolean stillValid(Player player) {
        return cable != null && !cable.isRemoved() && player.level().getBlockEntity(pos) == cable
                && player.distanceToSqr(pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5) <= 64;
    }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (button < 0 || button >= 6 || !stillValid(player) || player.isSpectator()
                || !player.mayBuild() || !player.level().mayInteract(player, pos)) return false;
        if (selected < 0) selected = button;
        else { cable.connectFaces(selected, button); selected = -1; }
        broadcastChanges();
        return true;
    }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
}
