package com.example.dao;

import com.example.domain.AuditoriaInventario;
import com.example.domain.Inventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditoriaInventarioDao extends JpaRepository<AuditoriaInventario, Long> {
    List<AuditoriaInventario> findByInventarioOrderByFechaCambioDesc(Inventario inventario);
    List<AuditoriaInventario> findByUsuario_IdUsuarioOrderByFechaCambioDesc(Long idUsuario);
}