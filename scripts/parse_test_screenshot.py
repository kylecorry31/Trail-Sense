import PIL.Image as Image
import base64
import io

log_file = 'emulator.log'

def load_screenshot_base64():
    with open(log_file, 'r') as f:
        text = f.readlines()
    
    lines = [line[line.index('Screenshot: ') + len('Screenshot: '):].strip() for line in text if 'Screenshot: ' in line]
    return ''.join(lines)

def convert_base64_to_image(base64_string):
    image = base64.b64decode(base64_string + '=' * (-len(base64_string) % 4))
    image = Image.open(io.BytesIO(image))
    return image

b64_image = load_screenshot_base64()
image = convert_base64_to_image(b64_image)
image.show()
