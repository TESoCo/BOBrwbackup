// FotoDato.java
package com.example.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "foto_dato",
        indexes = {
                @Index(name = "idx_foto_avance", columnList = "id_avance"),
                @Index(name = "idx_foto_fecha", columnList = "fecha_foto")
        })
public class FotoDato implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_FotoDato")
    private Long idFotoDato;


    // ---------- RELACIONES ----------
    // From contexto_fotodato
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_Avance", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Avance idAvance;

    // ---------- METADATOS DE IMAGEN ----------
    // From imagen_fotodato
    // ID del archivo en MongoDB GridFS
    @NotEmpty(message = "El ID del archivo es obligatorio")
    @Column(name = "gridfs_file_id", nullable = false, length = 100)
    private String gridfsFileId;

    @NotEmpty(message = "El nombre del archivo es obligatorio")
    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @NotNull(message = "El tamaño del archivo es obligatorio")
    @Column(name = "tamanio_archivo", nullable = false)
    private Long tamanioArchivo;

    @NotEmpty(message = "El tipo MIME es obligatorio")
    @Column(name = "tipo_mime", nullable = false, length = 100)
    private String tipoMime;


    // ---------- UBICACIÓN ----------
    // From ubicacion_fotodato
    @Column(name = "coordenada_norte")
    private Double cooNFoto;

    @Column(name = "coordenada_este")
    private Double cooEFoto;


    // ---------- FECHAS ----------
    // From fecha_fotodato
    @NotNull(message = "La fecha de la foto es obligatoria")
    @Column(name = "fecha_foto", nullable = false)
    private LocalDate fechaFoto;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;


    // ---------- AUDITORÍA ----------
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "activo")
    private Boolean activo = true;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        if (fechaFoto == null) {
            fechaFoto = LocalDate.now();
        }
    }


}