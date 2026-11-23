# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.1] - 2026-11-23
### Fixed:
- Fixed bow aiming animation on vehicles/mounts when the mod apply its whole body turn angle.
- Made Distant Horizons fix only apply when both it and Oculus mod installed on client side. 

## [1.0.0] - 2026-11-22
### Added
#### Upper-body twist tweak:
- Third-person upper-body twist toward where the player looks while holding a TaCZ gun on foot (standing, walking, 
running, crouching and creative flying), instead of TaCZ turning the whole player.
- Third-person upper-body twist while holding a TaCZ gun on any vehicle or mount.
- Optional torso twist toward the head without a gun, on foot and on vehicles.
- Whole body turn on vehicles when the look angle passes 85 degrees.
- Upper body twist compatibility with Entity Model Features and the Player Extension resource pack.

#### TaCZ small optimizations:
- without effecting gameplay:
1. Faster TaCZ synced entity data lookups.
2. Faster TaCZ data holder capability lookups.
3. TaCZ sprint and speed modifier checks skip unneeded work.
4. TaCZ cycle task helper skips empty task lists.

- others:
1. Lower lag spike on the first gun equip of a game session (toggleable, enableFirstEquipWarmUp): TaCZ's Lua scripts,
first person arms, gun model, attachments and, with shaders on, third person gun are prepared hidden after joining a
world.

#### TaCZ fixes:
- Fixed Distant Horizons mod LODs breaking when using a gun with sight attachment, with shaders on and were never
toggled off since client launch the current game.
- Fixed a black frame on the first equip of a gun with a sight attachment.

#### EMF/Player Extension fixes:
- Bow and crossbow poses fix with the Player Extension resource pack.
- Trident aiming animation fix while riding with the Player Extension resource pack.

#### General
- Can be installed on the client only, the server only, or both.
