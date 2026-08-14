# main.py - Versión que combina el estilo de pdf_fotodatos.py con imágenes reales
import importlib
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
import sys

# Configurar logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# ===== IMPORTAR DIRECTAMENTE DESDE LA RUTA CORRECTA =====
current_dir = os.path.dirname(os.path.abspath(__file__))

# Ruta al archivo pdf_fotodatos.py (con src/main/pdf_generator)
file_path = os.path.join(current_dir, 'src', 'main', 'pdf_generator', 'pdf_fotodatos.py')

# Cargar el módulo usando importlib
spec = importlib.util.spec_from_file_location("pdf_fotodatos", file_path)
pdf_fotodatos = importlib.util.module_from_spec(spec)
sys.modules["pdf_fotodatos"] = pdf_fotodatos
spec.loader.exec_module(pdf_fotodatos)

# Obtener la clase
PDFFotoDatosGenerator = pdf_fotodatos.PDFFotoDatosGenerator
logger.info("✅ PDFFotoDatosGenerator importado correctamente")

app = FastAPI(title="FastAPI PDF Generator", version="2.0")

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

# ===== INICIALIZAR GENERADOR =====
pdf_generator = PDFFotoDatosGenerator(springboot_base_url=SPRINGBOOT_URL)

# ===== ENDPOINTS =====
@app.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "service": "pdf-generator-integrated",
        "springboot_url": SPRINGBOOT_URL,
        "version": "2.0"
    }

@app.post("/pdf/fotos-con-info")
async def generar_pdf_con_info(request: FotosRequest):
    """
    Genera un PDF usando el estilo mejorado de pdf_fotodatos.py
    con imágenes reales desde Spring Boot
    """
    logger.info(f"📥 Recibida solicitud con {len(request.fotos)} fotos")
    logger.info(f"🔗 Usando Spring Boot URL: {SPRINGBOOT_URL}")

    try:
        if not request.fotos:
            raise HTTPException(status_code=400, detail="No hay fotos para procesar")

        # ===== USAR EL GENERADOR MEJORADO =====
        logger.info("🔄 Generando PDF con PDFFotoDatosGenerator...")

        # Generar PDF en bytes
        pdf_bytes = pdf_generator.generar_pdf_bytes(request.fotos)

        # ===== DEVOLVER EL PDF =====
        return Response(
            content=pdf_bytes,
            media_type="application/pdf",
            headers={
                "Content-Disposition": f"attachment; filename=reporte_fotos_{datetime.now().strftime('%Y%m%d_%H%M%S')}.pdf"
            }
        )

    except ValueError as e:
        logger.error(f"❌ Error de validación: {str(e)}")
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        logger.error(f"❌ Error general: {str(e)}")
        logger.exception(e)
        raise HTTPException(status_code=500, detail=f"Error generando PDF: {str(e)}")

@app.get("/")
async def root():
    return {
        "message": "FastAPI PDF Generator (Integrado)",
        "springboot_url": SPRINGBOOT_URL,
        "version": "2.0",
        "endpoints": {
            "/health": "Verificar estado del servicio",
            "/pdf/fotos-con-info": "Generar PDF con información de fotos (POST)",
            "/docs": "Documentación Swagger"
        }
    }

@app.get("/test-imagen/{id}")
async def test_imagen(id: int):
    """
    Endpoint de prueba para verificar que se puede obtener una imagen
    Usa el mismo generador para probar la conexión
    """
    try:
        # Usar el método interno del generador
        imagen_data = pdf_generator._obtener_imagen_desde_springboot(id)

        if imagen_data:
            return {
                "success": True,
                "size": len(imagen_data),
                "message": f"Imagen {id} obtenida correctamente"
            }
        else:
            return {
                "success": False,
                "message": f"No se pudo obtener la imagen {id}"
            }
    except Exception as e:
        return {
            "success": False,
            "error": str(e)
        }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)