# -*- coding: utf-8 -*-
import os
from reportlab.lib.pagesizes import letter
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, HRFlowable, KeepTogether

pdf_path = r"C:\Users\Diovar09\Desktop\GUIA_DESARROLLO_LOGIDESIGN_AI.pdf"
doc = SimpleDocTemplate(
    pdf_path,
    pagesize=letter,
    rightMargin=40,
    leftMargin=40,
    topMargin=40,
    bottomMargin=40
)

styles = getSampleStyleSheet()

# Custom styles
title_style = ParagraphStyle(
    'DocTitle',
    parent=styles['Normal'],
    fontName='Helvetica-Bold',
    fontSize=22,
    leading=26,
    textColor=colors.HexColor('#0F172A'),
    alignment=1
)

subtitle_style = ParagraphStyle(
    'DocSubtitle',
    parent=styles['Normal'],
    fontName='Helvetica',
    fontSize=12,
    leading=16,
    textColor=colors.HexColor('#64748B'),
    alignment=1
)

h1_style = ParagraphStyle(
    'SectionH1',
    parent=styles['Normal'],
    fontName='Helvetica-Bold',
    fontSize=14,
    leading=18,
    textColor=colors.HexColor('#1E293B'),
    spaceBefore=14,
    spaceAfter=6
)

h2_style = ParagraphStyle(
    'SectionH2',
    parent=styles['Normal'],
    fontName='Helvetica-Bold',
    fontSize=11,
    leading=15,
    textColor=colors.HexColor('#0284C7'),
    spaceBefore=10,
    spaceAfter=4
)

body_style = ParagraphStyle(
    'BodyDark',
    parent=styles['Normal'],
    fontName='Helvetica',
    fontSize=9.5,
    leading=14,
    textColor=colors.HexColor('#334155')
)

bullet_style = ParagraphStyle(
    'BulletDark',
    parent=styles['Normal'],
    fontName='Helvetica',
    fontSize=9,
    leading=13.5,
    textColor=colors.HexColor('#334155'),
    leftIndent=15
)

code_style = ParagraphStyle(
    'CodeSnippet',
    parent=styles['Normal'],
    fontName='Courier',
    fontSize=8.5,
    leading=11,
    textColor=colors.HexColor('#0F172A'),
    backColor=colors.HexColor('#F1F5F9'),
    borderPadding=6,
    spaceBefore=4,
    spaceAfter=6
)

callout_style = ParagraphStyle(
    'CalloutText',
    parent=styles['Normal'],
    fontName='Helvetica-Oblique',
    fontSize=9,
    leading=13,
    textColor=colors.HexColor('#0369A1'),
    backColor=colors.HexColor('#F0F9FF'),
    borderPadding=8,
    spaceBefore=6,
    spaceAfter=8
)

story = []

# Header
story.append(Paragraph("LOGIDESIGN AI — MANUAL DE DESARROLLO", title_style))
story.append(Spacer(1, 4))
story.append(Paragraph("Guia Paso a Paso para Clase: Creador de Webs con IA (Android Studio + Java + Ollama)", subtitle_style))
story.append(Spacer(1, 10))
story.append(HRFlowable(width="100%", thickness=1.5, color=colors.HexColor("#0EA5E9"), spaceAfter=14))

# Resumen de Estado
story.append(Paragraph("1. ESTADO ACTUAL: LO QUE YA TENEMOS FUNCIONANDO", h1_style))
story.append(Paragraph("Tu aplicacion ya cuenta con los cimientos mas complejos de la arquitectura:", body_style))
story.append(Paragraph("&bull; <b>Chat conversacional en tiempo real:</b> Burbujas dinamicas (usuario a la derecha, IA a la izquierda).", bullet_style))
story.append(Paragraph("&bull; <b>Despliegue automatico de teclado:</b> Al tocar el chat, el teclado sube inmediatamente sin pedir metodos extra.", bullet_style))
story.append(Paragraph("&bull; <b>Boton de envio inteligente:</b> Detecta texto y reemplaza los iconos de voz/micro por el boton Enviar.", bullet_style))
story.append(Paragraph("&bull; <b>Arquitectura Tri-Motor (Hibrida):</b>", bullet_style))
story.append(Paragraph("&nbsp;&nbsp;&nbsp;&nbsp;1) <b>Ollama Local (PC):</b> Conectado a tu modelo <code>qwen2.5-coder:7b</code> via <code>http://10.0.2.2:11434</code> (100% gratis, sin gastar saldo).", bullet_style))
story.append(Paragraph("&nbsp;&nbsp;&nbsp;&nbsp;2) <b>Google Gemini (Online):</b> Conectado a la API oficial <code>gemini-3.8-flash</code> listo para la entrega final.", bullet_style))
story.append(Paragraph("&nbsp;&nbsp;&nbsp;&nbsp;3) <b>Modo Local (Offline):</b> Respaldo autonomo dentro del movil en caso de estar en Modo Avion o sin red.", bullet_style))
story.append(Paragraph("&bull; <b>Experiencia sin tecnicismos:</b> Se eliminaron las palabras 'demo' y 'plantilla' para que el usuario sienta que la IA crea su web desde cero.", bullet_style))
story.append(Spacer(1, 10))

# Modulo 2: Conexion de Demos
story.append(Paragraph("2. SIGUIENTE PASO EN CLASE: INTEGRAR LOS DEMOS DE WEBS", h1_style))
story.append(Paragraph("<b>Objetivo:</b> Que la app tome las plantillas HTML ya disenadas y sustituya los datos del cliente.", body_style))
story.append(Spacer(1, 4))
story.append(Paragraph("<b>Paso 2.1: Ubicacion de los Demos en Android Studio</b>", h2_style))
story.append(Paragraph("Crea la carpeta <code>assets</code> dentro de <code>app/src/main/</code>. La estructura debe ser:", body_style))
story.append(Paragraph("<code>app/src/main/assets/templates/restaurante/index.html</code><br/>"
                       "<code>app/src/main/assets/templates/taller/index.html</code><br/>"
                       "<code>app/src/main/assets/templates/veterinaria/index.html</code><br/>"
                       "<code>app/src/main/assets/templates/reformas/index.html</code>", code_style))

story.append(Paragraph("<b>Paso 2.2: Etiquetas de reemplazo en el HTML</b>", h2_style))
story.append(Paragraph("En cada archivo <code>index.html</code>, coloca variables faciles de sustituir:", body_style))
story.append(Paragraph("&bull; <code>{{NOMBRE_NEGOCIO}}</code> en el titulo y en la cabecera del menu.", bullet_style))
story.append(Paragraph("&bull; <code>{{TELEFONO}}</code> y <code>{{ENLACE_WHATSAPP}}</code> en los botones de reserva y llamada.", bullet_style))
story.append(Paragraph("&bull; <code>{{DIRECCION}}</code> en el pie de pagina y seccion de ubicacion.", bullet_style))
story.append(Paragraph("&bull; <code>{{SERVICIOS}}</code> en la lista de especialidades o platos.", bullet_style))

story.append(Paragraph("<b>Paso 2.3: Inyeccion de datos y apertura en WebView</b>", h2_style))
story.append(Paragraph("En Java, leemos el archivo como texto, reemplazamos las etiquetas con <code>.replace()</code> y cargamos el HTML en pantalla completa con un <code>WebView</code>:", body_style))
story.append(Paragraph("String html = AssetUtils.leerAsset(this, \"templates/restaurante/index.html\");<br/>"
                       "html = html.replace(\"{{NOMBRE_NEGOCIO}}\", nombreCapturado);<br/>"
                       "html = html.replace(\"{{TELEFONO}}\", telefonoCapturado);<br/>"
                       "webView.loadDataWithBaseURL(\"file:///android_asset/templates/restaurante/\", html, \"text/html\", \"UTF-8\", null);", code_style))
story.append(Spacer(1, 10))

# Modulo 3: Historial de Chats
story.append(Paragraph("3. HISTORIAL DE CONVERSACIONES (ACTIVIDAD Chat.java)", h1_style))
story.append(Paragraph("<b>Objetivo:</b> Que al pulsar en el menu lateral 'Chats' (<code>Chat.java</code>), se muestre la lista de conversaciones pasadas.", body_style))
story.append(Paragraph("&bull; <b>Persistencia Local:</b> Usar <code>SharedPreferences</code> o una base de datos <code>SQLite (Room)</code>.", bullet_style))
story.append(Paragraph("&bull; <b>Estructura de cada chat:</b> <code>id</code>, <code>titulo</code> (ej. 'Web Pizzería Roma'), <code>fecha</code> y lista de mensajes.", bullet_style))
story.append(Paragraph("&bull; <b>Boton [+ Nuevo Chat]:</b> Al tocarlo en <code>activity_chat.xml</code>, abre <code>MainActivity</code> con la pantalla limpia y listo para empezar.", bullet_style))
story.append(Spacer(1, 10))

# Modulo 4: Proyectos Guardados
story.append(Paragraph("4. GESTION DE PROYECTOS (ProjectsActivity.java)", h1_style))
story.append(Paragraph("<b>Objetivo:</b> Un panel donde el usuario pueda ver todas las webs que ha creado.", body_style))
story.append(Paragraph("&bull; Tarjeta visual con el nombre de la empresa, fecha de creacion y estado (Listo).", bullet_style))
story.append(Paragraph("&bull; <b>Boton 'Ver Web':</b> Abre la web en el navegador del telefono o en el WebView.", bullet_style))
story.append(Paragraph("&bull; <b>Boton 'Compartir':</b> Envia el archivo HTML generado o mensaje por WhatsApp.", bullet_style))
story.append(Spacer(1, 10))

# Modulo 5: Defensa ante el profesor
story.append(Paragraph("5. COMO DEFENDER ESTE PROYECTO ANTE EL PROFESOR", h1_style))
story.append(Paragraph("Para obtener la calificacion maxima, explica la solucion con estos 3 puntos clave:", body_style))
story.append(Paragraph("1. <b>Arquitectura Hibrida:</b> Explica que usas tu propio servidor local de Ollama para no depender de terceros y ahorrar costes, pero que la app es modular y con 1 clic conmuta a Google Gemini oficial o al motor offline interno.", bullet_style))
story.append(Paragraph("2. <b>Diseno Basado en Plantillas Profesionales:</b> Justifica que la IA no inventa CSS roto desde cero; la IA actua como capturador inteligente de requerimientos y motor de inyeccion de datos sobre un estandar de alta calidad.", bullet_style))
story.append(Paragraph("3. <b>Zero-Crash Policy:</b> Demuestra que si se apaga el Wi-Fi en pleno examen, la aplicacion no se cierra ni muestra errores de red, sino que mantiene el flujo de negocio al 100%.", bullet_style))

story.append(Spacer(1, 14))
story.append(Paragraph("Documento generado para Diovar Valencia &bull; Proyecto LogiDesign AI", callout_style))

doc.build(story)
print("PDF generado con exito en:", pdf_path)
