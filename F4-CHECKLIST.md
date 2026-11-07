# F4 — Prueba con shaders (Oculus + Embeddium)

Anotá en cada fila **Sí / No / Parcial** y una nota corta. Para comparar, la tecla **K** apaga y prende el shaderpack (Oculus); la **O** abre el menú de shaders.

- Shaderpack y versión: 
- Versión de Oculus: oculus-mc1.20.1-1.8.0 (de tu carpeta mods)
- Mundo usado (nuevo / existente): 

## Preparación (una vez por mundo)

```
/gamerule doWeatherCycle false
/time set day
```

Hacé clic en las coordenadas verdes de cada `/locate` para teletransportarte. Después de cambiar el clima, esperá unos 10 segundos: la niebla entra de a poco.

## 1. Tormenta de arena — `/locate biome minecraft:desert` (o `terralith:ancient_sands`)

| # | Qué mirar | Clima | Con shaders | Sin shaders (K) | Nota |
|---|---|---|---|---|---|
| 1.1 | Niebla color arena (marrón claro), se acerca a ~32–64 bloques | `/weather rain` | | | |
| 1.2 | Niebla más espesa (~16–32 bloques) | `/weather thunder` | | | |
| 1.3 | Color de la niebla: ¿arena o el gris/azul del shader? | thunder | | | |
| 1.4 | Partículas de polvo volando hacia el oeste | rain | | | |
| 1.5 | Sonido de viento fuerte (ráfagas) | rain | | | |

## 2. Ventisca — `/locate biome minecraft:snowy_plains` (o `terralith:frozen_cliffs`)

| # | Qué mirar | Clima | Con shaders | Sin shaders (K) | Nota |
|---|---|---|---|---|---|
| 2.1 | Niebla gris clara cerrada (~32–64 bloques). Solo con tormenta | `/weather thunder` | | | |
| 2.2 | Color gris claro, no el del shader | thunder | | | |

## 3. Niebla densa

| # | Qué mirar | Clima | Con shaders | Sin shaders (K) | Nota |
|---|---|---|---|---|---|
| 3.1 | Pantano (`/locate biome minecraft:swamp` o `terralith:orchid_swamp`): niebla ligera | `/weather rain` | | | |
| 3.2 | Pantano: niebla espesa | `/weather thunder` | | | |
| 3.3 | Pale Garden (`/locate biome minecraft:pale_garden`): niebla densa | rain / thunder | | | |

## 4. Viento ambiental — `/locate biome minecraft:jagged_peaks` (o `terralith:windswept_spires`)

| # | Qué mirar | Clima | Con shaders | Sin shaders (K) | Nota |
|---|---|---|---|---|---|
| 4.1 | Partículas de polvo blancas/grises a ras del suelo | `/weather clear` | | | |
| 4.2 | Sonido de viento suave de vez en cuando (también en desierto despejado) | clear | | | |

## 5. Lluvia negra — Pale Garden, `/weather rain`

| # | Qué mirar | Con shaders | Sin shaders (K) | Nota |
|---|---|---|---|---|
| 5.1 | Columnas de lluvia negras / muy oscuras | | | |
| 5.2 | Salpicaduras negras en el suelo | | | |
| 5.3 | Al salir del bioma, la lluvia vuelve a la normal | | | |
| 5.4 | Config → desactivar "Enable black rain": la lluvia vuelve a la normal | | | |

## 6. Convivencia con Subtle Effects (lluvia fuera del Pale Garden)

| # | Qué mirar | Con shaders | Sin shaders (K) | Nota |
|---|---|---|---|---|
| 6.1 | Lluvia normal teñida con el color de agua del bioma (biomeColorRain) | | | |
| 6.2 | Salpicaduras normales de lluvia | | | |
| 6.3 | Ondas en un caldero con agua bajo la lluvia | | | |
| 6.4 | Vapor cuando llueve sobre magma o lava | | | |
| 6.5 | Ondas del caldero dentro del Pale Garden | | | |

## 7. General

| # | Qué mirar | Resultado | Nota |
|---|---|---|---|
| 7.1 | Mods → Immersive Storms: figura con ese nombre, ícono y botón Config | | |
| 7.2 | JEI: no aparece nada raro con el nombre del mod | | |
| 7.3 | FPS: ¿alguna caída notable en tormentas? | | |
| 7.4 | ¿Algún error o crash? | | |

## Notas libres

