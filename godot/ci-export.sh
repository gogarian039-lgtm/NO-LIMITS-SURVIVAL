#!/bin/sh
set -eu
mkdir -p /root/.config/godot /root/.android /project/build
keytool -genkeypair -keystore /root/.android/debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
printf '%s\n' '[gd_resource type="EditorSettings" format=3]' '' '[resource]' 'export/android/android_sdk_path = "/usr/lib/android-sdk"' 'export/android/debug_keystore = "/root/.android/debug.keystore"' 'export/android/debug_keystore_user = "androiddebugkey"' 'export/android/debug_keystore_pass = "android"' > /root/.config/godot/editor_settings-4.tres
cat /root/.config/godot/editor_settings-4.tres
godot --headless --path /project --export-debug Android build/NO-LIMITS-SURVIVAL-3D-debug.apk
test -s /project/build/NO-LIMITS-SURVIVAL-3D-debug.apk
