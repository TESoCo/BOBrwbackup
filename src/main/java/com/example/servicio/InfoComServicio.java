package com.example.servicio;

import com.example.domain.InformacionComercial;
import java.util.List;

public interface InfoComServicio {

        List<InformacionComercial> comercialList()  ;

        InformacionComercial salvar(InformacionComercial informacionComercial);

        void borrar(InformacionComercial informacionComercial);

        InformacionComercial localizarInformacionComercial(InformacionComercial informacionComercial);

        InformacionComercial localizarPorId(Long id);

        InformacionComercial localizarPorNitRut(String NitRut);
    }




