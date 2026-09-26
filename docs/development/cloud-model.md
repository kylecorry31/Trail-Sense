# Updating the CNN weights for the Cloud Scanner

1. Place `CCSN.zip` in the repository root. The trainer reads images directly from its `CCSN_v2` folders; it does not extract or modify the archive or the original images in `app/src/androidTest/assets/clouds`.
2. Create and activate a virtual environment, then install the training dependencies:

   ```sh
   python3 -m venv .venv-cloud-cnn
   source .venv-cloud-cnn/bin/activate
   python -m pip install numpy Pillow
   python -m pip install -r scripts/experiments/requirements-cloud-cnn.txt --index-url https://download.pytorch.org/whl/cpu
   ```

3. Run `python scripts/experiments/cloud_cnn.py --report /tmp/cloud-training.json`. Use `--dataset /path/to/CCSN.zip` to select another copy of the archive.
4. The trainer selects a checkpoint on a seeded, stratified 20% validation split, then trains a fresh model on all selected images for the selected number of epochs. It writes `app/src/main/assets/cloud_cnn_weights.webp`. The optional JSON report includes the selected epoch, validation history, confusion matrix (actual classes in rows, predictions in columns), and accuracy. Use `--epochs`, `--learning-rate`, and `--seed` to override the defaults.

The ten outputs follow `CloudCNNClassifier.CLOUD_GENUSES`: cirrus (Ci), cirrocumulus (Cc), cirrostratus (Cs), altostratus (As), altocumulus (Ac), nimbostratus (Ns), stratocumulus (Sc), cumulus (Cu), stratus (St), and cumulonimbus (Cb). CCSN's Ct folder is excluded. There is no clear-sky output in this model.

## Architecture

Both implementations consume 128×128 RGB images scaled to [0, 1] in channel-first order. Python resizes with Pillow bilinear filtering and Android uses filtered bitmap scaling. The model uses standard convolutions:

| Stage | Output |
| --- | --- |
| 3×3 convolution, stride 2, ReLU | 64×64×8 |
| 3×3 convolution, stride 2, ReLU | 32×32×12 |
| 3×3 convolution, stride 2, ReLU | 16×16×20 |
| 3×3 convolution, stride 2, ReLU | 8×8×32 |
| Concatenated global average and global max pooling | 64 |
| Dense, softmax | 10 |

All convolutions use one pixel of zero padding. Average pooling summarizes broad cloud coverage while max pooling retains localized features. Training includes batch normalization before each ReLU; the exporter folds it into the convolution weights and biases. Kotlin needs no batch normalization implementation or extra normalization parameters. Cross entropy consumes logits during training; inference applies softmax.

The exported model has 9,722 float32 parameters (38,888 bytes before compression), compared with 10,482 in the previous depthwise model. Convolution and dense multiply-accumulate counts are 2,691,712 per image versus 2,520,192 previously (about 7% more). The largest convolution activation shrinks from 49,152 to 32,768 floats. These are operation/storage counts, not device latency measurements.

## Training and augmentation

Defaults are 120 maximum epochs, AdamW at a 0.003 initial learning rate, 0.01 weight decay, cosine learning-rate decay to 0.0001, batches of 32, and 0.05 label smoothing. Every ten epochs, and at the last epoch, the trainer evaluates unaugmented validation images and retains the most accurate checkpoint. The full-data deployment run uses that selected duration with the same learning-rate schedule as the validation run.

Augmentation happens only in training, with fresh transformations each batch:

- Horizontal flip with probability 0.5.
- Crop scale of 0.7–1.0, translation of ±0.1 in normalized image coordinates, and rotation of approximately ±14°; border sampling fills areas outside the image.
- Independent brightness and contrast factors of 0.8–1.2.
- Gaussian noise with standard deviation 0.015 on the [0, 1] scale, followed by clipping to that range.

The source images and validation images remain unchanged. The original personal dataset is not used for training.

## WebP format and verification

Inference remains entirely in Kotlin. Weights are packed as opaque RGB bytes in a lossless WebP with `TSCL` magic, version 3, an unsigned little-endian 16-bit parameter count, and little-endian float32 values. Parameters follow layer order, with each convolution's folded OIHW weights followed by its biases, then dense output-major weights and biases. Dense inputs contain the 32 averages followed by the 32 maxima. Version 3 rejects weights from the previous architecture. Android decodes the asset on first classification.

The Python tests check CCSN category mapping, augmentation invariants, batch normalization folding, the parameter budget, and exact WebP byte preservation. Run them with `python scripts/experiments/test_cloud_cnn.py`. The Kotlin parity test checks inference against deterministic PyTorch predictions. The emulator test checks loading the deployed WebP through Android's bitmap decoder.

## Model selection

The previous 128×128 depthwise model achieved 30.4% validation accuracy after 100 epochs. Experiments compared standard and depthwise convolutions, 64/96/128-pixel inputs, wider models, stronger augmentation, average/max/variance pooling, spatial pooling, a longer 300-epoch schedule, and normalization recalibration. The selected standard-convolution model stays within the approximately 10K-parameter budget. Larger models did not provide enough additional accuracy to justify their parameter counts, and the extra pooling statistics/spatial layout did not improve the compact model.

With seed 1 and the defaults above, the final reproducibility run selected epoch 120 and classified 188 of 464 validation images correctly: **40.5%**, compared with **30.4%** for the previous model. The validation training subset contains 1,879 images. Balanced accuracy (mean recall across the ten classes) is 37.9%, so classification accuracy remains limited. The full-data deployment WebP is 36,252 bytes.

The validation split is reused for architecture and checkpoint selection. Its score is a tuning result, not an unbiased test-set estimate or a guarantee of field accuracy. All validation examples are excluded from the corresponding training run; the separately trained deployment model uses all 2,343 selected CCSN images.
