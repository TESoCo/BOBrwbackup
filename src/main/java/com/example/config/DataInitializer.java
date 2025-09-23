package com.example.config;

import com.example.dao.PermisoDao;
import com.example.dao.RolDao;
import com.example.domain.Permiso;
import com.example.domain.Rol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RolDao rolDao;

    @Autowired
    private PermisoDao permisoDao;

    @Override
    public void run(String... args) throws Exception {

        // Create basic permissions
        Permiso crearApu = createAndSavePermisoIfNotExists("CREAR_APU");
        Permiso editarApu = createAndSavePermisoIfNotExists("EDITAR_APU");

        Permiso crearAvance = createAndSavePermisoIfNotExists("CREAR_AVANCE");
        Permiso editarAvance = createAndSavePermisoIfNotExists("EDITAR_AVANCE");

        Permiso crearContratista = createAndSavePermisoIfNotExists("CREAR_CONTRATISTA");
        Permiso editarContratista = createAndSavePermisoIfNotExists("EDITAR_CONTRATISTA");

        Permiso crearFotodato = createAndSavePermisoIfNotExists("CREAR_FOTODATO");
        Permiso editarFotodato = createAndSavePermisoIfNotExists("EDITAR_FOTODATO");

        Permiso crearInfoComercial = createAndSavePermisoIfNotExists("CREAR_INFOCOMERCIAL");
        Permiso editarInfoComercial = createAndSavePermisoIfNotExists("EDITAR_INFOCOMERCIAL");

        Permiso crearInventario = createAndSavePermisoIfNotExists("CREAR_INVENTARIO");
        Permiso editarInventario = createAndSavePermisoIfNotExists("EDITAR_INVENTARIO");

        Permiso crearMaterial = createAndSavePermisoIfNotExists("CREAR_MATERIAL");
        Permiso editarMaterial = createAndSavePermisoIfNotExists("EDITAR_MATERIAL");

        Permiso crearObra = createAndSavePermisoIfNotExists("CREAR_OBRA");
        Permiso editarObra = createAndSavePermisoIfNotExists("EDITAR_OBRA");
        Permiso leerObra = createAndSavePermisoIfNotExists("LEER_OBRA");

        Permiso crearPermiso = createAndSavePermisoIfNotExists("CREAR_PERMISO");
        Permiso editarPermiso = createAndSavePermisoIfNotExists("EDITAR_PERMISO");

        Permiso crearProveedor = createAndSavePermisoIfNotExists("CREAR_PROVEEDOR");
        Permiso editarProveedor = createAndSavePermisoIfNotExists("EDITAR_PROVEEDOR");

        // ADMIN ONLY permissions
        Permiso crearRol = createAndSavePermisoIfNotExists("CREAR_ROL");
        Permiso editarRol = createAndSavePermisoIfNotExists("EDITAR_ROL");
        Permiso crearUsuario = createAndSavePermisoIfNotExists("CREAR_USUARIO");
        Permiso editarUsuario = createAndSavePermisoIfNotExists("EDITAR_USUARIO");

        // Create roles with specific permissions (only if they don't exist)
        createAdminRoleIfNotExists();
        createSupervisorRoleIfNotExists(crearRol, editarRol, crearUsuario, editarUsuario, crearPermiso, editarPermiso);
        createOperativeRoleIfNotExists(crearFotodato, editarFotodato, crearAvance, editarAvance,
                crearInventario, editarInventario, leerObra);

        System.out.println("=== Inicialización de datos completada ===");
    }

    private Permiso createAndSavePermisoIfNotExists(String nombrePermiso) {
        // Verificar si el permiso ya existe buscando por nombre
        List<Permiso> permisosExistentes = permisoDao.findByNombrePermiso(nombrePermiso);

        if (!permisosExistentes.isEmpty()) {
            System.out.println("Permiso ya existe: " + nombrePermiso);
            return permisosExistentes.get(0);
        } else {
            Permiso permiso = new Permiso();
            permiso.setNombrePermiso(nombrePermiso);
            Permiso savedPermiso = permisoDao.save(permiso);
            System.out.println("Permiso creado: " + nombrePermiso);
            return savedPermiso;
        }
    }

    private void createAdminRoleIfNotExists() {
        // Verificar si el rol ya existe buscando por nombre
        List<Rol> rolesExistentes = rolDao.findByNombreRolIgnoreCase("ADMIN");

        if (!rolesExistentes.isEmpty()) {
            System.out.println("Rol ADMIN ya existe");
            return;
        }

        Rol adminRol = new Rol();
        adminRol.setNombreRol("ADMIN");
        adminRol.setDescripRol("Administrador del sistema con todos los permisos");
        adminRol.setPermisoList(permisoDao.findAll());
        rolDao.save(adminRol);
        System.out.println("Rol ADMIN creado");
    }

    private void createSupervisorRoleIfNotExists(Permiso... excludedPermissions) {
        // Verificar si el rol ya existe buscando por nombre
        List<Rol> rolesExistentes = rolDao.findByNombreRolIgnoreCase("SUPERVISOR");

        if (!rolesExistentes.isEmpty()) {
            System.out.println("Rol SUPERVISOR ya existe");
            return;
        }

        List<Permiso> allPermissions = permisoDao.findAll();
        List<Permiso> supervisorPermissions = new ArrayList<>(allPermissions);

        // Remove admin-only permissions
        for (Permiso excluded : excludedPermissions) {
            supervisorPermissions.removeIf(perm ->
                    perm.getNombrePermiso().equals(excluded.getNombrePermiso()));
        }

        Rol supervisorRol = new Rol();
        supervisorRol.setNombreRol("SUPERVISOR");
        supervisorRol.setDescripRol("Supervisor con todos los permisos excepto usuarios, roles y permisos");
        supervisorRol.setPermisoList(supervisorPermissions);
        rolDao.save(supervisorRol);
        System.out.println("Rol SUPERVISOR creado");
    }

    private void createOperativeRoleIfNotExists(Permiso... includedPermissions) {
        // Verificar si el rol ya existe buscando por nombre
        List<Rol> rolesExistentes = rolDao.findByNombreRolIgnoreCase("OPERATIVO");

        if (!rolesExistentes.isEmpty()) {
            System.out.println("Rol OPERATIVO ya existe");
            return;
        }

        List<Permiso> operativePermissions = new ArrayList<>();

        // Add specific permissions for operative role
        for (Permiso included : includedPermissions) {
            operativePermissions.add(included);
        }

        Rol operativeRol = new Rol();
        operativeRol.setNombreRol("OPERATIVO");
        operativeRol.setDescripRol("Rol operativo con permisos limitados: fotodato, avance, inventario y lectura de obras");
        operativeRol.setPermisoList(operativePermissions);
        rolDao.save(operativeRol);
        System.out.println("Rol OPERATIVO creado");
    }
}