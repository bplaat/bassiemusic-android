#!/bin/bash

# --- Bassie Android Build Script v1.0 ---

# The default gradle Android build toolchain is so slow and produces bloated apks
# So I use this nice build shell script to get the job done!

# Install the OpenJDK JDK 8 and add all binaries to your path
# Install your Android SDK at ~/android-sdk/ with the following packages:
# platform-tools, platforms;android-30, build-tools;30.0.3
# Run this script with bash on Linux or a Git Bash / Msys install on Windows
# For inspecting apks you need to install Jadx GUI and add it to your path

PATH=$PATH:~/android-sdk/build-tools/30.0.3:~/android-sdk/platform-tools
PLATFORM=~/android-sdk/platforms/android-30/android.jar

name="bassiemusic"
package="nl.plaatsoft.bassiemusic"
password="bassiemusic"
main_activity=".activities.MainActivity"

if [ "$1" == "key" ]; then
    keytool -genkey -validity 7120 -keystore keystore.jks -keyalg RSA -keysize 4096 -storepass $password -keypass $password

elif [ "$1" == "log" ]; then
    adb logcat -c
    adb logcat *:E

elif [ "$1" == "clear" ]; then
    echo "Clearing data and opening application"
    adb shell pm clear $package
    adb shell am start -n $package/$main_activity

else
    mkdir res-compiled
    echo "Compiling resources files"
    if aapt2 compile --dir res -o res-compiled; then
        if aapt2 link res-compiled/*.flat --manifest AndroidManifest.xml --java src -I $PLATFORM -o $name-unaligned.apk; then

            echo "Compiling java code"
            mkdir src-compiled
            find src -name *.java > sources.txt
            if javac -Xlint -cp $PLATFORM -d src-compiled @sources.txt; then

                echo "Packing and signing application"
                find src-compiled -name *.class > classes.txt
                if [ "$(uname -o)" == "Msys" ]; then
                    d8.bat --release --lib $PLATFORM --min-api 21 @classes.txt
                else
                    d8 --release --lib $PLATFORM --min-api 21 @classes.txt
                fi
                aapt add $name-unaligned.apk classes.dex > /dev/null

                zipalign -f -p 4 $name-unaligned.apk $name.apk

                if [ "$(uname -o)" == "Msys" ]; then
                    apksigner.bat sign --v4-signing-enabled false --ks keystore.jks --ks-pass pass:$password --ks-pass pass:$password $name.apk
                else
                    apksigner sign --v4-signing-enabled false --ks keystore.jks --ks-pass pass:$password --ks-pass pass:$password $name.apk
                fi

                if [ "$1" == "inspect" ]; then
                    echo "Inspecting application"
                    jadx-gui $name.apk
                else
                    echo "Installing and opening application"
                    adb install -r $name.apk
                    adb shell am start -n $package/$main_activity
                fi

                rm -f classes.txt classes.dex
            fi
            rm -f -r src-compiled sources.txt
        fi
        rm -f -r $name-unaligned.apk src/${package//\./\/}/R.java
    fi
    rm -f -r res-compiled
fi
