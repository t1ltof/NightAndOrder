"""Short 16-bit mono punches for SoundPool."""
import math
import random
import struct
import wave
from pathlib import Path

OUT = Path(r"C:\Users\Liinad\Desktop\NightAndOrder\app\src\main\assets")
OUT.mkdir(parents=True, exist_ok=True)
SR = 22050
rng = random.Random(7)


def write(name: str, samples: list[float]) -> None:
    path = OUT / name
    with wave.open(str(path), "w") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        frames = bytearray()
        for s in samples:
            v = int(max(-1.0, min(1.0, s)) * 32767)
            frames += struct.pack("<h", v)
        w.writeframes(bytes(frames))
    print("wrote", name, len(samples))


def env(t: float, a: float, d: float) -> float:
    if t < a:
        return t / a
    return math.exp(-(t - a) / d)


def tone(t: float, hz: float) -> float:
    return math.sin(2.0 * math.pi * hz * t)


def noise() -> float:
    return rng.random() * 2.0 - 1.0


def render(seconds: float, fn) -> list[float]:
    n = int(SR * seconds)
    return [fn(i / SR) for i in range(n)]


def hit(thump: float, noise_amt: float, decay: float) -> list[float]:
    def fn(t: float) -> float:
        e = env(t, 0.002, decay)
        body = tone(t, thump) * 0.62 + tone(t, thump * 2.05) * 0.22
        click = noise() * noise_amt * math.exp(-t / 0.014)
        return (body + click) * e * 0.95

    return render(0.13, fn)


def hurt() -> list[float]:
    def fn(t: float) -> float:
        e = env(t, 0.004, 0.11)
        body = tone(t, 92) * 0.45 + tone(t, 48) * 0.4
        grit = noise() * 0.35 * math.exp(-t / 0.04)
        return (body + grit) * e

    return render(0.22, fn)


def kill() -> list[float]:
    def fn(t: float) -> float:
        e = env(t, 0.003, 0.09)
        body = tone(t, 70) * 0.5 + tone(t, 140) * 0.18
        crunch = noise() * 0.55 * math.exp(-t / 0.03)
        return (body + crunch) * e

    return render(0.18, fn)


write("sfx_hit_a.wav", hit(96, 0.42, 0.045))
write("sfx_hit_b.wav", hit(118, 0.38, 0.038))
write("sfx_hurt.wav", hurt())
write("sfx_kill.wav", kill())
