package dev.shadowsoffire.apothic_attributes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apothic_attributes.impl.AttributeEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

/**
 * EntityJoinLevelEvent for fresh arrows only (loaded ones never pass through addFreshEntity), for arrow damage and velocity.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Inject(method = "addFreshEntity", at = @At("HEAD"))
    private void apoth_arrow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof AbstractArrow arrow) {
            AttributeEvents.arrow(arrow);
        }
    }
}
