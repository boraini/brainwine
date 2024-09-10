# Touhou Project - Bad Apple License Statement

[badapple.json](./badapple.json) has been created from the frames found in <https://archive.org/details/bad_apple_is.7z> using the following Python script:

```python
import sys
import cv2
import numpy as np

EVERY = 30 / 10

USE_UP_TO = 6562

LINE_BREAK = ""

print(file=sys.stderr)

print("[", end=LINE_BREAK)

for result_index in range(int(USE_UP_TO / EVERY)):
    frame_number = round(result_index * EVERY) + 1

    if frame_number >= 6562:
        break

    print(f"Frame: {frame_number}", file=sys.stderr, end="\r")

    img = cv2.imread(f"image_sequence/bad_apple_{str(frame_number).zfill(3)}.png", cv2.IMREAD_GRAYSCALE)

    im_gray = cv2.resize(img, (20, 15))

    im_bw = cv2.threshold(im_gray, 127, 255, cv2.THRESH_BINARY)[1]

    print("[", end=LINE_BREAK)
    for row in range(im_bw.shape[0]):
        # print()

        bin = np.packbits([x > 127 for x in im_bw[row]])
        print("[", end=LINE_BREAK)
        print(",".join(str(x) for x in bin), end=LINE_BREAK)
        print("]", end=LINE_BREAK)
        if row < im_bw.shape[0] - 1:
            print(",", end=LINE_BREAK)
    print("],", end=LINE_BREAK)
print("]")
```