package com.example.servicio;

import com.example.dao.ProyectoDao;
import com.example.domain.Obra;
import com.example.domain.Proyecto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProyectoServicioImpl implements ProyectoServicio {

    @Autowired
    private ProyectoDao proyectoDao;

    @Override
    public List<Proyecto> listarProyectos() {
        return proyectoDao.findAll();
    }

    @Override
    public Proyecto encontrarPorId(Long id) {
        return proyectoDao.findById(id).orElse(null);
    }

    @Override
    public void guardar(Proyecto proyecto) {
        proyectoDao.save(proyecto);
    }

    @Override
    public void borrar(Proyecto proyecto) {
        proyectoDao.delete(proyecto);
    }

    @Override
    public List<Proyecto> buscarPorDescripcion(String descripcion) {
        return proyectoDao.findByDescProyectoContainingIgnoreCase(descripcion);
    }

    @Override
    public List<Proyecto> buscarPorEquipo(Long idEquipo) {
        return proyectoDao.findByEquipoIdEquipo(idEquipo);
    }

    @Override
    public List<Proyecto> buscarProyectosSinEquipo() {
        return proyectoDao.findByEquipoIsNull();
    }

    @Override
    public List<Proyecto> buscarProyectosConObras() {
        return proyectoDao.findByObrasIsEmpty();
    }

    @Override
    public List<Obra> getObrasProyectoPresupuesto(Long idProyecto) {
        Proyecto proyecto = proyectoDao.findById(idProyecto).get();
        if (proyecto == null) return null;
        return proyecto.getObras().stream()
                .filter(o -> Obra.EtapaObra.PRESUPUESTO.equals(o.getEtapa()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Obra> getObrasProyectoEjecucion(Long idProyecto) {
        Proyecto proyecto = proyectoDao.findById(idProyecto).get();
        if (proyecto == null) return null;
        return proyecto.getObras().stream()
                .filter(o -> Obra.EtapaObra.EJECUCION.equals(o.getEtapa()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Obra> getObrasProyectoCierre(Long idProyecto) {
        Proyecto proyecto = proyectoDao.findById(idProyecto).get();
        if (proyecto == null) return null;
        return proyecto.getObras().stream()
                .filter(o -> Obra.EtapaObra.CIERRE.equals(o.getEtapa()))
                .collect(Collectors.toList());
    }

}