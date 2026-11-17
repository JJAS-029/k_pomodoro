# Capturas para Google Play: título, subtítulo y el teléfono enmarcado sobre el azul del ícono.
# Uso: python play/componer_capturas.py <carpeta con es/ y en/ de capturas 1080x2400 del emulador>
# Nombres de entrada: 1_timer, 3_plan, 4_table, 5_element, 6_synth, 7_stats, 8_year, 9_medals.
# Sale en play/listing/{es-419,en-US}/screenshots/ a 1080x1920 (16:9, lo que Play acepta).
import os
import sys
from PIL import Image, ImageDraw, ImageFilter, ImageFont

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FUENTE = os.path.join(RAIZ, 'android/app/src/main/res/font/space_grotesk.ttf')
W, H = 1080, 1920

# Azul marino del ícono, con un brillo verde abajo como el líquido; títulos en blanco y verde de la app
FONDO_ARRIBA, FONDO_ABAJO = (14, 26, 46), (30, 51, 80)
TITULO, SUBTITULO = (245, 247, 255), (170, 240, 150)

TEXTOS = {
    'es-419': [
        ('1_timer', 'Tu enfoque se vuelve química', 'Cada pomodoro te da un elemento'),
        ('3_plan', 'Arma tu plan en segundos', 'Horas, descansos, etiquetas y metas'),
        ('4_table', 'Completa la tabla periódica', '118 elementos por descubrir'),
        ('5_element', 'Aprende de cada elemento', 'Qué es, para qué sirve y quién lo descubrió'),
        ('6_synth', 'Fusiona y crea sintéticos', 'Suma números atómicos en el sintetizador'),
        ('7_stats', 'Mira cómo vas', 'Enfoque, racha y tus mejores días'),
        ('8_year', 'Tu año de estudio', 'Cada día con enfoque, de un vistazo'),
        ('9_medals', 'Gana medallas y retos', 'Constancia que se nota'),
    ],
    'en-US': [
        ('1_timer', 'Turn focus into chemistry', 'Every pomodoro earns you an element'),
        ('3_plan', 'Plan your session in seconds', 'Hours, breaks, tags and goals'),
        ('4_table', 'Complete the periodic table', '118 elements to discover'),
        ('5_element', 'Learn about every element', "What it is, what it's for, who found it"),
        ('6_synth', 'Fuse elements, make new ones', 'Add atomic numbers in the synthesizer'),
        ('7_stats', 'See how you are doing', 'Focus time, streaks and your best days'),
        ('8_year', 'Your year of focus', 'Every focused day at a glance'),
        ('9_medals', 'Earn medals and challenges', 'Consistency you can see'),
    ],
}
# Nombre final en la ficha (orden de Play)
SALIDA = {'1_timer': '1_timer', '3_plan': '2_plan', '4_table': '3_table', '5_element': '4_element',
          '6_synth': '5_synth', '7_stats': '6_stats', '8_year': '7_year', '9_medals': '8_medals'}


def fuente(tam, peso):
    f = ImageFont.truetype(FUENTE, tam)
    f.set_variation_by_axes([peso])
    return f


def texto_que_cabe(d, texto, tam, peso, max_ancho=W - 96):
    """La letra más grande (hasta `tam`) con la que el texto cabe en una línea."""
    while True:
        f = fuente(tam, peso)
        if d.textlength(texto, font=f) <= max_ancho or tam <= 30:
            return f
        tam -= 2


def fondo():
    im = Image.new('RGB', (W, H), FONDO_ARRIBA)
    d = ImageDraw.Draw(im)
    for y in range(H):
        t = y / H
        d.line((0, y, W, y), fill=tuple(round(a + (b - a) * t) for a, b in zip(FONDO_ARRIBA, FONDO_ABAJO)))
    # Brillo verde suave detrás del teléfono
    brillo = Image.new('L', (W, H), 0)
    ImageDraw.Draw(brillo).ellipse((140, 900, 940, 1900), fill=70)
    brillo = brillo.filter(ImageFilter.GaussianBlur(160))
    im.paste((60, 200, 90), (0, 0), brillo)
    return im


def componer(captura, titulo, subtitulo):
    im = fondo()
    d = ImageDraw.Draw(im)
    f1 = texto_que_cabe(d, titulo, 66, 700)
    f2 = texto_que_cabe(d, subtitulo, 40, 400)
    for texto, f, y, color in ((titulo, f1, 92, TITULO), (subtitulo, f2, 188, SUBTITULO)):
        w = d.textlength(texto, font=f)
        d.text(((W - w) / 2, y), texto, font=f, fill=color)
    # Teléfono: 880 de ancho, sale por abajo
    pw, x, y = 880, 100, 300
    cap = captura.convert('RGB')
    cap = cap.resize((pw, round(cap.height * pw / cap.width)), Image.LANCZOS)
    cap = cap.crop((0, 0, pw, min(H - y + 60, cap.height)))
    marco = 18
    sombra = Image.new('L', (W, H), 0)
    ImageDraw.Draw(sombra).rounded_rectangle((x - marco, y - marco + 14, x + pw + marco, H + 200), 72, fill=110)
    sombra = sombra.filter(ImageFilter.GaussianBlur(28))
    im.paste((0, 0, 0), (0, 0), sombra)
    ImageDraw.Draw(im).rounded_rectangle((x - marco, y - marco, x + pw + marco, H + 200), 72, fill=(22, 24, 30),
                                         outline=(70, 80, 100), width=3)
    mascara = Image.new('L', cap.size, 0)
    ImageDraw.Draw(mascara).rounded_rectangle((0, 0, cap.width, cap.height + 200), 56, fill=255)
    im.paste(cap, (x, y), mascara)
    return im


if __name__ == '__main__':
    entrada = sys.argv[1]
    for idioma, carpeta in (('es-419', 'es'), ('en-US', 'en')):
        destino = os.path.join(RAIZ, 'play/listing', idioma, 'screenshots')
        for nombre, titulo, subtitulo in TEXTOS[idioma]:
            captura = Image.open(os.path.join(entrada, carpeta, nombre + '.png'))
            assert captura.size == (1080, 2400), (nombre, captura.size)
            componer(captura, titulo, subtitulo).save(os.path.join(destino, SALIDA[nombre] + '.png'), optimize=True)
    print('listo')
