package dev.shadowsoffire.apothic_attributes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apothic_attributes.impl.AttributeEvents;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.phys.HitResult;

/**
 * ProjectileImpactEvent: a dodged projectile doesn't hit (it keeps flying, as a canceled impact does on NeoForge).
 */
@Mixin(Projectile.class)
public abstract class ProjectileMixin {

    @Inject(method = "hitTargetOrDeflectSelf", at = @At("HEAD"), cancellable = true)
    private void apoth_dodge(HitResult hit, CallbackInfoReturnable<ProjectileDeflection> cir) {
        if (AttributeEvents.dodgeProjectile((Projectile) (Object) this, hit)) {
            cir.setReturnValue(ProjectileDeflection.NONE);
        }
    }
}
