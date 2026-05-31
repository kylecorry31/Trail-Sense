# Updating the ML model weights for the Cloud Scanner

1. Extract new categorized cloud images to `app/src/androidTest/assets/clouds`. Use the lowercase name of each CloudGenus enum as the folder name and place all images of that cloud in the folder. Not supported yet, but use "clear" as the folder name for images without clouds.
2. Uncomment @Test and run CloudTrainingDataGenerator.generateTrainingData() on an emulator
3. Run `scripts/update-cloud-data.bat`
4. Uncomment @Test and run CloudTrainer.train()
5. Update the cloud weights in SoftmaxCloudClassifier with src/data/output/weights.txt