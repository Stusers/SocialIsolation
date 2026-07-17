# Social Isolation

A server-side NeoForge mod that encourages players to meet up and discourges far away bases for SMP health without relying on small borders.

---

## How it works

Every player has a **Social Meter** (0–100). Being near others fills it. Being alone drains it. Where your meter sits determines what tier you're in:

| Tier | Range | Effect |
|------|-------|--------|
| Thriving | ≥ 60 | +15% mining speed, +15% XP gain |
| Neutral | 40–60 | Nothing |
| Lonely | 15–40 | -15% mining speed |
| Isolated | < 15 | -30% mining speed + phantoms think you haven't slept in days |

The meter fills faster the more people are around you, with a small bonus for groups.

---

## Familiarity

Spending time near the same person builds **familiarity**, which gradually reduces how much meter gain they give you. Max familiarity takes ~5 hours to reach and ~24 hours apart to decay. This keeps the social dynamic fresh — you can't just park next to your friend AFK and call it a day.

---

## Willson

Name any slime **Willson** with a name tag and it becomes your social companion. Counts as a nearby player for meter purposes and builds familiarity just like a real person. No setup required.

---

## Open Parties and Claims

If OPAC is installed, players earn **bonus claim chunks** the more they socialise. The cost scales up with each chunk so early chunks come quickly and later ones take real commitment. Progress shows as a second bar under the social meter HUD.

Use `/social chunks` to see your current count and progress toward the next one.

---

## Commands

| Command | Who | What |
|---------|-----|-------|
| `/social status [player]` | Anyone (OP for others) | Meter, tier, and lifetime points |
| `/social set [player] <value>` | OP | Override a player's meter |
| `/social chunks [player]` | Anyone (OP for others) | Bonus chunk progress |
| `/social chunks sync [player]` | OP | Force OPAC sync (all online if no player given) |
| `/social config list` | OP | Show all config values |
| `/social config get <key>` | OP | Get a single value |
| `/social config set <key> <value>` | OP | Change a value live |

---

## Configuration

Tunable server-side in `socialisolation-server.toml`. Key settings:

| Setting | Default | Meaning |
|---------|---------|---------|
| `proximityRadius` | 24 | Blocks to count as "nearby" |
| `minutesToFullMeter` | 10 | Minutes near a fresh player to go 0→100 |
| `minutesToEmptyMeter` | 30 | Minutes alone to go 100→0 |
| `hoursToMaxFamiliarity` | 5 | Hours together before familiarity maxes out |
| `hoursToLoseFamiliarity` | 24 | Hours apart to fully reset familiarity |
| `thresholdThriving` | 60 | Meter value to enter Thriving |
| `thresholdLonely` | 40 | Meter value to enter Lonely |
| `thresholdIsolated` | 15 | Meter value to enter Isolated |
| `enableBenefits` | true | Toggle positive effects |
| `enablePenalties` | true | Toggle negative effects |
| `enableFamiliarity` | true | Toggle the familiarity system |
| `phantomSpawnWhenIsolated` | true | Phantoms for isolated players |
| `opacPointsPerBonusChunk` | 5000 | Base points per bonus chunk (scales up) |
| `opacMaxBonusChunks` | 200 | Chunk cap |

The HUD can be repositioned by dragging it while chat is open, and scaled by scrolling over it.

---

## Installation

1. Install NeoForge 1.21.1
2. Drop the JAR in your `mods/` folder
3. Start the server — config generates automatically

Clients don't need the mod. OPAC is optional.

---

## License

MIT — Stusers
