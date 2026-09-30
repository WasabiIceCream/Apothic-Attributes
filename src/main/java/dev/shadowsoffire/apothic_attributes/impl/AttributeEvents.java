package dev.shadowsoffire.apothic_attributes.impl;

import java.util.Random;

import dev.shadowsoffire.apothic_attributes.ALConfig;
import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.apothic_attributes.api.ALObjects.Attachments;
import dev.shadowsoffire.apothic_attributes.payload.ConfigPayload;
import dev.shadowsoffire.apothic_attributes.payload.CritParticlePayload;
import dev.shadowsoffire.apothic_attributes.util.AttributesUtil;
import dev.shadowsoffire.apothic_attributes.util.AuxDmgTracker;
import dev.shadowsoffire.apothic_attributes.util.LEInvoker;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The attribute mechanics. Upstream subscribes these to NeoForge events; this port calls them from mixins
 * ({@code LivingEntityHooksMixin}, {@code PlayerMixin}, {@code ProjectileMixin}, {@code ServerLevelMixin},
 * {@code BlockMixin}) and Fabric API events ({@link #register()}), keeping upstream's handler order where
 * several shared one event. The logic inside each handler is upstream's.
 * <p>
 * Not ported (deferred with the modifiers API): bonus attribute modifier components and the display-only
 * Attack Range modifier. Vanilla elytra gliding works without upstream's Elytra Flight item modifier.
 */
public class AttributeEvents {

    /**
     * The player breaking a block right now, for {@link #blockXp}. Set by Fabric's break events; block breaking is
     * main-thread only.
     */
    private static Player blockBreaker;

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            blockBreaker = player;
            return true;
        });
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> blockBreaker = null);
        PlayerBlockBreakEvents.CANCELED.register((level, player, pos, state, blockEntity) -> blockBreaker = null);
        // A break that bails out between BEFORE and AFTER (e.g. the block couldn't be removed) fires neither AFTER nor
        // CANCELED; don't let that player's multiplier leak onto experience popped later by something else.
        ServerTickEvents.END_SERVER_TICK.register(server -> blockBreaker = null);

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return ApothicAttributes.loc("al_config");
            }

            @Override
            public void onResourceManagerReload(ResourceManager resourceManager) {
                ALConfig.makeReloader().onResourceManagerReload(resourceManager);
            }
        });

        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> ServerPlayNetworking.send(player, new ConfigPayload()));
    }

    private static boolean canBenefitFromDrawSpeed(ItemStack stack) {
        return stack.getItem() instanceof ProjectileWeaponItem || stack.getItem() instanceof TridentItem;
    }

    /**
     * This handler is the implementation for {@link ALObjects.Attributes#DRAW_SPEED}.<br>
     * Each full point of draw speed provides an extra using tick per game tick.<br>
     * Each partial point of draw speed provides an extra using tick periodically.
     *
     * @return The new remaining use duration.
     */
    public static int drawSpeed(LivingEntity entity, ItemStack item, int duration) {
        if (entity instanceof Player player) {
            double t = player.getAttribute(ALObjects.Attributes.DRAW_SPEED).getValue() - 1;
            if (t == 0 || !canBenefitFromDrawSpeed(item)) return duration;

            // Handle negative draw speed.
            int offset = -1;
            if (t < 0) {
                offset = 1;
                t = -t;
            }

            while (t > 1) { // Every 100% triggers an immediate extra tick
                duration += offset;
                t--;
            }

            if (t > 0.5F) { // Special case 0.5F so that values in (0.5, 1) don't round to 1.
                if (entity.tickCount % 2 == 0) duration += offset;
                t -= 0.5F;
            }

            int mod = (int) Math.floor(1 / Math.min(1, t));
            if (entity.tickCount % mod == 0) duration += offset;
        }
        return duration;
    }

    public static void recordPreDamageHealth(LivingEntity entity) {
        entity.setAttached(Attachments.PRE_DAMAGE_HEALTH, entity.getHealth());
    }

    /**
     * This handler manages the Life Steal and Overheal attributes.
     *
     * @param healthDamage The damage dealt to health, after armor, protection and absorption.
     */
    public static void lifeStealOverheal(LivingEntity target, DamageSource source, float healthDamage) {
        if (source.getDirectEntity() instanceof LivingEntity attacker && AttributesUtil.isPhysicalDamage(source)) {
            Float preHealth = target.getAttached(Attachments.PRE_DAMAGE_HEALTH);
            float oldEntityHealth = preHealth == null ? target.getHealth() : preHealth;
            float lifesteal = (float) attacker.getAttributeValue(ALObjects.Attributes.LIFE_STEAL);
            float dmg = Math.min(healthDamage, oldEntityHealth);
            if (lifesteal > 0.001) {
                attacker.heal(dmg * lifesteal);
            }
            float overheal = (float) attacker.getAttributeValue(ALObjects.Attributes.OVERHEAL);
            float maxOverheal = attacker.getMaxHealth() * 0.5F;
            if (overheal > 0 && attacker.getAbsorptionAmount() < maxOverheal) {
                // Overheal needs to bypass the max absorption attribute, which is used for natural absorption regeneration, but also clamps the total number of abs hearts.
                ((LEInvoker) attacker).apoth_setInternalAbsorption(Math.min(maxOverheal, attacker.getAbsorptionAmount() + dmg * overheal));
            }
        }
    }

    /**
     * Upstream's LivingIncomingDamageEvent listeners, in their priority order: projectile damage (HIGHEST), critical
     * strike and dodge (HIGH), then the melee damage attributes (LOWEST). A canceled event skips the rest, as NeoForge's
     * bus does.
     *
     * @return The new damage amount, or a negative value to cancel the damage.
     */
    public static float onIncomingDamage(LivingEntity target, DamageSource source, float amount) {
        amount = projDmg(source, amount);
        amount = apothCriticalStrike(target, source, amount);
        if (dodge(target, source)) return -1;
        if (meleeDamageAttributes(target, source)) return -1;
        return amount;
    }

    /**
     * Recursion guard for {@link #meleeDamageAttributes}.<br>
     * Doesn't need to be ThreadLocal as attack logic is main-thread only.
     */
    private static boolean noRecurse = false;

    /**
     * Applies the following melee damage attributes:<br>
     * <ul>
     * <li>{@link ALObjects.Attributes#CURRENT_HP_DAMAGE}</li>
     * <li>{@link ALObjects.Attributes#FIRE_DAMAGE}</li>
     * <li>{@link ALObjects.Attributes#COLD_DAMAGE}</li>
     * </ul>
     *
     * @return True if the original damage should be canceled (the target already died to aux damage).
     */
    private static boolean meleeDamageAttributes(LivingEntity target, DamageSource source) {
        if (target.level().isClientSide() || target.isDeadOrDying()) return false;
        if (noRecurse) return false;
        noRecurse = true;
        boolean cancel = false;
        try {
            if (source.getDirectEntity() instanceof LivingEntity attacker && AttributesUtil.isPhysicalDamage(source)) {
                AuxDmgTracker.executeWith(target, tracker -> {
                    float hpDmg = (float) attacker.getAttributeValue(ALObjects.Attributes.CURRENT_HP_DAMAGE) * target.getHealth();
                    tracker.attackWith(attacker, target, ALObjects.DamageTypes.CURRENT_HP_DAMAGE, hpDmg, null);
                    tracker.attackWith(attacker, target, ALObjects.DamageTypes.FIRE_DAMAGE, ALObjects.Attributes.FIRE_DAMAGE, AttributeEvents::applyPostFireDamage);
                    tracker.attackWith(attacker, target, ALObjects.DamageTypes.COLD_DAMAGE, ALObjects.Attributes.COLD_DAMAGE, AttributeEvents::applyPostColdDamage);
                });

                if (target.isDeadOrDying()) {
                    // Communicates back to PlayerMixin that the return value of hurt() should be true, so post-attack effects (like sweep attacks) are applied.
                    target.setAttached(Attachments.KILLED_BY_AUX_DMG, true);
                    // Cancel if the target is already dead, otherwise the original attack will go through and call die() twice.
                    cancel = true;
                }
            }
        }
        finally {
            noRecurse = false;
        }
        return cancel;
    }

    private static void applyPostFireDamage(LivingEntity attacker, LivingEntity target, DamageSource src, float dmg, float delta) {
        target.setRemainingFireTicks(target.getRemainingFireTicks() + (int) (10 * dmg));
    }

    private static void applyPostColdDamage(LivingEntity attacker, LivingEntity target, DamageSource src, float dmg, float delta) {
        int duration = (int) Math.min(150, 15 * dmg);
        int amp = Math.max(0, Mth.log2(Math.round(dmg / 5)));
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, amp));
    }

    /**
     * Handles {@link ALObjects.Attributes#CRIT_CHANCE} and {@link ALObjects.Attributes#CRIT_DAMAGE}
     */
    private static float apothCriticalStrike(LivingEntity target, DamageSource source, float amount) {
        LivingEntity attacker = source.getEntity() instanceof LivingEntity le ? le : null;
        if (attacker == null || source.is(ALObjects.Tags.CANNOT_CRITICALLY_STRIKE)) return amount;

        double critChance = attacker.getAttributeValue(ALObjects.Attributes.CRIT_CHANCE);
        float critDmg = (float) attacker.getAttributeValue(ALObjects.Attributes.CRIT_DAMAGE);

        RandomSource rand = target.getRandom();

        float damage = amount;

        // Roll for crits. Each overcrit reduces the effectiveness by 15%
        // We stop rolling when crit chance fails or the crit damage would reduce the total damage dealt.
        // Multicrits are additive with previous crits.
        while (rand.nextFloat() <= critChance && critDmg > 1.0F) {
            critChance--;
            damage += amount * (critDmg - 1);
            critDmg *= 0.85F;
        }

        if (damage > amount && attacker.level() instanceof ServerLevel level) {
            CritParticlePayload payload = new CritParticlePayload(target.getId());
            for (ServerPlayer player : PlayerLookup.tracking(level, target.chunkPosition())) {
                ServerPlayNetworking.send(player, payload);
            }
        }

        return damage;
    }

    /**
     * Handles {@link ALObjects.Attributes#CRIT_DAMAGE}'s interactions with vanilla critical strikes.
     *
     * @return The vanilla critical strike multiplier to use (vanilla's is 1.5).
     */
    public static float vanillaCritDmg(Player player, float multiplier) {
        float critDmg = (float) player.getAttributeValue(ALObjects.Attributes.CRIT_DAMAGE);
        return Math.max(multiplier, critDmg);
    }

    /**
     * This handler, and {@linkplain #mobXp the one below}, handle {@link ALObjects.Attributes#EXPERIENCE_GAINED}.
     * Called for experience popped by a block; applies when a player is breaking it.
     */
    public static int blockXp(int amount) {
        if (blockBreaker != null) {
            double xpMult = blockBreaker.getAttributeValue(ALObjects.Attributes.EXPERIENCE_GAINED);
            return (int) (amount * xpMult);
        }
        return amount;
    }

    public static int mobXp(LivingEntity dying, int amount) {
        Player player = dying.getLastHurtByPlayer();
        if (player == null) return amount;
        double xpMult = player.getAttributeValue(ALObjects.Attributes.EXPERIENCE_GAINED);
        return (int) (amount * xpMult);
    }

    /**
     * Handles {@link ALObjects.Attributes#HEALING_RECEIVED}
     *
     * @return The new heal amount; zero or less cancels the heal.
     */
    public static float heal(LivingEntity entity, float amount) {
        float factor = (float) entity.getAttributeValue(ALObjects.Attributes.HEALING_RECEIVED);
        return amount * factor;
    }

    /**
     * Handles {@link ALObjects.Attributes#ARROW_DAMAGE} and {@link ALObjects.Attributes#ARROW_VELOCITY}.
     * Called when an arrow is first added to a server level (never for arrows loaded from disk, so no "done" flag is needed).
     */
    public static void arrow(AbstractArrow arrow) {
        if (arrow.getOwner() instanceof LivingEntity le) {
            arrow.setBaseDamage(arrow.baseDamage * le.getAttributeValue(ALObjects.Attributes.ARROW_DAMAGE));
            arrow.setDeltaMovement(arrow.getDeltaMovement().scale(le.getAttributeValue(ALObjects.Attributes.ARROW_VELOCITY)));
        }
    }

    private static float projDmg(DamageSource src, float amount) {
        if (src.getDirectEntity() instanceof Projectile && src.getEntity() instanceof LivingEntity projOwner) {
            double projDmgMult = projOwner.getAttributeValue(ALObjects.Attributes.PROJECTILE_DAMAGE);
            return amount * (float) projDmgMult;
        }
        return amount;
    }

    /**
     * Handles {@link ALObjects.Attributes#DODGE_CHANCE} for melee attacks.
     *
     * @return True if the attack was dodged.
     */
    private static boolean dodge(LivingEntity target, DamageSource source) {
        if (target.level().isClientSide()) return false;
        Entity attacker = source.getDirectEntity();
        if (attacker instanceof Player player) {
            double atkRange = player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
            double atkRangeSqr = atkRange * atkRange;
            if (attacker.distanceToSqr(target) <= atkRangeSqr && isDodging(target)) {
                onDodge(target);
                return true;
            }
        }
        else if (attacker instanceof Mob mob) {
            if (mob.isWithinMeleeAttackRange(target) && isDodging(target)) {
                onDodge(target);
                return true;
            }
        }
        return false;
    }

    /**
     * Handles {@link ALObjects.Attributes#DODGE_CHANCE} for projectiles, and the MinecraftForge#9370 fix upstream applies
     * to canceled impacts: a piercing arrow that was dodged must not hit the same entity again.
     *
     * @return True if the impact was dodged (and should be canceled).
     */
    public static boolean dodgeProjectile(Projectile proj, HitResult hit) {
        Entity target = hit instanceof EntityHitResult entRes ? entRes.getEntity() : null;
        if (target instanceof LivingEntity lvTarget && !lvTarget.level().isClientSide()) {
            // We can skip the distance check for projectiles, as "Projectile Impact" means the projectile is on the target.
            if (isDodging(lvTarget)) {
                onDodge(lvTarget);
                if (proj instanceof AbstractArrow arrow && arrow.getPierceLevel() > 0) {
                    if (arrow.piercingIgnoreEntityIds == null) {
                        arrow.piercingIgnoreEntityIds = new IntOpenHashSet(arrow.getPierceLevel());
                    }
                    arrow.piercingIgnoreEntityIds.add(target.getId());
                }
                return true;
            }
        }
        return false;
    }

    private static void onDodge(LivingEntity target) {
        target.level().playSound(null, target, ALObjects.Sounds.DODGE, SoundSource.NEUTRAL, 1, 0.7F + target.getRandom().nextFloat() * 0.3F);
        if (target.level() instanceof ServerLevel sl) {
            double height = target.getBbHeight();
            double width = target.getBbWidth();
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, target.getX() - width / 4, target.getY(), target.getZ() - width / 4, 6, -width / 4, height / 8, -width / 4, 0);
        }
    }

    public static void tickDmgTracker(LivingEntity entity) {
        if (!entity.level().isClientSide() && entity.hasAttached(Attachments.AUX_DMG_TRACKER)) {
            entity.getAttached(Attachments.AUX_DMG_TRACKER).tick();
        }
    }

    /**
     * Random used for dodge calculations.<br>
     * This random is seeded with the target entity's tick count before use.
     */
    private static Random dodgeRand = new Random();

    /**
     * Computes the dodge random seed for the entity. This seed is only unique for the current tick, so that
     * multiple damage instances in the same tick are all dodged.
     * <p>
     * Without this, it would be possible for multiple-instances attacks to only be partially dodged.
     *
     * @param target The entity being attecked who is rolling to dodge.
     * @return The random seed to use when computing the dodge roll
     */
    public static int computeDodgeSeed(LivingEntity target) {
        int delta = 0x9E3779B9;
        int base = target.tickCount + target.getUUID().hashCode();
        return base + delta + (base << 6) + (base >> 2);
    }

    /**
     * Checks if the target entity will dodge attacks in the current tick, by checking the {@link ALObjects.Attributes#DODGE_CHANCE} value and rolling a random.
     *
     * @param target The entity being attecked who is rolling to dodge.
     * @return True if the target may dodge, false otherwise.
     */
    public static boolean isDodging(LivingEntity target) {
        double chance = target.getAttributeValue(ALObjects.Attributes.DODGE_CHANCE);
        dodgeRand.setSeed(computeDodgeSeed(target));
        return dodgeRand.nextFloat() <= chance;
    }
}
