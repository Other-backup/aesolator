# Wi-Fi ADB Debug

Updated: `2026-03-18`

This runbook keeps the local `Termux` side ready for Android `Wireless
debugging` without reconstructing the pairing/connect/install commands from old
chat logs.

## Local Base

- `adb` comes from the Termux `android-tools` package
- local ADB keys live under:
  `/data/data/com.termux/files/home/.android`
- helper script:
  `tools/adb-wifi-debug.sh`

Current dry-check command:

```sh
cd /data/data/com.termux/files/home/aesolator
sh tools/adb-wifi-debug.sh status
```

## Pair And Connect

On the Android device:

1. open `Developer options`
2. open `Wireless debugging`
3. choose `Pair device with pairing code`
4. note:
   - pairing endpoint: `IP:pair_port`
   - pairing code
   - debug endpoint: `IP:connect_port`

From Termux:

```sh
cd /data/data/com.termux/files/home/aesolator
sh tools/adb-wifi-debug.sh pair 192.168.0.10:37099 123456 192.168.0.10:42363
```

If the device is already paired and only needs reconnect:

```sh
sh tools/adb-wifi-debug.sh connect 192.168.0.10:42363
```

## Same-Device Loopback Fallback

When `Termux` runs on the same Android device, the most stable practical
fallback is to switch `adbd` into classic TCP mode and then reconnect through
loopback instead of the ephemeral Wireless Debugging port.

Bootstrap from a working wireless-debug endpoint:

```sh
sh tools/adb-wifi-debug.sh tcpip-loopback 192.168.0.10:42363
```

After that, reuse the local same-device endpoint:

```sh
sh tools/adb-wifi-debug.sh connect-loopback
```

This yields `127.0.0.1:5555` on the same phone.

Important:

- this is a practical fallback, not a permanent no-root system hack
- after reboot or `adbd` restart, `tcpip 5555` may need to be re-enabled once
- for same-device `Termux`, `127.0.0.1:5555` is preferable to chasing a fresh
  Wireless Debugging port every session

## Status / Disconnect

```sh
sh tools/adb-wifi-debug.sh status
sh tools/adb-wifi-debug.sh disconnect 192.168.0.10:42363
```

Without an explicit endpoint, `disconnect` drops every current ADB target.

## Install And Launch

Default debug APK path:

```text
/data/data/com.termux/files/home/aesolator/app/build/outputs/apk/debug/app-debug.apk
```

Install the latest local debug APK:

```sh
sh tools/adb-wifi-debug.sh install-debug 192.168.0.10:42363
```

Launch smoke test:

```sh
sh tools/adb-wifi-debug.sh launch 192.168.0.10:42363
```

The install helper uses `adb install -r -d`, which matches the existing local
debug workflow when the device already carries a higher `versionCode`.
