import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

out_path = 'art/play-store/feature_graphic_1024x500.png'
bg_path = 'app/src/main/res/drawable/bg_dashboard.jpg'

# 1. Base wood texture
if os.path.exists(bg_path):
    bg = Image.open(bg_path).convert('RGB')
    w, h = bg.size
    target_ratio = 1024 / 500
    if w / h > target_ratio:
        new_w = int(h * target_ratio)
        left = (w - new_w) // 2
        bg = bg.crop((left, 0, left + new_w, h))
    else:
        new_h = int(w / target_ratio)
        top = (h - new_h) // 2
        bg = bg.crop((0, top, w, top + new_h))
    bg = bg.resize((1024, 500), Image.Resampling.LANCZOS)
else:
    bg = Image.new('RGB', (1024, 500), (15, 23, 42))

# 2. Dark vignette / overlay (matches app's bg_wood_overlay: #D9000000 -> #80000000 -> #00000000 etc.)
# We will just apply a dark translucent layer #50000000
overlay = Image.new('RGBA', (1024, 500), (0, 0, 0, 120))
bg.paste(overlay, (0, 0), overlay)

# 3. Frosted Dark Glass Card (matches app's MaterialCardView #59000000 with #99C4A24A border)
card_layer = Image.new('RGBA', (1024, 500), (0, 0, 0, 0))
cdraw = ImageDraw.Draw(card_layer)

card_margin_x = 120
card_margin_y = 45
card_w = 1024 - (2 * card_margin_x)
card_h = 500 - (2 * card_margin_y)

# Soft outer shadow for depth
shadow_layer = Image.new('RGBA', (1024, 500), (0, 0, 0, 0))
sdraw = ImageDraw.Draw(shadow_layer)
sdraw.rounded_rectangle([card_margin_x - 4, card_margin_y - 4, card_margin_x + card_w + 4, card_margin_y + card_h + 8], radius=28, fill=(0, 0, 0, 160))
shadow_layer = shadow_layer.filter(ImageFilter.GaussianBlur(16))
bg.paste(shadow_layer, (0, 0), shadow_layer)

# Glass card body (59000000 is 89 alpha) and gold border (99C4A24A is 153 alpha)
cdraw.rounded_rectangle(
    [card_margin_x, card_margin_y, card_margin_x + card_w, card_margin_y + card_h],
    radius=24,
    fill=(0, 0, 0, 89),
    outline=(196, 162, 74, 153),
    width=2
)
bg.paste(card_layer, (0, 0), card_layer)

# 4. Gold Book Icon
icon_path = 'art/play-store/book_icon_transparent.png'
if os.path.exists(icon_path):
    icon = Image.open(icon_path).convert('RGBA')
    icon_sz = 130
    icon_resized = icon.resize((icon_sz, icon_sz), Image.Resampling.LANCZOS)
    
    icon_cx = 1024 // 2
    icon_y = card_margin_y + 35
    bg.paste(icon_resized, (icon_cx - icon_sz // 2, icon_y), icon_resized)

# 5. Serif Typography (StudyMate branding)
font_title = ImageFont.truetype('C:/Windows/Fonts/georgiab.ttf', 56)
font_sub = ImageFont.truetype('C:/Windows/Fonts/georgiab.ttf', 18)
font_features = ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf', 16)

draw = ImageDraw.Draw(bg)

# Title: StudyMate
title_text = "StudyMate"
t_bbox = draw.textbbox((0, 0), title_text, font=font_title)
t_w = t_bbox[2] - t_bbox[0]
draw.text(((1024 - t_w) // 2, card_margin_y + 175), title_text, font=font_title, fill=(212, 188, 126)) # #D4BC7E

# Thin gold ornamental divider
div_y = card_margin_y + 258
div_half_len = 160
cx = 1024 // 2
draw.line([(cx - div_half_len, div_y), (cx - 15, div_y)], fill=(196, 162, 74, 150), width=1)
draw.line([(cx + 15, div_y), (cx + div_half_len, div_y)], fill=(196, 162, 74, 150), width=1)
# Center diamond
draw.polygon([(cx, div_y - 4), (cx + 4, div_y), (cx, div_y + 4), (cx - 4, div_y)], fill=(212, 188, 126))

# Subtitle: Tracked Uppercase
sub_text = "A C A D E M I C   S T U D Y   C O M P A N I O N"
s_bbox = draw.textbbox((0, 0), sub_text, font=font_sub)
s_w = s_bbox[2] - s_bbox[0]
draw.text(((1024 - s_w) // 2, card_margin_y + 278), sub_text, font=font_sub, fill=(250, 248, 245)) # #FAF8F5

# Minimal feature bar
features_text = "Spaced Repetition  •  Mock Exams  •  Pomodoro Focus  •  Widgets"
f_bbox = draw.textbbox((0, 0), features_text, font=font_features)
f_w = f_bbox[2] - f_bbox[0]
draw.text(((1024 - f_w) // 2, card_margin_y + 325), features_text, font=font_features, fill=(196, 162, 74)) # #C4A24A

bg.save(out_path, 'PNG')
print(f"Generated transparent glass Feature Graphic at: {out_path} ({bg.size})")
