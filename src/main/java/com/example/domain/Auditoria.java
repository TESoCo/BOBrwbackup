package com.example.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "auditoria",
        indexes = {
                @Index(name = "idx_auditoria_entidad", columnList = "entidad, id_entidad"),
                @Index(name = "idx_auditoria_fecha", columnList = "fecha_cambio"),
                @Index(name = "idx_auditoria_usuario", columnList = "id_usuario")
        })
public class Auditoria implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long idAuditoria;

    @Column(name = "entidad", nullable = false, length = 50)
    private String entidad;

    @Column(name = "id_entidad", nullable = false)
    private Long idEntidad;

    @Column(name = "campo", length = 50)
    private String campo;

    @Column(name = "valor_anterior", columnDefinition = "TEXT")
    private String valorAnterior;

    @Column(name = "valor_nuevo", columnDefinition = "TEXT")
    private String valorNuevo;

    @Column(name = "accion", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AccionAuditoria accion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    @JsonIgnore
    private Usuario usuario;

    @Column(name = "fecha_cambio", nullable = false)
    @CreationTimestamp
    private LocalDateTime fechaCambio;

    @Column(name = "comentario", columnDefinition = "TEXT")
    private String comentario;

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    // ---------- ENUM ----------
    public enum AccionAuditoria {
        INSERT, UPDATE, DELETE, STATUS_CHANGE
    }

    // ---------- MÉTODOS DE FÁBRICA ----------
    public static Auditoria crear(String entidad, Long idEntidad,
                                  AccionAuditoria accion, Usuario usuario) {
        return crear(entidad, idEntidad, accion, usuario, null, null, null);
    }

    public static Auditoria crear(String entidad, Long idEntidad,
                                  AccionAuditoria accion, Usuario usuario,
                                  String campo, String valorAnterior, String valorNuevo) {
        Auditoria auditoria = new Auditoria();
        auditoria.setEntidad(entidad);
        auditoria.setIdEntidad(idEntidad);
        auditoria.setAccion(accion);
        auditoria.setUsuario(usuario);
        auditoria.setCampo(campo);
        auditoria.setValorAnterior(valorAnterior);
        auditoria.setValorNuevo(valorNuevo);
        return auditoria;
    }
}