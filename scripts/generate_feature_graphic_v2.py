import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

out_path = 'art/play-store/feature_graphic_v2_1024x500.png'
bg_path = 'app/src/main/res/drawable/bg_dashboard.jpg'
screenshot_path = 'art/play-store/screenshot_2_focustimer.png'
icon_path = 'art/play-store/book_icon_transparent.png'

# 1. Base wood texture (1024x500)
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
    bg = Image.new('RGB', (1024, 500), (14, 18, 28))

# 2. Ambient Studio Lighting & Bokeh Orbs (replicating StudyMate's OrbField)
bokeh_layer = Image.new('RGBA', (1024, 500), (0, 0, 0, 0))
bdraw = ImageDraw.Draw(bokeh_layer)

# Soft floating ambient orbs in wood gutters
orbs = [
    (140, 80, 55, (196, 162, 74, 35)),
    (80, 380, 65, (160, 120, 50, 25)),
    (480, 420, 45, (210, 175, 80, 30)),
    (960, 90, 70, (196, 162, 74, 40)),
    (980, 400, 60, (150, 110, 40, 25)),
    (600, 70, 40, (180, 140, 60, 20)),
]
for ox, oy, r, col in orbs:
    bdraw.ellipse([ox - r, oy - r, ox + r, oy + r], fill=col)

bokeh_layer = bokeh_layer.filter(ImageFilter.GaussianBlur(32))
bg.paste(bokeh_layer, (0, 0), bokeh_layer)

# Studio dark vignette
vignette = Image.new('RGBA', (1024, 500), (0, 0, 0, 0))
vdraw = ImageDraw.Draw(vignette)
vdraw.rectangle([0, 0, 1024, 500], fill=(5, 8, 14, 110))
# Top/bottom gradient
for y in range(80):
    alpha = int((1.0 - (y / 80.0)) * 90)
    vdraw.line([(0, y), (1024, y)], fill=(2, 4, 8, alpha))
    vdraw.line([(0, 499 - y), (1024, 499 - y)], fill=(2, 4, 8, alpha))
bg.paste(vignette, (0, 0), vignette)

# 3. RIGHT SIDE: Sleek Premium Smartphone Mockup displaying authentic app
phone_w = 250
phone_h = 472
phone_x = 715
phone_y = 14
corner_r = 34

# Deep multi-stage phone shadow
shadow = Image.new('RGBA', (1024, 500), (0, 0, 0, 0))
sdraw = ImageDraw.Draw(shadow)
# Broad ambient shadow
sdraw.rounded_rectangle([phone_x - 16, phone_y + 8, phone_x + phone_w + 20, phone_y + phone_h + 20], radius=corner_r + 10, fill=(0, 0, 0, 160))
shadow = shadow.filter(ImageFilter.GaussianBlur(24))
# Crisp contact shadow
contact = Image.new('RGBA', (1024, 500), (0, 0, 0, 0))
cdraw = ImageDraw.Draw(contact)
cdraw.rounded_rectangle([phone_x - 6, phone_y + 2, phone_x + phone_w + 6, phone_y + phone_h + 10], radius=corner_r + 4, fill=(0, 0, 0, 190))
contact = contact.filter(ImageFilter.GaussianBlur(10))

bg.paste(shadow, (0, 0), shadow)
bg.paste(contact, (0, 0), contact)

# Phone chassis (Titanium black body with subtle gold-accent rim)
chassis = Image.new('RGBA', (phone_w, phone_h), (0, 0, 0, 0))
ch_draw = ImageDraw.Draw(chassis)
ch_draw.rounded_rectangle([0, 0, phone_w - 1, phone_h - 1], radius=corner_r, fill=(18, 20, 26, 255), outline=(196, 162, 74, 180), width=2)
# Inner bezel line
ch_draw.rounded_rectangle([2, 2, phone_w - 3, phone_h - 3], radius=corner_r - 2, outline=(35, 40, 52, 255), width=2)

# Phone screen content
bezel = 8
screen_w = phone_w - (2 * bezel)
screen_h = phone_h - (2 * bezel)
screen_r = corner_r - 6

if os.path.exists(screenshot_path):
    sc = Image.open(screenshot_path).convert('RGB')
    sw, sh = sc.size
    # Center crop the core card from fold screenshot
    target_crop_w = int(sh * screen_w / screen_h)
    crop_left = (sw - target_crop_w) // 2
    sc_crop = sc.crop((crop_left, 0, crop_left + target_crop_w, sh))
    sc_resized = sc_crop.resize((screen_w, screen_h), Image.Resampling.LANCZOS)
    
    # Mask screen to rounded corners
    screen_mask = Image.new('L', (screen_w, screen_h), 0)
    sm_draw = ImageDraw.Draw(screen_mask)
    sm_draw.rounded_rectangle([0, 0, screen_w, screen_h], radius=screen_r, fill=255)
    
    chassis.paste(sc_resized, (bezel, bezel), screen_mask)
    
    # Add subtle glass glare over screen
    glare = Image.new('RGBA', (screen_w, screen_h), (0, 0, 0, 0))
    gldraw = ImageDraw.Draw(glare)
    for gy in range(screen_h // 2):
        galpha = int((1.0 - (gy / float(screen_h // 2))) * 28)
        gldraw.line([(0, gy), (screen_w, gy)], fill=(255, 255, 255, galpha))
    chassis.paste(glare, (bezel, bezel), screen_mask)

# Dynamic island / camera punch hole
cam_w, cam_h = 56, 10
cam_x = (phone_w - cam_w) // 2
cam_y = bezel + 6
ch_draw.rounded_rectangle([cam_x, cam_y, cam_x + cam_w, cam_y + cam_h], radius=5, fill=(8, 10, 14, 255))

bg.paste(chassis, (phone_x, phone_y), chassis)

# 4. LEFT SIDE: Refined Dark Academic Typography & Glass Feature Cards
left_draw = ImageDraw.Draw(bg)
lx = 65

# A. Emblem Badge: Frosted glass circle with gold rim + golden book
emblem_sz = 68
emblem_y = 38
emblem = Image.new('RGBA', (emblem_sz, emblem_sz), (0, 0, 0, 0))
edraw = ImageDraw.Draw(emblem)
edraw.rounded_rectangle([0, 0, emblem_sz - 1, emblem_sz - 1], radius=20, fill=(12, 16, 26, 220), outline=(196, 162, 74, 180), width=2)

if os.path.exists(icon_path):
    bk = Image.open(icon_path).convert('RGBA')
    bk_sz = 44
    bk_resized = bk.resize((bk_sz, bk_sz), Image.Resampling.LANCZOS)
    emblem.paste(bk_resized, ((emblem_sz - bk_sz) // 2, (emblem_sz - bk_sz) // 2), bk_resized)

bg.paste(emblem, (lx, emblem_y), emblem)

# B. Brand Title: Georgia Bold with soft drop shadow
font_title = ImageFont.truetype('C:/Windows/Fonts/georgiab.ttf', 48)
title_text = "StudyMate"
# Shadow
left_draw.text((lx + 86, emblem_y + 4), title_text, font=font_title, fill=(0, 0, 0, 160))
# Fill (Luxurious warm antique gold #EBD5A0)
left_draw.text((lx + 84, emblem_y + 2), title_text, font=font_title, fill=(235, 213, 160))

# Category tag under title
font_sub = ImageFont.truetype('C:/Windows/Fonts/georgiab.ttf', 13)
sub_text = "THE PRIVATE ACADEMIC COMPANION"
left_draw.text((lx + 88, emblem_y + 54), sub_text, font=font_sub, fill=(205, 175, 110))

# C. Thin decorative gold rule
div_y = 126
left_draw.line([(lx, div_y), (lx + 580, div_y)], fill=(196, 162, 74, 90), width=1)
# Accent diamond
left_draw.polygon([(lx + 290, div_y - 3), (lx + 294, div_y), (lx + 290, div_y + 3), (lx + 286, div_y)], fill=(212, 188, 126))

# D. Hero Value Proposition: Frosted Glass Badges
font_badge_bold = ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf', 16)
font_badge_sm = ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf', 13)

badges = [
    {
        "title": "Zero AI   •   Zero Ads   •   100% Free",
        "sub": "No subscriptions, paywalls, or AI gimmicks. Fast & authentic.",
        "icon": "⚡",
        "accent": (235, 213, 160)
    },
    {
        "title": "Spaced Repetition & Mock Exams",
        "sub": "Scientific SM-2 flashcard decks + Quizlet & lecture slide import.",
        "icon": "📚",
        "accent": (212, 188, 126)
    },
    {
        "title": "100% Offline   •   You Keep All Your Data",
        "sub": "Local-first on-device database. Never tracks or sells your data.",
        "icon": "🛡️",
        "accent": (196, 162, 74)
    }
]

badge_w = 580
badge_h = 74
by = 148

for b in badges:
    # Drop shadow
    cshadow = Image.new('RGBA', (badge_w, badge_h), (0, 0, 0, 0))
    csdraw = ImageDraw.Draw(cshadow)
    csdraw.rounded_rectangle([2, 4, badge_w - 2, badge_h - 2], radius=16, fill=(0, 0, 0, 110))
    cshadow = cshadow.filter(ImageFilter.GaussianBlur(6))
    bg.paste(cshadow, (lx, by), cshadow)
    
    # Glass card container
    card = Image.new('RGBA', (badge_w, badge_h), (0, 0, 0, 0))
    cdraw = ImageDraw.Draw(card)
    
    # Frosted dark glass fill (#59000000 style) with gold rim
    cdraw.rounded_rectangle([0, 0, badge_w - 1, badge_h - 1], radius=16, fill=(15, 20, 32, 190), outline=(196, 162, 74, 120), width=1)
    
    # Left accent indicator pill
    cdraw.rounded_rectangle([14, 18, 18, badge_h - 18], radius=2, fill=b["accent"])
    
    # Title
    cdraw.text((32, 14), b["title"], font=font_badge_bold, fill=b["accent"])
    # Subtitle explanation
    cdraw.text((32, 42), b["sub"], font=font_badge_sm, fill=(215, 218, 225))
    
    bg.paste(card, (lx, by), card)
    by += 88

# 5. Bottom subtle signature footer
font_foot = ImageFont.truetype('C:/Windows/Fonts/georgia.ttf', 12)
left_draw.text((lx + 4, 428), "Handcrafted with Dark Academic wood & frosted glass aesthetics.", font=font_foot, fill=(150, 140, 125))

# Save output
bg.save(out_path, 'PNG')
print(f"Generated Version 2 Feature Graphic: {out_path} ({bg.size})")
