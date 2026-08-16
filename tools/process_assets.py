"""Key magenta backgrounds and write game PNGs + launcher mipmaps."""
from pathlib import Path
from PIL import Image
import math

SRC = Path(r"C:\Users\Liinad\.grok\sessions\C%3A%5CUsers%5CLiinad\01a00ab1-d814-76a3-998c-5ecd90e3cd29\images")
OUT = Path(r"C:\Users\Liinad\Desktop\NightAndOrder\app\src\main\assets")
RES = Path(r"C:\Users\Liinad\Desktop\NightAndOrder\app\src\main\res")
OUT.mkdir(parents=True, exist_ok=True)

MAP = {
    "1.jpg": "char_lucia.png",
    "3.jpg": "char_hale.png",
    "5.jpg": "char_morvan.png",
    "6.jpg": "char_lilith.png",
    "7.jpg": "enemy_thrall.png",
    "8.jpg": "enemy_bat.png",
    "9.jpg": "enemy_knight.png",
    "10.jpg": "enemy_flagellant.png",
    "11.jpg": "enemy_boss.png",
}


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
    return crop(im)


def crop(im: Image.Image) -> Image.Image:
    bbox = im.getbbox()
    if not bbox:
        return im
    pad = 8
    l, t, r, b = bbox
    l = max(0, l - pad)
    t = max(0, t - pad)
    r = min(im.width, r + pad)
    b = min(im.height, b + pad)
    cut = im.crop((l, t, r, b))
    side = max(cut.size)
    canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    canvas.paste(cut, ((side - cut.size[0]) // 2, (side - cut.size[1]) // 2))
    return canvas


for src_name, dest_name in MAP.items():
    img = Image.open(SRC / src_name)
    keyed = key_magenta(img)
    keyed.save(OUT / dest_name, "PNG")
    print("wrote", dest_name, keyed.size)

tile = Image.open(SRC / "4.jpg").convert("RGB")
# take a 256 crop from the more regular interior
tw = min(tile.size)
tile = tile.crop((0, 0, tw, tw)).resize((256, 256), Image.NEAREST)
tile.save(OUT / "tile_ground.png", "PNG")
print("wrote tile_ground.png")

icon = Image.open(SRC / "2.jpg").convert("RGBA")
# if magenta-ish corners, key; else just square
icon_k = key_magenta(icon)
for density, size in (("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)):
    folder = RES / f"mipmap-{density}"
    folder.mkdir(parents=True, exist_ok=True)
    resized = icon_k.resize((size, size), Image.LANCZOS)
    resized.save(folder / "ic_launcher.png", "PNG")
    resized.save(folder / "ic_launcher_round.png", "PNG")
    print("mipmap", density, size)

print("done")
