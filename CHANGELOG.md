This update brings full **Cobblemon 1.8.2** support, a brand-new **Alpha Awakening** event, Cobblemon 1.8 items across all Lucky Block pools and, for the first time, a **NeoForge** version.

## Added

### NeoForge support
* Lucky Block Cobblemon is now available for **Fabric** and **NeoForge** (Minecraft 1.21.1).
* Each loader has its own file. Pick the one for your loader on the Versions page.

### Alpha Awakening (new event)
An **Alpha Pokémon** bursts out of the Lucky Block!
* Lightning, sounds, particles, an on-screen title and a chat announcement for nearby players.
* A **boss bar** with the Alpha's name, level and remaining HP.
* The Alpha comes with a **herd of its pre-evolutions**.
* Its level **follows the strongest Pokémon in your party** (+3 by default).
* **Defeat it** to earn **1 reward** (Type Gems, a TM, a Gem Block or a bonus item). Cobblemon's normal drops still fall on the ground.
* **Capture it** to earn the **full loot**: Type Gems plus a chance of a TM, a Gem Block and a bonus item.
* Default Lucky Block: random strong final-evolution species (no legendaries, mythicals, Ultra Beasts or Paradox Pokémon).
* Themed Lucky Blocks: Alphas that match the block's type.
* **Never** triggers from locked-tier Lucky Blocks (`/luckyblockgive`), so pre-set adventure modpacks stay safe.

### Cobblemon 1.8 items in the pools
* New `tm` event: drops a random **Technical Machine**. Themed Lucky Blocks drop TMs of their own types.
* TM crafting materials: **Type Gem Clusters** and **Blank TMs**.
* Rare drops: **TM Machine** and **Deepslate Crystal Core**.
* **Terrain plants** and **Oricorio flowers**.
* Themed Lucky Blocks: **Type Gems**, **Gem Clusters** and rare **Gem Blocks** of their types.
* The "all items" event now includes 46 new Cobblemon 1.8 items (plaques, disc shelf, gem clusters and more).

### Legendary Lucky Block bonus
* Legendary Lucky Blocks now also drop **Type Gems of the legendary's types**, with a chance of a **matching TM**.
* Fully configurable in `pools/legendary.json` → `bonusDrops`.

### New settings
* `enableAlphaEvents`: turns the Alpha Awakening event on or off globally.
* `autoUpgradePools`: automatically adds new content to your existing pool files.

### Translations
* Added Brazilian Portuguese (`pt_br`).

## Changed
* Updated to **Cobblemon 1.8.2**.
* **Existing configs are upgraded automatically:**
  * New events are added to your pool files once.
  * Your custom values are kept, and a backup is saved as `.pre-0.2.0.bak`.
  * You can turn this off with `autoUpgradePools: false` in `settings.json`.
* Structure events are a bit more common in the default Lucky Block pool (new configs).
* The generated `README.txt` documents all the new events and options.

## Configuration
Everything is configurable in `config/luckyblockcobblemon/`:
* **Alpha:** species list or random species, `excludeLabels`, level rules (`scaleWithParty`, `partyLevelBonus`, `maxLevelCap`), shiny chance, herd size, boss bar and rewards.
* **TM event:** TM types and amount.
* **Legendary bonus:** `enabled`, `chance`, `gemMin`, `gemMax`, `tmChance`.

Use `/luckyblock reload` after editing the pool files.

## Requirements
| | Fabric | NeoForge |
|---|---|---|
| Minecraft | 1.21.1 | 1.21.1 |
| Loader | Fabric Loader 0.16.10+ and Fabric API | NeoForge 21.1+ |
| Cobblemon | 1.8.x (built on 1.8.2) | 1.8.x (built on 1.8.2) + Kotlin for Forge |

## Notes
* The project now builds both loaders from a single codebase, so future updates ship for Fabric and NeoForge at the same time.
* Cobblemon versions older than 1.8 are not supported by this version.
* Worldgen changes only affect newly generated chunks.
* As always, back up your world before updating.
