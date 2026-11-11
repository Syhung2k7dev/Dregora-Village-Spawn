# Dregora Village Spawn

Forge mod for Minecraft 1.12.2, intended for RLCraft Dregora.

## First-login spawn behavior

- On a player's first login in the Overworld, asks the active world generator for its nearest structure named `Village`.
- Measures biome proximity from the returned village center (not from the original world spawn).
- Checks for these target biome types within 500 blocks of the village:
  - Desert (including Desert Hills)
  - Ocean (vanilla ocean variants and biomes registered with Forge's Ocean biome dictionary type)
  - Ice Plains
- If one, two, or all three target biome types are found within 500 blocks, the nearest target biome must be no farther than 128 blocks from the village. Any other target types found may be up to 500 blocks away.
- Samples biome positions every 4 blocks to keep login-time checks bounded.
- Searches within 64 blocks of the village center for a safe surface position with a solid floor and two air blocks above.
- Sets the world spawn and that player's respawn point, then moves the player to the selected position.
- Stores a per-player marker so the action is not repeated after a successful placement.

## Compatibility and search limitations

This prototype uses Forge/Minecraft's structure lookup API and validates the nearest `Village` structure returned by the active generator. It does not enumerate every village in the world or continue to a farther village if the returned nearest village fails the biome rules. A custom Dregora generator may also not expose its villages through the structure name `Village`. In either case, the mod leaves the player at the normal spawn and logs a warning. The exact Dregora pack must be tested before treating this as confirmed compatible.

## Online build

A GitHub Actions workflow is included. It runs when files are pushed to `main`, and can also be started manually from the repository's **Actions** tab. After a successful run, open the run and download the `DregoraVillageSpawn` artifact. The JAR is inside the downloaded ZIP.

## Target

- Minecraft 1.12.2
- Forge 14.23.5.2860
- Java 8
- ForgeGradle (the build currently uses ForgeGradle 3+)
