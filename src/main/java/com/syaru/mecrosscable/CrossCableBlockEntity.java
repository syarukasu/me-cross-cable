package com.syaru.mecrosscable;

import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.pathing.ChannelMode;
import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import java.util.Arrays;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class CrossCableBlockEntity extends BlockEntity implements IInWorldGridNodeHost {
    private static final IGridNodeListener<CrossCableBlockEntity> LISTENER = new IGridNodeListener<>() {
        @Override public void onSaveChanges(CrossCableBlockEntity owner, IGridNode node) { owner.setChanged(); }
        @Override public void onStateChanged(CrossCableBlockEntity owner, IGridNode node, State state) { owner.refreshVisuals(); }
        @Override public void onInWorldConnectionChanged(CrossCableBlockEntity owner, IGridNode node) { owner.refreshVisuals(); }
    };
    private IManagedGridNode[] nodes;
    private CompoundTag savedNodes = new CompoundTag();
    private boolean initQueued;
    private Player placementOwner;
    private final AEColor[] colors = {AEColor.TRANSPARENT, AEColor.TRANSPARENT, AEColor.TRANSPARENT};
    private final AEColor[] lineColors = {AEColor.TRANSPARENT, AEColor.TRANSPARENT, AEColor.TRANSPARENT};
    private final AEColor[] portColors = new AEColor[6];
    private final int[] used = new int[3];
    private final int[] capacity = new int[3];
    private final boolean[] powered = new boolean[3];
    private final boolean[] infinite = new boolean[3];
    private int connected;
    private PortRouting routing = new PortRouting();

    public CrossCableBlockEntity(BlockPos pos, BlockState state) {
        super(CrossCableMod.ENTITY.get(), pos, state);
        Arrays.fill(portColors,AEColor.TRANSPARENT);
    }
    public CrossCableBlock cableBlock() { return (CrossCableBlock) getBlockState().getBlock(); }
    public AEColor color(int lane) { return colors[lane]; }
    public AEColor lineColor(int lane) { return lineColors[lane]; }
    public AEColor portColor(int port) { return portColors[port]; }
    public boolean connected(int port) { return (connected & (1<<port))!=0; }
    public void onAdjacentChanged() { refreshVisuals(); }
    public int laneForSide(Direction side) { return routing.lane(side.ordinal()); }
    public PortRouting routing() { return routing; }
    public int used(int lane) { return used[lane]; }
    public int capacity(int lane) { return capacity[lane]; }
    public boolean powered(int lane) { return powered[lane]; }
    public boolean infinite(int lane) { return infinite[lane]; }
    public int litSegments(int lane) {
        return ChannelDisplay.litSegments(used[lane], capacity[lane], cableBlock().dense() ? 32 : 8, powered[lane], infinite[lane]);
    }

    @Override public IGridNode getGridNode(Direction side) {
        return side == null || nodes == null ? null : nodes[laneForSide(side)].getNode();
    }
    @Override public AECableType getCableConnectionType(Direction side) {
        return cableBlock().dense() ? AECableType.DENSE_SMART : cableBlock().smart() ? AECableType.SMART : AECableType.GLASS;
    }

    @Override public void onLoad() { super.onLoad(); scheduleInit(); }
    @Override public void clearRemoved() { super.clearRemoved(); scheduleInit(); }
    private void scheduleInit() {
        if (level != null && !level.isClientSide && !initQueued && nodes == null && !isRemoved()) {
            initQueued = true;
            GridHelper.onFirstTick(this, CrossCableBlockEntity::initialize);
        }
    }
    private void initialize() {
        initQueued = false;
        if (isRemoved() || level == null || level.isClientSide || nodes != null) return;
        if (getBlockState().getValue(CrossCableBlock.CUSTOM) != routing.custom())
            level.setBlock(worldPosition, getBlockState().setValue(CrossCableBlock.CUSTOM, routing.custom()), Block.UPDATE_CLIENTS);
        nodes = new IManagedGridNode[3];
        for (int lane = 0; lane < 3; lane++) {
            EnumSet<Direction> sides = EnumSet.noneOf(Direction.class);
            for (Direction side : Direction.values()) if (laneForSide(side) == lane) sides.add(side);
            nodes[lane] = GridHelper.createManagedNode(this, LISTENER).setTagName("lane" + lane)
                    .setInWorldNode(true).setExposedOnSides(sides).setGridColor(colors[lane])
                    .setFlags(cableBlock().dense() ? GridFlags.DENSE_CAPACITY : GridFlags.PREFERRED)
                    .setIdlePowerUsage(0).setVisualRepresentation(getBlockState().getBlock());
            nodes[lane].loadFromNBT(savedNodes);
            if (placementOwner != null) nodes[lane].setOwningPlayer(placementOwner);
        }
        for (var node : nodes) node.create(level, worldPosition);
        placementOwner = null;
        refreshVisuals();
    }
    public void setOwner(Player player) {
        if (nodes == null) placementOwner = player;
        else for (var node : nodes) node.setOwningPlayer(player);
    }

    private CompoundTag captureNodes() {
        if (nodes == null) return savedNodes.copy();
        var tag = new CompoundTag();
        for (var node : nodes) node.saveToNBT(tag);
        return tag;
    }
    private void destroyNodes() {
        if (nodes != null) {
            savedNodes = captureNodes();
            var detached = nodes;
            nodes = null;
            for (var node : detached) node.destroy();
        }
        placementOwner = null;
    }
    @Override public void setRemoved() { super.setRemoved(); destroyNodes(); }
    @Override public void onChunkUnloaded() { super.onChunkUnloaded(); destroyNodes(); }

    public boolean recolor(int lane, AEColor color) {
        if (level == null || level.isClientSide || isRemoved() || colors[lane] == color) return false;
        colors[lane] = color;
        if (nodes != null) nodes[lane].setGridColor(color);
        refreshVisuals();
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        return true;
    }

    public void connectFaces(int anchor, int target) {
        if (level == null || level.isClientSide || isRemoved()) return;
        PortRouting next = routing.connect(anchor, target);
        if (next == routing) return;
        // Remove old external connections before any new face-to-node identity is visible.
        if (nodes != null) for (var node : nodes) node.setExposedOnSides(EnumSet.noneOf(Direction.class));
        routing = next;
        if (nodes != null) for (int lane = 0; lane < 3; lane++) {
            var sides = EnumSet.noneOf(Direction.class);
            for (Direction side : Direction.values()) if (laneForSide(side) == lane) sides.add(side);
            nodes[lane].setExposedOnSides(sides);
        }
        level.setBlock(worldPosition, getBlockState().setValue(CrossCableBlock.CUSTOM, routing.custom()), Block.UPDATE_CLIENTS);
        setChanged(); refreshVisuals();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("nodes", captureNodes());
        tag.putIntArray("ports", routing.ports());
        tag.putIntArray("colors", Arrays.stream(colors).mapToInt(Enum::ordinal).toArray());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        destroyNodes();
        savedNodes = tag.getCompound("nodes").copy();
        int[] ports = tag.getIntArray("ports");
        routing = PortRouting.valid(ports) ? new PortRouting(ports) : new PortRouting();
        int[] loaded = tag.getIntArray("colors");
        for (int i = 0; i < 3; i++) colors[i] = i < loaded.length && loaded[i] >= 0 && loaded[i] < AEColor.values().length
                ? AEColor.values()[loaded[i]] : AEColor.TRANSPARENT;
        scheduleInit();
    }

    private void refreshVisuals() {
        if (level == null || level.isClientSide || isRemoved()) return;
        boolean changed = false;
        int nextConnected = 0;
        for (int i = 0; i < 3; i++) {
            IGridNode node = nodes == null || nodes[i] == null ? null : nodes[i].getNode();
            int nextUsed = 0;
            var connections=node==null?java.util.Map.<Direction,appeng.api.networking.IGridConnection>of():node.getInWorldConnections();
            AEColor first=AEColor.TRANSPARENT,second=AEColor.TRANSPARENT;
            boolean foundFirst=false;
            for(var side:Direction.values())if(laneForSide(side)==i) {
                AEColor nextPort=colors[i];
                var connection=connections.get(side);
                if(connection!=null) {
                    AEColor adjacent=connection.getOtherSide(node).getGridColor();
                    nextPort=adjacent;
                }
                changed |= portColors[side.ordinal()]!=nextPort;
                portColors[side.ordinal()]=nextPort;
                if(!foundFirst){first=nextPort;foundFirst=true;}else second=nextPort;
            }
            AEColor nextLine=AEColor.values()[CableAppearance.lineColor(colors[i].ordinal(),first.ordinal(),second.ordinal(),AEColor.TRANSPARENT.ordinal())];
            changed |= lineColors[i]!=nextLine;
            lineColors[i]=nextLine;
            for (var entry : connections.entrySet()) {
                nextConnected |= 1 << entry.getKey().ordinal();
                nextUsed = Math.max(nextUsed, entry.getValue().getUsedChannels());
            }
            int nextCapacity = node == null ? 0 : node.getMaxChannels();
            boolean nextPower = node != null && node.isPowered();
            boolean nextInfinite = node != null && node.getGrid().getPathingService().getChannelMode() == ChannelMode.INFINITE;
            changed |= used[i] != nextUsed || capacity[i] != nextCapacity || powered[i] != nextPower || infinite[i] != nextInfinite;
            used[i] = nextUsed; capacity[i] = nextCapacity; powered[i] = nextPower; infinite[i] = nextInfinite;
        }
        changed |= nextConnected != connected;
        connected = nextConnected;
        BlockState state=getBlockState();
        for(int port=0;port<6;port++)state=state.setValue(CrossCableBlock.CONNECTIONS[port],connected(port));
        if(state!=getBlockState())level.setBlock(worldPosition,state,Block.UPDATE_CLIENTS);
        else if(changed)level.sendBlockUpdated(worldPosition,state,state,Block.UPDATE_CLIENTS);
    }

    public Component status(int lane) {
        refreshVisuals();
        return Component.translatable("message.me_cross_cable.status", Component.translatable("axis.me_cross_cable." + lane),
                Component.translatable(colors[lane].translationKey), used[lane],
                infinite[lane] ? Component.translatable("message.me_cross_cable.unlimited") : Component.literal(Integer.toString(capacity[lane])));
    }
    @Override public CompoundTag getUpdateTag() {
        var tag = new CompoundTag();
        tag.putIntArray("ports", routing.ports());
        tag.putIntArray("lineColors", Arrays.stream(lineColors).mapToInt(Enum::ordinal).toArray());
        tag.putIntArray("portColors", Arrays.stream(portColors).mapToInt(Enum::ordinal).toArray());
        tag.putIntArray("colors", Arrays.stream(colors).mapToInt(Enum::ordinal).toArray());
        tag.putIntArray("used", used); tag.putIntArray("capacity", capacity);
        int flags = 0;
        for (int i = 0; i < 3; i++) { if (powered[i]) flags |= 1 << i; if (infinite[i]) flags |= 1 << (i+3); }
        tag.putInt("flags", flags); tag.putInt("connected", connected);
        return tag;
    }
    @Override public void handleUpdateTag(CompoundTag tag) {
        int[] ports = tag.getIntArray("ports");
        routing = PortRouting.valid(ports) ? new PortRouting(ports) : new PortRouting();
        int[] lines=tag.getIntArray("lineColors"),ends=tag.getIntArray("portColors");
        for(int i=0;i<3;i++)lineColors[i]=readColor(lines,i);
        for(int i=0;i<6;i++)portColors[i]=readColor(ends,i);
        int[] dye = tag.getIntArray("colors"), amounts = tag.getIntArray("used"), limits = tag.getIntArray("capacity");
        for (int i = 0; i < 3; i++) {
            colors[i] = i < dye.length && dye[i] >= 0 && dye[i] < AEColor.values().length ? AEColor.values()[dye[i]] : AEColor.TRANSPARENT;
            used[i] = i < amounts.length ? amounts[i] : 0; capacity[i] = i < limits.length ? limits[i] : 0;
            powered[i] = (tag.getInt("flags") & (1 << i)) != 0; infinite[i] = (tag.getInt("flags") & (1 << (i+3))) != 0;
        }
        connected = tag.getInt("connected");
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) handleUpdateTag(packet.getTag());
    }
    private static AEColor readColor(int[] values,int index) {
        return index<values.length&&values[index]>=0&&values[index]<AEColor.values().length?AEColor.values()[values[index]]:AEColor.TRANSPARENT;
    }
}
