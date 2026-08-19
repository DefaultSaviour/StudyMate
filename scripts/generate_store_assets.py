import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

os.makedirs('art/play-store', exist_ok=True)

# Colors
GOLD = (231, 206, 140)
GOLD_DARK = (196, 162, 74)
NAVY_DEEP = (14, 20, 32)
WHITE = (255, 255, 255)
SURFACE = (245, 240, 230)
CARD_BG = (20, 26, 40, 220)

font_serif_lg = ImageFont.truetype('C:/Windows/Fonts/georgiab.ttf', 56)
font_serif_md = ImageFont.truetype('C:/Windows/Fonts/georgiab.ttf', 36)
font_serif_sm = ImageFont.truetype('C:/Windows/Fonts/georgiab.ttf', 24)
font_sans_bold = ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf', 26)
font_sans_sm = ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf', 20)

# ==========================================
# 1. 512x512 APP ICON
# ==========================================
def create_app_icon():
    out_path = 'art/play-store/app_icon_512.png'
    icon_base = Image.open('app/src/main/res/mipmap-xxxhdpi/ic_launcher.png').convert('RGBA')
    
    # Fit into 512x512
    icon_512 = icon_base.resize((512, 512), Image.Resampling.LANCZOS)
    
    # Canvas with subtle rounded mask and gold rim
    canvas = Image.new('RGBA', (512, 512), (0, 0, 0, 0))
    canvas.paste(icon_512, (0, 0))
    canvas.save(out_path, 'PNG')
    print(f"Generated: {out_path} ({canvas.size})")

# ==========================================
# 2. 1024x500 FEATURE GRAPHIC
# ==========================================
def create_feature_graphic():
    out_path = 'art/play-store/feature_graphic_1024x500.png'
    bg_path = 'art/backgrounds-original/bg_dashboard.png'
    
    if os.path.exists(bg_path):
        bg = Image.open(bg_path).convert('RGB')
        # Center crop to 1024x500
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
        bg = Image.new('RGB', (1024, 500), NAVY_DEEP)

    # Dark overlay
    overlay = Image.new('RGBA', (1024, 500), (10, 16, 28, 170))
    bg.paste(overlay, (0, 0), overlay)

    draw = ImageDraw.Draw(bg)

    # Draw decorative glass card in center
    card_w, card_h = 920, 420
    cx, cy = 1024 // 2, 500 // 2
    card_x0, card_y0 = cx - card_w // 2, cy - card_h // 2
    card_x1, card_y1 = card_x0 + card_w, card_y0 + card_h

    # Card background
    card_layer = Image.new('RGBA', (1024, 500), (0, 0, 0, 0))
    card_draw = ImageDraw.Draw(card_layer)
    card_draw.rounded_rectangle([card_x0, card_y0, card_x1, card_y1], radius=24, fill=(15, 22, 36, 210), outline=GOLD_DARK, width=2)
    bg.paste(card_layer, (0, 0), card_layer)

    # Icon on left
    icon_src = Image.open('app/src/main/res/mipmap-xxxhdpi/ic_launcher.png').convert('RGBA')
    icon_180 = icon_src.resize((180, 180), Image.Resampling.LANCZOS)
    bg.paste(icon_180, (card_x0 + 50, cy - 90), icon_180)

    # Text on right
    tx = card_x0 + 260
    draw = ImageDraw.Draw(bg)
    draw.text((tx, cy - 110), "StudyMate", font=font_serif_lg, fill=GOLD)
    draw.text((tx, cy - 40), "Modern Study Planner & Flashcards", font=font_serif_sm, fill=WHITE)
    draw.text((tx, cy + 5), "Spaced Repetition • Mock Exams • Pomodoro Focus • Widgets", font=font_sans_sm, fill=GOLD_DARK)

    # Pill tags
    tags = ["🎯 100% Offline", "📚 Smart Decks", "⏱️ Focus Timer", "🏆 Trophy Room"]
    px = tx
    for tag in tags:
        bbox = draw.textbbox((0, 0), tag, font=font_sans_sm)
        tw = bbox[2] - bbox[0]
        pill_w = tw + 24
        pill_h = 32
        py = cy + 50
        draw.rounded_rectangle([px, py, px + pill_w, py + pill_h], radius=16, fill=(30, 42, 65), outline=GOLD_DARK, width=1)
        draw.text((px + 12, py + 5), tag, font=font_sans_sm, fill=WHITE)
        px += pill_w + 12

    bg.save(out_path, 'PNG')
    print(f"Generated: {out_path} ({bg.size})")

# ==========================================
# 3. 4x PHONE SCREENSHOTS (1080x1920)
# ==========================================
def create_phone_screenshots():
    screens_data = [
        {
            "filename": "screenshot_1_decks.png",
            "bg": "art/backgrounds-original/bg_flashcards.png",
            "title": "SMART FLASHCARD DECKS",
            "subtitle": "Spaced Repetition & Peer CSV Deck Sharing",
            "highlights": ["Leitner Box System", "Instant Card Search", "Progress & Accuracy Metrics"]
        },
        {
            "filename": "screenshot_2_exam.png",
            "bg": "art/backgrounds-original/bg_dashboard.png",
            "title": "MOCK EXAM SIMULATOR",
            "subtitle": "Test Your Knowledge Under Real Conditions",
            "highlights": ["Custom Question Counts", "Instant Scoring Breakdown", "Detailed Answer Reviews"]
        },
        {
            "filename": "screenshot_3_focustimer.png",
            "bg": "art/backgrounds-original/bg_focustimer.png",
            "title": "FOCUS POMODORO TIMER",
            "subtitle": "Custom Study Intervals & Ambient Visual Alerts",
            "highlights": ["1 to 180 Min Sessions", "Screen-Edge Golden Halos", "Integrated Assignment Tasks"]
        },
        {
            "filename": "screenshot_4_calendar.png",
            "bg": "art/backgrounds-original/bg_calendar.png",
            "title": "CALENDAR & HOME WIDGETS",
            "subtitle": "Track Deadlines & Never Miss a Session",
            "highlights": ["2x3 Tall Calendar Widget", "Inline Event Scheduling", "Dark Academic Aesthetics"]
        }
    ]

    font_title = ImageFont.truetype('C:/Windows/Fonts/georgiab.ttf', 54)
    font_sub = ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf', 32)
    font_body = ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf', 28)

    for data in screens_data:
        out_path = os.path.join('art/play-store', data["filename"])
        bg_path = data["bg"]
        if os.path.exists(bg_path):
            bg = Image.open(bg_path).convert('RGB')
            # Crop/resize to 1080x1920
            w, h = bg.size
            target_ratio = 1080 / 1920
            if w / h > target_ratio:
                new_w = int(h * target_ratio)
                left = (w - new_w) // 2
                bg = bg.crop((left, 0, left + new_w, h))
            else:
                new_h = int(w / target_ratio)
                top = (h - new_h) // 2
                bg = bg.crop((0, top, w, top + new_h))
            bg = bg.resize((1080, 1920), Image.Resampling.LANCZOS)
        else:
            bg = Image.new('RGB', (1080, 1920), NAVY_DEEP)

        # Gradient overlay
        overlay = Image.new('RGBA', (1080, 1920), (12, 18, 30, 190))
        bg.paste(overlay, (0, 0), overlay)

        # Header Title Banner
        draw = ImageDraw.Draw(bg)
        
        # Top banner card
        card_layer = Image.new('RGBA', (1080, 1920), (0, 0, 0, 0))
        cdraw = ImageDraw.Draw(card_layer)
        cdraw.rounded_rectangle([60, 100, 1020, 340], radius=28, fill=(16, 24, 40, 230), outline=GOLD_DARK, width=3)
        bg.paste(card_layer, (0, 0), card_layer)

        draw = ImageDraw.Draw(bg)
        # Centered Title
        t_bbox = draw.textbbox((0, 0), data["title"], font=font_title)
        tw = t_bbox[2] - t_bbox[0]
        draw.text(((1080 - tw) // 2, 140), data["title"], font=font_title, fill=GOLD)

        s_bbox = draw.textbbox((0, 0), data["subtitle"], font=font_sub)
        sw = s_bbox[2] - s_bbox[0]
        draw.text(((1080 - sw) // 2, 230), data["subtitle"], font=font_sub, fill=WHITE)

        # Mock Phone / Main Center Card
        phone_layer = Image.new('RGBA', (1080, 1920), (0, 0, 0, 0))
        pdraw = ImageDraw.Draw(phone_layer)
        pdraw.rounded_rectangle([80, 420, 1000, 1780], radius=36, fill=(12, 18, 32, 235), outline=GOLD_DARK, width=3)

        # Inside Phone Content: Header + List of feature cards
        cy = 500
        for i, item in enumerate(data["highlights"]):
            item_y0 = cy + i * 180
            item_y1 = item_y0 + 140
            pdraw.rounded_rectangle([130, item_y0, 950, item_y1], radius=20, fill=(24, 34, 54, 240), outline=GOLD, width=1)

        bg.paste(phone_layer, (0, 0), phone_layer)

        draw = ImageDraw.Draw(bg)
        for i, item in enumerate(data["highlights"]):
            item_y0 = cy + i * 180
            draw.text((170, item_y0 + 48), f"✦  {item}", font=font_body, fill=SURFACE)

        # Bottom StudyMate branding watermark
        b_text = "STUDYMATE  •  ACADEMIC EXCELLENCE"
        b_bbox = draw.textbbox((0, 0), b_text, font=font_sub)
        bw = b_bbox[2] - b_bbox[0]
        draw.text(((1080 - bw) // 2, 1680), b_text, font=font_sub, fill=GOLD_DARK)

        bg.save(out_path, 'PNG')
        print(f"Generated: {out_path} ({bg.size})")

if __name__ == '__main__':
    create_app_icon()
    create_feature_graphic()
    create_phone_screenshots()
