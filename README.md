# Remove service numbers

An LSPosed / Vector module for **ColorOS** that keeps the operator's read-only
"service numbers" from the SIM card out of your contacts.

## The problem

Some SIM cards carry a list of operator service numbers (the SIM's *SDN* file:
customer service, balance check and so on). ColorOS's contacts storage
(`com.android.providers.contacts`) copies them into the contacts database as
read-only contacts (account type `SDN Account`, named `SDN1` / `SDN2` per SIM slot).
They then show up in Google Phone, Google Contacts and every other app that reads
contacts, and they can't be deleted. Deleted copies come back at the next boot,
and the SIM file itself can't be changed.

## What the module does

Every time it starts, ColorOS's contacts storage deletes its service-number
contacts and copies them from the SIM again. The module lets the deletion
happen and skips the copy step for service-number accounts. Normal contacts
stored on the SIM are not affected.

Things that are **not** affected:

- Emergency numbers (these come from the modem, the SIM's emergency list and
  carrier settings, not from these entries)
- Voicemail
- The SIM card itself (nothing is written to it)

What you lose: the operator's service numbers no longer appear in contact lists
or search, and calls from those numbers show the plain number without a name.

## Requirements

- ColorOS 12 or later with the Oplus contacts storage. Tested on a realme RMX3363
  running ColorOS 12 / Android 12.
- Root and an LSPosed-compatible framework that supports the modern Xposed API
  (API 101 or later), for example [Vector](https://github.com/JingMatrix/Vector).

## Installing

1. Install the APK.
2. In the LSPosed / Vector manager, enable **Remove service numbers**. Its scope
   is preset to **Contacts Storage** (`com.android.providers.contacts`).
3. Reboot.

To get the service numbers back, disable the module and reboot.

## Building

You need JDK 17 or newer and the Android SDK (platform 37, build tools 36).
Point `local.properties` at the SDK (`sdk.dir=C:/path/to/android-sdk`), then run:

```
gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/`.

To build a signed release, create a `keystore.properties` file in the project
root with `storeFile`, `storePassword`, `keyAlias` and `keyPassword` and run
`gradlew assembleRelease`. The build also accepts the environment variables
`RSN_KEYSTORE_FILE`, `RSN_KEYSTORE_PASSWORD`, `RSN_KEY_ALIAS` and
`RSN_KEY_PASSWORD`. Keystores and `keystore.properties` are excluded from git.

## How it works (technical)

The module runs in `android.process.acore` and hooks
`com.customize.manager.AbstractSimOverlayManager.insertSimContacts(AccountWithDataSet, SimContact[])`
in the Oplus contacts provider. When the account type is `SDN Account`
(or `SDN Account SPRD` / `com.android.contacts.sdn`), the call is skipped.
The hook uses the `PROTECTIVE` exception mode, so a failure falls back to normal
behaviour instead of crashing the contacts provider. The module logs to the
LSPosed log under the tag `RemoveServiceNumbers`.

## Licence

Copyright (C) 2026 Chris (chris-v8)

This program is free software: you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free Software
Foundation, either version 3 of the License, or (at your option) any later
version. See [LICENSE](LICENSE).
