// InformacionComercial.java
package com.example.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.io.Serializable;

@Data
@Entity
@Table(name = "informacion_comercial")
public class InformacionComercial implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Info_Comerc")
    private Long idInfoComerc;

    @NotEmpty
    @Column(name = "NIT_RUT", unique = true)
    private String nitRut;

    @NotEmpty
    @Column(name = "forma_Pago")
    private String formaPago;

    @NotEmpty
    @Column(name = "banco")
    private String banco;

    @NotEmpty
    @Column(name = "num_Cuenta")
    private String numCuenta;

    @Pattern(
            regexp = "^(Calle|Carrera|Avenida|Diagonal|Transversal)\\s+\\d+[A-Za-z]?\\s+#\\s*\\d+-\\d+.*$",
            message = "Formato de dirección inválido. Use: Calle/Carrera/Avenida + Número + # + Número-Número"
    )
    @NotEmpty
    @Column(name = "direccion")
    private String direccion;

    @NotEmpty
    @Column(name = "correo_Electronico")
    private String correoElectronico;

    @NotEmpty
    @Column(name = "producto")
    private String producto;

}