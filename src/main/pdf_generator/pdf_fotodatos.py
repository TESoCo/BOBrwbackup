# pdf-generator/pdf_fotodatos.py
import pandas as pd
from reportlab.lib.pagesizes import A4, landscape
from reportlab.platypus import SimpleDocTemplate, Table, TableStyle, Paragraph, Spacer, PageBreak, Image, KeepTogether
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib import colors
from reportlab.lib.units import inch, cm
from reportlab.lib.enums import TA_CENTER, TA_LEFT, TA_RIGHT
from reportlab.graphics.shapes import Drawing, Rect, Line
from reportlab.graphics.charts.barcharts import VerticalBarChart
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
import requests
from datetime import datetime
import json
import os
from io import BytesIO
import logging

logger = logging.getLogger(__name__)

class PDFFotoDatosGenerator:
    def __init__(self, springboot_base_url: str = "http://localhost:8080"):
        self.base_url = springboot_base_url
        self._setup_styles()
        # Cache para imágenes
        self.image_cache = {}

    def _setup_styles(self):
        """Configurar estilos personalizados"""
        self.styles = getSampleStyleSheet()

        # Título principal
        self.styles.add(ParagraphStyle(
            name='MainTitle',
            parent=self.styles['Heading1'],
            fontSize=16,
            textColor=colors.HexColor('#1a365d'),
            alignment=TA_CENTER,
            spaceAfter=20,
            fontName='Helvetica-Bold'
        ))

        # Subtítulo
        self.styles.add(ParagraphStyle(
            name='SubTitle',
            parent=self.styles['Heading2'],
            fontSize=13,
            textColor=colors.HexColor('#2d3748'),
            spaceAfter=15,
            fontName='Helvetica-Bold'
        ))

        # Encabezado de sección
        self.styles.add(ParagraphStyle(
            name='SectionHeader',
            parent=self.styles['Heading3'],
            fontSize=11,
            textColor=colors.HexColor('#2b6cb0'),
            spaceAfter=8,
            fontName='Helvetica-Bold'
        ))

        # Texto normal mejorado
        self.styles.add(ParagraphStyle(
            name='CustomNormal',
            parent=self.styles['Normal'],
            fontSize=9,
            textColor=colors.HexColor('#2d3748'),
            alignment=TA_LEFT,
            leading=12
        ))

        # Texto de metadatos
        self.styles.add(ParagraphStyle(
            name='MetaData',
            parent=self.styles['Normal'],
            fontSize=8,
            textColor=colors.HexColor('#4a5568'),
            alignment=TA_LEFT,
            spaceAfter=2,
            leading=10
        ))

        # Texto para celdas de tabla
        self.styles.add(ParagraphStyle(
            name='TableCell',
            parent=self.styles['Normal'],
            fontSize=8,
            textColor=colors.HexColor('#2d3748'),
            alignment=TA_LEFT,
            leading=10
        ))

        # Texto para encabezados de tabla
        self.styles.add(ParagraphStyle(
            name='TableHeader',
            parent=self.styles['Normal'],
            fontSize=8,
            textColor=colors.white,
            alignment=TA_CENTER,
            fontName='Helvetica-Bold',
            leading=10
        ))

    def convertir_datos_desde_api(self, fotos_data):
        """
        Convierte los datos del formato de main.py al formato interno
        """
        datos_convertidos = []

        for foto in fotos_data:
            # Determinar si es un objeto FotoInfo o un dict
            if hasattr(foto, 'dict'):
                foto_dict = foto.dict()
            else:
                foto_dict = foto

            # Mapear campos
            dato = {
                'id_foto': foto_dict.get('id_fotodato') or foto_dict.get('id_foto'),
                'nombre_obra': foto_dict.get('nombre_obra', 'Sin obra'),
                'actividad': foto_dict.get('descripcion_actividad', 'Sin actividad'),
                'fecha': foto_dict.get('fecha_foto', ''),
                'coordenadas': self._formatear_coordenadas(
                    foto_dict.get('coordenadas_n'),
                    foto_dict.get('coordenadas_e')
                ),
                'nombre_archivo': foto_dict.get('nombre_archivo', ''),
                'tamanio_kb': (foto_dict.get('tamanio_archivo', 0) / 1024) if foto_dict.get('tamanio_archivo') else 0,
                'usuario': foto_dict.get('nombre_usuario', ''),
                'descripcion': foto_dict.get('descripcion_actividad', ''),
                # Campos adicionales para imágenes
                'gridfs_id': foto_dict.get('gridfs_id'),
                'id_fotodato': foto_dict.get('id_fotodato'),
                'coordenadas_n': foto_dict.get('coordenadas_n'),
                'coordenadas_e': foto_dict.get('coordenadas_e'),
                'tamanio_archivo': foto_dict.get('tamanio_archivo'),
                'tipo_mime': foto_dict.get('tipo_mime')
            }
            datos_convertidos.append(dato)

        return datos_convertidos

    def _formatear_coordenadas(self, lat, lon):
        """Formatear coordenadas para mostrar"""
        if lat is not None and lon is not None:
            return f"{lat:.6f}, {lon:.6f}"
        return 'N/A'

    def _obtener_imagen_desde_springboot(self, id_fotodato):
        """Obtener imagen desde Spring Boot con caché"""
        if not id_fotodato:
            return None

        # Verificar caché
        if id_fotodato in self.image_cache:
            return self.image_cache[id_fotodato]

        try:
            url = f"{self.base_url}/fotodatos/imagen/{id_fotodato}"
            logger.info(f"🖼️ Obteniendo imagen: {url}")

            response = requests.get(url, timeout=30)

            if response.status_code == 200:
                # Guardar en caché
                self.image_cache[id_fotodato] = response.content
                logger.info(f"✅ Imagen obtenida para ID {id_fotodato}")
                return response.content
            else:
                logger.warning(f"⚠️ Imagen no disponible (HTTP {response.status_code})")
                return None

        except Exception as e:
            logger.error(f"❌ Error obteniendo imagen: {e}")
            return None

    def obtener_datos_prueba(self):
        """Datos de prueba mientras configuras la API real"""
        return [
            {
                'id_foto': 1,
                'nombre_obra': 'Obra prueba 1 (EJECUCIÓN)',
                'actividad': 'RETIRO Y BOTADA DE MATERIAL EXCAVADA',
                'fecha': '2026-08-12',
                'coordenadas': '4.748028, -74.043607',
                'nombre_archivo': 'foto_capturada.jpg',
                'tamanio_kb': 23.6,
                'usuario': 'jose.taylor.m',
                'descripcion': 'Excavación finalizada en sector norte'
            }
        ]

    def obtener_fotodatos_real(self, filtros: dict = None):
        """Conectar con tu API Spring Boot real"""
        try:
            url = f"{self.base_url}/api/fotodatos"
            headers = {
                'Content-Type': 'application/json',
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

        if filtros.get('obra'):
            datos_filtrados = [d for d in datos_filtrados
                               if filtros['obra'].lower() in d['nombre_obra'].lower()]

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
        fecha_actual = datetime.now().strftime('%d de %B de %Y a las %H:%M')

        elementos.append(Spacer(1, 10))

        # Línea decorativa superior
        line = Drawing(450, 2)
        line.add(Line(0, 1, 450, 1, strokeColor=colors.HexColor('#2b6cb0'), strokeWidth=3))
        elementos.append(line)

        # Título
        title_text = f"<font color='#1a365d' size='16'><b>📸 REPORTE DE FOTODATOS</b></font>"
        title = Paragraph(title_text, self.styles['MainTitle'])
        elementos.append(title)

        # Subtítulo con fecha
        subtitle = Paragraph(f"<font color='#4a5568' size='10'>Generado: {fecha_actual}</font>",
                             self.styles['CustomNormal'])
        elementos.append(subtitle)
        elementos.append(Spacer(1, 5))

        # Línea decorativa inferior
        line2 = Drawing(450, 2)
        line2.add(Line(0, 1, 450, 1, strokeColor=colors.HexColor('#48bb78'), strokeWidth=2))
        elementos.append(line2)
        elementos.append(Spacer(1, 15))

    def _crear_resumen_estadisticas(self, datos):
        """Crear resumen de estadísticas"""
        elementos = []

        total_fotos = len(datos)
        total_tamanio = sum(d.get('tamanio_kb', 0) for d in datos)
        tamanio_mb = total_tamanio / 1024
        obras = list(set(d.get('nombre_obra', '') for d in datos))
        actividades = list(set(d.get('actividad', '') for d in datos))

        stats_data = [
            ['Total Fotos', 'Obras', 'Tamaño Total', 'Actividades'],
            [str(total_fotos), str(len(obras)), f"{tamanio_mb:.2f} MB", str(len(actividades))]
        ]

        stats_table = Table(stats_data, colWidths=[2.2*inch, 2.2*inch, 2.2*inch, 2.2*inch])
        stats_table.setStyle(TableStyle([
            ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#2b6cb0')),
            ('TEXTCOLOR', (0, 0), (-1, 0), colors.white),
            ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
            ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
            ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
            ('FONTSIZE', (0, 0), (-1, 0), 10),
            ('FONTSIZE', (0, 1), (-1, 1), 14),
            ('FONTNAME', (0, 1), (-1, 1), 'Helvetica-Bold'),
            ('BOTTOMPADDING', (0, 0), (-1, 0), 6),
            ('TOPPADDING', (0, 0), (-1, 0), 6),
            ('BOTTOMPADDING', (0, 1), (-1, 1), 6),
            ('TOPPADDING', (0, 1), (-1, 1), 6),
            ('GRID', (0, 0), (-1, -1), 0.5, colors.HexColor('#cbd5e0')),
            ('BACKGROUND', (0, 1), (-1, -1), colors.HexColor('#f7fafc')),
            ('ROUNDEDCORNERS', [2, 2, 2, 2]),
        ]))

        elementos.append(Paragraph("📊 Resumen de Estadísticas", self.styles['SectionHeader']))
        elementos.append(Spacer(1, 5))
        elementos.append(stats_table)
        elementos.append(Spacer(1, 15))

        return elementos

    def _crear_tabla_datos(self, datos):
        """Crear tabla principal de datos"""
        elementos = []

        elementos.append(Paragraph("📋 Detalle de Fotos", self.styles['SectionHeader']))
        elementos.append(Spacer(1, 8))

        tabla_datos = []
        tabla_datos.append([
            Paragraph('<b>ID</b>', self.styles['TableHeader']),
            Paragraph('<b>Obra</b>', self.styles['TableHeader']),
            Paragraph('<b>Actividad</b>', self.styles['TableHeader']),
            Paragraph('<b>Fecha</b>', self.styles['TableHeader']),
            Paragraph('<b>Tamaño</b>', self.styles['TableHeader']),
            Paragraph('<b>Usuario</b>', self.styles['TableHeader'])
        ])

        row_colors = ['#f7fafc', '#edf2f7']

        for idx, dato in enumerate(datos):
            color_idx = idx % 2

            actividad = dato['actividad']
            if len(actividad) > 30:
                actividad = actividad[:27] + '...'

            obra = dato['nombre_obra']
            if len(obra) > 25:
                obra = obra[:22] + '...'

            tabla_datos.append([
                Paragraph(f"<b>{dato['id_foto']}</b>", self.styles['TableCell']),
                Paragraph(obra, self.styles['TableCell']),
                Paragraph(actividad, self.styles['TableCell']),
                Paragraph(dato['fecha'], self.styles['TableCell']),
                Paragraph(f"{dato['tamanio_kb']:.1f} KB", self.styles['TableCell']),
                Paragraph(dato['usuario'], self.styles['TableCell'])
            ])

        tabla = Table(tabla_datos, colWidths=[0.6*inch, 1.6*inch, 1.8*inch, 0.9*inch, 0.8*inch, 1.0*inch])

        style = TableStyle([
            ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#2d3748')),
            ('TEXTCOLOR', (0, 0), (-1, 0), colors.white),
            ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
            ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
            ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
            ('FONTSIZE', (0, 0), (-1, 0), 9),
            ('BOTTOMPADDING', (0, 0), (-1, 0), 6),
            ('TOPPADDING', (0, 0), (-1, 0), 6),
            ('GRID', (0, 0), (-1, -1), 0.5, colors.HexColor('#cbd5e0')),
            ('FONTSIZE', (0, 1), (-1, -1), 8),
            ('TOPPADDING', (0, 1), (-1, -1), 4),
            ('BOTTOMPADDING', (0, 1), (-1, -1), 4),
            ('BOX', (0, 0), (-1, -1), 1, colors.HexColor('#2d3748')),
        ])

        for idx in range(1, len(tabla_datos)):
            color = row_colors[idx % 2]
            style.add('BACKGROUND', (0, idx), (-1, idx), colors.HexColor(color))

        tabla.setStyle(style)
        elementos.append(tabla)
        elementos.append(Spacer(1, 15))

        total_text = f"<font color='#4a5568' size='9'><b>Total de registros:</b> {len(datos)}</font>"
        elementos.append(Paragraph(total_text, self.styles['CustomNormal']))
        elementos.append(Spacer(1, 10))

        return elementos

    def _crear_detalle_completo_con_imagenes(self, datos):
        """Crear detalle completo de cada foto con imágenes reales"""
        elementos = []

        elementos.append(PageBreak())
        elementos.append(Paragraph("📸 Detalle Completo de Fotos", self.styles['SectionHeader']))
        elementos.append(Spacer(1, 10))

        for idx, dato in enumerate(datos, 1):
            # Card para cada foto
            card_content = []

            # Título de la foto
            card_content.append([
                Paragraph(f"<font color='white' size='11'><b>Foto #{dato['id_foto']}</b></font>",
                          self.styles['CustomNormal'])
            ])

            # Detalles en formato de lista
            detalles = [
                f"📅 <b>Fecha:</b> {dato['fecha']}",
                f"🏗️ <b>Obra:</b> {dato['nombre_obra']}",
                f"📝 <b>Actividad:</b> {dato['actividad']}",
                f"👤 <b>Usuario:</b> {dato['usuario']}",
                f"💾 <b>Tamaño:</b> {dato['tamanio_kb']:.1f} KB",
                f"📍 <b>Coordenadas:</b> {dato.get('coordenadas', 'N/A')}"
            ]

            if dato.get('descripcion'):
                detalles.append(f"📌 <b>Descripción:</b> {dato['descripcion']}")

            for detalle in detalles:
                card_content.append([
                    Paragraph(detalle, self.styles['MetaData'])
                ])

            # INTENTAR AGREGAR IMAGEN REAL
            if dato.get('id_fotodato'):
                imagen_data = self._obtener_imagen_desde_springboot(dato['id_fotodato'])
                if imagen_data:
                    try:
                        # Crear imagen desde bytes
                        image_buffer = BytesIO(imagen_data)
                        img = Image(image_buffer, width=4*inch, height=3*inch)
                        card_content.append([img])
                        logger.info(f"✅ Imagen agregada para ID {dato['id_fotodato']}")
                    except Exception as e:
                        logger.error(f"❌ Error creando imagen: {e}")
                        card_content.append([
                            Paragraph(f"<font color='red'>⚠️ Error mostrando imagen</font>",
                                      self.styles['MetaData'])
                        ])
                else:
                    card_content.append([
                        Paragraph(f"<font color='gray'>📷 Imagen no disponible</font>",
                                  self.styles['MetaData'])
                    ])

            card_table = Table(card_content, colWidths=[5.5*inch])
            card_table.setStyle(TableStyle([
                ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#2b6cb0')),
                ('TEXTCOLOR', (0, 0), (-1, 0), colors.white),
                ('TOPPADDING', (0, 0), (-1, 0), 6),
                ('BOTTOMPADDING', (0, 0), (-1, 0), 6),
                ('BACKGROUND', (0, 1), (-1, -1), colors.HexColor('#f7fafc')),
                ('GRID', (0, 0), (-1, -1), 0.5, colors.HexColor('#e2e8f0')),
                ('TOPPADDING', (0, 1), (-1, -1), 3),
                ('BOTTOMPADDING', (0, 1), (-1, -1), 3),
                ('LEFTPADDING', (0, 0), (-1, -1), 10),
                ('RIGHTPADDING', (0, 0), (-1, -1), 10),
                ('BOX', (0, 0), (-1, -1), 1, colors.HexColor('#2b6cb0')),
                ('ROUNDEDCORNERS', [5, 5, 5, 5]),
            ]))

            elementos.append(card_table)
            elementos.append(Spacer(1, 8))

            if idx < len(datos):
                line = Drawing(450, 1)
                line.add(Line(0, 0, 450, 0, strokeColor=colors.HexColor('#e2e8f0'), strokeWidth=1))
                elementos.append(line)
                elementos.append(Spacer(1, 8))

        return elementos

    def generar_pdf(self, filtros: dict = None, output_path: str = "reporte_fotodatos.pdf"):
        """Generar el PDF (versión legacy)"""
        datos = self.obtener_datos_prueba()
        datos = self._aplicar_filtros(datos, filtros)

        if not datos:
            print("❌ No hay datos para generar el PDF")
            return False

        try:
            doc = SimpleDocTemplate(
                output_path,
                pagesize=A4,
                leftMargin=1.2*cm,
                rightMargin=1.2*cm,
                topMargin=1.5*cm,
                bottomMargin=1.5*cm
            )
            elementos = []

            self._crear_header(elementos)
            elementos.extend(self._crear_resumen_estadisticas(datos))
            elementos.extend(self._crear_tabla_datos(datos))
            elementos.extend(self._crear_detalle_completo_con_imagenes(datos))

            elementos.append(Spacer(1, 20))
            footer_text = f"<font color='#718096' size='8'>Reporte generado el {datetime.now().strftime('%d/%m/%Y %H:%M')} | Página 1 de 1</font>"
            elementos.append(Paragraph(footer_text, self.styles['CustomNormal']))

            doc.build(elementos)
            print(f"✅ PDF generado exitosamente: {output_path}")
            return True

        except Exception as e:
            print(f"❌ Error generando PDF: {e}")
            import traceback
            traceback.print_exc()
            return False

    def generar_pdf_desde_datos(self, fotos_data, output_path: str = "reporte_fotodatos.pdf"):
        """
        NUEVO MÉTODO: Generar PDF desde datos ya obtenidos (usado por main.py)
        """
        # Convertir datos al formato interno
        datos = self.convertir_datos_desde_api(fotos_data)

        if not datos:
            logger.error("❌ No hay datos para generar el PDF")
            return False

        try:
            doc = SimpleDocTemplate(
                output_path,
                pagesize=A4,
                leftMargin=1.2*cm,
                rightMargin=1.2*cm,
                topMargin=1.5*cm,
                bottomMargin=1.5*cm
            )
            elementos = []

            self._crear_header(elementos)
            elementos.extend(self._crear_resumen_estadisticas(datos))
            elementos.extend(self._crear_tabla_datos(datos))
            elementos.extend(self._crear_detalle_completo_con_imagenes(datos))

            elementos.append(Spacer(1, 20))
            footer_text = f"<font color='#718096' size='8'>Reporte generado el {datetime.now().strftime('%d/%m/%Y %H:%M')} | Página 1 de 1</font>"
            elementos.append(Paragraph(footer_text, self.styles['CustomNormal']))

            doc.build(elementos)
            logger.info(f"✅ PDF generado exitosamente: {output_path}")
            return True

        except Exception as e:
            logger.error(f"❌ Error generando PDF: {e}")
            import traceback
            traceback.print_exc()
            return False

    def generar_pdf_bytes(self, fotos_data):
        """
        NUEVO MÉTOD0: Generar PDF y devolver bytes (para respuesta HTTP)
        """
        buffer = BytesIO()

        # Convertir datos al formato interno
        datos = self.convertir_datos_desde_api(fotos_data)

        if not datos:
            raise ValueError("No hay datos para generar el PDF")

        try:
            doc = SimpleDocTemplate(
                buffer,
                pagesize=A4,
                leftMargin=1.2*cm,
                rightMargin=1.2*cm,
                topMargin=1.5*cm,
                bottomMargin=1.5*cm
            )
            elementos = []

            self._crear_header(elementos)
            elementos.extend(self._crear_resumen_estadisticas(datos))
            elementos.extend(self._crear_tabla_datos(datos))
            elementos.extend(self._crear_detalle_completo_con_imagenes(datos))

            elementos.append(Spacer(1, 20))
            footer_text = f"<font color='#718096' size='8'>Reporte generado el {datetime.now().strftime('%d/%m/%Y %H:%M')} | Página 1 de 1</font>"
            elementos.append(Paragraph(footer_text, self.styles['CustomNormal']))

            doc.build(elementos)

            pdf_bytes = buffer.getvalue()
            buffer.close()
            logger.info(f"✅ PDF generado exitosamente ({len(pdf_bytes)} bytes)")
            return pdf_bytes

        except Exception as e:
            logger.error(f"❌ Error generando PDF: {e}")
            import traceback
            traceback.print_exc()
            raise

if __name__ == "__main__":
    generator = PDFFotoDatosGenerator()
    generator.generar_pdf("reporte_mejorado.pdf")