package dev.shadowsoffire.apothic_attributes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import dev.shadowsoffire.apothic_attributes.impl.AttributeEvents;
import net.minecraft.world.level.block.Block;

/**
 * BlockDropsEvent's experience (experience gained, for the player breaking the block).
 */
@Mixin(Block.class)
public abstract class BlockMixin {

    @ModifyVariable(method = "popExperience", at = @At("HEAD"), argsOnly = true)
    private int apoth_blockXp(int amount) {
        return AttributeEvents.blockXp(amount);
    }
}
