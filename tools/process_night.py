"""Key magenta night-content sprites and write chapel tile."""
from pathlib import Path
from PIL import Image
import math

SRC = Path(r"C:\Users\Liinad\.grok\sessions\C%3A%5CUsers%5CLiinad\01a00ab1-d814-76a3-998c-5ecd90e3cd29\images")
OUT = Path(r"C:\Users\Liinad\Desktop\NightAndOrder\app\src\main\assets")
OUT.mkdir(parents=True, exist_ok=True)

JOBS = [
    ("28.jpg", "enemy_warden.png", 160),
    ("29.jpg", "enemy_herald.png", 192),
    ("30.jpg", "enemy_archer.png", 96),
    ("31.jpg", "prop_slab.png", 96),
    ("32.jpg", "prop_cross.png", 112),
    ("34.jpg", "enemy_vessel.png", 112),
    ("35.jpg", "prop_pillar.png", 128),
]


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
    return im


def crop_square(im: Image.Image) -> Image.Image:
    bbox = im.getbbox()
    if not bbox:
        return im
    pad = 6
    l, t, r, b = bbox
    l = max(0, l - pad)
    t = max(0, t - pad)
    r = min(im.width, r + pad)
    b = min(im.height, b + pad)
    cut = im.crop((l, t, r, b))
    side = max(cut.size)
    canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    canvas.paste(cut, ((side - cut.size[0]) // 2, (side - cut.size[1]) // 2), cut)
    return canvas


for src, dest, size in JOBS:
    path = SRC / src
    if not path.exists():
        print("missing", src)
        continue
    keyed = crop_square(key_magenta(Image.open(path)))
    keyed = keyed.resize((size, size), Image.LANCZOS)
    keyed.save(OUT / dest, "PNG")
    print("wrote", dest, keyed.size)

tile = Image.open(SRC / "33.jpg").convert("RGB")
side = min(tile.size)
tile = tile.crop((0, 0, side, side)).resize((128, 128), Image.BILINEAR)
tile.save(OUT / "tile_chapel.png", "PNG")
print("wrote tile_chapel.png")
