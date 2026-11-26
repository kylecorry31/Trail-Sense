# Updating the ML model weights for the Cloud Scanner

The Cloud Scanner is a softmax classifier over 11 features computed by `SoftmaxCloudClassifier`:

- The mean and standard deviation of NRBR, `(R - B) / (R + B)`, on linearized sRGB.
- Local binary pattern shapes (flat, edge, irregular) at radii 1, 2, and 4 on a 200x200 brightness image.

1. Extract categorized cloud images to `app/src/androidTest/assets/clouds`. Use the lowercase name of each CloudGenus enum as the folder name, and `clear` for images without clouds.
2. Run `CloudTrainingDataGenerator.generateTrainingData()` on an emulator: `tsctl test android com.kylecorry.trail_sense.tools.weather.domain.clouds.CloudTrainingDataGenerator`. If it fails with a permission error, delete the old `/sdcard/Documents/clouds.csv` on the emulator first.
3. Pull `/sdcard/Documents/clouds.csv` into `app/src/data/clouds.csv` (`scripts/update-cloud-data.bat`, or `tsctl adb -- pull /sdcard/Documents/clouds.csv app/src/data/clouds.csv`).
4. Run `python3 scripts/train-cloud-model.py` (requires numpy and scipy). It prints the cross-validated accuracy, grouped by capture date so burst photos don't leak between train and test, and the weights.
5. Replace the weights in `SoftmaxCloudClassifier` with the printed weights.
6. Check the result with `tsctl test android com.kylecorry.trail_sense.tools.clouds.CloudClassifierTest`.
