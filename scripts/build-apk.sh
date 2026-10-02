#!/usr/bin/env bash
# Dependency-free debug APK build using official Android SDK tools.
set -euo pipefail
cd "$(dirname "$0")/.."
sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$sdk" ]]; then echo 'Set ANDROID_HOME to an Android SDK with platform 35 and build-tools 35.0.0.' >&2; exit 1; fi
bt="$sdk/build-tools/35.0.0"
android_jar="$sdk/platforms/android-35/android.jar"
for tool in "$bt/aapt2" "$bt/d8" "$bt/zipalign" "$bt/apksigner"; do
    [[ -x "$tool" ]] || { echo "Missing SDK tool: $tool" >&2; exit 1; }
done
rm -rf build/apk/classes build/apk/dex
mkdir -p build/apk/classes build/apk/dex artifacts
"$bt/aapt2" compile --dir app/src/main/res -o build/apk/resources.zip
# Gradle supplies the namespace; raw aapt2 needs it in a temporary manifest.
sed 's/<manifest /<manifest package="org.heatlab" /' app/src/main/AndroidManifest.xml > build/apk/AndroidManifest.xml
"$bt/aapt2" link -o build/apk/resources.apk -I "$android_jar" --manifest build/apk/AndroidManifest.xml build/apk/resources.zip
javac --release 17 -cp "$android_jar" -d build/apk/classes app/src/main/java/org/heatlab/*.java
jar --create --file build/apk/classes.jar -C build/apk/classes .
"$bt/d8" --min-api 26 --lib "$android_jar" --output build/apk/dex build/apk/classes.jar
cp build/apk/resources.apk build/apk/unsigned.apk
(cd build/apk/dex && zip -q -u ../unsigned.apk classes*.dex)
"$bt/zipalign" -f 4 build/apk/unsigned.apk build/apk/aligned.apk
if [[ ! -f build/apk/debug.jks ]]; then
    keytool -genkeypair -keystore build/apk/debug.jks -storepass android -keypass android -alias androiddebugkey \
        -dname 'CN=Heat Lab Debug,O=Local Development,C=US' -keyalg RSA -keysize 2048 -validity 10000
fi
"$bt/apksigner" sign --ks build/apk/debug.jks --ks-pass pass:android --v4-signing-enabled false --out artifacts/heat-lab-debug.apk build/apk/aligned.apk
"$bt/apksigner" verify --verbose artifacts/heat-lab-debug.apk
sha256sum artifacts/heat-lab-debug.apk > artifacts/heat-lab-debug.apk.sha256
echo 'Built artifacts/heat-lab-debug.apk'
