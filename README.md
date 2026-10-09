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
- **SQLite & MySQL** - choose your storage backend; HikariCP connection pooling for MySQL; `/dp migrate` moves existing data between them
- **Web Statistics Export** - writes vanilla statistics (mobs killed, deaths, playtime, blocks mined, ...) and Vault balances of every player to the database, so a website can show leaderboards and player pages
- **Anti-AFK & Anti-farm** - built-in tracking of player-placed blocks (also when pistons move them) stops break/place farming with no database lookups; AFK detection for time-played achievements
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
| `/dp migrate [prefix] [add]` | Copy all data from the inactive storage backend (SQLite ↔ MySQL) into the active one; `add` sums counters from a source that started empty |
| `/dp webstats backfill` | Re-import offline players into the web statistics export and refresh all balances |

Full reference: **[Commands](../../wiki/Commands)**

---

## Web Statistics Export

Enable `web-stats` in `config.yml` and the plugin keeps these tables up to date for external tools (it never reads them itself):

| Table | Contents |
|---|---|
| `<prefix>player_stats` | one row per player: `username`, `mob_kills`, `player_kills`, `deaths`, `playtime_seconds` (vanilla, includes AFK), `blocks_mined`, `balance` (Vault, `NULL` if unknown), `updated_at` (epoch ms) |
| `<prefix>achievement_catalog` | id, display name, description, category, points, hidden, icon, sort order of every configured achievement |
| `<prefix>badge_catalog` | id, display name, description, icon of every configured badge |

Online players are snapshotted every `web-stats.interval` seconds and on quit. On startup, every player who ever joined is imported from the world's `stats/*.json` files (only files newer than the stored row), so leaderboards are complete from day one. Combine with `<prefix>players` (AFK- and vanish-excluded playtime, first/last seen, `is_private`), `<prefix>achievement_progress` and `<prefix>badges` for full player pages.

Running several servers against one database? Give each its own `storage.table-prefix` (e.g. `survival_`, `skyblock_`) - vanilla statistics are per server anyway.

### Switching from SQLite to MySQL

1. Set `storage.type: mysql` and the connection details in `config.yml`, restart.
2. With no players online, run `/dp migrate` once. It finds the old tables in `data.db` on its own (their prefix may differ from the new one - typically they have none) and merges them into MySQL without lowering anything that is already there, so re-running it is harmless. If several table sets exist (e.g. the prefix was changed while still on SQLite), all of them are merged; `/dp migrate <prefix>` (`none` = unprefixed) restricts it to one.

   **Recovering from a regenerated config.** If the plugin ran on default settings for a while (e.g. `config.yml` was recreated and it silently wrote to a fresh `data.db` instead of MySQL), that SQLite data is not a copy of the history but the *delta* since the reset - and the plain merge would discard it, since it only keeps the higher value. Put the MySQL settings back, restart, and with no players online run `/dp migrate <prefix> add` **exactly once** (`none` for unprefixed tables; with several table sets in the source, `add` insists on being told which one started empty, since adding an old full copy on top would inflate every counter): playtime and cumulative achievement progress are added onto the MySQL values (login streak and skill-level achievements take the higher value as usual). Achievements that only reach their goal through the sum complete on the player's next bit of progress, so notifications and rewards still fire in-game. Delete or rename `data.db` afterwards - a second `add` run would add the same time again. The migration also refuses to run if `storage.table-prefix` does not point at this plugin's own tables in the target (see below).

   **Table prefix and other plugins.** An unprefixed `players` table is a popular name; if another plugin already owns `players` in the same database, dPlayerProfiles now stops at startup with `'players' exists but is not a dPlayerProfiles table` instead of failing on every profile load. Give it a `storage.table-prefix` (each server its own, e.g. `dpp_survival_`), restart, and migrate the data as above. Vanilla web statistics (kills, deaths, blocks, balance) need nothing: they are re-exported from the world's stats files on startup.

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
