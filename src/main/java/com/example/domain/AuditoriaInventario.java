package com.example.domain;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "auditoria_inventario")
@EntityListeners(AuditingEntityListener.class)
public class AuditoriaInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long idAuditoria;

    @ManyToOne
    @JoinColumn(name = "id_inventario", nullable = false)
    private Inventario inventario;

    @Column(name = "estado_anterior")
    private String estadoAnterior;

    @Column(name = "estado_nuevo", nullable = false)
    private String estadoNuevo;

    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_cambio", nullable = false)
    @CreationTimestamp
    private LocalDateTime fechaCambio;

    @Column(name = "comentario")
    private String comentario;

    @Column(name = "ip_origen")
    private String ipOrigen;

    @Column(name = "user_agent")
    private String userAgent;

    // Constructor para facilitar la creación
    public static AuditoriaInventario crear(Inventario inventario,
                                            String estadoAnterior,
                                            String estadoNuevo,
                                            Usuario usuario,
                                            String comentario) {
        AuditoriaInventario auditoria = new AuditoriaInventario();
        auditoria.setInventario(inventario);
        auditoria.setEstadoAnterior(estadoAnterior);
        auditoria.setEstadoNuevo(estadoNuevo);
        auditoria.setUsuario(usuario);
        auditoria.setComentario(comentario);
        return auditoria;
    }
}