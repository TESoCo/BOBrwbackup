package com.example.servicio;

import com.mongodb.BasicDBObject;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class GridFSService {

    @Autowired
    private GridFsOperations gridFsOperations;

    /**
     * Guarda un archivo en MongoDB GridFS
     */
    public String storeFile(MultipartFile file, Long avanceId) throws IOException {
        // Metadatos para el archivo
        var metadata = new BasicDBObject();
        metadata.put("avanceId", avanceId);
        metadata.put("originalFileName", file.getOriginalFilename());
        metadata.put("uploadTime", System.currentTimeMillis());

        // Guardar en GridFS
        ObjectId fileId = gridFsOperations.store(
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getContentType(),
                metadata
        );

        return fileId.toString();
    }

    /**
     * Recupera un archivo de GridFS
     */
    public byte[] getFile(String fileId) throws IOException {
        try {
            GridFSFile gridFSFile = gridFsOperations.findOne(
                    new Query(Criteria.where("_id").is(new ObjectId(fileId)))
            );

            if (gridFSFile == null) {
                return null;
            }

            GridFsResource resource = gridFsOperations.getResource(gridFSFile);
            try (InputStream inputStream = resource.getInputStream();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                return outputStream.toByteArray();
            }
        } catch (Exception e) {
            throw new IOException("Error al obtener archivo de GridFS: " + e.getMessage(), e);
        }
    }

    /**
     * Elimina un archivo de GridFS
     */
    public void deleteFile(String fileId) {
        try {
            gridFsOperations.delete(new Query(Criteria.where("_id").is(new ObjectId(fileId))));
        } catch (Exception e) {
            throw new RuntimeException("Error al eliminar archivo de GridFS: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene información del archivo
     */
    public GridFSFile getFileInfo(String fileId) {
        return gridFsOperations.findOne(new Query(Criteria.where("_id").is(new ObjectId(fileId))));
    }
}