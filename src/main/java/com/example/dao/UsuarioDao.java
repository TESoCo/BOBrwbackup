package com.example.dao;

import com.example.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsuarioDao extends JpaRepository <Usuario, Long> {
    Usuario findBynombreUsuario(String username);
    List<Usuario> findByRol_NombreRol(String nomRol);
    List <Usuario> findByNombreUsuario(String username);



}
