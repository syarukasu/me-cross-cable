package com.syaru.mecrosscable;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

@Mod(CrossCableMod.ID)
public final class CrossCableMod {
    public static final String ID = "me_cross_cable";
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ID);
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> CONFIGURATION =
            COMPONENTS.register("configuration", () -> DataComponentType.<CompoundTag>builder()
                    .persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG).build());
    public static final DeferredHolder<MenuType<?>, MenuType<CrossCableMenu>> MENU = MENUS.register("routing",
            () -> IMenuTypeExtension.create((id, inventory, buffer) -> new CrossCableMenu(id, inventory, buffer.readBlockPos())));
    public static final DeferredHolder<Block, CrossCableBlock> GLASS = BLOCKS.register("cross_cable", () -> new CrossCableBlock(false, false));
    public static final DeferredHolder<Block, CrossCableBlock> SMART = BLOCKS.register("smart_cross_cable", () -> new CrossCableBlock(true, false));
    public static final DeferredHolder<Block, CrossCableBlock> DENSE = BLOCKS.register("dense_smart_cross_cable", () -> new CrossCableBlock(true, true));
    public static final DeferredHolder<Item, Item> GLASS_ITEM = ITEMS.register("cross_cable", () -> new BlockItem(GLASS.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> SMART_ITEM = ITEMS.register("smart_cross_cable", () -> new BlockItem(SMART.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> DENSE_ITEM = ITEMS.register("dense_smart_cross_cable", () -> new BlockItem(DENSE.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrossCableBlockEntity>> ENTITY = ENTITIES.register("cross_cable",
            () -> BlockEntityType.Builder.of(CrossCableBlockEntity::new, GLASS.get(), SMART.get(), DENSE.get()).build(null));
    private static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("cables", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.me_cross_cable"))
            .icon(() -> SMART_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> { output.accept(GLASS_ITEM.get()); output.accept(SMART_ITEM.get()); output.accept(DENSE_ITEM.get()); })
            .build());

    public CrossCableMod(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); TABS.register(bus); MENUS.register(bus); COMPONENTS.register(bus);
        bus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) ->
                event.registerBlockEntity(appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST, ENTITY.get(),
                        (entity, context) -> entity));
    }
}
