# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - YYYY-MM-DD
### Added
#### Upper-body twist tweak:
- Third-person upper-body twist toward where the player looks while holding a TACZ gun on foot (standing, walking, 
running, crouching and creative flying), instead of TACZ turning the whole player.
- Third-person upper-body twist while holding a TACZ gun on any vehicle or mount.
- Optional torso twist toward the head without a gun, on foot and on vehicles.
- Whole body turn on vehicles when the look angle passes 85 degrees.
- Upper body twist compatibility with Entity Model Features and the Player Extension resource pack.

#### Tacz small optimizations:
- without effecting gameplay:
1. Faster TACZ synced entity data lookups.
2. Faster TACZ data holder capability lookups.
3. TACZ sprint and speed modifier checks skip unneeded work.
4. TACZ cycle task helper skips empty task lists.

- others:
1. Lower lag spike on the first gun equip of a game session (toggleable, enableFirstEquipWarmUp): TACZ's Lua scripts,
first person arms, gun model, attachments and, with shaders on, third person gun are prepared hidden after joining a
world.

#### Tacz fixes:
- Fixed Distant Horizons mod LODs breaking when using a gun with sight attachment, with shaders on and were never
toggled off since client launch the current game.
- Fixed a black frame on the first equip of a gun with a sight attachment.

#### EMF/Player Extension fixes:
- Bow and crossbow poses fix with the Player Extension resource pack.
- Trident aiming animation fix while riding with the Player Extension resource pack.

#### General
- Can be installed on the client only, the server only, or both.
