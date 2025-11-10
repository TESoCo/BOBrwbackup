package com.example.servicio;

import com.example.domain.InformacionComercial;
import java.util.List;

public interface InfoComServicio {

        public List<InformacionComercial> comercialList()  ;

        public InformacionComercial salvar(InformacionComercial informacionComercial);

        public void borrar(InformacionComercial informacionComercial);

        public InformacionComercial localizarInformacionComercial(InformacionComercial informacionComercial);

        public InformacionComercial localizarPorId(Long id);
    }




