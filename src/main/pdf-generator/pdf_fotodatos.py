# pdf-generator/pdf_fotodatos.py
import pandas as pd
from reportlab.lib.pagesizes import A4, landscape
from reportlab.platypus import SimpleDocTemplate, Table, TableStyle, Paragraph, Spacer, PageBreak, Image
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib import colors
from reportlab.lib.units import inch, cm
from reportlab.lib.enums import TA_CENTER, TA_LEFT, TA_RIGHT
from reportlab.graphics.shapes import Drawing, Rect, Line
from reportlab.graphics.charts.barcharts import VerticalBarChart
import requests
from datetime import datetime
import json
import os

class PDFFotoDatosGenerator:
    def __init__(self, springboot_base_url: str = "http://localhost:8080"):
        self.base_url = springboot_base_url
        self._setup_styles()

    def _setup_styles(self):
        """Configurar estilos personalizados"""
        self.styles = getSampleStyleSheet()

        # Título principal
        self.styles.add(ParagraphStyle(
            name='MainTitle',
            parent=self.styles['Heading1'],
            fontSize=18,
            textColor=colors.HexColor('#1a365d'),
            alignment=TA_CENTER,
            spaceAfter=30,
            fontName='Helvetica-Bold'
        ))

        # Subtítulo
        self.styles.add(ParagraphStyle(
            name='SubTitle',
            parent=self.styles['Heading2'],
            fontSize=14,
            textColor=colors.HexColor('#2d3748'),
            spaceAfter=20,
            fontName='Helvetica-Bold'
        ))

        # Encabezado de sección
        self.styles.add(ParagraphStyle(
            name='SectionHeader',
            parent=self.styles['Heading3'],
            fontSize=12,
            textColor=colors.HexColor('#2b6cb0'),
            spaceAfter=10,
            fontName='Helvetica-Bold'
        ))

        # Texto normal mejorado
        self.styles.add(ParagraphStyle(
            name='CustomNormal',
            parent=self.styles['Normal'],
            fontSize=10,
            textColor=colors.HexColor('#2d3748'),
            alignment=TA_LEFT
        ))

        # Texto de metadatos
        self.styles.add(ParagraphStyle(
            name='MetaData',
            parent=self.styles['Normal'],
            fontSize=9,
            textColor=colors.HexColor('#4a5568'),
            alignment=TA_LEFT,
            spaceAfter=3
        ))

    def obtener_datos_prueba(self):
        """Datos de prueba mientras configuras la API real"""
        return [
            {
                'id_foto': 1,
                'nombre_obra': 'Obra Norte',
                'actividad': 'Cimentación - Excavación',
                'fecha': '2024-01-15',
                'coordenadas': '4.6097, -74.0817',
                'nombre_archivo': 'foto1.jpg',
                'tamanio_kb': 2450,
                'usuario': 'juan.perez',
                'descripcion': 'Excavación finalizada en sector norte'
            },
            {
                'id_foto': 2,
                'nombre_obra': 'Obra Sur',
                'actividad': 'Estructura - Columnas',
                'fecha': '2024-01-16',
                'coordenadas': '4.7107, -74.0721',
                'nombre_archivo': 'foto2.jpg',
                'tamanio_kb': 3120,
                'usuario': 'maria.gomez',
                'descripcion': 'Montaje de columnas estructura principal'
            }
        ]

    def obtener_fotodatos_real(self, filtros: dict = None):
        """Conectar con tu API Spring Boot real"""
        try:
            # AJUSTA ESTA URL SEGÚN TU API
            url = f"{self.base_url}/api/fotodatos"

            # Si tu API requiere autenticación, agrega headers
            headers = {
                'Content-Type': 'application/json',
                # 'Authorization': 'Bearer tu-token'  # Si necesitas autenticación
            }

            response = requests.get(url, headers=headers, params=filtros)

            if response.status_code == 200:
                return response.json()
            else:
                print(f"❌ Error API: {response.status_code}")
                return self.obtener_datos_prueba()

        except Exception as e:
            print(f"⚠️  Error conectando API, usando datos de prueba: {e}")
            return self.obtener_datos_prueba()

    def _aplicar_filtros(self, datos: list, filtros: dict):
        """Aplicar filtros locales"""
        if not filtros:
            return datos

        datos_filtrados = datos

        # Filtrar por obra
        if filtros.get('obra'):
            datos_filtrados = [d for d in datos_filtrados
                               if filtros['obra'].lower() in d['nombre_obra'].lower()]

        # Ordenar
        orden = filtros.get('ordenar_por', 'fecha')
        direccion = filtros.get('direccion_orden', 'desc')

        reverse = (direccion == 'desc')

        if orden == 'fecha':
            datos_filtrados.sort(key=lambda x: x.get('fecha', ''), reverse=reverse)
        elif orden == 'obra':
            datos_filtrados.sort(key=lambda x: x.get('nombre_obra', ''), reverse=reverse)
        elif orden == 'tamanio':
            datos_filtrados.sort(key=lambda x: x.get('tamanio_kb', 0), reverse=reverse)

        return datos_filtrados


    def _crear_header(self, elementos):
        """Crear encabezado del reporte"""
        # Logo o encabezado visual
        header_frame = Drawing(400, 50)
        header_frame.add(Rect(0, 0, 400, 50, fillColor=colors.HexColor('#1a365d'), strokeColor=None))
        header_frame.add(Rect(0, 45, 400, 5, fillColor=colors.HexColor('#48bb78'), strokeColor=None))

        # Título con fecha
        fecha_actual = datetime.now().strftime('%d de %B de %Y a las %H:%M')
        title_text = f"<font color='white' size='16'><b>📸 FOTODATOS - REPORTE DE AVANCE</b></font>"
        title = Paragraph(title_text, self.styles['CustomNormal'])

        subtitle = Paragraph(f"<font color='#a0aec0' size='10'>Generado: {fecha_actual}</font>",
                             self.styles['CustomNormal'])

        elementos.append(header_frame)
        elementos.append(Spacer(1, 10))
        elementos.append(title)
        elementos.append(subtitle)
        elementos.append(Spacer(1, 15))

        # Línea decorativa
        line = Drawing(450, 2)
        line.add(Line(0, 1, 450, 1, strokeColor=colors.HexColor('#48bb78'), strokeWidth=2))
        elementos.append(line)
        elementos.append(Spacer(1, 15))

    def _crear_resumen_estadisticas(self, datos):
        """Crear resumen de estadísticas"""
        elementos = []

        # Estadísticas
        total_fotos = len(datos)
        total_tamanio = sum(d.get('tamanio_kb', 0) for d in datos)
        tamanio_mb = total_tamanio / 1024
        obras = list(set(d.get('nombre_obra', '') for d in datos))
        actividades = list(set(d.get('actividad', '') for d in datos))

        # Tarjetas de estadísticas
        stats_data = [
            ['📸 Total Fotos', '📊 Obras', '💾 Tamaño Total', '📋 Actividades'],
            [str(total_fotos), str(len(obras)), f"{tamanio_mb:.1f} MB", str(len(actividades))]
        ]

        stats_table = Table(stats_data, colWidths=[2.2*inch, 2.2*inch, 2.2*inch, 2.2*inch])
        stats_table.setStyle(TableStyle([
            ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#2b6cb0')),
            ('TEXTCOLOR', (0, 0), (-1, 0), colors.white),
            ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
            ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
            ('FONTSIZE', (0, 0), (-1, 0), 11),
            ('FONTSIZE', (0, 1), (-1, 1), 14),
            ('FONTNAME', (0, 1), (-1, 1), 'Helvetica-Bold'),
            ('BOTTOMPADDING', (0, 0), (-1, 0), 8),
            ('TOPPADDING', (0, 1), (-1, 1), 8),
            ('GRID', (0, 0), (-1, -1), 1, colors.HexColor('#e2e8f0')),
            ('BACKGROUND', (0, 1), (-1, -1), colors.HexColor('#f7fafc')),
        ]))

        elementos.append(Paragraph("📊 RESUMEN DE ESTADÍSTICAS", self.styles['SectionHeader']))
        elementos.append(Spacer(1, 5))
        elementos.append(stats_table)
        elementos.append(Spacer(1, 15))

        return elementos

    def _crear_tabla_datos(self, datos):
        """Crear tabla principal de datos con mejor formato"""
        elementos = []

        # Encabezado
        elementos.append(Paragraph("📋 DETALLE DE FOTOS", self.styles['SectionHeader']))
        elementos.append(Spacer(1, 10))

        # Preparar datos para la tabla
        tabla_datos = []
        tabla_datos.append([
            Paragraph('<b>ID</b>', self.styles['CustomNormal']),
            Paragraph('<b>Obra</b>', self.styles['CustomNormal']),
            Paragraph('<b>Actividad</b>', self.styles['CustomNormal']),
            Paragraph('<b>Fecha</b>', self.styles['CustomNormal']),
            Paragraph('<b>Archivo</b>', self.styles['CustomNormal']),
            Paragraph('<b>Tamaño</b>', self.styles['CustomNormal']),
            Paragraph('<b>Usuario</b>', self.styles['CustomNormal'])
        ])

        # Colores alternos para filas
        row_colors = ['#f7fafc', '#edf2f7']

        for idx, dato in enumerate(datos):
            color_idx = idx % 2
            # Truncar nombre de archivo si es muy largo
            nombre_archivo = dato['nombre_archivo']
            if len(nombre_archivo) > 20:
                nombre_archivo = nombre_archivo[:17] + '...'

            tabla_datos.append([
                Paragraph(f"<b>{dato['id_foto']}</b>", self.styles['CustomNormal']),
                Paragraph(dato['nombre_obra'], self.styles['CustomNormal']),
                Paragraph(dato['actividad'], self.styles['CustomNormal']),
                Paragraph(dato['fecha'], self.styles['CustomNormal']),
                Paragraph(nombre_archivo, self.styles['CustomNormal']),
                Paragraph(f"{dato['tamanio_kb']:.1f} KB", self.styles['CustomNormal']),
                Paragraph(dato['usuario'], self.styles['CustomNormal'])
            ])

        # Crear tabla
        tabla = Table(tabla_datos, colWidths=[0.7*inch, 1.8*inch, 1.5*inch, 1.0*inch, 1.5*inch, 0.8*inch, 1.0*inch])

        # Estilos de la tabla
        style = TableStyle([
            # Encabezado
            ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#2d3748')),
            ('TEXTCOLOR', (0, 0), (-1, 0), colors.white),
            ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
            ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
            ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
            ('FONTSIZE', (0, 0), (-1, 0), 9),
            ('BOTTOMPADDING', (0, 0), (-1, 0), 10),
            ('TOPPADDING', (0, 0), (-1, 0), 10),
            # Filas
            ('BACKGROUND', (0, 1), (-1, -1), colors.HexColor('#f7fafc')),
            ('GRID', (0, 0), (-1, -1), 0.5, colors.HexColor('#cbd5e0')),
            ('FONTSIZE', (0, 1), (-1, -1), 8),
            ('TOPPADDING', (0, 1), (-1, -1), 6),
            ('BOTTOMPADDING', (0, 1), (-1, -1), 6),
            # Borde exterior
            ('BOX', (0, 0), (-1, -1), 1.5, colors.HexColor('#2d3748')),
        ])

        # Aplicar color alterno a filas
        for idx in range(1, len(tabla_datos)):
            color = row_colors[idx % 2]
            style.add('BACKGROUND', (0, idx), (-1, idx), colors.HexColor(color))

        tabla.setStyle(style)
        elementos.append(tabla)
        elementos.append(Spacer(1, 20))

        return elementos

    def _crear_detalle_completo(self, datos):
        """Crear detalle completo de cada foto"""
        elementos = []

        elementos.append(PageBreak())
        elementos.append(Paragraph("📸 DETALLE COMPLETO DE FOTOS", self.styles['SectionHeader']))
        elementos.append(Spacer(1, 10))

        for idx, dato in enumerate(datos, 1):
            # Card para cada foto
            card_data = [
                [Paragraph(f"<b>Foto #{dato['id_foto']}</b> - {dato['nombre_archivo']}",
                           self.styles['SectionHeader'])],
                [Paragraph(f"📅 <b>Fecha:</b> {dato['fecha']}", self.styles['MetaData'])],
                [Paragraph(f"🏗️ <b>Obra:</b> {dato['nombre_obra']}", self.styles['MetaData'])],
                [Paragraph(f"📝 <b>Actividad:</b> {dato['actividad']}", self.styles['MetaData'])],
                [Paragraph(f"👤 <b>Usuario:</b> {dato['usuario']}", self.styles['MetaData'])],
                [Paragraph(f"💾 <b>Tamaño:</b> {dato['tamanio_kb']:.1f} KB", self.styles['MetaData'])],
                [Paragraph(f"📍 <b>Coordenadas:</b> {dato.get('coordenadas', 'N/A')}", self.styles['MetaData'])],
            ]

            if dato.get('descripcion'):
                card_data.append([Paragraph(f"📌 <b>Descripción:</b> {dato['descripcion']}",
                                            self.styles['MetaData'])])

            card_table = Table(card_data, colWidths=[5*inch])
            card_table.setStyle(TableStyle([
                ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#2b6cb0')),
                ('TEXTCOLOR', (0, 0), (-1, 0), colors.white),
                ('TOPPADDING', (0, 0), (-1, 0), 8),
                ('BOTTOMPADDING', (0, 0), (-1, 0), 8),
                ('BACKGROUND', (0, 1), (-1, -1), colors.HexColor('#f7fafc')),
                ('GRID', (0, 0), (-1, -1), 0.5, colors.HexColor('#e2e8f0')),
                ('TOPPADDING', (0, 1), (-1, -1), 4),
                ('BOTTOMPADDING', (0, 1), (-1, -1), 4),
                ('LEFTPADDING', (0, 0), (-1, -1), 10),
                ('RIGHTPADDING', (0, 0), (-1, -1), 10),
                ('BOX', (0, 0), (-1, -1), 1, colors.HexColor('#2b6cb0')),
            ]))

            elementos.append(card_table)
            elementos.append(Spacer(1, 10))

            # Línea separadora entre fotos
            if idx < len(datos):
                elementos.append(Spacer(1, 5))
                line = Drawing(450, 1)
                line.add(Line(0, 0, 450, 0, strokeColor=colors.HexColor('#e2e8f0'), strokeWidth=1))
                elementos.append(line)
                elementos.append(Spacer(1, 10))

        return elementos



    def generar_pdf(self, filtros: dict = None, output_path: str = "reporte_fotodatos.pdf"):
        """Generar el PDF"""

        # Obtener datos (cambia a obtener_fotodatos_real cuando la API esté lista)
        datos = self.obtener_datos_prueba()
        datos = self._aplicar_filtros(datos, filtros)

        if not datos:
            print("❌ No hay datos para generar el PDF")
            return False

        # Crear PDF
        try:
            doc = SimpleDocTemplate(
                output_path,
                pagesize=A4,
                leftMargin=1.5*cm,
                rightMargin=1.5*cm,
                topMargin=2*cm,
                bottomMargin=2*cm
            )
            elementos = []


            # 1. Encabezado
            self._crear_header(elementos)

            # 2. Resumen de estadísticas
            elementos.extend(self._crear_resumen_estadisticas(datos))

            # 3. Tabla de datos
            elementos.extend(self._crear_tabla_datos(datos))

            # 4. Detalle completo
            elementos.extend(self._crear_detalle_completo(datos))

            # 5. Pie de página
            elementos.append(Spacer(1, 20))
            footer_text = f"<font color='#718096' size='8'>Reporte generado el {datetime.now().strftime('%d/%m/%Y %H:%M')} | Página 1 de 1</font>"
            elementos.append(Paragraph(footer_text, self.styles['CustomNormal']))

            # Generar PDF
            doc.build(elementos)
            print(f"✅ PDF generado: {output_path}")
            return True

        except Exception as e:
            print(f"❌ Error generando PDF: {e}")
            import traceback
            traceback.print_exc()
            return False

if __name__ == "__main__":
    generator = PDFFotoDatosGenerator()
    generator.generar_pdf()