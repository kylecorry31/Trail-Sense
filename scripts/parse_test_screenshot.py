import PIL.Image as Image
import base64
import io

log_file = 'test_screenshot_log.txt'

def load_screenshot_base64():
    with open(log_file, 'r') as f:
        text = f.readlines()
    
    lines = [line[line.index('System.out: ') + len('System.out: '):].strip() for line in text if line.strip()]
    joined = ''.join(lines)
    joined = joined.replace('Screenshot: ', '')
    return joined

def convert_base64_to_image(base64_string):
    image = base64.b64decode(base64_string)
    image = Image.open(io.BytesIO(image))
    return image

b64_image = load_screenshot_base64()
image = convert_base64_to_image(b64_image)
image.show()
