from pathlib import Path
import numpy as np
from PIL import Image

SRC = Path(r"C:\Users\Liinad\.grok\sessions\C%3A%5CUsers%5CLiinad\01a00ab1-d814-76a3-998c-5ecd90e3cd29\images")
OUT = Path(r"C:\Users\Liinad\Desktop\NightAndOrder\app\src\main\assets")


def key_and_crop(im: Image.Image, size: int) -> Image.Image:
    arr = np.array(im.convert("RGBA"))
    h, w = arr.shape[:2]
    corners = np.stack([arr[2, 2], arr[2, w - 3], arr[h - 3, 2], arr[h - 3, w - 3]]).astype(np.float32)
    cr, cg, cb = corners[:, :3].mean(0)
    dist = np.sqrt(((arr[:, :, :3].astype(np.float32) - np.array([cr, cg, cb])) ** 2).sum(2))
    t = np.clip(dist / 80.0, 0, 1)
    alpha = np.where(t < 0.26, 0, np.clip((t - 0.26) / 0.74 * 255, 0, 255))
    arr[:, :, 3] = np.minimum(arr[:, :, 3], alpha.astype(np.uint8))
    vis = arr[:, :, 3] > 16
    if vis.any():
        ys, xs = np.where(vis)
        pad = 6
        l, t0 = max(0, int(xs.min()) - pad), max(0, int(ys.min()) - pad)
        r, b = min(w, int(xs.max()) + pad + 1), min(h, int(ys.max()) + pad + 1)
        cut = arr[t0:b, l:r]
    else:
        cut = arr
    ch, cw = cut.shape[:2]
    side = max(ch, cw)
    canvas = np.zeros((side, side, 4), np.uint8)
    canvas[(side - ch) // 2 : (side - ch) // 2 + ch, (side - cw) // 2 : (side - cw) // 2 + cw] = cut
    return Image.fromarray(canvas).resize((size, size), Image.LANCZOS)


Image.open(SRC / "15.jpg").convert("RGB").resize((256, 256), Image.NEAREST).save(OUT / "tile_ground.png")
key_and_crop(Image.open(SRC / "16.jpg"), 128).save(OUT / "prop_rock.png")
key_and_crop(Image.open(SRC / "18.jpg"), 96).save(OUT / "prop_stone.png")
key_and_crop(Image.open(SRC / "17.jpg"), 160).save(OUT / "prop_tree.png")
key_and_crop(Image.open(SRC / "19.jpg"), 160).save(OUT / "prop_tree2.png")
print("field assets ready")
