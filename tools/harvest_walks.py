"""Harvest walk-cycle frames from imagine videos and pack sprite sheets."""
from pathlib import Path
import cv2
import numpy as np
from PIL import Image

VIDEOS = Path(r"C:\Users\Liinad\.grok\sessions\C%3A%5CUsers%5CLiinad\01a00ab1-d814-76a3-998c-5ecd90e3cd29\videos")
OUT = Path(r"C:\Users\Liinad\Desktop\NightAndOrder\app\src\main\assets")
WORK = Path(r"C:\Users\Liinad\Desktop\NightAndOrder\tools\anim_work")
WORK.mkdir(parents=True, exist_ok=True)

# video file -> asset stem
MAP = {
    "1.mp4": "walk_morvan",
    "2.mp4": "walk_lilith",
    "3.mp4": "walk_nix",
    "4.mp4": "walk_lucia",
    "5.mp4": "walk_hale",
    "6.mp4": "walk_sera",
    "7.mp4": "walk_thrall",
    "8.mp4": "walk_bat",
    "9.mp4": "walk_flagellant",
    "10.mp4": "walk_knight",
    "11.mp4": "walk_boss",
}


def key_frame(bgr: np.ndarray) -> Image.Image:
    rgb = cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)
    arr = np.dstack([rgb, np.full(rgb.shape[:2], 255, np.uint8)])
    h, w = arr.shape[:2]
    corners = np.stack([arr[2, 2], arr[2, w - 3], arr[h - 3, 2], arr[h - 3, w - 3]]).astype(np.float32)
    cr, cg, cb = corners[:, 0].mean(), corners[:, 1].mean(), corners[:, 2].mean()
    dist = np.sqrt(((arr[:, :, :3].astype(np.float32) - np.array([cr, cg, cb])) ** 2).sum(2))
    # also treat near-black studio floors as transparent
    lum = arr[:, :, :3].astype(np.float32).mean(2)
    t = np.clip(dist / 70.0, 0, 1)
    alpha = np.where(t < 0.22, 0, np.clip((t - 0.22) / 0.78 * 255, 0, 255))
    alpha = np.where(lum < 18, np.minimum(alpha, lum * 8), alpha)
    arr[:, :, 3] = alpha.astype(np.uint8)
    vis = arr[:, :, 3] > 18
    if vis.any():
        ys, xs = np.where(vis)
        pad = 8
        l, t0 = max(0, xs.min() - pad), max(0, ys.min() - pad)
        r, b = min(w, xs.max() + pad + 1), min(h, ys.max() + pad + 1)
        cut = arr[t0:b, l:r]
    else:
        cut = arr
    ch, cw = cut.shape[:2]
    side = max(ch, cw)
    canvas = np.zeros((side, side, 4), np.uint8)
    canvas[(side - ch) // 2 : (side - ch) // 2 + ch, (side - cw) // 2 : (side - cw) // 2 + cw] = cut
    return Image.fromarray(canvas).resize((96, 96), Image.LANCZOS)


def harvest(src: Path, dest_stem: str, frames_wanted: int = 8) -> None:
    cap = cv2.VideoCapture(str(src))
    fps = cap.get(cv2.CAP_PROP_FPS) or 24
    total = int(cap.get(cv2.CAP_PROP_FRAME_COUNT) or 0)
    raw = []
    i = 0
    while True:
        ok, frame = cap.read()
        if not ok:
            break
        # skip first and last 0.6s, sample ~10 fps
        t = i / fps
        dur = total / fps if total else 6
        if 0.55 < t < dur - 0.45 and int(t * 10) != int((t - 1 / fps) * 10):
            raw.append(frame)
        i += 1
    cap.release()
    if len(raw) < 4:
        cap = cv2.VideoCapture(str(src))
        raw = []
        while True:
            ok, frame = cap.read()
            if not ok:
                break
            raw.append(frame)
        cap.release()
        raw = raw[:: max(1, len(raw) // 12)]
    if not raw:
        print("empty", src)
        return
    # pick evenly across the harvested band for a loop
    idx = np.linspace(0, len(raw) - 1, frames_wanted).astype(int)
    frames = [key_frame(raw[i]) for i in idx]
    sheet = Image.new("RGBA", (96 * len(frames), 96), (0, 0, 0, 0))
    for i, fr in enumerate(frames):
        sheet.paste(fr, (i * 96, 0), fr)
        fr.save(WORK / f"{dest_stem}_{i:02d}.png")
    out = OUT / f"{dest_stem}.png"
    sheet.save(out)
    print("wrote", out.name, "frames", len(frames), "from", src.name)


for name, stem in MAP.items():
    p = VIDEOS / name
    if p.exists():
        harvest(p, stem)
    else:
        print("skip missing", name)
