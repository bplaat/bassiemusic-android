#!/bin/bash

# The default gradle Android build toolchain is slow on my old laptop and produces bloated apks
# So I use this nice build shell script to get the job done!

PATH=$PATH:~/android-sdk/build-tools/30.0.2:~/android-sdk/platform-tools
PLATFORM=~/android-sdk/platforms/android-30/android.jar

name="bassiemusic"
package="nl.plaatsoft.bassiemusic"
password="bassiemusic"

if [ "$1" == "key" ]; then
    keytool -genkey -validity 7120 -keystore keystore.jks -keyalg RSA -keysize 4096 -storepass $password -keypass $password

elif [ "$1" == "log" ]; then
    adb logcat -c
    adb logcat *:E

else
    mkdir res-compiled
    if aapt2 compile --dir res -o res-compiled; then
        if aapt2 link res-compiled/*.flat --manifest AndroidManifest.xml --java src -I $PLATFORM -o $name-unaligned.apk; then

            mkdir src-compiled
            find src -name *.java > sources.txt
            if javac -Xlint -cp $PLATFORM -d src-compiled @sources.txt; then

                find src-compiled -name *.class > classes.txt
                d8.bat --release --lib $PLATFORM @classes.txt
                aapt add $name-unaligned.apk classes.dex > /dev/null

                zipalign -f -p 4 $name-unaligned.apk $name.apk

                apksigner.bat sign --v4-signing-enabled false --ks keystore.jks --ks-pass pass:$password --ks-pass pass:$password $name.apk

                adb install -r $name.apk
                adb shell am start -n $package/.MainActivity

                rm -f classes.txt classes.dex
            fi
            rm -f -r src-compiled sources.txt
        fi
        rm -f -r $name-unaligned.apk src/${package//\./\/}/R.java
    fi
    rm -f -r res-compiled
fi
