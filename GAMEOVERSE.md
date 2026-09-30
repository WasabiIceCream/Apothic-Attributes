# Apothic Attributes, Fabric port (Gameoverse)

Fork of `github.com/Shadows-of-Fire/Apothic-Attributes` (branch `26.1`, 3.0.1, MIT code; the art in
`assets/` is "All Rights Reserved" per upstream's LICENSE_ASSETS) ported to Fabric for 26.1.2.
Branch `fabric-26.1`; `origin` push URL disabled; published at `github.com/WasabiIceCream/Apothic-Attributes` (remote `wasabi`). Needs our `placebo-fabric` 0.1.1+ (config system).
Crit Chance defaults to 0 here (upstream 0.05): the server already has a separate 5% baseline crit.
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
The StackAttributeModifiers API and the `/apoth bonus_modifier` command (with its bonus-modifier components), Curios
compat, datagen, and the Flying effect with its three potions. See the wiring audit below for why each is skipped.

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

## Wiring audit (2026-09-30, 3.0.1-fabric.6)

Every integration point upstream `26.1` (3.0.1, `20acf56`+JEI commit `bd1d3a5`) has, and what calls the port's code at
runtime. Line numbers are in `src/main/java/dev/shadowsoffire/apothic_attributes/`. "Bridge" is
`mod-dev/gameoverse-attribute-bridge` (its `ApothicEventsMixin` hooks the method names below; keep their signatures).
54 rows (some group several hooks): 32 wired, 11 fixed now (9 missing, 2 wired but wrong), 11 skipped with a reason.

### Setup, registration, networking
| Upstream point | Port wiring | Status |
|---|---|---|
| `EntityAttributeModificationEvent`: 21 attributes on every living type | `mixin/LivingEntityAttributesMixin:19` (`createLivingAttributes` RETURN; every mob/player builder starts there) | wired |
| `AttackEntityEvent` -> `localAtkStrength` | `mixin/PlayerMixin:32` (`Player.attack` HEAD) | wired |
| `PlayerLoggedInEvent` -> prune cooldowns | `ApothicAttributes` `ServerPlayConnectionEvents.JOIN` | wired |
| Common setup: `ALConfig.load`, Blindness -75% Follow Range | `ApothicAttributes#onInitialize` | wired |
| Common setup: player attributes syncable | `ServerLifecycleEvents.SERVER_STARTING` | wired |
| Registrations: attributes, 6 effects, particle, sound, attachments | Placebo `DeferredHelper` (immediate) + Fabric attachments | wired |
| Potions (33) | `api/ALObjects.Potions` | **fixed**: 30 registered (not the 3 Flying). `apothic_attributes:wither` is used by Apotheosis's Withering Archer elite and did not exist |
| Brewing mixes (`placebo/brewing_mixes`, 37) | Placebo 0.1.3 `MixRegistry`, data in `src/main/resources` | **fixed**: 34 (not the 3 Flying ones). Was in `src/generated`, which the build never packaged |
| Flying effect (NeoForge `CREATIVE_FLIGHT` attribute) | none | skipped: no creative-flight attribute on Fabric here (Apotheosis's port owns `apotheosis:creative_flight`); only the Flying potions used it |
| Bonus-modifier data components | none | skipped: only set by the `/apoth bonus_modifier` admin command |
| EquipmentSlot registries, `StackAttributeModifiers(Event)` | none | skipped: API only; our Apotheosis port hooks `ItemStack#forEachModifier` itself |
| `CritParticlePayload`, `ConfigPayload` | Placebo `PayloadHelper` (+ client receivers from `PlaceboClient`) | wired |
| `OnDatapackSyncEvent` -> `ConfigPayload` | `impl/AttributeEvents#register` `SYNC_DATA_PACK_CONTENTS` | wired |
| `AddServerReloadListenersEvent` (`al_config`) | `impl/AttributeEvents#register` `ResourceManagerHelper` | wired |
| `RegisterCommandsEvent`: `/apoth` root, `ApotheosisCommandEvent`, `bonus_modifier` | none | skipped: our Apotheosis port registers `/apoth` itself; `bonus_modifier` is an admin tool (vanilla `attribute_modifiers` covers it) |
| Curios compat (slots) | none | skipped: Trinkets here; Trinkets applies its own modifiers |
| Curios compat (GUI modifier sources) -> Trinkets | `compat/TrinketsModifierSources` (registered from `AttributesLibClient` when `trinkets` is loaded; compiled against `reference-jars/trinkets-4.0.1+26.1.jar`) | **fixed**: items in Trinkets slots are listed with their icon as the source of their modifiers (were "unknown"). Uses Trinkets' impl `TrinketUtilities.forEachModifier`, the call Trinkets applies them with (same per-slot ids), skipping slots where `canApplyEffects` is false; re-check on Trinkets updates |
| `GatherDataEvent` (`MixProvider`) | none | skipped: build-time datagen |
| `NeoForgeMod.enableMergedAttributeTooltips` | none | skipped: display only; Dynamic Tooltips draws item stat lines |

### Attribute and effect mechanics (`impl/AttributeEvents`)
| Upstream point | Port wiring | Status |
|---|---|---|
| Draw Speed (`LivingEntityUseItemEvent.Tick`) | `mixin/LivingEntityHooksMixin:68` (`updateUsingItem` HEAD) -> `drawSpeed` | wired; bridge returns early for bow/crossbow use animations (Ranged Weapon API's hook applies the same total), so it acts on tridents here |
| Pre-damage health (`LivingDamageEvent.Pre`) | `LivingEntityHooksMixin:45` (`actuallyHurt` HEAD) | wired |
| Life Steal, Overheal (`LivingDamageEvent.Post`) | `LivingEntityHooksMixin:51` (`actuallyHurt` TAIL, post-absorption amount) | wired |
| Projectile Damage, Crit Chance/Damage, Dodge (melee), Current HP / Fire / Cold damage (`LivingIncomingDamageEvent`, upstream priority order) | `LivingEntityHooksMixin:33` (`hurtServer` at its only `isSleeping()` call, i.e. after invulnerability/death/fire-resistance exits, before shield and armor) -> `onIncomingDamage` | wired; bridge: no Apothic crit on spell damage, crits flagged for Spell Engine, dodge within Better Combat reach and fires Spell Engine evasion |
| Vanilla crit x Crit Damage (`CriticalHitEvent`) | `PlayerMixin:78` (the single `1.5F` in `Player.attack`, checked with javap) | wired |
| Experience Gained, blocks (`BlockDropsEvent`) | `mixin/BlockMixin:16` (`Block.popExperience`) + breaker from `PlayerBlockBreakEvents` | wired; **fixed**: breaker also cleared every server tick (a break that fails between BEFORE and AFTER fires neither AFTER nor CANCELED) |
| Experience Gained, mobs (`LivingExperienceDropEvent`) | `LivingEntityHooksMixin:76` (`dropExperience` -> `ExperienceOrb.award` amount) | wired |
| Healing Received (`LivingHealEvent`) | `LivingEntityHooksMixin:57` (`heal` HEAD; <= 0 cancels) | wired |
| Arrow Damage, Arrow Velocity (`EntityJoinLevelEvent`) | `mixin/ServerLevelMixin:19` (`addFreshEntity` HEAD; loaded arrows never pass it, so no "done" flag) | wired |
| Dodge (projectiles) + MinecraftForge#9370 piercing fix (`ProjectileImpactEvent`) | `mixin/ProjectileMixin:19` (`hitTargetOrDeflectSelf` HEAD, returns `NONE`) | wired; only `Projectile` declares it and every vanilla projectile's tick calls it (javap) |
| Aux damage trackers (`EntityTickEvent.Post`) | `LivingEntityHooksMixin:83` (`LivingEntity.tick` TAIL) | wired |
| Attack Range display modifier, Elytra Flight modifier on glider items (`ItemAttributeModifierEvent`) | none | skipped: display only (the Elytra Flight value is never read for vanilla gliders, and it's hidden in the GUI by default) |
| Bonus modifiers / stack modifier compat (`ItemAttributeModifierEvent`) | none | skipped with the components above |
| Ghost Health | none | no effect upstream either: registered, never read (see "Gaps") |
| Cooldown Reduction | `api/AbilityCooldowns`, called by Apotheosis's affix/gem cooldowns | wired |
| Armor/Protection Pierce and Shred, armor/protection formulas | `mixin/CombatRulesMixin`, `mixin/LivingEntityMixin:87` -> `ALCombatRules` | wired |
| Elytra Flight | `LivingEntityMixin:106` (`canGlide`), `:138` (`canGlideUsing`), `:163` (`updateFallFlying`) | wired (port: requires a glider slot; reads dynamic modifiers) |
| Sundering | `LivingEntityMixin:51/67/76` (`getDamageAfterMagicAbsorb`) | wired |
| Bleeding, Flaming Detonation | vanilla `applyEffectTick` | wired |
| Grievous Wounds, Bursting Vitality | effect attribute modifiers on Healing Received | wired |
| Ancient Knowledge | effect modifier on Experience Gained | wired; **fixed**: `KnowledgeEffect` builds the modifier from the current (server-synced) multiplier, as upstream's NeoForge function did; it was read once at registration |

### Other upstream mixins
| Upstream mixin | Port | Status |
|---|---|---|
| `EntityMixin` (Gravity scales fall damage) | `mixin/EntityMixin:17` | wired |
| `NearestAttackableTargetGoalMixin` (live Follow Range) | `mixin/NearestAttackableTargetGoalMixin:31` | wired |
| `PlayerMixin`: aux-damage kill counts as a hit; sweep flag; MC-268917 edge fix | `mixin/PlayerMixin:44/54/70` (sweep uses vanilla's 4-argument `doSweepAttack`) | wired |
| `ThrownTridentMixin` (trident damage from base damage) | `mixin/ThrownTridentMixin:19` | wired |
| client `AbstractContainerScreenMixin` (drags reach the GUI) | Fabric `ScreenMouseEvents.allowMouseDrag` in `client/AttributesLibClient` | **fixed** (with the GUI) |
| NeoForge `PercentageAttribute` display | `mixin/PercentTooltipMixin:25`, `mixin/PotionPercentTooltipMixin:24` | wired (port) |

### Client
| Upstream point | Port wiring | Status |
|---|---|---|
| Attributes GUI (`ScreenEvent.Init.Post`) | `client/AttributesLibClient#addAttribComponent` on `ScreenEvents.AFTER_INIT`; panel drawn from `ScreenEvents.afterExtract`, fed `allowMouseClick/Drag/Release/Scroll` | **fixed**. Clicks on the open panel stop there (upstream's let a click on the panel count as outside the inventory and drop the carried item) |
| Scroll forwarding (`ScreenEvent.MouseScrolled.Pre`) | `ScreenMouseEvents.allowMouseScroll` | **fixed** |
| Effect tooltips (`GatherEffectScreenTooltipsEvent`) | `mixin/client/EffectsInInventoryMixin` -> `AttributesLibClient#effectTooltip` | **fixed**; vanilla only shows this tooltip when the effect's name is cut off or the list is compact |
| Potion description tooltips (`ItemTooltipEvent`) | `ItemTooltipCallback` | wired |
| `/apothic_attributes_client set_btn_pos` (`RegisterClientCommandsEvent`) | `ClientCommandRegistrationCallback` | **fixed** |
| JEI exclusion zones (`@JeiPlugin`) | `compat/AttributesJEIPlugin`, `jei_mod_plugin` entrypoint | **fixed** |
| Client config reload, crit particle provider | `ResourceManagerHelper` (client), `ParticleProviderRegistry` | wired |
| Curios client compat | none | skipped (Trinkets) |

GUI port notes: NeoForge's attribute display methods are `client/AttributeDisplay` (vanilla translation keys: flat
values as numbers, percentage attributes and multiplier totals as percentages, boolean attributes as ON/OFF). Attributes
whose value is NaN are hidden, and `AttributesGui.addHiddenFilter` lets another mod hide attribute instances it replaces.

### Gaps outside this mod (not fixed here)
- Ghost Health does nothing (upstream too), yet Apotheosis's Royalty gem lists it among its random helmet bonuses.
- The bridge's source attributes (`critical_strike:*`, `spell_engine:evasion_chance`/`healing_taken`,
  `ranged_weapon:haste`, `too_many_bows:*`, `puffish_attributes:*` it links) will show in the Attributes GUI as
  duplicate entries under the target's name; the bridge can hide them with `AttributesGui.addHiddenFilter(i -> i
  instanceof BridgedAttributeInstance)` (client side), or they can go in the client config's "Hidden Attributes".
- Apotheosis's port registers no creative tab (upstream fills an Adventure tab through `TabFillingRegistry`, now in
  Placebo 0.1.3).

### Test steps (3.0.1-fabric.6 + Placebo 0.1.3)
Server/RCON: `/give @p minecraft:potion[potion_contents={potion:"apothic_attributes:wither"}]` (and `sundering`,
`strong_knowledge`, `vitality`, `grievous`, `haste`, `resistance`); boot log: `Registered 34 placebo:brewing_mixes`, no
`brewing_mixes` errors. Brewing stand: Awkward + Wither Skeleton Skull -> Potion of Wither; Awkward + Sweet
Berries -> Potion of Healing Boost; that + Fermented Spider Eye -> Potion of Healing Reduction; check JEI's brewing category for doubled recipes.
Ancient Knowledge: `/effect give @p apothic_attributes:knowledge 60 1`, then `/attribute @p
apothic_attributes:experience_gained get` -> 9 (1 x (1 + 4 x 2)). Client: open the inventory, click the sword button
on the player preview; the panel lists attributes with values, hover shows modifiers with item/effect icons and the
formula; scroll wheel and scrollbar drag work; with an item on the cursor, clicking the panel does not drop it;
"hide unchanged" toggles; recipe book button closes it. `/apothic_attributes_client set_btn_pos top_left 0 0` moves the
button. In a narrow window (effects compact), hovering an effect shows "Name (m:ss)", its description and modifiers.
