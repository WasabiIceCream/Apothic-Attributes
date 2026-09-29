Upstream files not compiled in the first Fabric version (moved here with `git mv`, unchanged):
Curios compat (we run Trinkets), the JEI plugin, datagen, the `/apoth bonus_modifier` command,
the StackAttributeModifiers API (our Apotheosis port has its own hook), the Attributes GUI and its
tooltip helpers, and the Flying effect (only used by the potions, which are also deferred).
Port them back into src/ as needed.
