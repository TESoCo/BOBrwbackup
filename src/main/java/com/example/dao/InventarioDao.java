package com.example.dao;


import com.example.domain.Inventario;
import com.example.domain.Obra;
import org.springframework.data.repository.CrudRepository;
import java.time.LocalDate;
import java.util.List;

public interface InventarioDao extends CrudRepository<Inventario, Long> {

    List<Inventario> findByIdUsuario_idUsuario(Long IdUsuario);
    List<Inventario> findByIdObra(Obra obra);
    List<Inventario> findByFechaIngreso(LocalDate fecha);

}
