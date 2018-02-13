# Unlock emulator
# adb shell input keyevent 82
mkdir -p app/build/outputs
adb shell screencap -p > app/build/outputs/bootscreen.png
# Continue running some more tests