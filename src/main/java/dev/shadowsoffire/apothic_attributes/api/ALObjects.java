package dev.shadowsoffire.apothic_attributes.api;

import static dev.shadowsoffire.apothic_attributes.ApothicAttributes.R;


import org.jetbrains.annotations.ApiStatus;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.apothic_attributes.mob_effect.BleedingEffect;
import dev.shadowsoffire.apothic_attributes.mob_effect.DetonationEffect;
import dev.shadowsoffire.apothic_attributes.mob_effect.GrievousEffect;
import dev.shadowsoffire.apothic_attributes.mob_effect.KnowledgeEffect;
import dev.shadowsoffire.apothic_attributes.mob_effect.SunderingEffect;
import dev.shadowsoffire.apothic_attributes.mob_effect.VitalityEffect;
import dev.shadowsoffire.apothic_attributes.util.AuxDmgTracker;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.projectile.Projectile;

public class ALObjects {


    public static class Attributes {

        /**
         * Flat armor penetration. Base value = (0.0) = 0 armor reduced during damage calculations.
         */
        public static final Holder<Attribute> ARMOR_PIERCE = R.attribute("armor_pierce", () -> new RangedAttribute("apothic_attributes:armor_pierce", 0.0D, 0.0D, 1000.0D).setSyncable(true));

        /**
         * Percentage armor reduction. Base value = (0.0) = 0% of armor reduced during damage calculations.
         */
        public static final Holder<Attribute> ARMOR_SHRED = R.attribute("armor_shred", () -> new PercentageAttribute("apothic_attributes:armor_shred", 0.0D, 0.0D, 2.0D).setSyncable(true));

        /**
         * Arrow Damage. Base value = (1.0) = 100% default arrow damage
         */
        public static final Holder<Attribute> ARROW_DAMAGE = R.attribute("arrow_damage", () -> new PercentageAttribute("apothic_attributes:arrow_damage", 1.0D, 0.0D, 10.0D).setSyncable(true));

        /**
         * Arrow Velocity. Base value = (1.0) = 100% default arrow velocity
         * <p>
         * Arrow damage scales with the velocity as well as {@link #ARROW_DAMAGE} and the base damage of the arrow entity.
         */
        public static final Holder<Attribute> ARROW_VELOCITY = R.attribute("arrow_velocity", () -> new PercentageAttribute("apothic_attributes:arrow_velocity", 1.0D, 0.0D, 10.0D).setSyncable(true));

        /**
         * Bonus magic damage that slows enemies hit. Base value = (0.0) = 0 damage
         */
        public static final Holder<Attribute> COLD_DAMAGE = R.attribute("cold_damage", () -> new RangedAttribute("apothic_attributes:cold_damage", 0.0D, 0.0D, 1000.0D).setSyncable(true));

        /**
         * Chance that any attack will critically strike. Base value = (0.05) = 5% chance to critically strike.
         * <p>
         * Not related to vanilla (jump) critical strikes.
         */
        // Gameoverse: default 0 (upstream 0.05). The server already has a separate 5% baseline crit; this chance comes only from gear.
        public static final Holder<Attribute> CRIT_CHANCE = R.attribute("crit_chance", () -> new PercentageAttribute("apothic_attributes:crit_chance", 0.0D, 0.0D, 10.0D).setSyncable(true));

        /**
         * Amount of damage caused by critical strikes. Base value = (1.5) = 150% normal damage dealt.
         * <p>
         * Also impacts vanilla (jump) critical strikes.
         */
        public static final Holder<Attribute> CRIT_DAMAGE = R.attribute("crit_damage", () -> new PercentageAttribute("apothic_attributes:crit_damage", 1.5D, 1.0D, 100.0D).setSyncable(true));

        /**
         * Bonus physical damage dealt equal to enemy's current health. Base value = (0.0) = 0%
         */
        public static final Holder<Attribute> CURRENT_HP_DAMAGE = R.attribute("current_hp_damage", () -> new PercentageAttribute("apothic_attributes:current_hp_damage", 0.0D, 0.0D, 1.0D).setSyncable(true));

        /**
         * Chance to dodge incoming melee damage. Base value = (0.0) = 0% chance to dodge.
         * "Melee" damage is considered as damage from another entity within the player's attack range.
         * <p>
         * This includes projectile attacks, as long as the projectile actually impacts the player.
         */
        public static final Holder<Attribute> DODGE_CHANCE = R.attribute("dodge_chance", () -> new PercentageAttribute("apothic_attributes:dodge_chance", 0.0D, 0.0D, 1.0D).setSyncable(true));

        /**
         * How fast a ranged weapon is charged. Base Value = (1.0) = 100% default draw speed.
         */
        public static final Holder<Attribute> DRAW_SPEED = R.attribute("draw_speed", () -> new PercentageAttribute("apothic_attributes:draw_speed", 1.0D, 0.0D, 4.0D).setSyncable(true));

        /**
         * Experience mulitplier, from killing mobs or breaking ores. Base value = (1.0) = 100% xp gained.
         */
        public static final Holder<Attribute> EXPERIENCE_GAINED = R.attribute("experience_gained", () -> new PercentageAttribute("apothic_attributes:experience_gained", 1.0D, 0.0D, 1000.0D).setSyncable(true));

        /**
         * Bonus magic damage that burns enemies hit. Base value = (0.0) = 0 damage
         */
        public static final Holder<Attribute> FIRE_DAMAGE = R.attribute("fire_damage", () -> new RangedAttribute("apothic_attributes:fire_damage", 0.0D, 0.0D, 1000.0D).setSyncable(true));

        /**
         * Extra health that regenerates when not taking damage. Base value = (0.0) = 0 damage
         */
        public static final Holder<Attribute> GHOST_HEALTH = R.attribute("ghost_health", () -> new RangedAttribute("apothic_attributes:ghost_health", 0.0D, 0.0D, 1000.0D).setSyncable(true));

        /**
         * Adjusts all healing received. Base value = (1.0) = 100% incoming healing.
         */
        public static final Holder<Attribute> HEALING_RECEIVED = R.attribute("healing_received", () -> new PercentageAttribute("apothic_attributes:healing_received", 1.0D, 0.0D, 1000.0D).setSyncable(true));

        /**
         * Percent of physical damage dealt converted to health. Base value = (0.0) = 0%
         */
        public static final Holder<Attribute> LIFE_STEAL = R.attribute("life_steal", () -> new PercentageAttribute("apothic_attributes:life_steal", 0.0D, 0.0D, 10.0D).setSyncable(true));

        /**
         * Percent of physical damage converted to absorption hearts. Base value = (0.0) = 0%
         */
        public static final Holder<Attribute> OVERHEAL = R.attribute("overheal", () -> new PercentageAttribute("apothic_attributes:overheal", 0.0D, 0.0D, 10.0D).setSyncable(true));

        /**
         * Generic Projectile Damage. Base value = (1.0) = 100% default projectile damage.
         * <p>
         * Applies to any incoming damage where the direct attacking entity is a {@link Projectile}.
         */
        public static final Holder<Attribute> PROJECTILE_DAMAGE = R.attribute("projectile_damage", () -> new PercentageAttribute("apothic_attributes:projectile_damage", 1.0D, 0.0D, 10.0D).setSyncable(true));

        /**
         * Flat protection penetration. Base value = (0.0) = 0 protection points bypassed during damage calculations.
         */
        public static final Holder<Attribute> PROT_PIERCE = R.attribute("prot_pierce", () -> new RangedAttribute("apothic_attributes:prot_pierce", 0.0D, 0.0D, 34.0D).setSyncable(true));

        /**
         * Percentage protection reduction. Base value = (0.0) = 0% of protection points bypassed during damage calculations.
         */
        public static final Holder<Attribute> PROT_SHRED = R.attribute("prot_shred", () -> new PercentageAttribute("apothic_attributes:prot_shred", 0.0D, 0.0D, 1.0D).setSyncable(true));

        /**
         * Boolean attribute for if elytra flight is enabled. Default value = false.
         */
        public static final Holder<Attribute> ELYTRA_FLIGHT = R.attribute("elytra_flight", () -> new BooleanAttribute("apothic_attributes:elytra_flight", false).setSyncable(true));

        /**
         * Multiplicative reduction to ability and effect cooldowns tracked through {@link AbilityCooldowns}.
         * <p>
         * Base value = (0.0) = no reduction. 0.25 = cooldowns last 75% as long. Capped at 0.95 (95% reduction); negative values lengthen cooldowns.
         */
        public static final Holder<Attribute> COOLDOWN_REDUCTION = R.attribute("cooldown_reduction",
            () -> new PercentageAttribute("apothic_attributes:cooldown_reduction", 0.0D, -10.0D, 0.95D).setSyncable(true));

        private static void bootstrap() {}
    }

    public static class MobEffects extends net.minecraft.world.effect.MobEffects { // Hack to bring vanilla things in-scope

        /**
         * Bleeding inflicts 1 + level damage every two seconds. Things that apply bleeding usually stack.
         */
        public static final Holder<MobEffect> BLEEDING = R.effect("bleeding", BleedingEffect::new);

        /**
         * Flaming Detonation, when it expires, consumes all fire ticks and deals armor-piercing damage based on the duration.
         */
        public static final Holder<MobEffect> DETONATION = R.effect("detonation", DetonationEffect::new);

        /**
         * Grievous Wounds reduces healing received by 40%/level.
         */
        public static final Holder<MobEffect> GRIEVOUS = R.effect("grievous", GrievousEffect::new);

        /**
         * Ancient Knowledge multiplies experience dropped by mobs by level * {@link MobFxLib#knowledgeMult}.<br>
         * The multiplier is configurable.
         */
        public static final Holder<MobEffect> KNOWLEDGE = R.effect("knowledge", KnowledgeEffect::new);

        /**
         * Sundering is the inverse of resistance. It increases damage taken by 20%/level.<br>
         * Each point of sundering cancels out a single point of resistance, if present.
         */
        public static final Holder<MobEffect> SUNDERING = R.effect("sundering", SunderingEffect::new);

        /**
         * Bursting Vitality increases healing received by 20%/level.
         */
        public static final Holder<MobEffect> VITALITY = R.effect("vitality", VitalityEffect::new);

        /**
         * Grants Creative Flight
         */

        private static void bootstrap() {}
    }

    public static class Particles {

        public static final SimpleParticleType APOTH_CRIT = R.simpleParticle("apoth_crit", false);

        private static void bootstrap() {}

    }

    public static class Sounds {

        public static final SoundEvent DODGE = R.sound("dodge");

        private static void bootstrap() {}

    }

    public static class DamageTypes {

        /**
         * Damage type used by {@link MobEffects#BLEEDING}. Bypasses armor.
         */
        public static final ResourceKey<DamageType> BLEEDING = ResourceKey.create(Registries.DAMAGE_TYPE, ApothicAttributes.loc("bleeding"));

        /**
         * Damage type used by {@link MobEffects#DETONATION}. Bypasses armor, and is marked as magic damage.
         */
        public static final ResourceKey<DamageType> DETONATION = ResourceKey.create(Registries.DAMAGE_TYPE, ApothicAttributes.loc("detonation"));

        /**
         * Damage type used by {@link Attributes#CURRENT_HP_DAMAGE}. Same properties as generic physical damage. Has attacker context.
         */
        public static final ResourceKey<DamageType> CURRENT_HP_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, ApothicAttributes.loc("current_hp_damage"));

        /**
         * Damage type used by {@link Attributes#FIRE_DAMAGE}. Bypasses armor, and is marked as magic damage. Has attacker context.<br>
         * Not marked as fire damage until fire resistance is reworked to not block all fire damage.
         */
        public static final ResourceKey<DamageType> FIRE_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, ApothicAttributes.loc("fire_damage"));

        /**
         * Damage type used by {@link Attributes#COLD_DAMAGE}. Bypasses armor, and is marked as magic damage. Has attacker context.
         */
        public static final ResourceKey<DamageType> COLD_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, ApothicAttributes.loc("cold_damage"));

        private static void bootstrap() {}
    }



    public static class Attachments {

        /** Health right before damage is applied, read by life steal. Not saved. */
        public static final AttachmentType<Float> PRE_DAMAGE_HEALTH = AttachmentRegistry.create(ApothicAttributes.loc("pre_damage_health"));

        public static final AttachmentType<AuxDmgTracker> AUX_DMG_TRACKER = AttachmentRegistry.create(ApothicAttributes.loc("aux_dmg_tracker"),
            b -> b.initializer(AuxDmgTracker::new).persistent(AuxDmgTracker.CODEC));

        public static final AttachmentType<CooldownTracker> COOLDOWNS = AttachmentRegistry.create(ApothicAttributes.loc("cooldowns"),
            b -> b.initializer(CooldownTracker::new)
                .persistent(CooldownTracker.CODEC.codec())
                .syncWith(CooldownTracker.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
                .copyOnDeath());

        /** Replaces upstream's "apoth.killed_by_aux_dmg" persistent-data flag (read by PlayerMixin). Not saved. */
        public static final AttachmentType<Boolean> KILLED_BY_AUX_DMG = AttachmentRegistry.create(ApothicAttributes.loc("killed_by_aux_dmg"));

        /** Replaces upstream's "apoth.hit_by_sweep_attack" persistent-data flag. Not saved. */
        public static final AttachmentType<Boolean> HIT_BY_SWEEP_ATTACK = AttachmentRegistry.create(ApothicAttributes.loc("hit_by_sweep_attack"));

        private static void bootstrap() {}
    }

    public static class Tags {

        /**
         * An attribute with a dynamic base cannot have its value computed out of context, and is instead treated as a list of modifiers
         * that will be applied when the event occurs. The applied modifiers will use the normal rules of {@link Operation} but on the dynamic base.
         */
        public static final TagKey<Attribute> DYNAMIC_BASE_ATTRIBUTES = TagKey.create(Registries.ATTRIBUTE, ApothicAttributes.loc("dynamic_base"));

        /**
         * This tag is a joined tag of all non-physical damage type tags (magic, fire, lightning, explosions, etc).
         * <p>
         * It is used to determine if a damage type is physical or not.
         */
        public static final TagKey<DamageType> IS_NON_PHYSICAL = TagKey.create(Registries.DAMAGE_TYPE, ApothicAttributes.loc("is_non_physical"));

        /**
         * Damage Types with this tag are immune from any processing by {@link Attributes#CRIT_CHANCE} and {@link Attributes#CRIT_DAMAGE}.
         */
        public static final TagKey<DamageType> CANNOT_CRITICALLY_STRIKE = TagKey.create(Registries.DAMAGE_TYPE, ApothicAttributes.loc("cannot_critically_strike"));

        /**
         * NeoForge's physical damage tag. NeoForge ships its base content; on Fabric this mod ships it (with NeoForge's defaults) under the
         * same id, so datapacks written for NeoForge keep working.
         */
        public static final TagKey<DamageType> IS_PHYSICAL = TagKey.create(Registries.DAMAGE_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("neoforge", "is_physical"));
    }



    @ApiStatus.Internal
    public static void bootstrap() {
        Attributes.bootstrap();
        MobEffects.bootstrap();
        Particles.bootstrap();
        Sounds.bootstrap();
        DamageTypes.bootstrap();
        Attachments.bootstrap();
    }
}
