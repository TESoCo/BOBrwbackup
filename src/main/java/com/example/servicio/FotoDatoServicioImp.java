package com.example.servicio;

import com.example.dao.APUDao;
import com.example.dao.AvanceDao;
import com.example.dao.FotoDatoDao;
import com.example.domain.Apu;
import com.example.domain.Avance;
import com.example.domain.FotoDato;
import com.example.domain.Usuario;
import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class FotoDatoServicioImp implements FotoDatoServicio{

    @Autowired
    private AvanceDao avanceDao;

    @Autowired
    private APUDao mateDao;

    @Autowired
    private FotoDatoDao fotoDatoDao;

    @Autowired
    private GridFSService gridFSService;

    @Autowired
    private GridFsOperations gridFsOperations;

    @Override
    @Transactional(readOnly = true)
    public List<FotoDato> listaFotoDatoAv (Avance avance) {
        return (List<FotoDato>) fotoDatoDao.findByIdAvance(avance);
    }




    @Override
    @Transactional
    public void salvar(FotoDato fotoDato, MultipartFile archivo) throws IOException {
        try {
            // Validaciones básicas
            if (archivo == null || archivo.isEmpty()) {
                throw new IllegalArgumentException("El archivo no puede ser nulo o vacío");
            }

            if (fotoDato.getIdAvance() == null) {
                throw new IllegalArgumentException("El avance asociado no puede ser nulo");
            }

            System.out.println("💾 Guardando foto para avance ID: " + fotoDato.getIdAvance().getIdAvance());
            System.out.println("📊 Tamaño del archivo: " + archivo.getSize() + " bytes");
            System.out.println("📝 Tipo MIME: " + archivo.getContentType());

            // Guardar archivo en MongoDB GridFS
            String gridfsFileId = gridFSService.storeFile(archivo, fotoDato.getIdAvance().getIdAvance());

            System.out.println("🔗 GridFS File ID generado: " + gridfsFileId);

            // Actualizar entidad FotoDato con referencia a GridFS
            fotoDato.setGridfsFileId(gridfsFileId);
            fotoDato.setNombreArchivo(archivo.getOriginalFilename());
            fotoDato.setTamanioArchivo(archivo.getSize());
            fotoDato.setTipoMime(archivo.getContentType());

            // Guardar en MySQL con el resto de datos del sistema
            fotoDatoDao.save(fotoDato);

            System.out.println("FotoDato guardado en base de datos con ID: " + fotoDato.getIdFotoDato());

        } catch (Exception e) {
            System.err.println("Error en FotoDatoServicioImp.salvar: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // NUEVO MÉTOD0 COMPLETO AGREGADO
    @Override
    @Transactional
    public void salvarConBytes(FotoDato fotoDato, byte[] imageBytes, String filename, String contentType) throws IOException {
        try {
            // Validaciones básicas
            if (imageBytes == null || imageBytes.length == 0) {
                throw new IllegalArgumentException("Los bytes de la imagen no pueden ser nulos o vacíos");
            }

            if (fotoDato.getIdAvance() == null) {
                throw new IllegalArgumentException("El avance asociado no puede ser nulo");
            }

            System.out.println("Guardando foto con bytes para avance ID: " + fotoDato.getIdAvance().getIdAvance());
            System.out.println("Tamaño de bytes: " + imageBytes.length + " bytes");

            // Crear un InputStream temporal para GridFS
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);

            // Metadatos para el archivo
            var metadata = new BasicDBObject();
            metadata.put("avanceId", fotoDato.getIdAvance().getIdAvance());
            metadata.put("originalFileName", filename);
            metadata.put("uploadTime", System.currentTimeMillis());
            metadata.put("contentType", contentType);

            // Guardar en GridFS directamente usando gridFsOperations
            ObjectId fileId = gridFsOperations.store(
                    inputStream,
                    filename,
                    contentType,
                    metadata
            );

            String gridfsFileId = fileId.toString();

            System.out.println("GridFS File ID generado: " + gridfsFileId);

            // Actualizar entidad FotoDato
            fotoDato.setGridfsFileId(gridfsFileId);
            fotoDato.setNombreArchivo(filename);
            fotoDato.setTamanioArchivo((long) imageBytes.length);
            fotoDato.setTipoMime(contentType);

            // Guardar en MySQL
            fotoDatoDao.save(fotoDato);

            System.out.println("FotoDato guardado en base de datos con ID: " + fotoDato.getIdFotoDato());

            // Cerrar el stream
            inputStream.close();

        } catch (Exception e) {
            System.err.println("Error en FotoDatoServicioImp.salvarConBytes: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }



    @Override
    @Transactional
    public void borrar(FotoDato fotoDato) {

        // Eliminar archivo de MongoDB GridFS si existe
        if (fotoDato.getGridfsFileId() != null) {
            try {
                gridFSService.deleteFile(fotoDato.getGridfsFileId());
            } catch (Exception e) {
                // Log del error pero continuar con la eliminación de la entidad
                System.err.println("Error al eliminar archivo de GridFS: " + e.getMessage());
            }
        }

        // Eliminar entidad de MySQL
        fotoDatoDao.delete(fotoDato);
    }


    @Override
    @Transactional(readOnly = true)
    public FotoDato localizarFotoDato(Long entryId) {
        return fotoDatoDao.findById(entryId).orElse(null);
    }


    /**
     * Obtiene los datos del archivo desde MongoDB GridFS
     */
    public byte[] obtenerArchivoFoto(String gridfsFileId) throws IOException {
        return gridFSService.getFile(gridfsFileId);
    }



    // Implementation of new search methods
    @Override
    @Transactional(readOnly = true)
    public List<FotoDato> buscarPorIdAvance(Long idAvance) {
        List<Avance> avances =avanceDao.findByIdAvance(idAvance);
        List<FotoDato> result = new ArrayList<>();
        if(avances!= null && !avances.isEmpty())
        {
            for(Avance avance: avances)
            {
                result.addAll(fotoDatoDao.findByIdAvance(avance));
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FotoDato> buscarPorIdUsuario(Long idUsuario) {
        List<Avance> avances = avanceDao.findByIdUsuario_IdUsuario(idUsuario);
        List<FotoDato> result = new ArrayList<>();
        if(avances!= null && !avances.isEmpty())
        {
            for(Avance avance: avances)
            {
                result.addAll(fotoDatoDao.findByIdAvance(avance));
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FotoDato> buscarPorIdObra(Long idObra) {
        List<Avance> avances = avanceDao.findByIdObra_IdObra(idObra);
        List<FotoDato> result = new ArrayList<>();
        if(avances!= null && !avances.isEmpty())
        {
            for(Avance avance: avances)
            {
                result.addAll(fotoDatoDao.findByIdAvance(avance));
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FotoDato> buscarPorIdFotoDato(Long idFotoDato){
        return fotoDatoDao.findByIdFotoDato(idFotoDato);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FotoDato> buscarPorIdApu(Long idApu) {
        List<Avance> avances = avanceDao.findByIdApu_IdAPU(idApu);
        List<FotoDato> result = new ArrayList<>();
        if(avances!= null && !avances.isEmpty())
        {
            for(Avance avance: avances)
            {
                result.addAll(fotoDatoDao.findByIdAvance(avance));
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FotoDato> buscarPorFecha(LocalDate fecha) {
        return fotoDatoDao.findByFechaFoto(fecha);
    }



    @Override
    @Transactional(readOnly = true)
    public List<FotoDato> buscarPorUsuarioYFecha(Usuario idUsuario, LocalDate fecha) {
        return fotoDatoDao.findByIdAvance_IdUsuarioAndFechaFoto(idUsuario,fecha);
    }


    public List<Apu> listarMateriales() {
        return (List<Apu>) mateDao.findAll();
    }

}
