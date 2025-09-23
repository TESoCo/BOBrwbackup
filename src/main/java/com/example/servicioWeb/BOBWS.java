package com.example.servicioWeb;

import com.example.domain.*;
import com.example.servicio.AvanceServicio;
import com.example.servicio.ObraServicio;
import com.example.servicio.APUServicio;
import com.example.servicio.UsuarioServicio;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@WebService(serviceName = "BOBWS")

public class BOBWS {

    @Autowired
    private AvanceServicio avanceServicio;

    @Autowired
    private ObraServicio obraServicio;

    @Autowired
    private APUServicio APUServicio;

    @Autowired
    private UsuarioServicio usuarioServicio;

    @WebMethod
    public String reportarAvance(Long idUsuario, Obra idObra, String fecha, Apu apuReportado, Double cantidad)
    {
        try {
            // Create and save advance
            Avance avance = new Avance();
            avance.setIdUsuario(usuarioServicio.encontrarPorId(idUsuario));
            avance.setIdObra(idObra);
            avance.setFechaAvance(LocalDate.parse(fecha));
            avance.setIdApu(apuReportado);
            avance.setCantEjec(cantidad);

            avanceServicio.salvar(avance);
            return "Avance guardado con éxito";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @WebMethod
    public List<Avance> obtenerAvancesPorObra(Long idObra) {
        return avanceServicio.buscarPorIdObra(idObra);
    }

    @WebMethod
    public List<Apu> obtenerListaMateriales() {
        return APUServicio.listarElementos();
    }

    @WebMethod
    public List<Obra> obtenerPresupuestos() {
        return obraServicio.listaObra();
    }


/*
    @WebMethod
    public String borrarAvancesAntiguos(String fecha) {
        try {
            List<Avance> avances = avanceServicio.buscarPorFecha(fecha);
            for (Avance avance : avances) {
                avanceServicio.borrar(avance);
            }
            return "SUCCESS: Deleted " + avances.size() + " advances from " + fecha;
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
*/




}


