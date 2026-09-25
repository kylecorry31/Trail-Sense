# Updating the CNN weights for the Cloud Scanner

1. Add categorized images to `app/src/androidTest/assets/clouds`. Use lowercase `CloudGenus` names for the cloud folders and `clear` for images without clouds.
2. Create and activate a virtual environment, then install the training dependencies:

	```sh
	python3 -m venv .venv-cloud-cnn
	source .venv-cloud-cnn/bin/activate
	python -m pip install numpy Pillow
	python -m pip install -r scripts/experiments/requirements-cloud-cnn.txt --index-url https://download.pytorch.org/whl/cpu
	```

3. Run `python scripts/experiments/cloud_cnn.py`.
4. The PyTorch trainer defaults to 800 epochs at a 0.005 learning rate. It reports stratified validation accuracy, then trains on all images and writes a lossless WebP model asset to `app/src/main/assets/cloud_cnn_weights.webp`. Use `--epochs` and `--learning-rate` to override these defaults.

The class order is defined by `CloudCNNClassifier.CLOUD_GENUSES`, followed by `clear`. The model uses channel-first 16x16 RGB inputs, 12 channels in each convolution layer, 2x2 max pooling, global average pooling, and an 11-class output. Its 1,787 float32 weights occupy about 7 KiB; weights plus live inference arrays are about 23 KiB. Weights are packed as opaque RGB bytes with a versioned header and little-endian float32 payload; Android decodes the WebP to an `IntArray` on first classification.

An NRBR single-channel input was evaluated on the same three stratified splits and averaged 44.2% accuracy versus 50.4% for RGB, so RGB remains the deployed input.