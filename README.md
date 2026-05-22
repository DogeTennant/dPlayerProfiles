# dPlayerProfiles

> Player retention plugin for Paper 1.21+ - profiles, achievements, badges, a points path, and deep third-party integrations, all driven by YAML and an in-game GUI.

![Version](https://img.shields.io/badge/version-1.0.0-blueviolet)
![Paper](https://img.shields.io/badge/Paper-1.21+-orange)
![Java](https://img.shields.io/badge/Java-21+-blue)
![License](https://img.shields.io/badge/license-MIT-green)

---

## Features

- **Achievements** - 30+ trigger types, prerequisites, hidden achievements, group progressions, and server-wide broadcasts
- **Badges** - collectible titles pinnable to chat hover tooltips
- **Points Path** - a linear node progression system with per-node rewards and milestone indicators
- **Leaderboard** - ranked by achievements completed and badge count
- **Profile Privacy** - players can hide their profile from others
- **In-game Reward Editor** - edit achievement and points node rewards live via GUI, no file editing required
- **Multi-language** - swap languages at runtime; ships with `en_us` and `cs_cz`
- **SQLite & MySQL** - choose your storage backend; HikariCP connection pooling for MySQL
- **Anti-AFK & Anti-farm** - CoreProtect integration prevents block farming; built-in AFK detection for time-played achievements
- **PlaceholderAPI** - expose profile data to scoreboards, tab lists, and other plugins
- **Developer API** - query profiles, drive achievement progress, and manage badges from your own plugin

---

## Integrations

| Plugin | Triggers |
|---|---|
| MythicMobs | `MYTHICMOBS_KILL` |
| mcMMO | `MCMMO_LEVEL_UP` |
| AuraSkills | `AURASKILLS_LEVEL_UP` |
| Jobs Reborn | `JOBS_LEVEL_UP`, `JOBS_JOIN` |
| FluxShops | `SHOP_BUY`, `SHOP_SELL`, `SHOP_CREATE` |
| EliteMobs | `ELITEMOBS_KILL`, `ELITEMOBS_DUNGEON_COMPLETE`, `ELITEMOBS_ARENA_COMPLETE`, `ELITEMOBS_QUEST_COMPLETE` |
| CrazyCrates | `CRATE_OPEN` |
| Duels | `DUEL_WIN`, `DUEL_KILL` |
| OneInTheChamberReborn | `OITC_KILL`, `OITC_WIN` |
| dTournaments | `TOURNAMENT_WIN` |
| PinataParty | `PINATA_KILL` |
| CoreProtect | Anti-farm protection |
| PlaceholderAPI | `%dpp_*%` placeholders |

All integrations are **soft-dependencies** - the plugin works fine without any of them installed.

---

## Commands

| Command | Description |
|---|---|
| `/profile [player]` | Open a player's profile GUI |
| `/achievements [player]` | Open a player's achievements GUI |
| `/dp help` | List all subcommands |
| `/dp leaderboard` | Open the leaderboard GUI |
| `/dp rewards <id>` | Edit achievement rewards in-game |
| `/dp complete <player> <id>` | Force-complete an achievement |
| `/dp badge give/remove <player> <id>` | Grant or revoke a badge |
| `/dp reset <player>` | Wipe all data for a player |
| `/dp reload` | Reload config and content files |

Full reference: **[Commands](../../wiki/Commands)**

---

## Requirements

- **Paper** (or a fork) 1.21+
- **Java** 21+

---

## Documentation

The full documentation lives in the [Wiki](../../wiki):

- [Installation](../../wiki/Installation)
- [Commands](../../wiki/Commands)
- [Permissions](../../wiki/Permissions)
- [Configuration](../../wiki/Configuration)
- [Achievements](../../wiki/Achievements)
- [Badges](../../wiki/Badges)
- [Rewards](../../wiki/Rewards)
- [Points System](../../wiki/Points-System)
- [GUI Layouts](../../wiki/GUI-Layouts)
- [Integrations](../../wiki/Integrations)
- [PlaceholderAPI](../../wiki/PlaceholderAPI)
- [Developer API](../../wiki/Developer-API)
- [FAQ & Troubleshooting](../../wiki/FAQ-and-Troubleshooting)
