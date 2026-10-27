# Shockwave (Android)

![License: Apache-2.0](https://img.shields.io/badge/license-Apache%202.0-blue) ![Pre-release v0.1.0](https://img.shields.io/badge/release-v0.1.0%20pre--release-orange)

A fast, battery-friendly **local music player** for Android 8.0+. Your files only — no accounts, no ads, no tracking.

## Features

- **Library & Home** — incremental MediaStore scan into on-device storage; albums, artists, playlists, likes, history and play counts. Home feed ranked by your own listening.
- **Playback** — Media3 ExoPlayer in a foreground service: notification, lock-screen / Bluetooth controls, audio focus, pause on headphone unplug.
- **Queue** — play next, drag to reorder, swipe to remove with undo, shuffle and repeat with undo, queue restored after restart.
- **Lyrics** — synced lines from embedded tags or `.lrc` files, with optional online lookup on [LRCLIB](https://lrclib.net) (turn it off for a fully offline app).
- **Player** — light / dark UI, accent colors from the cover art, three seek-bar styles (Wavy, Waveform, Bars), sleep timer with fade-out.
- **Persian & RTL** — full Persian translation, Vazirmatn font, mirrored layouts.
- **Battery saver** — disables motion, blur and grain; honours the system reduce-motion setting.

## Install

Download `Shockwave-v0.1.0.apk` from the [Releases page](https://github.com/mmxkarimi/Shockwave/releases), open it on your device and allow "install unknown apps".

## Privacy

Nothing leaves the device: your library, likes, playlists and history stay local. The only network request is the optional LRCLIB lyrics lookup (track title, artist, album, duration), disable in Settings.

## Credits

Shockwave is a fork of the Android music player originally written by **mhrsdev** — original source code: [github.com/mhrsdev/music-player-mowj](https://github.com/mhrsdev/music-player-mowj). The original work was released under the MIT License; that copyright notice is retained in the [NOTICE](NOTICE) file as required.

## License

[Apache License 2.0](LICENSE) — Copyright 2026 Mohammad mahdi Karimi.

This project is distributed under the Apache License, Version 2.0, in accordance with the upstream license terms (MIT permits relicensing of derivative works as long as the original copyright notice is retained — see [NOTICE](NOTICE)). Bundled fonts (Google Sans Flex, Vazirmatn) are under the [SIL Open Font License 1.1](app/src/main/assets/licenses/FONTS-NOTICE.txt).
