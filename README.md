# Account Manager

A Forge 1.8.9 Minecraft account manager mod with Microsoft login, cookie login, access token login, refresh token login, and cracked/offline login.

Based on [Lumiere](https://github.com/xanning/Lumiere) with a full source tree and additional authentication fixes.

## Features

- **Microsoft login** — OAuth via browser URL
- **Cookie login** — Import browser cookies (Netscape `.txt` or JSON)
- **Access token login** — Paste JWT access tokens, including bulk import
- **Refresh token login** — Authenticate with Microsoft OAuth refresh tokens (`M.C...`)
- **Cracked login** — Offline mode usernames

## Building

Requires **JDK 8**.

```bash
./gradlew build
```

On Windows:

```bat
gradlew.bat build
```

The built mod JAR is written to `build/libs/AccountManager-1.8.jar`.

## Project layout

| Path | Purpose |
|------|---------|
| `src/main/java/` | Mod source code |
| `src/main/resources/` | Mod metadata and resources |

## Credits

- **[Lumiere](https://github.com/xanning/Lumiere)** — Original account manager mod (based on ksyz's Account Manager)
- **[refresh-token-authentication](https://github.com/ravioli-a/refresh-token-authentication)** — Microsoft OAuth refresh token authentication flow used for refresh token login

## License

MIT License — see [LICENSE](LICENSE).
