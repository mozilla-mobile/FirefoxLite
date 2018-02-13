# Unlock emulator
# adb shell input keyevent 82
mkdir -p app/build/outputs/apk/focusWebkit/test/
adb shell screencap -p > app/build/outputs/apk/focusWebkit/test/app-focus-webkit-test.apk
# Continue running some more tests