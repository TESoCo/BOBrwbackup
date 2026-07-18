package com.example.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EstadoInventario {
    SOLICITADO("SOLICITADO", "Solicitado", "Estado inicial al crear el registro"),
    APROBADO("APROBADO", "Aprobado", "Aprobado por personal autorizado"),
    ENTREGADO("ENTREGADO", "Entregado", "Material entregado en obra"),
    RECHAZADO("RECHAZADO", "Rechazado", "Solicitud rechazada"),
    ANULADO("ANULADO", "Anulado", "Registro anulado");

    private final String codigo; // Valor que se guarda en BD
    private final String displayName;
    private final String descripcion;

    EstadoInventario(String codigo, String displayName, String descripcion) {
        this.codigo = codigo;
        this.displayName = displayName;
        this.descripcion = descripcion;
    }

    @JsonValue // Para serialización JSON
    public String getCodigo() {
        return codigo;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescripcion() {
        return descripcion;
    }

    @JsonCreator // Para deserialización desde JSON
    public static EstadoInventario fromCodigo(String codigo) {
        for (EstadoInventario estado : EstadoInventario.values()) {
            if (estado.codigo.equalsIgnoreCase(codigo)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado inválido: " + codigo);
    }

    public boolean isTransicionValida(EstadoInventario nuevoEstado) {
        return switch (this) {
            case SOLICITADO -> nuevoEstado == APROBADO || nuevoEstado == RECHAZADO || nuevoEstado == ANULADO;
            case APROBADO -> nuevoEstado == ENTREGADO || nuevoEstado == ANULADO;
            case ENTREGADO -> false; // Estado final
            case RECHAZADO -> false; // Estado final
            case ANULADO -> false; // Estado final
        };
    }

    // Para saber si el estado es final
    public boolean isEstadoFinal() {
        return this == ENTREGADO || this == RECHAZADO || this == ANULADO;
    }

    // Para saber si permite acciones de stock
    public boolean permiteAccionesStock() {
        return this == APROBADO || this == ENTREGADO;
    }
}