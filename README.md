# Social Isolation

**A server-side Minecraft SMP mod that subtly incentivises players to spend time together.**

## Overview

Social Isolation adds a **Social Meter** mechanic to multiplayer servers. Players who spend time near others gain meter and receive benefits. Players who are alone lose meter and face mild penalties -- with a familiarity system that prevents AFK exploitation.

## Features

### Social Meter
- **Thriving (>=60)** -- +15% block break speed, +15% XP gain
- **Neutral (40-60)** -- No effects
- **Lonely (15-40)** -- Mild mining fatigue, hunger drain
- **Isolated (<15)** -- Severe penalties + phantom insomnia (as if 3 days without sleep)

### Familiarity System
- Real players and Willson slimes build familiarity over time (~5 hours to max)
- At max familiarity, that source contributes **zero** meter gain
- Familiarity decays slowly when apart (~24 hours to fully decay)
- Prevents AFK players from exploiting the proximity mechanic

### Willson -- The Companion Slime
- Name any slime **Willson** (case-insensitive) with a name tag
- Willson counts as a social source and participates in the familiarity system just like a real player
- No commands or registration required -- purely name-based detection

## Configuration

All values are tunable server-side via `socialisolation-server.toml`:

| Setting | Default | Description |
|---------|---------|-------------|
| `proximityRadius` | 24 | Block radius for nearby detection |
| `meterGainRate` | 0.1667 | Social meter gain per second when near others |
| `meterDrainRate` | 0.0556 | Social meter drain per second when alone |
| `thresholdThriving` | 60.0 | Threshold for Thriving tier |
| `thresholdLonely` | 40.0 | Threshold for Lonely tier |
| `thresholdIsolated` | 15.0 | Threshold for Isolated tier |
| `phantomSpawnWhenIsolated` | true | Enable phantom spawning when isolated |
| `enableBenefits` | true | Enable positive effects |
| `enablePenalties` | true | Enable negative effects |

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/social meter` | Anyone | View your current social meter |
| `/social config` | OP Level 2 | View or reload server config |

## Installation

1. Install NeoForge for Minecraft 1.21.1
2. Place the mod JAR in your server's `mods/` folder
3. Launch the server -- config generates automatically

## Compatibility

- **Server-side only** -- clients do not need the mod installed
- Works with any NeoForge 21.1.229+ server

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-05-15 | Initial release -- proximity meter, familiarity system, Willson companion, phantom insomnia, drag-and-drop HUD editor |

## License

All Rights Reserved -- Stusers
