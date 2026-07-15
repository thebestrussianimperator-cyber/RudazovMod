package com.poleesteel.rudazovmod.init;

import com.poleesteel.rudazovmod.blocks.BloodIronBlock;
import com.poleesteel.rudazovmod.blocks.BloodIronOre;
import com.poleesteel.rudazovmod.items.BloodIronIngot;
import com.poleesteel.rudazovmod.items.ItemBloodChain;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import com.poleesteel.rudazovmod.entities.EntityBloodChain;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import com.poleesteel.rudazovmod.client.render.entity.RenderBloodChain;
import net.minecraftforge.common.capabilities.CapabilityManager;
import com.poleesteel.rudazovmod.capabilities.IActiveSpirit;
import com.poleesteel.rudazovmod.capabilities.ActiveSpiritStorage;
import com.poleesteel.rudazovmod.capabilities.ActiveSpiritData;

@Mod.EventBusSubscriber
public class RegistryHandler {

    // Инициализируем объекты
    public static final Item BLOOD_IRON_INGOT = new BloodIronIngot("blood_iron_ingot");
    public static final Block BLOOD_IRON_BLOCK = new BloodIronBlock("blood_iron_block");
    public static final Block BLOOD_IRON_ORE = new BloodIronOre("blood_iron_ore");
    public static final Item BLOOD_CHAIN_ITEM = new ItemBloodChain("blood_chain");
    // 1. Регистрируем блоки в игре
    @SubscribeEvent
    public static void onBlockRegister(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(BLOOD_IRON_BLOCK, BLOOD_IRON_ORE);
    }

    // 2. Регистрируем предметы (и блоки как предметы для инвентаря)
    @SubscribeEvent
    public static void onItemRegister(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(BLOOD_IRON_INGOT);
        event.getRegistry().register(BLOOD_CHAIN_ITEM);

        // Делаем так, чтобы блоки можно было держать в руках и ставить
        event.getRegistry().register(new ItemBlock(BLOOD_IRON_BLOCK).setRegistryName(BLOOD_IRON_BLOCK.getRegistryName()));
        event.getRegistry().register(new ItemBlock(BLOOD_IRON_ORE).setRegistryName(BLOOD_IRON_ORE.getRegistryName()));
    }

    // 3. Регистрируем модели для рендеринга текстур (только на стороне клиента)
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onModelRegister(ModelRegistryEvent event) {
        registerModel(BLOOD_IRON_INGOT);
        registerModel(BLOOD_CHAIN_ITEM);

        registerModel(Item.getItemFromBlock(BLOOD_IRON_BLOCK));
        registerModel(Item.getItemFromBlock(BLOOD_IRON_ORE));

        // РЕГИСТРАЦИЯ ВИЗУАЛА СУЩНОСТИ
        RenderingRegistry.registerEntityRenderingHandler(EntityBloodChain.class, RenderBloodChain::new);
    }

    @SideOnly(Side.CLIENT)
    private static void registerModel(Item item) {
        ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
    @SubscribeEvent
    public static void onEntityRegister(RegistryEvent.Register<EntityEntry> event) {
        // Уникальный ID для энтити внутри мода (например, 1)
        int networkId = 1;

        EntityEntry chainEntry = EntityEntryBuilder.create()
                .entity(EntityBloodChain.class)
                .id(new ResourceLocation("rudazovmod", "blood_chain_entity"), networkId)
                .name("blood_chain_entity")
                .tracker(64, 1, true) // Как далеко видно, как часто обновляется, нужна ли синхронизация скорости
                .build();

        event.getRegistry().register(chainEntry);
    }

    public static void registerCapabilities() {
        CapabilityManager.INSTANCE.register(IActiveSpirit.class, new ActiveSpiritStorage(), ActiveSpiritData.class);
    }
}