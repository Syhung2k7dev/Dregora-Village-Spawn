# Dregora Village Spawn

Forge mod for Minecraft 1.12.2, intended for RLCraft Dregora.

## Current first prototype

- On a player's first login in the Overworld, asks the active world generator for its nearest structure named `Village`.
- Searches outward from the returned village position for a safe surface, checking for a solid floor and two air blocks above.
- Sets the world spawn and that player's respawn point, then moves the player to the selected position.
- Stores a per-player marker so the action is not repeated after a successful placement.

## Compatibility note

This prototype uses Forge/Minecraft's structure lookup API. A custom Dregora generator may not expose its villages through the structure name `Village`. If the lookup returns no village, the mod leaves the player at the normal spawn and logs a warning. The exact Dregora pack must be tested before treating this as confirmed compatible.

## Online build

A GitHub Actions workflow is included. It runs when files are pushed to `main`, and can also be started manually from the repository's **Actions** tab. After a successful run, open the run and download the `DregoraVillageSpawn` artifact. The JAR is inside the downloaded ZIP.

## Target

- Minecraft 1.12.2
- Forge 14.23.5.2860
- Java 8
- ForgeGradle 2.3
