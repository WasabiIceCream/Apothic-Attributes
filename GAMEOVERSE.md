# Apothic Attributes, Fabric port (Gameoverse)

Fork of `github.com/Shadows-of-Fire/Apothic-Attributes` (branch `26.1`, 3.0.1, MIT code; the art in
`assets/` is "All Rights Reserved" per upstream's LICENSE_ASSETS) ported to Fabric for 26.1.2.
Branch `fabric-26.1`; `origin` push URL disabled. Needs our `placebo-fabric` 0.1.1+ (config system).
Built for our Apotheosis port: 15 of Apotheosis's 21 gems and 24 of its affixes need these attributes.

## What's ported (first version)
- All 21 attributes, added to every living entity (`LivingEntityAttributesMixin`, replacing
  NeoForge's EntityAttributeModificationEvent), the 6 effects (Bleeding, Detonation, Grievous,
  Knowledge, Sundering, Vitality), the 5 damage types, the crit particle and dodge sound, the
  cooldown/aux-damage data (Fabric attachments), the config (Placebo `.cfg`) and its sync payload.
- Every mechanic in `impl/AttributeEvents`, logic unchanged, hooked where NeoForge fires its events:
  incoming damage (`hurtServer` at `isSleeping()`), pre/post damage (`actuallyHurt` head/tail), heal,
  item-use tick (draw speed), mob XP (`dropExperience`), block XP (`Block.popExperience` with the
  breaking player from Fabric's break events), fresh arrows (`ServerLevel.addFreshEntity`), projectile
  impact (`Projectile.hitTargetOrDeflectSelf`), vanilla crit multiplier (`Player.attack`'s 1.5F).
- Upstream's own mixins (combat formulas, sundering, elytra flight, sweep/aux kill flags, gravity fall
  damage, trident damage, follow range). `doSweepAttack` targets vanilla's 4-argument signature
  (upstream targets NeoForge's patched one). NeoForge's `neoforge:is_physical/is_magic/is_poison/
  is_wither` damage tags are shipped with NeoForge's default content.

## Deferred (in `deferred/`, not compiled)
Attributes GUI and its tooltips/command, the StackAttributeModifiers API and bonus-modifier command,
Curios compat, JEI plugin, datagen, and the potions (plus the Flying effect they use). Percent-style
attribute tooltips aren't done yet (values show as decimals).

## Gameoverse config
`gameoverse/apothic_attributes.cfg` is the deployed `config/apotheosis/apothic_attributes.cfg`: armor
and protection use vanilla's formulas (checked against 26.1's `CombatRules` bytecode), so balance only
changes where pierce/shred apply. Upstream's defaults are Apothic's own curves.

## Tested (2026-09-28, local server, `/damage` between NoAI husks)
Crit x2, dodge, life steal, overheal, fire (+burning), cold (+slowness), current-HP damage, armor
formula, armor pierce/shred, protection (+Prot IV) and prot pierce, healing received, Sundering,
Bleeding, Grievous: all exact. Needs a player: arrow damage/velocity, draw speed, XP gained, elytra
flight, crit particle, vanilla crit multiplier. Note: with no player online the server pauses after
60 s (`pause-when-empty-seconds`), freezing entities; test harnesses must account for it.
