package com.poleesteel.rudazovmod.items;

import com.google.common.collect.Multimap;
import com.poleesteel.rudazovmod.entities.EntityBloodChain;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;

import java.util.UUID;

public class ItemBloodChain extends Item {

    private static final UUID ATTACK_DAMAGE_MODIFIER = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    private static final UUID ATTACK_SPEED_MODIFIER = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");

    public ItemBloodChain(String name) {
        setTranslationKey(name);
        setRegistryName(name);
        setCreativeTab(CreativeTabs.COMBAT); // Логично положить цепь во вкладку "Оружие"
        setMaxStackSize(1); // Оружие обычно не стакается по 64 штуки
    }

    // Этот метод вызывается, когда игрок кликает ПКМ по какому-либо живому существу
    @Override
    public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer playerIn, EntityLivingBase target, EnumHand hand) {

        if (playerIn.getCooldownTracker().hasCooldown(this)) {
            return false;
        }

        if (!playerIn.world.isRemote) {

            // --- НОВОЕ: ОГРАНИЧЕНИЕ В 1 ЦЕЛЬ ---
            // Ищем в мире все существующие цепи, у которых ты являешься хозяином, и удаляем их
            for (EntityBloodChain oldChain : playerIn.world.getEntities(EntityBloodChain.class, e -> e.getOwner() == playerIn)) {
                oldChain.setDead();
            }
            // -----------------------------------

            EntityBloodChain chain = new EntityBloodChain(playerIn.world, target, playerIn);
            playerIn.world.spawnEntity(chain);

            playerIn.world.playSound(null, playerIn.posX, playerIn.posY, playerIn.posZ,
                    SoundEvents.ITEM_ARMOR_EQUIP_CHAIN, SoundCategory.PLAYERS, 1.0F, 1.0F);

            playerIn.getCooldownTracker().setCooldown(this, 60);
        }

        return true;
    }
    // 1. Задаем базовый урон и скорость атаки для ЛКМ
    @Override
    public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot equipmentSlot) {
        Multimap<String, AttributeModifier> multimap = super.getItemAttributeModifiers(equipmentSlot);

        if (equipmentSlot == EntityEquipmentSlot.MAINHAND) {
            // Урон (например, 6.0D — это 3 сердечка, как у железного меча)
            multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", 6.0D, 0));
            // Скорость атаки (отрицательное значение. -2.4D означает среднюю скорость между мечом и топором)
            multimap.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", -2.4D, 0));
        }

        return multimap;
    }

    // 2. Делаем что-то особенное при попадании ЛКМ (Опционально)
    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        // Здесь можно добавить эффект при ударе.
        // Например, звук или наложение эффекта кровотечения (иссушения).

        // Расходуем прочность предмета при ударе
        stack.damageItem(1, attacker);
        return true;
    }
}