from PIL import Image, ImageDraw
def face(path, fill, mood):
    s = 4; W = 120 * s
    im = Image.new("RGBA", (W, W), (255, 255, 255, 0)); d = ImageDraw.Draw(im)
    d.ellipse([4*s, 4*s, W-4*s, W-4*s], fill=fill, outline=(60, 60, 60), width=4*s)
    for x in (40, 80):
        d.ellipse([(x-8)*s, 38*s, (x+8)*s, 56*s], fill=(40, 40, 40))
    if mood == "happy":
        d.arc([30*s, 45*s, 90*s, 95*s], 20, 160, fill=(40, 40, 40), width=6*s)
    elif mood == "neutral":
        d.line([36*s, 80*s, 84*s, 80*s], fill=(40, 40, 40), width=6*s)
    else:
        d.arc([30*s, 72*s, 90*s, 118*s], 200, 340, fill=(40, 40, 40), width=6*s)
    im.resize((240, 240), Image.LANCZOS).save(path)
face("smiley_gut.png", (112, 190, 90), "happy")
face("smiley_mittel.png", (250, 210, 70), "neutral")
face("smiley_schlecht.png", (235, 110, 95), "sad")
