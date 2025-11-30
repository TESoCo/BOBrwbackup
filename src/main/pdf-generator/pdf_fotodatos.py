# pdf-generator/pdf_fotodatos.py
import pandas as pd
from reportlab.lib.pagesizes import A4
from reportlab.platypus import SimpleDocTemplate, Table, TableStyle, Paragraph, Spacer
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib import colors
from reportlab.lib.units import inch
import requests
from datetime import datetime
import json

class PDFFotoDatosGenerator:
    def __init__(self, springboot_base_url: str = "http://localhost:8080"):
        self.base_url = springboot_base_url

    def obtener_datos_prueba(self):
        """Datos de prueba mientras configuras la API real"""
        return [
            {
                'id_foto': 1,
                'nombre_obra': 'Obra Norte',
                'actividad': 'Cimentación',
                'fecha': '2024-01-15',
                'coordenadas': '4.6097, -74.0817',
                'nombre_archivo': 'foto1.jpg',
                'tamanio_kb': 2450,
                'usuario': 'juan.perez'
            },
            {
                'id_foto': 2,
                'nombre_obra': 'Obra Sur',
                'actividad': 'Estructura',
                'fecha': '2024-01-16',
                'coordenadas': '4.7107, -74.0721',
                'nombre_archivo': 'foto2.jpg',
                'tamanio_kb': 3120,
                'usuario': 'maria.gomez'
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
            doc = SimpleDocTemplate(output_path, pagesize=A4)
            elements = []
            styles = getSampleStyleSheet()

            # Título
            title = Paragraph(f"Reporte de FotoDatos - {datetime.now().strftime('%d/%m/%Y %H:%M')}",
                              styles['Heading1'])
            elements.append(title)
            elements.append(Spacer(1, 20))

            # Información de filtros
            if filtros:
                filtros_text = "Filtros: "
                if filtros.get('obra'):
                    filtros_text += f"Obra: {filtros['obra']} "
                if filtros.get('ordenar_por'):
                    filtros_text += f"Orden: {filtros['ordenar_por']} ({filtros.get('direccion_orden', 'asc')})"

                elements.append(Paragraph(filtros_text, styles['Normal']))
                elements.append(Spacer(1, 10))

            # Tabla de datos
            tabla_datos = [['ID', 'Obra', 'Actividad', 'Fecha', 'Archivo', 'Tamaño (KB)', 'Usuario']]

            for dato in datos:
                tabla_datos.append([
                    str(dato['id_foto']),
                    dato['nombre_obra'],
                    dato['actividad'],
                    dato['fecha'],
                    dato['nombre_archivo'],
                    str(dato['tamanio_kb']),
                    dato['usuario']
                ])

            # Crear y estilizar tabla
            tabla = Table(tabla_datos, colWidths=[0.5*inch, 1.5*inch, 1.2*inch, 1.0*inch,
                                                  1.2*inch, 0.8*inch, 1.0*inch])

            tabla.setStyle(TableStyle([
                ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#2E86AB')),
                ('TEXTCOLOR', (0, 0), (-1, 0), colors.whitesmoke),
                ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
                ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
                ('FONTSIZE', (0, 0), (-1, 0), 9),
                ('BOTTOMPADDING', (0, 0), (-1, 0), 12),
                ('BACKGROUND', (0, 1), (-1, -1), colors.HexColor('#F8F9FA')),
                ('GRID', (0, 0), (-1, -1), 1, colors.grey),
                ('FONTSIZE', (0, 1), (-1, -1), 8),
            ]))

            elements.append(tabla)
            elements.append(Spacer(1, 20))

            # Resumen
            resumen = Paragraph(f"Total de registros: {len(datos)}", styles['Normal'])
            elements.append(resumen)

            # Generar PDF
            doc.build(elements)
            print(f"✅ PDF generado: {output_path}")
            return True

        except Exception as e:
            print(f"❌ Error generando PDF: {e}")
            return False

if __name__ == "__main__":
    generator = PDFFotoDatosGenerator()
    generator.generar_pdf()