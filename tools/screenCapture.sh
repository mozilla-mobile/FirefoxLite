# Unlock emulator
# adb shell input keyevent 82
mkdir -p app/build/outputs/apk/focusWebkit/debug/
adb shell screencap -p > app/build/outputs/apk/focusWebkit/debug/bootscreen.png
# Continue running some more tests