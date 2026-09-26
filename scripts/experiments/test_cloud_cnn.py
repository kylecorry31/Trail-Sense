import io
import struct
import tempfile
import unittest
import zipfile
from pathlib import Path

import numpy as np
import torch
from PIL import Image

import cloud_cnn as cnn


class CloudCNNTest(unittest.TestCase):
    def test_archive_class_mapping_and_exclusions(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "CCSN.zip"
            with zipfile.ZipFile(path, "w") as archive:
                for label, category in enumerate(cnn.CCSN_CLASSES):
                    stream = io.BytesIO()
                    Image.new("RGB", (8, 8), (label * 20, 100, 200)).save(stream, "PNG")
                    archive.writestr(f"CCSN_v2/{category}/sample.png", stream.getvalue())
                archive.writestr("CCSN_v2/Ct/ignored.jpg", b"not an image")
                archive.writestr("__MACOSX/CCSN_v2/Ci/._sample.jpg", b"metadata")
            inputs, labels = cnn.load_dataset(path)
            self.assertEqual(inputs.shape, (10, 3, cnn.INPUT_SIZE, cnn.INPUT_SIZE))
            np.testing.assert_array_equal(labels, np.arange(10))
            np.testing.assert_allclose(inputs[:, 0, 0, 0], np.arange(10) * 20 / 255)
            np.testing.assert_allclose(inputs[:, 1, 0, 0], 100 / 255)
            np.testing.assert_allclose(inputs[:, 2, 0, 0], 200 / 255)

    def test_webp_preserves_header_and_float_parameters(self):
        weights = cnn.flatten_weights(cnn.CloudCNN())
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "weights.webp"
            cnn.save_weights_webp(weights, path)
            with Image.open(path) as image:
                payload = np.asarray(image.convert("RGB"), dtype=np.uint8).tobytes()
            self.assertEqual(payload[:4], b"TSCL")
            self.assertEqual(struct.unpack("<BH", payload[4:7]), (3, cnn.WEIGHT_COUNT))
            decoded = np.frombuffer(payload, dtype="<f4", count=cnn.WEIGHT_COUNT, offset=7)
            np.testing.assert_array_equal(decoded, weights)

    def test_batch_normalization_folding_preserves_predictions(self):
        torch.manual_seed(9)
        model = cnn.CloudCNN()
        images = torch.rand(4, 3, cnn.INPUT_SIZE, cnn.INPUT_SIZE)
        # Exercise learned normalization parameters and non-default running statistics.
        for module in model.modules():
            if isinstance(module, torch.nn.BatchNorm2d):
                with torch.no_grad():
                    module.weight.uniform_(0.5, 1.5)
                    module.bias.uniform_(-0.2, 0.2)
        model(images)
        model.eval()
        folded = cnn.inference_model(model)
        self.assertFalse(any(isinstance(layer, torch.nn.BatchNorm2d)
                             for layer in folded.modules()))
        self.assertEqual(sum(p.numel() for p in folded.parameters()), cnn.WEIGHT_COUNT)
        self.assertLessEqual(cnn.WEIGHT_COUNT, 11000)
        with torch.no_grad():
            torch.testing.assert_close(folded(images), model(images), atol=1e-5, rtol=1e-5)

    def test_augmentation_preserves_shape_range_and_source(self):
        torch.manual_seed(3)
        images = torch.linspace(0, 1, 2 * 3 * cnn.INPUT_SIZE ** 2).reshape(
            2, 3, cnn.INPUT_SIZE, cnn.INPUT_SIZE
        )
        original = images.clone()
        augmented = cnn.augment(images)
        torch.testing.assert_close(images, original)
        self.assertEqual(augmented.shape, images.shape)
        self.assertTrue(torch.isfinite(augmented).all())
        self.assertTrue(((augmented >= 0) & (augmented <= 1)).all())
        self.assertFalse(torch.equal(augmented, images))


if __name__ == "__main__":
    torch.set_num_threads(2)
    unittest.main()
