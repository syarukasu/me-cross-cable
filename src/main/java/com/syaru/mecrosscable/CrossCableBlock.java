package com.syaru.mecrosscable;

import appeng.api.util.AEColor;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CrossCableBlock extends BaseEntityBlock {
    public static final BooleanProperty CUSTOM = BooleanProperty.create("custom");
    public static final BooleanProperty[] CONNECTIONS={BooleanProperty.create("down"),BooleanProperty.create("up"),
            BooleanProperty.create("north"),BooleanProperty.create("south"),BooleanProperty.create("west"),BooleanProperty.create("east")};
    private final boolean smart;
    private final boolean dense;
    private final VoxelShape[] shapes=new VoxelShape[64];

    public CrossCableBlock(boolean smart, boolean dense) {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.8F).sound(SoundType.METAL).noOcclusion());
        this.smart = smart; this.dense = dense;
        BlockState defaults=stateDefinition.any().setValue(CUSTOM,false);
        for(var property:CONNECTIONS)defaults=defaults.setValue(property,false);
        registerDefaultState(defaults);
        VoxelShape result = Shapes.empty();
        for (var b : CableGeometry.housing(dense)) result = Shapes.or(result, box(b.x0(),b.y0(),b.z0(),b.x1(),b.y1(),b.z1()));
        for (var b : CableGeometry.frame(dense)) result = Shapes.or(result, box(b.x0(),b.y0(),b.z0(),b.x1(),b.y1(),b.z1()));
        for(int port=0;port<6;port++)for(var b:CableGeometry.collar(port,dense))
            result=Shapes.or(result,box(b.x0(),b.y0(),b.z0(),b.x1(),b.y1(),b.z1()));
        shapes[0]=result.optimize();
        for(int mask=1;mask<shapes.length;mask++) {
            int port=Integer.numberOfTrailingZeros(mask);
            var b=CableGeometry.connector(port,dense);
            var shape=Shapes.or(shapes[mask & (mask-1)],box(b.x0(),b.y0(),b.z0(),b.x1(),b.y1(),b.z1()));
            for(var collar:CableGeometry.connectedCollar(port,dense))shape=Shapes.or(shape,
                    box(collar.x0(),collar.y0(),collar.z0(),collar.x1(),collar.y1(),collar.z1()));
            shapes[mask]=shape.optimize();
        }
    }

    public boolean smart() { return smart; }
    public boolean dense() { return dense; }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos neighbor,boolean moving) {
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof CrossCableBlockEntity cable)cable.onAdjacentChanged();
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(CUSTOM).add(CONNECTIONS); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CrossCableBlockEntity(pos, state); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask=0;
        for(int port=0;port<6;port++)if(state.getValue(CONNECTIONS[port]))mask|=1<<port;
        return shapes[mask];
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity entity, ItemStack stack) {
        if (!level.isClientSide && entity instanceof Player player && level.getBlockEntity(pos) instanceof CrossCableBlockEntity cable) cable.setOwner(player);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CrossCableBlockEntity cable)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        int lane = cable.laneForSide(hit.getDirection());
        if (held.getItem() instanceof DyeItem dye) {
            if (!player.mayUseItemAt(pos, hit.getDirection(), held) || !level.mayInteract(player, pos)) return InteractionResult.FAIL;
            if (!level.isClientSide && cable.recolor(lane, AEColor.values()[dye.getDyeColor().getId()]) && !player.isCreative()) held.shrink(1);
        } else if (held.isEmpty()) {
            if (!level.isClientSide) {
                if (player.isShiftKeyDown()) {
                    if (!player.mayUseItemAt(pos, hit.getDirection(), held) || !level.mayInteract(player, pos)) return InteractionResult.FAIL;
                    cable.recolor(lane, AEColor.TRANSPARENT);
                    player.displayClientMessage(cable.status(lane), true);
                } else {
                    NetworkHooks.openScreen((ServerPlayer) player,
                            new SimpleMenuProvider((id, inventory, owner) -> new CrossCableMenu(id, inventory, pos),
                                    Component.translatable("menu.me_cross_cable")), pos);
                }
            }
        } else return InteractionResult.PASS;
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static int lane(Direction side) {
        return switch (side.getAxis()) { case X -> 0; case Y -> 1; case Z -> 2; };
    }

    @Override public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.me_cross_cable.axes"));
        tooltip.add(Component.translatable("tooltip.me_cross_cable." + (dense ? "dense" : "normal")));
        tooltip.add(Component.translatable("tooltip.me_cross_cable.dye"));
    }
}
