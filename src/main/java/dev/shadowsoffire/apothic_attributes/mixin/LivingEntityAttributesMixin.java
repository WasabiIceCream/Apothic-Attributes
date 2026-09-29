package dev.shadowsoffire.apothic_attributes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

/**
 * Upstream's EntityAttributeModificationEvent handler: adds the attributes to every living entity type. Every
 * player and mob attribute builder starts from {@code createLivingAttributes}, so this covers modded mobs too.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityAttributesMixin {

    @Inject(method = "createLivingAttributes", at = @At("RETURN"))
    private static void apoth_addAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        cir.getReturnValue()
            .add(ALObjects.Attributes.DRAW_SPEED)
            .add(ALObjects.Attributes.CRIT_CHANCE)
            .add(ALObjects.Attributes.CRIT_DAMAGE)
            .add(ALObjects.Attributes.COLD_DAMAGE)
            .add(ALObjects.Attributes.FIRE_DAMAGE)
            .add(ALObjects.Attributes.LIFE_STEAL)
            .add(ALObjects.Attributes.CURRENT_HP_DAMAGE)
            .add(ALObjects.Attributes.OVERHEAL)
            .add(ALObjects.Attributes.GHOST_HEALTH)
            .add(ALObjects.Attributes.ARROW_DAMAGE)
            .add(ALObjects.Attributes.ARROW_VELOCITY)
            .add(ALObjects.Attributes.EXPERIENCE_GAINED)
            .add(ALObjects.Attributes.HEALING_RECEIVED)
            .add(ALObjects.Attributes.ARMOR_PIERCE)
            .add(ALObjects.Attributes.ARMOR_SHRED)
            .add(ALObjects.Attributes.PROJECTILE_DAMAGE)
            .add(ALObjects.Attributes.PROT_PIERCE)
            .add(ALObjects.Attributes.PROT_SHRED)
            .add(ALObjects.Attributes.DODGE_CHANCE)
            .add(ALObjects.Attributes.ELYTRA_FLIGHT)
            .add(ALObjects.Attributes.COOLDOWN_REDUCTION);
    }
}
