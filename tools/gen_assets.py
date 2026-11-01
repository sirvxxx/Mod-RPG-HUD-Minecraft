#!/usr/bin/env python3
"""
Generator tekstur Custom RPG HUD.

Menghasilkan PNG per tema ke:
  src/main/resources/assets/custom_rpg_hud/textures/gui/<tema>/{frame,badge,ring,icon_coords,icon_fps}.png

Ukuran tetap (jangan diubah kalau mau tetap cocok dengan kode Java):
  frame.png  20x20  (9-slice, border 5px)  -> panel coords & FPS
  badge.png  16x16  (9-slice, border 4px)  -> kapsul nama & level
  ring.png   96x96                         -> lingkaran utama (rim, cincin dalam, bayangan, track kotak kosong)
  icon_*.png 8x8  (coords, fps, heart, food, armor)

Pakai:  python3 tools/gen_assets.py            -> tulis semua PNG
        python3 tools/gen_assets.py --java     -> cetak baris preset untuk HudTheme.java
Butuh: pip install pillow

Mau bikin tema/tekstur sendiri? Edit PNG-nya langsung (atau timpa lewat resource pack
dengan path yang sama), warna bar & teks diatur di HudTheme.java.
"""
import math
import os
import sys
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                   "assets", "custom_rpg_hud", "textures", "gui")

# --------------------------------------------------------------------------- palet
# metal: outline mid hl_top hl_left sh_bottom sh_right bronze inner_top inner_bottom stud reflect
# ring : band_back inner_disc
# teks : text level      bar: hp food armor ghost sat      kosong: e0 e1 e2 e3
PALETTES = {
    "hardcore": dict(  # Hitam arang & merah darah
        outline=0x0A0A0B, mid=0x2C2C30, hl_top=0x9C2B2B, hl_left=0x6A1D1D, sh_bottom=0x121214,
        sh_right=0x1C1C1F, bronze=0x4D1010, inner_top=0x1A1315, inner_bottom=0x080607,
        stud=0xD94141, reflect=0xE05050, band_back=0x060405, inner_disc=0x100B0C,
        text=0xFF5A4F, level=0xFF7B6B, hp=0xC81414, food=0xE8801A, armor=0xB4B6BC,
        ghost=0xE8C8C0, sat=0xFFD9A0, e0=0x1B1112, e1=0x2A1C1D, e2=0x382627, e3=0x0A0506),
    "netherite": dict(  # Obsidian & emas
        outline=0x08060C, mid=0x3A3345, hl_top=0xF2D27A, hl_left=0xC9A24A, sh_bottom=0x1A1524,
        sh_right=0x261F33, bronze=0x8A6A2A, inner_top=0x1D1727, inner_bottom=0x0A0810,
        stud=0xFFE6A0, reflect=0xE0BB62, band_back=0x07050B, inner_disc=0x120E19,
        text=0xFFD36B, level=0x9CFF7A, hp=0xE02A2A, food=0xFF9F1C, armor=0xC4CAD2,
        ghost=0xEBDDB8, sat=0xFFF2A0, e0=0x1E1511, e1=0x2C211A, e2=0x3A2D24, e3=0x0B0705),
    "forest": dict(  # Hijau zamrud & lumut
        outline=0x07100A, mid=0x3F6B3A, hl_top=0xA8E6A0, hl_left=0x6FBF6A, sh_bottom=0x1D3A22,
        sh_right=0x275030, bronze=0x2E7D4A, inner_top=0x142418, inner_bottom=0x08120B,
        stud=0xD8FFC8, reflect=0x7FD08A, band_back=0x050B07, inner_disc=0x0D1A10,
        text=0x7DFFA0, level=0xB8FF6A, hp=0xD8322E, food=0xE8A22A, armor=0xB0BFB8,
        ghost=0xE6E0B8, sat=0xF4F08A, e0=0x14201A, e1=0x1F2E25, e2=0x2B3D31, e3=0x070D09),
    "celestial": dict(  # Perak & biru magis
        outline=0x0A0C14, mid=0x8E98B0, hl_top=0xF0F4FF, hl_left=0xC8D2EA, sh_bottom=0x4A5470,
        sh_right=0x5A6684, bronze=0x6B78A8, inner_top=0x172038, inner_bottom=0x090C1A,
        stud=0xFFFFFF, reflect=0x9CB4FF, band_back=0x060810, inner_disc=0x0E1226,
        text=0x9CC8FF, level=0xC8E0FF, hp=0xE0455A, food=0xFFB347, armor=0xD6DEF2,
        ghost=0xE8E8FF, sat=0xFFF0B0, e0=0x131828, e1=0x1D243A, e2=0x293250, e3=0x080A12),
    "cyber": dict(  # Metalik & neon cyan
        outline=0x05080C, mid=0x4A5560, hl_top=0x7DF9FF, hl_left=0x3FC9D6, sh_bottom=0x1B2229,
        sh_right=0x242D36, bronze=0x14808C, inner_top=0x0F1C22, inner_bottom=0x050A0E,
        stud=0x9DFFFF, reflect=0x3FE0F0, band_back=0x03070A, inner_disc=0x081217,
        text=0x5CF2FF, level=0x7DFFE0, hp=0xFF3860, food=0xFFC400, armor=0x9DB2C4,
        ghost=0xD0F0F5, sat=0xFFFFA0, e0=0x0E1A20, e1=0x16262E, e2=0x203541, e3=0x050C10),
    "classic": dict(  # Kayu & vintage
        outline=0x1A0F08, mid=0x7A5230, hl_top=0xD9AE72, hl_left=0xB38650, sh_bottom=0x4A2E16,
        sh_right=0x5C3A1C, bronze=0x8C6A3A, inner_top=0x2A1D12, inner_bottom=0x120C07,
        stud=0xE8C890, reflect=0xC9A15C, band_back=0x0D0805, inner_disc=0x1C130C,
        text=0xF0C878, level=0xB8E086, hp=0xC0362C, food=0xE08A2A, armor=0xB8B0A0,
        ghost=0xEBDDB8, sat=0xF2DC8A, e0=0x261A12, e1=0x35271C, e2=0x443426, e3=0x100A06),
}
ORDER = ["hardcore", "netherite", "forest", "celestial", "cyber", "classic"]

# geometri lingkaran (HARUS sama dengan HudRenderer.java)
CIRCLE_R = 46
BAND_RO = 40
BAND_RI = 33


# --------------------------------------------------------------------------- util gambar
def rgba(c, a=255):
    return ((c >> 16) & 255, (c >> 8) & 255, c & 255, a)


def blend(dst, src):
    sa, da = src[3] / 255.0, dst[3] / 255.0
    oa = sa + da * (1 - sa)
    if oa <= 0:
        return (0, 0, 0, 0)
    out = [int(round((src[i] * sa + dst[i] * da * (1 - sa)) / oa)) for i in range(3)]
    return (out[0], out[1], out[2], int(round(oa * 255)))


class Canvas:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.img = Image.new("RGBA", (w, h), (0, 0, 0, 0))

    def fill(self, x0, y0, x1, y1, col):
        for y in range(max(0, y0), min(self.h, y1)):
            for x in range(max(0, x0), min(self.w, x1)):
                self.img.putpixel((x, y), blend(self.img.getpixel((x, y)), col))

    def vgrad(self, x, y, w, h, top, bottom):
        for i in range(h):
            t = 0.0 if h <= 1 else i / (h - 1)
            c = tuple(int(top[k] * (1 - t) + bottom[k] * t) for k in range(3)) + (255,)
            self.fill(x, y + i, x + w, y + i + 1, c)

    def disc(self, cx, cy, r, col):
        for dy in range(-r, r):
            fy = dy + 0.5
            half = int(math.floor(math.sqrt(max(0.0, r * r - fy * fy)) + 0.5))
            if half > 0:
                self.fill(cx - half, cy + dy, cx + half, cy + dy + 1, col)

    def save(self, path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        self.img.save(path)


# --------------------------------------------------------------------------- bagian tekstur
def draw_frame(cv, x, y, w, h, P):
    o, m = rgba(P["outline"]), rgba(P["mid"])
    cv.fill(x + 1, y, x + w - 1, y + h, o)
    cv.fill(x, y + 1, x + w, y + h - 1, o)
    cv.fill(x + 2, y + 1, x + w - 2, y + h - 1, m)
    cv.fill(x + 1, y + 2, x + w - 1, y + h - 2, m)
    cv.fill(x + 2, y + 1, x + w - 2, y + 2, rgba(P["hl_top"]))
    cv.fill(x + 1, y + 2, x + 2, y + h - 2, rgba(P["hl_left"]))
    cv.fill(x + 2, y + h - 2, x + w - 2, y + h - 1, rgba(P["sh_bottom"]))
    cv.fill(x + w - 2, y + 2, x + w - 1, y + h - 2, rgba(P["sh_right"]))
    cv.fill(x + 3, y + 3, x + w - 3, y + h - 3, rgba(P["bronze"]))
    cv.vgrad(x + 4, y + 4, w - 8, h - 8, rgba(P["inner_top"]), rgba(P["inner_bottom"]))
    cv.fill(x + 4, y + 4, x + w - 4, y + 5, o)
    cv.fill(x + 4, y + 5, x + 5, y + h - 4, o)
    cv.fill(x + 4, y + h - 5, x + w - 4, y + h - 4, rgba(P["reflect"], 0x40))
    st = rgba(P["stud"])
    cv.fill(x + 2, y + 2, x + 4, y + 4, st)
    cv.fill(x + w - 4, y + 2, x + w - 2, y + 4, st)
    cv.fill(x + 2, y + h - 4, x + 4, y + h - 2, st)
    cv.fill(x + w - 4, y + h - 4, x + w - 2, y + h - 2, st)


def draw_badge(cv, x, y, w, h, P):
    o = rgba(P["outline"])
    cv.fill(x + 1, y, x + w - 1, y + h, o)
    cv.fill(x, y + 1, x + w, y + h - 1, o)
    cv.fill(x + 1, y + 1, x + w - 1, y + h - 1, rgba(P["mid"]))
    cv.fill(x + 2, y + 1, x + w - 2, y + 2, rgba(P["hl_top"]))
    cv.fill(x + 1, y + 2, x + 2, y + h - 2, rgba(P["hl_left"]))
    cv.fill(x + 2, y + h - 2, x + w - 2, y + h - 1, rgba(P["sh_bottom"]))
    cv.vgrad(x + 3, y + 3, w - 6, h - 6, rgba(P["inner_top"]), rgba(P["inner_bottom"]))


CELL_DEG = 9.0


def band_pixels():
    """Pixel cincin kotak: (dx, dy, shade). shade 0/1/2 = isi (dalam/tengah/luar), 3 = garis tepi.
    HARUS identik dengan CircleLayout.initBand() di Java."""
    out = []
    for dy in range(-BAND_RO, BAND_RO):
        fy = dy + 0.5
        for dx in range(-BAND_RO, BAND_RO):
            fx = dx + 0.5
            d2 = fx * fx + fy * fy
            if d2 < BAND_RI * BAND_RI or d2 >= BAND_RO * BAND_RO:
                continue
            r = math.sqrt(d2)
            th = math.degrees(math.atan2(fx, -fy))
            if th < 0:
                th += 360.0
            if th >= 180.0:
                n, l = 20, th - 180.0
            elif th < 90.0:
                n, l = 10, 90.0 - th
            else:
                n, l = 10, 180.0 - th
            k = min(n - 1, int(math.floor(l / CELL_DEG)))
            p = l - k * CELL_DEG
            e = math.degrees(1.0 / r)
            edge = (r < BAND_RI + 1.0 or r >= BAND_RO - 1.0 or p < e
                    or (k == n - 1 and p > CELL_DEG - e))
            sh = 3 if edge else (0 if r < BAND_RI + 2.0 else (2 if r >= BAND_RO - 2.0 else 1))
            out.append((dx, dy, sh))
    return out


def draw_ring(cv, P):
    c = CIRCLE_R
    cv.disc(c + 1, c + 2, c + 1, (0, 0, 0, 0x55))                 # bayangan
    cv.disc(c, c, c, rgba(P["outline"]))
    cv.disc(c, c, c - 1, rgba(P["hl_top"]))
    cv.disc(c + 1, c + 1, c - 2, rgba(P["sh_bottom"]))
    cv.disc(c, c, c - 2, rgba(P["mid"]))
    cv.disc(c, c, c - 3, rgba(P["bronze"]))                      # garis ukir rim
    cv.disc(c, c, c - 4, rgba(P["mid"]))
    cv.disc(c, c, BAND_RO + 1, rgba(P["band_back"]))             # alas cincin kotak
    cv.disc(c, c, BAND_RI, rgba(P["hl_left"]))                   # cincin dalam (cekung)
    cv.disc(c - 1, c - 1, BAND_RI - 1, rgba(P["sh_bottom"]))
    cv.disc(c, c, BAND_RI - 2, rgba(P["inner_disc"]))            # alas kepala
    # track kotak kosong + garis tepi dipanggang di sini; kode Java hanya menggambar kotak yang terisi
    empty = [rgba(P["e0"]), rgba(P["e1"]), rgba(P["e2"]), rgba(P["e3"])]
    for dx, dy, sh in band_pixels():
        cv.img.putpixel((c + dx, c + dy), empty[sh])


ICON_COORDS = [
    "..1111..",
    ".122221.",
    ".123321.",
    ".123321.",
    "..1221..",
    "..1221..",
    "...11...",
    "...11...",
]
ICON_FPS = [
    "11111111",
    "12222221",
    "12333321",
    "12333321",
    "12222221",
    "11111111",
    "..1221..",
    ".111111.",
]


ICON_HEART = [
    ".11..11.",
    "12311221",
    "12222221",
    "12222221",
    ".122221.",
    "..1221..",
    "...11...",
    "........",
]
ICON_FOOD = [
    "..1111..",
    ".122221.",
    ".123221.",
    ".122221.",
    "..1221..",
    "...11...",
    "..1111..",
    "..1..1..",
]
ICON_ARMOR = [
    "11111111",
    "12222221",
    "12332221",
    "12222221",
    "12222221",
    ".122221.",
    "..1221..",
    "...11...",
]


def draw_icon(rows, P, key="text"):
    cv = Canvas(8, 8)
    base = P[key]
    light = tuple(min(255, int(((base >> s) & 255) * 0.45 + 255 * 0.55)) for s in (16, 8, 0))
    pal = {"1": rgba(P["outline"]), "2": rgba(base), "3": (light[0], light[1], light[2], 255)}
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                cv.img.putpixel((x, y), pal[ch])
    return cv


# --------------------------------------------------------------------------- main
def build_all():
    for name in ORDER:
        P = PALETTES[name]
        d = os.path.join(OUT, name)
        fr = Canvas(20, 20); draw_frame(fr, 0, 0, 20, 20, P); fr.save(os.path.join(d, "frame.png"))
        bd = Canvas(16, 16); draw_badge(bd, 0, 0, 16, 16, P); bd.save(os.path.join(d, "badge.png"))
        rg = Canvas(96, 96); draw_ring(rg, P); rg.save(os.path.join(d, "ring.png"))
        draw_icon(ICON_COORDS, P).save(os.path.join(d, "icon_coords.png"))
        draw_icon(ICON_FPS, P).save(os.path.join(d, "icon_fps.png"))
        draw_icon(ICON_HEART, P, "hp").save(os.path.join(d, "icon_heart.png"))
        draw_icon(ICON_FOOD, P, "food").save(os.path.join(d, "icon_food.png"))
        draw_icon(ICON_ARMOR, P, "armor").save(os.path.join(d, "icon_armor.png"))
    print("PNG ditulis ke", os.path.abspath(OUT))


def java_presets():
    keys = ["text", "level", "hp", "food", "armor", "ghost", "sat", "e0", "e1", "e2", "e3", "band_back"]
    for name in ORDER:
        P = PALETTES[name]
        vals = ", ".join("0xFF%06X" % P[k] for k in keys)
        print('            new HudTheme("%s", %s),' % (name, vals))


if __name__ == "__main__":
    if "--java" in sys.argv:
        java_presets()
    else:
        build_all()
