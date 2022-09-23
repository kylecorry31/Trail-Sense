@REM accepts a single argument which is the zip file of the folders containing cloud images (ex. stratus/ cirrus/)
adb push -p %1 sdcard/Documents/clouds.zip
adb shell rm -rf sdcard/Documents/clouds
adb shell unzip sdcard/Documents/clouds.zip -d sdcard/Documents/clouds
adb shell rm sdcard/Documents/clouds.zip