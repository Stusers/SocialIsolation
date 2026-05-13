# MobEffect Icons for Social Isolation

## Where the textures go

Place your 18×18 PNG files here:

```
src/main/resources/assets/socialisolation/textures/mob_effect/
├── social_thriving.png   (green icon for "In Good Company")
├── social_lonely.png     (orange icon for "Restless")
└── social_isolated.png   (red icon for "Isolated")
```

## Naming

The file name must exactly match the DeferredRegister name in `ModEffects.java`:

| File name             | Registry name        | Effect class              |
|-----------------------|----------------------|---------------------------|
| `social_thriving.png` | `socialisolation:social_thriving`  | `SocialThrivingEffect`    |
| `social_lonely.png`   | `socialisolation:social_lonely`    | `SocialLonelyEffect`      |
| `social_isolated.png` | `socialisolation:social_isolated`  | `SocialIsolatedEffect`    |

## How to create the textures

### Option 1: Reuse vanilla icons (quick)
Extract vanilla potion icons from the Minecraft jar:
```bash
# The jar is at:
~/.gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar

# Unzip and grab from:
assets/minecraft/textures/mob_effect/
```
Copy a vanilla icon, recolour it in an image editor, and rename.

### Option 2: Draw custom icons (best)
Use any image editor (Photoshop, GIMP, Aseprite, paint.net, etc.) and:
1. Create a new 18×18 canvas
2. Draw a simple symbol — e.g., a heart for Thriving, a tear for Isolated, a frown for Lonely
3. Use colours matching the effect tier (green/orange/red)
4. Export as PNG with transparency
5. Place in the `mob_effect/` folder

### Option 3: Use BlockBench
1. Open BlockBench → Texture → New → 18×18
2. Draw pixel-art style
3. Export PNG

## Build & test
After adding textures, run:
```bash
./gradlew build
```

Launch the game and check the HUD — your custom icons will appear alongside the social meter bar when the effects are active.

