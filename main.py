# main.py - Versión que combina el estilo de pdf_fotodatos.py con imágenes reales
from fastapi import FastAPI, HTTPException
from fastapi.responses import Response
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime
from reportlab.lib.pagesizes import A4
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Image, Table, TableStyle
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib import colors
from reportlab.lib.units import inch
from io import BytesIO
import requests
import logging
import os




# Configurar logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="FastAPI PDF Generator", version="1.0")

# ===== MODELOS =====
class FotoInfo(BaseModel):
    gridfs_id: str
    nombre_archivo: str
    fecha_foto: Optional[str] = None
    coordenadas_n: Optional[float] = None
    coordenadas_e: Optional[float] = None
    nombre_obra: Optional[str] = None
    nombre_usuario: Optional[str] = None
    descripcion_actividad: Optional[str] = None
    id_avance: Optional[int] = None
    fecha_avance: Optional[str] = None
    cantidad_ejecutada: Optional[float] = None
    id_fotodato: Optional[int] = None
    tamanio_archivo: Optional[int] = None
    tipo_mime: Optional[str] = None

class FotosRequest(BaseModel):
    fotos: List[FotoInfo]

# ===== CONFIGURACIÓN =====
SPRINGBOOT_URL = os.getenv('SPRINGBOOT_URL', 'http://localhost:2798')  # Puerto de tu Spring Boot

# ===== ENDPOINTS =====
@app.get("/health")
async def health_check():
    return {"status": "healthy", "service": "pdf-generator", "springboot_url": SPRINGBOOT_URL}

@app.post("/pdf/fotos-con-info")
async def generar_pdf_con_info(request: FotosRequest):
    """Genera un PDF con el mismo estilo que pdf_fotodatos.py pero con imágenes reales"""
    logger.info(f"📥 Recibida solicitud con {len(request.fotos)} fotos")
    logger.info(f"🔗 Usando Spring Boot URL: {SPRINGBOOT_URL}")

    try:
        if not request.fotos:
            raise HTTPException(status_code=400, detail="No hay fotos para procesar")

        # ===== CREAR PDF EN MEMORIA =====
        buffer = BytesIO()
        doc = SimpleDocTemplate(buffer, pagesize=A4)
        elements = []
        styles = getSampleStyleSheet()

        # ===== TÍTULO (igual que pdf_fotodatos.py) =====
        title = Paragraph(
            f"Reporte de FotoDatos - {datetime.now().strftime('%d/%m/%Y %H:%M')}",
            styles['Heading1']
        )
        elements.append(title)
        elements.append(Spacer(1, 20))

        # ===== TABLA DE DATOS (igual que pdf_fotodatos.py) =====
        tabla_datos = [['ID', 'Obra', 'Actividad', 'Fecha', 'Archivo', 'Tamaño (KB)', 'Usuario']]

        for foto in request.fotos:
            # Calcular tamaño en KB
            tamanio_kb = foto.tamanio_archivo / 1024 if foto.tamanio_archivo else 0

            tabla_datos.append([
                str(foto.id_fotodato or ''),
                foto.nombre_obra or '',
                foto.descripcion_actividad or '',
                foto.fecha_foto or '',
                foto.nombre_archivo or '',
                f"{tamanio_kb:.1f}",
                foto.nombre_usuario or ''
            ])

        # Crear y estilizar tabla (mismo estilo que pdf_fotodatos.py)
        tabla = Table(tabla_datos, colWidths=[0.5*inch, 1.5*inch, 1.5*inch, 1.0*inch,
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

        # ===== RESUMEN (igual que pdf_fotodatos.py) =====
        resumen = Paragraph(f"Total de registros: {len(request.fotos)}", styles['Normal'])
        elements.append(resumen)
        elements.append(Spacer(1, 20))

        # ===== SECCIÓN DE FOTOS (NUEVO: imágenes reales) =====
        elements.append(Paragraph("=" * 80, styles['Normal']))
        elements.append(Spacer(1, 10))

        titulo_fotos = Paragraph(
            "<b>DETALLE DE FOTOS</b>",
            ParagraphStyle('FotoTitle', parent=styles['Normal'], fontSize=12, alignment=1)
        )
        elements.append(titulo_fotos)
        elements.append(Spacer(1, 10))

        # Procesar cada foto con su imagen
        for idx, foto in enumerate(request.fotos, 1):
            try:
                # Título de la foto
                foto_title = f"Foto #{idx} - {foto.nombre_archivo}"
                elements.append(Paragraph(foto_title, styles['Heading2']))

                # Información de la foto
                info_lines = []
                if foto.fecha_foto:
                    info_lines.append(f"📅 Fecha: {foto.fecha_foto}")
                if foto.nombre_obra:
                    info_lines.append(f"🏗️ Obra: {foto.nombre_obra}")
                if foto.descripcion_actividad:
                    info_lines.append(f"📋 Actividad: {foto.descripcion_actividad}")
                if foto.nombre_usuario:
                    info_lines.append(f"👤 Usuario: {foto.nombre_usuario}")
                if foto.coordenadas_n and foto.coordenadas_e:
                    info_lines.append(f"📍 Coordenadas: {foto.coordenadas_n:.6f}, {foto.coordenadas_e:.6f}")
                if foto.tamanio_archivo:
                    info_lines.append(f"📊 Tamaño: {foto.tamanio_archivo / 1024:.1f} KB")

                for line in info_lines:
                    elements.append(Paragraph(line, styles['Normal']))

                # ===== OBTENER Y AGREGAR LA IMAGEN =====
                if foto.id_fotodato:
                    try:
                        image_url = f"{SPRINGBOOT_URL}/fotodatos/imagen/{foto.id_fotodato}"
                        logger.info(f"🖼️ Obteniendo imagen: {image_url}")

                        response = requests.get(image_url, timeout=30)

                        if response.status_code == 200:
                            # Usar BytesIO para la imagen
                            image_buffer = BytesIO(response.content)
                            img = Image(image_buffer, width=4*inch, height=3*inch)
                            elements.append(img)
                            elements.append(Spacer(1, 10))
                            logger.info(f"✅ Imagen agregada")
                        else:
                            elements.append(Paragraph(f"❌ Imagen no disponible (HTTP {response.status_code})", styles['Normal']))

                    except Exception as e:
                        logger.error(f"Error obteniendo imagen: {e}")
                        elements.append(Paragraph(f"❌ Error obteniendo imagen: {str(e)}", styles['Normal']))

                # Línea separadora
                elements.append(Spacer(1, 10))
                elements.append(Paragraph("─" * 80, styles['Normal']))
                elements.append(Spacer(1, 10))

            except Exception as e:
                logger.error(f"Error procesando foto {idx}: {e}")
                elements.append(Paragraph(f"❌ Error procesando foto {idx}: {str(e)}", styles['Normal']))
                elements.append(Spacer(1, 10))

        # ===== GENERAR EL PDF =====
        logger.info("📄 Generando PDF...")
        doc.build(elements)
        pdf_bytes = buffer.getvalue()
        buffer.close()
        logger.info(f"✅ PDF generado exitosamente ({len(pdf_bytes)} bytes)")

        # ===== DEVOLVER EL PDF =====
        return Response(
            content=pdf_bytes,
            media_type="application/pdf",
            headers={
                "Content-Disposition": f"attachment; filename=reporte_fotos_{datetime.now().strftime('%Y%m%d_%H%M%S')}.pdf"
            }
        )

    except Exception as e:
        logger.error(f"❌ Error general: {str(e)}")
        logger.exception(e)
        raise HTTPException(status_code=500, detail=f"Error generando PDF: {str(e)}")

@app.get("/")
async def root():
    return {
        "message": "FastAPI PDF Generator",
        "springboot_url": SPRINGBOOT_URL,
        "endpoints": {
            "/health": "Verificar estado del servicio",
            "/pdf/fotos-con-info": "Generar PDF con información de fotos (POST)",
            "/docs": "Documentación Swagger"
        }
    }

@app.get("/test-imagen/{id}")
async def test_imagen(id: int):
    """Endpoint de prueba para verificar que se puede obtener una imagen"""
    try:
        image_url = f"{SPRINGBOOT_URL}/fotodatos/imagen/{id}"
        response = requests.get(image_url, timeout=10)

        if response.status_code == 200:
            return {
                "success": True,
                "size": len(response.content),
                "status": response.status_code
            }
        else:
            return {
                "success": False,
                "status": response.status_code
            }
    except Exception as e:
        return {
            "success": False,
            "error": str(e)
        }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)