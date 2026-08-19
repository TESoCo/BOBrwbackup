package com.example.config;

import com.example.dao.PermisoDao;
import com.example.dao.RolDao;
import com.example.dao.UsuarioDao;
import com.example.domain.Permiso;
import com.example.domain.Persona;
import com.example.domain.Rol;
import com.example.domain.Usuario;
import com.example.servicio.PersonaServicio;
import com.example.servicio.UsuarioServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RolDao rolDao;

    @Autowired
    private PermisoDao permisoDao;

    @Autowired
    private UsuarioServicio usuarioServicio;

    @Autowired
    private PersonaServicio personaServicio;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== Iniciando inicialización de datos ===");

        // 1. Crear todos los permisos primero
        crearPermisosBasicos();

        // 2. Crear roles con sus permisos
        crearRolesConPermisos();

        // 3. Crear usuario administrador por defecto
        crearUsuarioAdministrador();

        System.out.println("=== Inicialización de datos completada ===");
    }

    private void crearPermisosBasicos() {
        // Crear permisos básicos

        //APU
        Permiso crearApu = createAndSavePermisoIfNotExists("CREAR_APU");
        Permiso editarApu = createAndSavePermisoIfNotExists("EDITAR_APU");
        Permiso leerApu = createAndSavePermisoIfNotExists("LEER_APU");

        //AVANCES
        Permiso crearAvance = createAndSavePermisoIfNotExists("CREAR_AVANCE");
        Permiso editarAvance = createAndSavePermisoIfNotExists("EDITAR_AVANCE");
        Permiso LeerAvance = createAndSavePermisoIfNotExists("LEER_AVANCE");

        //CONTRATISTAS
        Permiso crearContratista = createAndSavePermisoIfNotExists("CREAR_CONTRATISTA");
        Permiso editarContratista = createAndSavePermisoIfNotExists("EDITAR_CONTRATISTA");
        Permiso leerContratista = createAndSavePermisoIfNotExists("LEER_CONTRATISTA");

        //FOTODATOS
        Permiso crearFotodato = createAndSavePermisoIfNotExists("CREAR_FOTODATO");
        Permiso editarFotodato = createAndSavePermisoIfNotExists("EDITAR_FOTODATO");
        Permiso leerFotodato =         createAndSavePermisoIfNotExists("LEER_FOTODATO");

        //INFORMACIÓN COMERCIAL
        Permiso crearInfoComercial = createAndSavePermisoIfNotExists("CREAR_INFOCOMERCIAL");
        Permiso editarInfoComercial = createAndSavePermisoIfNotExists("EDITAR_INFOCOMERCIAL");
        Permiso leerInfoComercial = createAndSavePermisoIfNotExists("LEER_INFOCOMERCIAL");

        //INVENTARIO
        Permiso crearInventario = createAndSavePermisoIfNotExists("CREAR_INVENTARIO");
        Permiso editarInventario = createAndSavePermisoIfNotExists("EDITAR_INVENTARIO");
        Permiso leerInventario =         createAndSavePermisoIfNotExists("LEER_INVENTARIO");

        //MATERIAL
        Permiso crearMaterial = createAndSavePermisoIfNotExists("CREAR_MATERIAL");
        Permiso editarMaterial = createAndSavePermisoIfNotExists("EDITAR_MATERIAL");
        Permiso leerMaterial = createAndSavePermisoIfNotExists("LEER_MATERIAL");

        //OBRA
        Permiso crearObra = createAndSavePermisoIfNotExists("CREAR_OBRA");
        Permiso editarObra = createAndSavePermisoIfNotExists("EDITAR_OBRA");
        Permiso leerObra = createAndSavePermisoIfNotExists("LEER_OBRA");



        //PROVEEDORES
        Permiso crearProveedor = createAndSavePermisoIfNotExists("CREAR_PROVEEDOR");
        Permiso editarProveedor = createAndSavePermisoIfNotExists("EDITAR_PROVEEDOR");
        Permiso leerProveedor =         createAndSavePermisoIfNotExists("LEER_PROVEEDOR");

        // ADMIN ONLY permissions
        //Eliminación
        Permiso borrarApu = createAndSavePermisoIfNotExists("BORRAR_APU");
        Permiso borrarAvance = createAndSavePermisoIfNotExists("BORRAR_AVANCE");
        Permiso borrarContratista = createAndSavePermisoIfNotExists("BORRAR_CONTRATISTA");
        Permiso borrarEquipo = createAndSavePermisoIfNotExists("BORRAR_EQUIPO");
        Permiso borrarFotodato = createAndSavePermisoIfNotExists("BORRAR_FOTODATO");
        Permiso borrarInfoCom = createAndSavePermisoIfNotExists("BORRAR_INFOCOMERCIAL");
        Permiso borrarInventario = createAndSavePermisoIfNotExists("BORRAR_INVENTARIO");
        Permiso borrarMaterial = createAndSavePermisoIfNotExists("BORRAR_MATERIAL");
        Permiso borrarObra = createAndSavePermisoIfNotExists("BORRAR_OBRA");
        Permiso borrarPermiso = createAndSavePermisoIfNotExists("BORRAR_PERMISO");
        Permiso borrarPersona = createAndSavePermisoIfNotExists("BORRAR_PERSONA");
        Permiso borrarProveedor = createAndSavePermisoIfNotExists("BORRAR_PROVEEDOR");
        Permiso borrarProyecto = createAndSavePermisoIfNotExists("BORRAR_PROYECTO");
        Permiso borrarRol = createAndSavePermisoIfNotExists("BORRAR_ROL");
        Permiso borrarUsuario = createAndSavePermisoIfNotExists("BORRAR_USUARIO");
        //roles
        Permiso crearRol = createAndSavePermisoIfNotExists("CREAR_ROL");
        Permiso editarRol = createAndSavePermisoIfNotExists("EDITAR_ROL");
        //Usuarios
        Permiso crearUsuario = createAndSavePermisoIfNotExists("CREAR_USUARIO");
        Permiso editarUsuario = createAndSavePermisoIfNotExists("EDITAR_USUARIO");
        Permiso aprobarUsuario = createAndSavePermisoIfNotExists("APROBAR_USUARIO");
        //Equipos
        Permiso crearEquipo = createAndSavePermisoIfNotExists("CREAR_EQUIPO");
        Permiso editarEquipo = createAndSavePermisoIfNotExists("EDITAR_EQUIPO");
        Permiso eliminarEquipo = createAndSavePermisoIfNotExists("ELIMINAR_EQUIPO");
        //Proyectos
        Permiso crearProyecto= createAndSavePermisoIfNotExists("CREAR_PROYECTO");
        Permiso editarProyecto = createAndSavePermisoIfNotExists("EDITAR_PROYECTO");
        //Permisos del sistema
        Permiso crearPermiso = createAndSavePermisoIfNotExists("CREAR_PERMISO");
        Permiso editarPermiso = createAndSavePermisoIfNotExists("EDITAR_PERMISO");
        Permiso leerPermiso = createAndSavePermisoIfNotExists("LEER_PERMISO");
    }

    private void crearRolesConPermisos() {

        List<Permiso> todosLosPermisos = permisoDao.findAll();

        // Verificar que los permisos de usuario existen
        boolean tieneCrearUsuario = todosLosPermisos.stream()
                .anyMatch(p -> "CREAR_USUARIO".equals(p.getNombrePermiso()));
        boolean tieneEditarUsuario = todosLosPermisos.stream()
                .anyMatch(p -> "EDITAR_USUARIO".equals(p.getNombrePermiso()));

        System.out.println("Permiso CREAR_USUARIO existe: " + tieneCrearUsuario);
        System.out.println("Permiso EDITAR_USUARIO existe: " + tieneEditarUsuario);
        System.out.println("Total permisos para ADMIN: " + todosLosPermisos.size());

        // Create roles with specific permissions (only if they don't exist)
        // Rol ADMIN - Todos los permisos
        crearRolSiNoExiste("ADMIN", "Administrador del sistema con todos los permisos", todosLosPermisos);


        // Rol SUPERVISOR - Todos los permisos excepto gestión de usuarios, roles y permisos
        List<Permiso> permisosSupervisor = new ArrayList<>(todosLosPermisos);
        permisosSupervisor.removeIf(permiso ->
            permiso.getNombrePermiso().contains("USUARIO") ||
            permiso.getNombrePermiso().contains("ROL") ||
            permiso.getNombrePermiso().contains("PERMISO")
        );
        crearRolSiNoExiste("SUPERVISOR", "Supervisor con permisos excepto gestión de usuarios, roles y permisos", permisosSupervisor);


        // Rol OPERATIVO - Permisos limitados
        List<Permiso> permisosOperativo = new ArrayList<>();
        for (Permiso permiso : todosLosPermisos) {
            String nombrePermiso = permiso.getNombrePermiso();
            if (nombrePermiso.contains("FOTODATO") ||
                    nombrePermiso.contains("AVANCE") ||
                    nombrePermiso.contains("INVENTARIO")) {
                permisosOperativo.add(permiso);
            }
        }
        crearRolSiNoExiste("OPERATIVO", "Rol operativo con permisos limitados", permisosOperativo);


        // Invitado - Solo lectura
        List<Permiso> permisosInvitado = new ArrayList<>();
        for (Permiso permiso : todosLosPermisos) {
            if (permiso.getNombrePermiso().contains("LEER_")) {
                permisosInvitado.add(permiso);
            }
        }
        crearRolSiNoExiste("INVITADO", "Rol invitado con permisos de solo lectura", permisosInvitado);

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


    private void crearRolSiNoExiste(String nombreRol, String descripcion, List<Permiso> permisos) {
        List<Rol> rolesExistentes = rolDao.findByNombreRolIgnoreCase(nombreRol);

        if (rolesExistentes.isEmpty()) {
            Rol rol = new Rol();
            rol.setNombreRol(nombreRol);
            rol.setDescripRol(descripcion);
            rol.setPermisoList(permisos);
            rolDao.save(rol);
            System.out.println("Rol " + nombreRol + " creado con " + permisos.size() + " permisos");
        } else {
            System.out.println("Rol " + nombreRol + " ya existe");

        }

    }

    @Transactional
    protected void crearUsuarioAdministrador() {
        try {
            if (usuarioServicio.encontrarPorNombreUsuario("admin") == null) {

                Usuario adminExistente = usuarioServicio.encontrarPorNombreUsuario("admin");

                if (adminExistente != null) {
                    // Si existe pero no tiene el estado correcto, actualizarlo
                    if (!"APPROVED".equals(adminExistente.getStatus())) {
                        adminExistente.setStatus(Usuario.StatusUsuario.APPROVED);
                        adminExistente.setAuthProvider(Usuario.AuthProvider.LOCAL);
                        usuarioServicio.guardar(adminExistente);
                        System.out.println("Usuario admin actualizado a estado APPROVED");
                    }
                    System.out.println("✅ Usuario administrador ya existe");
                    return;
                }

                // Buscar rol ADMIN
                Rol rolAdmin = rolDao.findByNombreRolIgnoreCase("ADMIN")
                        .stream()
                        .findFirst()
                        .orElse(null);

                if (rolAdmin == null) {
                    System.err.println("No se pudo crear admin: Rol ADMIN no encontrado");
                    return;
                }

                //Guardar la cantidad de permisos
                int permisoCount = rolAdmin.getPermisoList().size();

                // Crear persona
                Persona persona = new Persona();
                persona.setNombre("Administrador");
                persona.setApellido("Sistema");
                persona.setTelefono("000-000-0000");
                persona.setCorreo("admin@bob.com");
                Persona personaGuardada = personaServicio.salvar(persona);

                // Crear usuario
                Usuario usuario = new Usuario();
                usuario.setNombreUsuario("admin");
                usuario.setPass_usuario(passwordEncoder.encode("admin123"));
                usuario.setCargo("Administrador del Sistema");
                usuario.setPersona(personaGuardada);
                usuario.setRol(rolAdmin);

                //Autoaprobar el Adimn por defecto para que tenga acceso
                usuario.setStatus(Usuario.StatusUsuario.APPROVED);      // ✅ Aprobado automáticamente
                usuario.setAuthProvider(Usuario.AuthProvider.LOCAL);   // 🔒 Autenticación local
                usuario.setEmailVerified(true);     // ✅ Email verificado (admin local)
                usuario.setGoogleRefreshToken(null); // Sin token de Google

                usuarioServicio.guardar(usuario);

                System.out.println("   👤 Usuario: admin");
                System.out.println("   🔑 Contraseña: admin123");
                System.out.println("   📌 Estado: APPROVED (aprobado automáticamente)");

                System.out.println("Usuario administrador creado con rol ADMIN y " +
                        permisoCount + " permisos");

            } else {
                System.out.println("Usuario administrador ya existe");
            }
        } catch (Exception e) {
            System.err.println("Error creando usuario administrador: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Metod0 adicional: Verificar y corregir usuarios existentes
     * Útil para migrar usuarios que ya existen en la base de datos
     */
    @Transactional
    public void verificarYCorregirUsuariosExistentes() {
        try {
            List<Usuario> usuarios = usuarioServicio.listarUsuarios();
            int corregidos = 0;

            for (Usuario usuario : usuarios) {
                boolean necesitaCorreccion = false;

                // Verificar que todos los usuarios tengan un estado
                if (usuario.getStatus() == null) {
                    usuario.setStatus(Usuario.StatusUsuario.PENDING);
                    necesitaCorreccion = true;
                }

                // Verificar que todos tengan un authProvider
                if (usuario.getAuthProvider() == null) {
                    usuario.setAuthProvider(Usuario.AuthProvider.LOCAL);
                    necesitaCorreccion = true;
                }

                // Verificar que el admin esté aprobado
                if ("admin".equals(usuario.getNombreUsuario()) && !"APPROVED".equals(usuario.getStatus())) {
                    usuario.setStatus(Usuario.StatusUsuario.APPROVED);
                    necesitaCorreccion = true;
                }

                if (necesitaCorreccion) {
                    usuarioServicio.guardar(usuario);
                    corregidos++;
                }
            }

            if (corregidos > 0) {
                System.out.println("🔄 " + corregidos + " usuarios corregidos/actualizados");
            }

        } catch (Exception e) {
            System.err.println("❌ Error verificando usuarios: " + e.getMessage());
        }
    }

}











