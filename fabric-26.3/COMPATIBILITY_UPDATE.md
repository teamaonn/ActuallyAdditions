# Fabric 26.3 compatibility validation

Paired builds: Actually Additions 0.3.0-beta.2, Ex Compressum
26.3.0.0-omnia.2, Ex Nihilo Omnia 0.5.0-alpha.4.

- 38 AA, 29 Omnia and 7 Ex Compressum required game tests passed.
- Compression and decompression have one matching recipe for each of the eight
  shared materials; legacy conversion preserves one compressed block's value.
- Manual and automatic heavy sieve client state and rendered mesh/fill were
  checked at 0%, 50%, 99% and completion with all three mods and JEI loaded.
- The original engineer house was placed in the client test world.
- A combined material audit resolved 949 concrete models and 471 textures,
  including inherited texture variables, with no missing references.
- Thirteen AA machine model files had unbound face materials repaired.

The corrected builds preserve existing registered IDs and use the Omnia versions
as the sole newly crafted compressed materials. Previously placed Ex Compressum
blocks remain readable and drop canonical items; older stacks convert one-for-one.
