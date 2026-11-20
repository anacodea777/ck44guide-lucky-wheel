"""Builds docs/banner.png from the screenshots in docs/screenshots.

Usage: python scripts/make-banner.py
Needs Pillow. Fonts default to Windows Segoe UI; pass --font/--font-bold to override.
"""
import argparse
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
SHOTS = ROOT / "docs" / "screenshots"
OUT = ROOT / "docs" / "banner.png"

W, H = 1600, 900
TOP, BOTTOM = (106, 27, 154), (49, 27, 146)
GOLD = (255, 193, 7)


def gradient(w, h):
    img = Image.new("RGB", (w, h))
    px = img.load()
    for y in range(h):
        t = y / (h - 1)
        c = tuple(round(a + (b - a) * t) for a, b in zip(TOP, BOTTOM))
        for x in range(w):
            px[x, y] = c
    return img


def phone(path, height):
    shot = Image.open(path).convert("RGB")
    scale = height / shot.height
    shot = shot.resize((round(shot.width * scale), height), Image.LANCZOS)
    radius = round(height * 0.045)
    bezel = round(height * 0.018)
    w, h = shot.width + bezel * 2, shot.height + bezel * 2

    frame = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(frame)
    d.rounded_rectangle((0, 0, w - 1, h - 1), radius + bezel, fill=(20, 16, 40, 255))
    mask = Image.new("L", shot.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, shot.width - 1, shot.height - 1), radius, fill=255)
    frame.paste(shot, (bezel, bezel), mask)
    return frame


def paste_with_shadow(canvas, img, xy):
    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    alpha = img.split()[3].point(lambda a: 110 if a else 0)
    shadow.paste((0, 0, 0, 255), (xy[0] + 14, xy[1] + 22), alpha)
    shadow = shadow.filter(ImageFilter.GaussianBlur(18))
    canvas.alpha_composite(shadow)
    canvas.alpha_composite(img, xy)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--font", default="C:/Windows/Fonts/segoeui.ttf")
    ap.add_argument("--font-bold", default="C:/Windows/Fonts/segoeuib.ttf")
    args = ap.parse_args()

    canvas = gradient(W, H).convert("RGBA")
    d = ImageDraw.Draw(canvas)

    # Soft decorative circles.
    deco = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    dd = ImageDraw.Draw(deco)
    dd.ellipse((-200, 520, 400, 1120), fill=(255, 193, 7, 28))
    dd.ellipse((1250, -220, 1800, 330), fill=(255, 255, 255, 18))
    canvas.alpha_composite(deco)

    title = ImageFont.truetype(args.font_bold, 74)
    sub = ImageFont.truetype(args.font, 34)
    chip = ImageFont.truetype(args.font_bold, 28)

    x = 80
    d.text((x, 210), "CK44GUIDE", font=title, fill=GOLD)
    d.text((x, 295), "Lucky Wheel", font=title, fill="white")
    d.text((x, 400), "Spin the wheel, let luck decide.", font=sub, fill=(235, 225, 255))

    y = 480
    for label in ("100% offline", "No permissions", "English + Bengali"):
        tw = d.textlength(label, font=chip)
        pill = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        ImageDraw.Draw(pill).rounded_rectangle(
            (x, y, x + tw + 44, y + 56), 28, fill=(255, 255, 255, 40), outline=GOLD, width=2)
        canvas.alpha_composite(pill)
        d = ImageDraw.Draw(canvas)
        d.text((x + 22, y + 10), label, font=chip, fill="white")
        y += 74

    phones = [
        (phone(SHOTS / "05_home_bn.png", 640), (900, 170)),
        (phone(SHOTS / "03_result_en.png", 640), (1230, 170)),
        (phone(SHOTS / "01_home_en.png", 720), (1040, 110)),
    ]
    for img, xy in phones:
        paste_with_shadow(canvas, img, xy)

    canvas.convert("RGB").save(OUT, optimize=True)
    print(f"wrote {OUT}")


if __name__ == "__main__":
    main()
