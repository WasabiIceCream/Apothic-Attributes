package dev.shadowsoffire.apothic_attributes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;

import dev.shadowsoffire.apothic_attributes.impl.AttributeEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * The NeoForge living-entity events Apothic Attributes listens to, re-created at the points NeoForge fires them.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHooksMixin {

    @Shadow
    protected int useItemRemaining;

    /**
     * LivingIncomingDamageEvent: after hurtServer's early exits (invulnerable, already dead, fire resistance), before
     * blocking, armor and the rest. Can change the amount or cancel the damage.
     */
    @Inject(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSleeping()Z"), cancellable = true)
    private void apoth_incomingDamage(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir,
        @Local(argsOnly = true) LocalFloatRef amount) {
        float result = AttributeEvents.onIncomingDamage((LivingEntity) (Object) this, source, amount.get());
        if (result < 0) {
            cir.setReturnValue(false);
            return;
        }
        amount.set(result);
    }

    /** LivingDamageEvent.Pre (only used to record health before the hit). */
    @Inject(method = "actuallyHurt", at = @At("HEAD"))
    private void apoth_preDamage(ServerLevel level, DamageSource source, float damage, CallbackInfo ci) {
        AttributeEvents.recordPreDamageHealth((LivingEntity) (Object) this);
    }

    /** LivingDamageEvent.Post: at the end, the amount is what health actually lost (after armor, protection and absorption). */
    @Inject(method = "actuallyHurt", at = @At("TAIL"))
    private void apoth_postDamage(ServerLevel level, DamageSource source, float damage, CallbackInfo ci, @Local(argsOnly = true) float healthDamage) {
        AttributeEvents.lifeStealOverheal((LivingEntity) (Object) this, source, healthDamage);
    }

    /** LivingHealEvent: scales the amount; a result of zero or less cancels the heal. */
    @Inject(method = "heal", at = @At("HEAD"), cancellable = true)
    private void apoth_heal(float heal, CallbackInfo ci, @Local(argsOnly = true) LocalFloatRef amount) {
        float result = AttributeEvents.heal((LivingEntity) (Object) this, amount.get());
        if (result <= 0) {
            ci.cancel();
            return;
        }
        amount.set(result);
    }

    /** LivingEntityUseItemEvent.Tick: may change the remaining use duration (draw speed). */
    @Inject(method = "updateUsingItem", at = @At("HEAD"))
    private void apoth_useItemTick(ItemStack stack, CallbackInfo ci) {
        if (!stack.isEmpty()) {
            this.useItemRemaining = AttributeEvents.drawSpeed((LivingEntity) (Object) this, stack, this.useItemRemaining);
        }
    }

    /** LivingExperienceDropEvent (experience gained). */
    @ModifyArg(method = "dropExperience", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"), index = 2)
    private int apoth_mobXp(int amount) {
        return AttributeEvents.mobXp((LivingEntity) (Object) this, amount);
    }

    /** EntityTickEvent.Post (aux damage trackers). */
    @Inject(method = "tick", at = @At("TAIL"))
    private void apoth_tickDmgTracker(CallbackInfo ci) {
        AttributeEvents.tickDmgTracker((LivingEntity) (Object) this);
    }
}
