from pathlib import Path
import numpy as np
from PIL import Image

ASSETS = Path(r"C:\Users\Liinad\Desktop\NightAndOrder\app\src\main\assets")
NEW_MORVAN = Path(
    r"C:\Users\Liinad\.grok\sessions\C%3A%5CUsers%5CLiinad\01a00ab1-d814-76a3-998c-5ecd90e3cd29\images\14.jpg"
)

FILES = [
    "char_morvan.png",
    "char_lilith.png",
    "char_lucia.png",
    "char_hale.png",
    "char_nix.png",
    "char_sera.png",
    "enemy_thrall.png",
    "enemy_bat.png",
    "enemy_flagellant.png",
    "enemy_knight.png",
    "enemy_boss.png",
]


def key_and_crop(im: Image.Image) -> Image.Image:
    arr = np.array(im.convert("RGBA"))
    h, w = arr.shape[:2]
    corners = np.stack([arr[0, 0], arr[0, -1], arr[-1, 0], arr[-1, -1]]).astype(np.float32)
    avg_a = corners[:, 3].mean()
    if avg_a > 80:
        cr, cg, cb = corners[:, 0].mean(), corners[:, 1].mean(), corners[:, 2].mean()
        magenta = cr > 140 and cb > 140 and cg < 140
        green = cg > 160 and cr < 120 and cb < 120
        if magenta or green:
            rgb = arr[:, :, :3].astype(np.float32)
            dist = np.sqrt(((rgb - np.array([cr, cg, cb])) ** 2).sum(axis=2))
            t = np.clip(dist / 88.0, 0, 1)
            alpha = np.where(t < 0.28, 0, np.clip((t - 0.28) / 0.72 * 255, 0, 255))
            arr[:, :, 3] = np.minimum(arr[:, :, 3], alpha.astype(np.uint8))

    visible = arr[:, :, 3] > 16
    if not visible.any():
        return Image.fromarray(arr)
    ys, xs = np.where(visible)
    pad = 6
    l = max(0, int(xs.min()) - pad)
    t = max(0, int(ys.min()) - pad)
    r = min(w, int(xs.max()) + pad + 1)
    b = min(h, int(ys.max()) + pad + 1)
    cut = arr[t:b, l:r]
    ch, cw = cut.shape[:2]
    side = max(ch, cw)
    canvas = np.zeros((side, side, 4), dtype=np.uint8)
    oy = (side - ch) // 2
    ox = (side - cw) // 2
    canvas[oy : oy + ch, ox : ox + cw] = cut
    out = Image.fromarray(canvas)
    return out.resize((256, 256), Image.LANCZOS)


Image.open(NEW_MORVAN).save(ASSETS / "char_morvan.png")

for name in FILES:
    path = ASSETS / name
    keyed = key_and_crop(Image.open(path))
    keyed.save(path, "PNG")
    print(name, keyed.size, "alpha")
