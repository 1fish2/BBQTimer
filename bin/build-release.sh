#!/bin/sh
# Builds a release APK.
# See https://github.com/almalkawi/Android-Guide/wiki/Generating-signed-release-APK-using-Gradle
# See http://stackoverflow.com/questions/18328730/how-to-create-a-release-signed-apk-file-using-gradle

export ANDROID_KEYSTORE="$HOME/.android/1fish2_keystore.jks"

./gradlew assembleRelease
