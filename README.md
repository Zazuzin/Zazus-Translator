# Zazu's Translator

A client-side chat translator for Minecraft 26.2 and 26.3 on Fabric.

Read other players' messages in your language and reply in theirs from normal
Minecraft chat. The original message stays visible, with a separate translated
line showing the sender and detected language.

## Download and install

Choose the download for your Minecraft version:

| Minecraft | Download | Fabric Loader |
| --- | --- | --- |
| 26.2 | [Zazus-Translator-1.0.0-mc26.2.jar](https://github.com/Zazuzin/Zazus-Translator/releases/download/v1.0.0/Zazus-Translator-1.0.0-mc26.2.jar) | 0.19.3 or newer |
| 26.3 | [Zazus-Translator-1.0.0-mc26.3.jar](https://github.com/Zazuzin/Zazus-Translator/releases/download/v1.0.0-mc26.3/Zazus-Translator-1.0.0-mc26.3.jar) | 0.20.1 or newer |

Both versions require **Java 25** and [Fabric API](https://modrinth.com/mod/fabric-api)
for the matching Minecraft version.

1. Put the JAR in your Minecraft instance's `mods` folder alongside Fabric API.
2. Remove any older Zazu Translate or Zazu's Translator JAR from that folder.
3. Start Minecraft. Press **F8** to open the translator settings.

Install the mod on your client. The server does not need it.

## Features

- Automatic translation of incoming player chat.
- Target language follows Minecraft's language, with a custom target option.
- Messages detected as already being in your target language are skipped.
- Click **[Translate]** to translate a message manually.
- Click **[Reply XX]** on a translated line to reply in its detected language.
- Optional bold translations and seven colours: red, blue, green, yellow,
  purple, orange and pink.
- Optional system-message translation and ignored player names.
- MyMemory, DeepL and LibreTranslate-compatible providers.
- Queued requests, a translation cache and rate-limit handling.

## Quick replies

Open chat and click **[Reply XX]** beside a translated message. A normal chat
box opens with an indicator showing the input and reply languages.

Type in the input language shown, then press **Enter** to translate and send.
Press **Escape** to cancel. The reply language applies to that submission only.

These replies go to public chat. Slash commands keep their normal behaviour.
A failed, empty, invalid or oversized translation is not sent. Click the Retry
button to restore the draft. Pending replies are stopped if the server
connection changes.

**F9** also opens the separate reply preview editor. This editor accepts English
and uses the reply language selected in settings. You review its translation
before clicking Send.

Both F8 and F9 can be changed in Minecraft's Controls menu.

## Providers and languages

**MyMemory** is the default and does not require an API key. It uses local
language detection in the mod. Short or ambiguous messages may be skipped, and
the provider can return incorrect translations. The mod checks for empty output
and obvious writing-system mismatches, with a clause retry where possible.

**DeepL** requires your own API key. Select DeepL in F8, enter the key, and use
the endpoint for your API account.

**LibreTranslate** uses a compatible endpoint of your choice. Some endpoints
require an API key.

Available languages and translation quality depend on the provider. The
language picker loads the provider's list for DeepL and LibreTranslate.
MyMemory has a common-language picker and a manual language-code field.

## Settings and existing installations

Settings are saved in `config/zazu-translate.json`. The internal mod ID remains
`zazu_translate` so existing installations keep their settings and key bindings.

When upgrading an older config, the former English target default follows
Minecraft's language. An existing non-English target remains a custom override.
Choose **Target: Minecraft** in F8 to follow the game's language instead.

## Chat data and API keys

Message text is sent to the translation provider you select. Translated incoming
messages are displayed locally. Quick replies reach the Minecraft server only
after you submit them.

API keys are stored in your local config and are visible while editing the
provider settings. Do not share your config or include your key in a bug report.
This repository does not include a shared provider key.

## Reporting a problem

Open an [issue](https://github.com/Zazuzin/Zazus-Translator/issues) with your
Minecraft, Fabric Loader, Fabric API and mod versions, provider name, and an
example message. Screenshots and relevant log lines help. Remove API keys and
private chat before posting.

## Build from source

The `main` branch targets Minecraft 26.2 with JDK 25 and Gradle 9.5.1.
The `mc26.3` branch targets Minecraft 26.3 with JDK 25 and Gradle 9.6.0:

```sh
gradle clean build
```

The installable JAR is written to `build/libs/`. GitHub Actions also runs the
language and reply-validation checks. A version tag in the public repository
builds the JAR, source archive and checksums before publishing the release.

## License

Copyright 2026 Zazuzin. Licensed under GPL-3.0-only. See [LICENSE](LICENSE).

