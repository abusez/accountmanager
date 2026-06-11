# Account Manager

A Forge 1.8.9 Minecraft account manager mod with Microsoft login, cookie login, access token login, refresh token login, and cracked/offline login.

This project is a development replica of [Lumiere](https://github.com/xanning/Lumiere) with a working Gradle build environment and additional authentication fixes.

## Features

- **Microsoft login** — OAuth via browser URL
- **Cookie login** — Import browser cookies (Netscape `.txt` or JSON)
- **Access token login** — Paste JWT access tokens, including bulk import
- **Refresh token login** — Authenticate with Microsoft OAuth refresh tokens (`M.C...`)
- **Cracked login** — Offline mode usernames

## Building

Requires **JDK 8**.

Before building, extract the base mod from [Lumiere](https://github.com/xanning/Lumiere) into a `LumiereAccManager/` folder at the project root (unzip `LumiereAccManager.jar`).

```bash
./gradlew replicaJar
```

On Windows:

```bat
gradlew.bat replicaJar
```

The built mod JAR is written to `build/libs/AccountManager-1.8.jar`.

## Project layout

| Path | Purpose |
|------|---------|
| `src/overlay/java/` | Source changes compiled into the final JAR |
| `src/reference/java/` | Decompiled reference source (not compiled directly) |
| `src/main/resources/` | Mod metadata and resources |
| `LumiereAccManager/` | Base mod bytecode from Lumiere (not in repo; extract from JAR locally) |

Overlay classes replace matching classes from the base JAR during the `replicaJar` task.

## Credits

- **[Lumiere](https://github.com/xanning/Lumiere)** — Original account manager mod (based on ksyz's Account Manager). This project uses its JAR contents as the mod base and reference implementation.
- **[refresh-token-authentication](https://github.com/ravioli-a/refresh-token-authentication)** — Microsoft OAuth refresh token authentication flow used for refresh token login.

## License

MIT License — see [LICENSE](LICENSE).
