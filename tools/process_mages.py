from pathlib import Path
from PIL import Image
import math

SRC = Path(r"C:\Users\Liinad\.grok\sessions\C%3A%5CUsers%5CLiinad\01a00ab1-d814-76a3-998c-5ecd90e3cd29\images")
OUT = Path(r"C:\Users\Liinad\Desktop\NightAndOrder\app\src\main\assets")
MAP = {"13.jpg": "char_nix.png", "12.jpg": "char_sera.png"}


def key_magenta(im: Image.Image) -> Image.Image:
    im = im.convert("RGBA")
    px = im.load()
    w, h = im.size
    samples = [px[0, 0], px[w - 1, 0], px[0, h - 1], px[w - 1, h - 1]]
    cr = sum(p[0] for p in samples) / 4
    cg = sum(p[1] for p in samples) / 4
    cb = sum(p[2] for p in samples) / 4
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            dist = math.sqrt((r - cr) ** 2 + (g - cg) ** 2 + (b - cb) ** 2)
            if dist < 95:
                t = dist / 95
                na = 0 if t < 0.32 else int((t - 0.32) / 0.68 * 255)
                px[x, y] = (r, g, b, na)
    bbox = im.getbbox()
    if not bbox:
        return im
    pad = 8
    l, t, r, b = bbox
    cut = im.crop((max(0, l - pad), max(0, t - pad), min(im.width, r + pad), min(im.height, b + pad)))
    side = max(cut.size)
    canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    canvas.paste(cut, ((side - cut.size[0]) // 2, (side - cut.size[1]) // 2))
    return canvas.resize((256, 256), Image.LANCZOS)


for src, dest in MAP.items():
    out = key_magenta(Image.open(SRC / src))
    out.save(OUT / dest, "PNG")
    print("wrote", dest, out.size)
