# Lucky Block Cobblemon

Lucky Blocks for [Cobblemon](https://cobblemon.com) on Minecraft 1.21.1, for **Fabric** and **NeoForge**.

## Project layout

```text
LuckyCobblemonMod/
├── common/     Shared code and resources (blocks, events, pools, Alpha event, configs,
│               commands, structures, textures, translations, default JSON configs)
├── fabric/     Fabric entrypoint, registration, events, resource conditions, fabric.mod.json
├── neoforge/   NeoForge entrypoint, registration, events, conditions, biome modifier, neoforge.mods.toml
├── build.gradle / settings.gradle / gradle.properties
└── gradle/     Gradle wrapper
```

- All modules use **Mojang (official) mappings**, so the same Java code compiles on both loaders.
- `common` is compiled against vanilla Minecraft (NeoForm) and the Cobblemon API. It must not use
  Fabric or NeoForge classes.
- Loader differences go through `platform.LoaderPlatform` (config folder, creative tab builder) and
  `registry.Registrar` (registration). Each loader implements them in its own module.
- Each loader compiles the common sources into its own complete JAR.

## Building

```text
./gradlew build
```

Outputs:

- `fabric/build/libs/lucky-block-cobblemon-fabric-1.21.1-<version>.jar`
- `neoforge/build/libs/lucky-block-cobblemon-neoforge-1.21.1-<version>.jar`

## Running in development

- Fabric: `./gradlew :fabric:runClient` (game folder: `run/`)
- NeoForge: `./gradlew :neoforge:runClient` (game folder: `neoforge/run/`)

Note: Fabric Loom leaves Minecraft's client-only libraries (such as `icu4j`) out of
`:fabric:runServer`, and Cobblemon's battle engine (Showdown) needs it, so it may fail to start
in the Fabric development server. This only affects the dev environment, not the built JAR.

## Requirements

| | Fabric | NeoForge |
|---|---|---|
| Minecraft | 1.21.1 | 1.21.1 |
| Loader | Fabric Loader 0.16.10+ and Fabric API | NeoForge 21.1+ |
| Cobblemon | 1.8.0+ | 1.8.0+ (with Kotlin for Forge) |
