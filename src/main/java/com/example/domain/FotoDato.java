// FotoDato.java
package com.example.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "fotodato")
@SecondaryTables({
        @SecondaryTable(name = "imagen_fotodato", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_FotoDato")),
        @SecondaryTable(name = "ubicacion_fotodato", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_FotoDato")),
        @SecondaryTable(name = "fecha_fotodato", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_FotoDato"))
})
public class FotoDato implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_FotoDato")
    private Long idFotoDato;

    // From contexto_fotodato
    @ManyToOne
    @JoinColumn(name = "id_Avance")
    private Avance idAvance;

    // From imagen_fotodato
    // ID del archivo en MongoDB GridFS
    @Column(name = "gridfs_file_id", table = "imagen_fotodato", length = 100)
    private String gridfsFileId;

    @Column(name = "nombre_archivo", table = "imagen_fotodato", length = 255)
    private String nombreArchivo;

    @Column(name = "tamanio_archivo", table = "imagen_fotodato")
    private Long tamanioArchivo;

    @Column(name = "tipo_mime", table = "imagen_fotodato", length = 100)
    private String tipoMime;


    // From ubicacion_fotodato
    @Column(name = "CooN_Foto", table = "ubicacion_fotodato")
    private Double cooNFoto;

    @Column(name = "CooE_Foto", table = "ubicacion_fotodato")
    private Double cooEFoto;

    // From fecha_fotodato
    @Column(name = "Fecha_Foto", table = "fecha_fotodato")
    private LocalDate fechaFoto;
}