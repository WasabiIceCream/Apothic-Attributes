Upstream files not compiled in the Fabric port (moved here with `git mv`, unchanged):
Curios compat (we run Trinkets), datagen (`data/MixProvider.java`), the `/apoth bonus_modifier` command and the
bonus-modifier components it sets, the StackAttributeModifiers API (our Apotheosis port has its own hook), the
Flying effect and its three potions' brewing mixes (`data/*flying*.json`; the effect needs NeoForge's creative
flight attribute), and upstream's `AbstractContainerScreenMixin` (the Attributes GUI gets its drags from Fabric's
screen mouse events instead). Port them back into src/ as needed.
