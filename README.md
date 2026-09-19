<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="128" alt="MK Volume Plus app icon">
</p>

<h1 align="center">MK Volume Plus</h1>

MK Volume Plus is a small Android utility for the **Mudita Kompakt** that unlocks a louder maximum in-call volume for the earpiece and speakerphone.

It was designed specifically for the Kompakt and its MediaTek audio stack.

## Features

- Earpiece Volume Unlock — up to +6 dB above the stock maximum
- Speakerphone Volume Unlock — up to +7 dB above the stock maximum
- Changes take effect during an active call
- Optional automatic reapply after reboot
- One-time safety warning before Volume Unlock is first enabled
- Localized interface in English, Spanish, French, Polish, German, Italian,
  European Portuguese, and Dutch
- MMD About, Limitations, Privacy, and License pages
- High-contrast, animation-free interface designed for the Kompakt's e-ink display

## Compatibility

Tested on:

- **Mudita Kompakt**
- MuditaOS K 1.5 only
- Android 12 / API 31
- 480 × 800 e-ink display

MK Volume Plus uses MediaTek-specific Android audio parameters. It should **not** be assumed to work on other Android devices.

No root access, bootloader unlock, system modification, or privileged installation is required.

## Safety

MK Volume Plus intentionally allows call volume to exceed the device's stock maximum.

The application displays the following warning before Volume Unlock is first enabled:

> **Warning!**
>
> MK Volume Plus allows call volume to exceed the device’s stock maximum. High volume may damage the speaker or even damage your hearing. Use with caution.

Use the lowest comfortable volume.

## How it works

MK Volume Plus modifies the Kompakt's runtime MediaTek call-volume parameter tables.

For the earpiece, the stock maximum uses:

21,18,15,12,9,6,3

MK Volume Plus uses:

21,18,15,12,9,0,3

For speakerphone, the stock table is:

22,19,16,13,10,7,4

MK Volume Plus uses:

22,19,16,13,10,0,4

These changes are runtime-only. MK Volume Plus does **not** permanently rewrite the device's vendor audio calibration files.

Because the runtime audio tables return to stock after reboot, the optional Reapply after reboot feature restores the user's enabled Volume Unlock settings after Android finishes booting.

## Installation

Download the APK from the project's GitHub Releases page and install it on the Mudita Kompakt.

Android may ask you to allow installation from the application you use to open the APK.

Application ID:

com.chad.mkvolumeplus

## Current release

**v1.2.0**

v1.2.0 adds seven European translations, MMD information and legal pages, the
Louisiana bayou artwork, and refined 480 × 800 layout and navigation. It also
standardizes the product name as MK Volume Plus.

Android version metadata:

versionCode = 3
versionName = 1.2.0

SHA-256 of the signed v1.2.0 APK:

4CCFB5B0FB863AD4EBDD2CFC79AB99273D579227A5EA344F3F0DC8B05C56DDD0

### Previous release

v1.1.0 was the first public release. v1.0.0 was an internal development
milestone and was not publicly released.

SHA-256 of the signed v1.1.0 APK:

45609976CC69037AE932406D1C3669D9377A3FDBBEEA69667A400AA90BB574DB

## Building from source

The project is an Android application written in Kotlin using Jetpack Compose.

Open the repository in Android Studio and build the `app` module.

Current Android configuration:

minSdk = 31
targetSdk = 37
compileSdk = 37

Release APKs published by this project are signed separately from the source repository. Signing keys are not included.

## Device-specific implementation

MK Volume Plus relies on MediaTek-specific audio behavior exposed through Android's `AudioManager` parameter interface.

This is device-specific behavior rather than a standard Android volume-control API. Firmware changes may affect compatibility.

The application intentionally does not:

- require root
- modify `/system`
- modify `/vendor`
- bypass SELinux
- permanently save altered MediaTek calibration files

## E-ink interface

The interface was designed for the Mudita Kompakt's e-ink display with an emphasis on:

- black-and-white presentation
- stable screen geometry
- large touch targets
- minimal screen changes
- no decorative animation
- no dependence on color

## Disclaimer

MK Volume Plus is an independent project and is not affiliated with, endorsed by, or supported by Mudita.

Increasing audio output above the manufacturer's stock maximum may cause distortion, speaker damage, or hearing damage. Use at your own risk.

## License

MK Volume Plus is released under the MIT License. See LICENSE.
