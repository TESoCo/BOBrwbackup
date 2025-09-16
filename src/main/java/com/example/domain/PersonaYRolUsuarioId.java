// PersonaYRolUsuarioId.java
package com.example.domain;
import lombok.Data;

import java.io.Serializable;

@Data
class PersonaYRolUsuarioId implements Serializable {
    private Integer usuario;
    private Integer persona;
    private Integer rol;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PersonaYRolUsuarioId that = (PersonaYRolUsuarioId) o;

        if (!usuario.equals(that.usuario)) return false;
        if (!persona.equals(that.persona)) return false;
        return rol.equals(that.rol);
    }


    @Override
    public int hashCode() {
        int result = usuario.hashCode();
        result = 31 * result + persona.hashCode();
        result = 31 * result + rol.hashCode();
        return result;
    }
}