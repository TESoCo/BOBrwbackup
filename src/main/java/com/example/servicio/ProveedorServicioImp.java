package com.example.servicio;

import com.example.dao.PersonaDao;
import com.example.dao.ProveedorDao;
import com.example.domain.Proveedor;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProveedorServicioImp implements ProveedorServicio {

    private final ProveedorDao proveedorDao;
    private final PersonaDao personaDao;

    public ProveedorServicioImp(ProveedorDao proveedorDao, PersonaDao personaDao) {
        this.proveedorDao = proveedorDao;
        this.personaDao = personaDao;
    }

    @Override
    public List<Proveedor> listar() {
        return proveedorDao.findAll();
    }

    @Override
    public Optional<Proveedor> buscarPorId(Long id) {
        return proveedorDao.findById(id);
    }

    @Override
    @jakarta.transaction.Transactional
    public Proveedor guardar(Proveedor form) {

        // Resolver Persona si viene con id
        if (form.getIdPersona() != null && form.getIdPersona().getIdPersona() != null) {
            form.setIdPersona(personaDao.findById(form.getIdPersona().getIdPersona())
                    .orElseThrow(() -> new IllegalArgumentException("Persona no existe")));
        } else {
            form.setIdPersona(null);
        }

        // === CREAR ===
        if (form.getIdProveedor() == null) {
            var ic = form.getInformacionComercial();
            if (ic != null && notBlank(ic.getNitRut())) {
                boolean dup = proveedorDao.existsByInformacionComercial_NitRut(ic.getNitRut());
                if (dup) throw new IllegalArgumentException("Ya existe un proveedor con ese NIT/RUT");
            }
            return proveedorDao.save(form); // Cascade ALL/MERGE recomendado en la relación
        }

        // === EDITAR ===
        Proveedor entity = proveedorDao.findById(form.getIdProveedor())
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no existe"));

        // Copia aquí otros campos "simples" del proveedor que tengas (nombre, estado, etc.)
        // entity.setXxx(form.getXxx());

        // ---- MERGE de InformacionComercial (evitar INSERT) ----
        var infoForm = form.getInformacionComercial();
        var infoEntity = entity.getInformacionComercial();
        if (infoEntity == null && infoForm != null) {
            // El proveedor no tenía info comercial: crear contenedor vacío (sin perder control del id)
            infoEntity = new com.example.domain.InformacionComercial();
            entity.setInformacionComercial(infoEntity);
        }

        if (infoForm != null) {
            // Si decides congelar NIT/Banco/Cuenta, valida aquí contra infoEntity antes de setear
            // (mantengo tu intención de “congelar”):
            if (notBlank(infoForm.getNitRut()) && infoEntity.getNitRut() != null &&
                    !equalsStr(infoForm.getNitRut(), infoEntity.getNitRut())) {
                throw new IllegalArgumentException("NIT/RUT no puede modificarse");
            }
            if (notBlank(infoForm.getBanco()) && infoEntity.getBanco() != null &&
                    !equalsStr(infoForm.getBanco(), infoEntity.getBanco())) {
                throw new IllegalArgumentException("Banco no puede modificarse");
            }
            if (notBlank(infoForm.getNumCuenta()) && infoEntity.getNumCuenta() != null &&
                    !equalsStr(infoForm.getNumCuenta(), infoEntity.getNumCuenta())) {
                throw new IllegalArgumentException("Cuenta no puede modificarse");
            }

            // Si NO congelaras esos campos, y quisieras permitir cambios de NIT, valida unicidad:
            // if (cambiaNIT) { validar que no exista otro proveedor con ese NIT }

            // Copiamos/actualizamos campos permitidos (NO tocamos el id)
            if (infoEntity.getNitRut() == null) infoEntity.setNitRut(infoForm.getNitRut());
            if (infoEntity.getBanco() == null) infoEntity.setBanco(infoForm.getBanco());
            if (infoEntity.getNumCuenta() == null) infoEntity.setNumCuenta(infoForm.getNumCuenta());

            infoEntity.setCorreoElectronico(infoForm.getCorreoElectronico());
            infoEntity.setDireccion(infoForm.getDireccion());
            infoEntity.setFormaPago(infoForm.getFormaPago());
            infoEntity.setProducto(infoForm.getProducto());
        } else {
            // Si en el formulario quitaron toda la info, decide tu regla:
            // entity.setInformacionComercial(null);
        }

        return proveedorDao.save(entity);
    }

    private boolean equalsStr(String a, String b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }
    private boolean notBlank(String s) { return s != null && !s.isBlank(); }


    @Override
    @Transactional
    public void eliminar(Long id) {
        proveedorDao.deleteById(id);
    }
}