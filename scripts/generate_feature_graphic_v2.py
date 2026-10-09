"""
StudyMate feature graphic V2 (1024x500).

Everything is drawn at S x resolution and downscaled with LANCZOS at the end,
which gives proper anti-aliasing (PIL shapes are not anti-aliased natively).
Text lines are positioned from their *measured* bounding boxes so nothing can
overlap, and all copy is checked against the available column width.
"""
import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

S = 3                      # supersample factor
W, H = 1024, 500
CW, CH = W * S, H * S

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
P = lambda *p: os.path.join(ROOT, *p)

OUT = P('art', 'play-store', 'feature_graphic_v2b_1024x500.png')
BG = P('app', 'src', 'main', 'res', 'drawable', 'bg_dashboard.jpg')
SHOT = P('art', 'play-store', 'screenshot_2_focustimer.png')
BOOK = P('art', 'play-store', 'book_icon_transparent.png')
FONTS = 'C:/Windows/Fonts/'

GOLD = (212, 175, 90)
GOLD_LIGHT = (236, 214, 160)
CREAM = (246, 241, 230)
SOFT = (214, 208, 196)


def font(name, px):
    return ImageFont.truetype(FONTS + name, px * S)


def s(v):
    return int(round(v * S))


# ---------------------------------------------------------------- background
bg = Image.open(BG).convert('RGB')
bw, bh = bg.size
r = W / H
if bw / bh > r:
    nw = int(bh * r); x0 = (bw - nw) // 2
    bg = bg.crop((x0, 0, x0 + nw, bh))
else:
    nh = int(bw / r); y0 = (bh - nh) // 2
    bg = bg.crop((0, y0, bw, y0 + nh))
canvas = bg.resize((CW, CH), Image.Resampling.LANCZOS).convert('RGBA')

# Left-weighted darkening so the copy has strong contrast; wood stays visible on the right.
shade = Image.new('RGBA', (CW, CH))
sd = ImageDraw.Draw(shade)
for x in range(CW):
    t = x / CW
    a = int(215 - 120 * min(1.0, t / 0.75))   # 215 on the left -> 95 on the right
    sd.line([(x, 0), (x, CH)], fill=(6, 7, 10, a))
canvas = Image.alpha_composite(canvas, shade)

# Soft top/bottom falloff
fall = Image.new('RGBA', (CW, CH))
fd = ImageDraw.Draw(fall)
band = s(70)
for y in range(band):
    a = int(110 * (1 - y / band))
    fd.line([(0, y), (CW, y)], fill=(0, 0, 0, a))
    fd.line([(0, CH - 1 - y), (CW, CH - 1 - y)], fill=(0, 0, 0, a))
canvas = Image.alpha_composite(canvas, fall)

# ---------------------------------------------------------------- device (right)
shot = Image.open(SHOT).convert('RGB')          # 2076 x 2152, Pixel Fold inner screen
dev_w = 400
bezel = 9
scr_w = dev_w - 2 * bezel
scr_h = int(scr_w * shot.size[1] / shot.size[0])
dev_h = scr_h + 2 * bezel
dev_x = 578
dev_y = 62                                       # bleeds off the bottom edge on purpose
dev_r = 30

# Shadow
sh = Image.new('RGBA', (CW, CH))
ImageDraw.Draw(sh).rounded_rectangle(
    [s(dev_x + 6), s(dev_y + 18), s(dev_x + dev_w + 6), s(dev_y + dev_h + 18)],
    radius=s(dev_r), fill=(0, 0, 0, 200))
sh = sh.filter(ImageFilter.GaussianBlur(s(22)))
canvas = Image.alpha_composite(canvas, sh)

# Body + hairline gold rim
body = Image.new('RGBA', (CW, CH))
bd = ImageDraw.Draw(body)
bd.rounded_rectangle([s(dev_x), s(dev_y), s(dev_x + dev_w), s(dev_y + dev_h)],
                     radius=s(dev_r), fill=(14, 15, 19, 255),
                     outline=GOLD + (200,), width=s(1.5))
canvas = Image.alpha_composite(canvas, body)

# Screen (real screenshot, rounded)
scr = shot.resize((s(scr_w), s(scr_h)), Image.Resampling.LANCZOS).convert('RGBA')
mask = Image.new('L', scr.size, 0)
ImageDraw.Draw(mask).rounded_rectangle([0, 0, scr.size[0] - 1, scr.size[1] - 1],
                                       radius=s(dev_r - bezel + 2), fill=255)
canvas.paste(scr, (s(dev_x + bezel), s(dev_y + bezel)), mask)

# Very faint diagonal sheen, top-left only (kept subtle so the screen stays readable)
sheen = Image.new('RGBA', scr.size)
shd = ImageDraw.Draw(sheen)
sw_, sh_ = scr.size
for i in range(0, int(sw_ * 0.55)):
    a = int(16 * (1 - i / (sw_ * 0.55)))
    shd.line([(i, 0), (0, i)], fill=(255, 255, 255, a), width=2)
sheen_masked = Image.new('RGBA', scr.size)
sheen_masked.paste(sheen, (0, 0), mask)
canvas.alpha_composite(sheen_masked, (s(dev_x + bezel), s(dev_y + bezel)))

# ---------------------------------------------------------------- copy (left)
d = ImageDraw.Draw(canvas)
LX = 64
COL_MAX = dev_x - LX - 40                         # usable text width

f_title = font('georgiab.ttf', 58)
f_tag = font('seguisb.ttf', 23)
f_sub = font('georgiai.ttf', 19)
f_feat = font('segoeui.ttf', 17)


def bbox(text, f):
    b = d.textbbox((0, 0), text, font=f)
    return b[0], b[1], b[2] - b[0], b[3] - b[1]


def text(x, y_top, t, f, fill, shadow=True):
    """Draw t so its *ink* top sits at y_top (1x coords). Returns ink bottom (1x)."""
    ox, oy, tw, th = bbox(t, f)
    px, py = s(x) - ox, s(y_top) - oy
    if shadow:
        d.text((px + s(1), py + s(1.5)), t, font=f, fill=(0, 0, 0, 170))
    d.text((px, py), t, font=f, fill=fill)
    assert tw / S <= COL_MAX + 60, f'text too wide: {t!r} {tw / S:.0f}px'
    return y_top + th / S, tw / S


# Brand row: book + wordmark, vertically centred on each other
book_px = 56
y = 112
_, _, tw, th = bbox('StudyMate', f_title)
title_h = th / S
book = Image.open(BOOK).convert('RGBA').resize((s(book_px), s(book_px)), Image.Resampling.LANCZOS)
book_y = y + (title_h - book_px) / 2
canvas.alpha_composite(book, (s(LX), s(book_y)))
d = ImageDraw.Draw(canvas)
title_x = LX + book_px + 16
y_after_title, title_w = text(title_x, y, 'StudyMate', f_title, GOLD_LIGHT)

# Short gold rule
y = y_after_title + 22
d.rectangle([s(LX), s(y), s(LX + 56), s(y + 2)], fill=GOLD + (255,))
y += 2 + 22

# Tagline
y, _ = text(LX, y, 'Zero AI  \u00b7  Zero Ads  \u00b7  100% Free', f_tag, CREAM)
y += 12
y, _ = text(LX, y, 'You keep all your data.', f_sub, GOLD)
y += 30

# Feature list with small gold diamonds
features = [
    'Spaced-repetition flashcards & mock exams',
    'Import from Quizlet, Anki & lecture slides',
    'Focus timer, planner & calendar widgets',
]
for ft in features:
    _, _, _, fh = bbox('Ag', f_feat)
    cy = y + (fh / S) / 2
    dx = LX + 4
    d.polygon([(s(dx), s(cy - 4)), (s(dx + 4), s(cy)), (s(dx), s(cy + 4)), (s(dx - 4), s(cy))],
              fill=GOLD + (255,))
    y_end, _ = text(LX + 20, y, ft, f_feat, SOFT)
    y = y_end + 14

# ---------------------------------------------------------------- output
out = canvas.convert('RGB').resize((W, H), Image.Resampling.LANCZOS)
out.save(OUT, 'PNG')
print('Saved', OUT, out.size, '| text column max', COL_MAX, 'px | content bottom', round(y), 'px')
