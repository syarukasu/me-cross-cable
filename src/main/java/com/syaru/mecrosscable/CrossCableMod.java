package com.syaru.mecrosscable;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(CrossCableMod.ID)
public final class CrossCableMod {
    public static final String ID = "me_cross_cable";
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ID);
    public static final RegistryObject<MenuType<CrossCableMenu>> MENU = MENUS.register("routing",
            () -> IForgeMenuType.create((id, inventory, buffer) -> new CrossCableMenu(id, inventory, buffer.readBlockPos())));
    public static final RegistryObject<CrossCableBlock> GLASS = BLOCKS.register("cross_cable", () -> new CrossCableBlock(false, false));
    public static final RegistryObject<CrossCableBlock> SMART = BLOCKS.register("smart_cross_cable", () -> new CrossCableBlock(true, false));
    public static final RegistryObject<CrossCableBlock> DENSE = BLOCKS.register("dense_smart_cross_cable", () -> new CrossCableBlock(true, true));
    public static final RegistryObject<Item> GLASS_ITEM = ITEMS.register("cross_cable", () -> new BlockItem(GLASS.get(), new Item.Properties()));
    public static final RegistryObject<Item> SMART_ITEM = ITEMS.register("smart_cross_cable", () -> new BlockItem(SMART.get(), new Item.Properties()));
    public static final RegistryObject<Item> DENSE_ITEM = ITEMS.register("dense_smart_cross_cable", () -> new BlockItem(DENSE.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<CrossCableBlockEntity>> ENTITY = ENTITIES.register("cross_cable",
            () -> BlockEntityType.Builder.of(CrossCableBlockEntity::new, GLASS.get(), SMART.get(), DENSE.get()).build(null));
    private static final RegistryObject<CreativeModeTab> TAB = TABS.register("cables", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.me_cross_cable"))
            .icon(() -> SMART_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> { output.accept(GLASS_ITEM.get()); output.accept(SMART_ITEM.get()); output.accept(DENSE_ITEM.get()); })
            .build());

    public CrossCableMod() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); TABS.register(bus); MENUS.register(bus);
    }
}
