package com.poleesteel.rudazovmod.init;

import com.poleesteel.rudazovmod.blocks.BloodIronBlock;
import com.poleesteel.rudazovmod.blocks.BloodIronOre;
import com.poleesteel.rudazovmod.items.BloodIronIngot;
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

@Mod.EventBusSubscriber
public class RegistryHandler {

    // Инициализируем объекты
    public static final Item BLOOD_IRON_INGOT = new BloodIronIngot("blood_iron_ingot");
    public static final Block BLOOD_IRON_BLOCK = new BloodIronBlock("blood_iron_block");
    public static final Block BLOOD_IRON_ORE = new BloodIronOre("blood_iron_ore");

    // 1. Регистрируем блоки в игре
    @SubscribeEvent
    public static void onBlockRegister(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(BLOOD_IRON_BLOCK, BLOOD_IRON_ORE);
    }

    // 2. Регистрируем предметы (и блоки как предметы для инвентаря)
    @SubscribeEvent
    public static void onItemRegister(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(BLOOD_IRON_INGOT);

        // Делаем так, чтобы блоки можно было держать в руках и ставить
        event.getRegistry().register(new ItemBlock(BLOOD_IRON_BLOCK).setRegistryName(BLOOD_IRON_BLOCK.getRegistryName()));
        event.getRegistry().register(new ItemBlock(BLOOD_IRON_ORE).setRegistryName(BLOOD_IRON_ORE.getRegistryName()));
    }

    // 3. Регистрируем модели для рендеринга текстур (только на стороне клиента)
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onModelRegister(ModelRegistryEvent event) {
        registerModel(BLOOD_IRON_INGOT);
        registerModel(Item.getItemFromBlock(BLOOD_IRON_BLOCK));
        registerModel(Item.getItemFromBlock(BLOOD_IRON_ORE));
    }

    @SideOnly(Side.CLIENT)
    private static void registerModel(Item item) {
        ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
}