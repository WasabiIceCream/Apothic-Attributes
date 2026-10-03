# Attribution and licensing (Fabric port)

Code: upstream Apothic Attributes is MIT (`LICENSE`); this port's changes are MIT too.

Assets: upstream split its licenses on 2025-03-12 (commit `ef9fb06`, "update licenses"), after which
everything under `assets/` is "All Rights Reserved" (`LICENSE_ASSETS`). Before that, the single MIT
`LICENSE` covered the whole repository. This port only ships assets from that MIT era:

- Every texture, the dodge sound, `sounds.json` and the particle definition are byte-identical to
  upstream's `ee745cc` (the last commit before the split).
- The language files are upstream's `ee745cc` versions, with 15 keys renamed to the ids 26.1 uses
  (e.g. `attribute.name.generic.armor.desc` -> `attribute.name.armor.desc`); the text is unchanged.
  Six English strings that postdate the split were written for this port instead (four attribute
  descriptions and Cooldown Reduction's name and description). The Vietnamese translation only exists
  after the split and is left out.
  Later upstream translation updates (e.g. pt_br, zh_cn, uk_ua in 2026-09) are post-split too and are not
  merged; the one English typo they fixed ("towrds") is corrected here independently.

`LICENSE_ASSETS` is kept as upstream's file; it applies to upstream's current assets, not to the
MIT-era files shipped here.

`data/neoforge/tags/damage_type/` lists NeoForge's default damage-type tag contents (plain lists of
vanilla ids) so the physical/magic checks work on Fabric.
