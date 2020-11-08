# keytool -genkey -validity 10000 -keystore key.keystore -keyalg RSA -keysize 2048 -storepass bassiemusic -keypass bassiemusic
PATH=$PATH:~/android-sdk/build-tools/30.0.2:~/android-sdk/platform-tools
PLATFORM=~/android-sdk/platforms/android-30/android.jar
if [ "$1" == "log" ]; then
    adb logcat -c
    adb logcat *:E
else
    if aapt package -m -J src -M AndroidManifest.xml -S res -I $PLATFORM; then
        mkdir classes
        if javac -Xlint -cp $PLATFORM -d classes src/nl/plaatsoft/bassiemusic/*.java; then
            dx.bat --dex --output=classes.dex classes
            aapt package -F bassiemusic-unaligned.apk -M AndroidManifest.xml -S res -I $PLATFORM
            aapt add bassiemusic-unaligned.apk classes.dex
            zipalign -f -p 4 bassiemusic-unaligned.apk bassiemusic.apk
            rm -r classes src/nl/plaatsoft/bassiemusic/R.java classes.dex bassiemusic-unaligned.apk
            apksigner.bat sign --ks key.keystore --ks-pass pass:bassiemusic --ks-pass pass:bassiemusic bassiemusic.apk
            adb install -r bassiemusic.apk
            adb shell am start -n nl.plaatsoft.bassiemusic/.MainActivity
        else
            rm -r classes src/nl/plaatsoft/bassiemusic/R.java
        fi
    fi
fi
