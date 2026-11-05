package com.hotel;

import com.hotel.modelo.EstadoReserva;
import com.hotel.modelo.Hotel;
import com.hotel.persistencia.RepositorioH2;
import com.hotel.persistencia.RepositorioHotel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la aplicación completa: se le "escribe" al menú una lista de respuestas y se
 * revisa lo que muestra y lo que quedó guardado en la base de datos H2 (en memoria).
 */
class ConsolaTest {

    private static int contador = 0;

    private String url;
    private RepositorioHotel repositorio;

    @BeforeEach
    void preparar() {
        contador++;
        url = "jdbc:h2:mem:consola" + contador + ";DB_CLOSE_DELAY=-1";
        repositorio = new RepositorioH2(url);
    }

    /** Ejecuta una sesión del programa con las respuestas dadas (una por línea) y devuelve lo que se mostró. */
    private String ejecutar(RepositorioHotel repo, String... respuestas) {
        String texto = String.join("\n", respuestas) + "\n";
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        PrintStream pantalla = new PrintStream(salida, true, StandardCharsets.UTF_8);
        Consola consola = new Consola(repo,
                new ByteArrayInputStream(texto.getBytes(StandardCharsets.UTF_8)), pantalla);
        consola.iniciar();
        return salida.toString(StandardCharsets.UTF_8);
    }

    /** Deja listo: habitación 101 (doble, 120 por noche), cliente Ana y la reserva RES-0001 confirmada. */
    private void prepararReservaConfirmada() {
        ejecutar(repositorio,
                "admin", "admin123",
                "14", "101", "2", "2", "120",
                "0");
        ejecutar(repositorio,
                "recepcion", "recep123",
                "1", "Ana Pérez", "5566", "3001112233", "ana@correo.com", "Colombiana",
                "5", "5566", "101", "2026-11-10", "2026-11-13",
                "6", "RES-0001",
                "0");
    }

    // ------------------------------------------------------------------ inicio de sesión

    @Test
    @DisplayName("La primera vez crea los usuarios por defecto y la segunda ya no")
    void primeraEjecucion() {
        String primera = ejecutar(repositorio, "admin", "admin123", "0");
        String segunda = ejecutar(repositorio, "admin", "admin123", "0");

        assertTrue(primera.contains("Primera ejecución"));
        assertTrue(primera.contains("Bienvenido(a), Administrador"));
        assertFalse(segunda.contains("Primera ejecución"));
        assertTrue(segunda.contains("Bienvenido(a)"));
    }

    @Test
    @DisplayName("Tras tres intentos fallidos el programa se cierra")
    void tresIntentosFallidos() {
        String salida = ejecutar(repositorio,
                "admin", "mala", "admin", "mala2", "otro", "x");

        assertTrue(salida.contains("Usuario o contraseña incorrectos."));
        assertTrue(salida.contains("Demasiados intentos fallidos"));
        assertFalse(salida.contains("MENÚ"));
    }

    @Test
    @DisplayName("Permite acertar en el segundo intento")
    void segundoIntento() {
        String salida = ejecutar(repositorio,
                "recepcion", "equivocada",
                "recepcion", "recep123",
                "0");

        assertTrue(salida.contains("Bienvenido(a), Recepcionista (RECEPCIONISTA)"));
        assertTrue(salida.contains("Hasta luego."));
    }

    // ------------------------------------------------------------------ flujo completo y persistencia

    @Test
    @DisplayName("Flujo completo: reservar, check-in, servicio, pago y check-out, guardado en la base de datos")
    void flujoCompleto() {
        prepararReservaConfirmada();

        String salida = ejecutar(repositorio,
                "recepcion", "recep123",
                "8", "RES-0001",
                "10", "RES-0001", "Desayuno", "15", "3",
                "11", "RES-0001", "405", "1",
                "9", "RES-0001",
                "12", "RES-0001",
                "0");

        assertTrue(salida.contains("Check-in realizado"));
        assertTrue(salida.contains("Nuevo total: 405.00"));
        assertTrue(salida.contains("COMPROBANTE DE PAGO RES-0001-P1"));
        assertTrue(salida.contains("Saldo pendiente: 0.00"));
        assertTrue(salida.contains("Check-out realizado"));
        assertTrue(salida.contains("Reserva RES-0001 - FINALIZADA"));

        // Se comprueba directamente en la base de datos, con un repositorio nuevo
        Hotel guardado = new RepositorioH2(url).cargar();
        assertEquals(EstadoReserva.FINALIZADA, guardado.buscarReserva("RES-0001").orElseThrow().getEstado());
        assertEquals(new BigDecimal("405.00"), guardado.buscarReserva("RES-0001").orElseThrow().getTotalPagado());
    }

    @Test
    @DisplayName("Los datos siguen ahí cuando se vuelve a abrir el programa")
    void datosSobrevivenAlReinicio() {
        prepararReservaConfirmada();

        // "Reiniciar": nuevo repositorio y nueva consola sobre la misma base de datos
        String salida = ejecutar(new RepositorioH2(url),
                "admin", "admin123",
                "2",
                "3",
                "13",
                "0");

        assertTrue(salida.contains("Ana Pérez"));
        assertTrue(salida.contains("Habitación 101 | DOBLE | capacidad 2 | tarifa 120.00"));
        assertTrue(salida.contains("RES-0001 | Ana Pérez | hab. 101 | 2026-11-10 a 2026-11-13 | CONFIRMADA"));
    }

    @Test
    @DisplayName("Una reserva cancelada queda cancelada en la base de datos y libera la habitación")
    void cancelarReserva() {
        prepararReservaConfirmada();

        String salida = ejecutar(repositorio,
                "recepcion", "recep123",
                "7", "RES-0001",
                "4", "2026-11-10", "2026-11-13",
                "0");

        assertTrue(salida.contains("Reserva RES-0001 cancelada."));
        assertTrue(salida.contains("Habitación 101 | DOBLE"));
        assertEquals(EstadoReserva.CANCELADA,
                new RepositorioH2(url).cargar().buscarReserva("RES-0001").orElseThrow().getEstado());
    }

    // ------------------------------------------------------------------ reglas y errores

    @Test
    @DisplayName("Muestra un error si se intenta reservar una habitación ocupada (sobreventa)")
    void sobreventa() {
        prepararReservaConfirmada();

        String salida = ejecutar(repositorio,
                "recepcion", "recep123",
                "1", "Luis Torres", "7788", "", "", "Peruana",
                "5", "7788", "101", "2026-11-12", "2026-11-14",
                "13",
                "0");

        assertTrue(salida.contains("Error: La habitación 101 no está disponible entre 2026-11-12 y 2026-11-14"));
        assertEquals(1, new RepositorioH2(url).cargar().getReservas().size());
    }

    @Test
    @DisplayName("Un recepcionista no puede usar las opciones del administrador")
    void recepcionistaNoEsAdministrador() {
        String salida = ejecutar(repositorio,
                "recepcion", "recep123",
                "14",
                "18",
                "0");

        assertEquals(2, contar(salida, "Esta opción es solo para el administrador"));
    }

    @Test
    @DisplayName("Un administrador no puede hacer check-in")
    void administradorNoHaceCheckIn() {
        prepararReservaConfirmada();

        String salida = ejecutar(repositorio,
                "admin", "admin123",
                "8", "RES-0001",
                "0");

        assertTrue(salida.contains("Solo un recepcionista puede registrar check-in y check-out"));
        assertEquals(EstadoReserva.CONFIRMADA,
                new RepositorioH2(url).cargar().buscarReserva("RES-0001").orElseThrow().getEstado());
    }

    @Test
    @DisplayName("No deja hacer check-out con saldo pendiente")
    void checkOutConSaldo() {
        prepararReservaConfirmada();

        String salida = ejecutar(repositorio,
                "recepcion", "recep123",
                "8", "RES-0001",
                "9", "RES-0001",
                "0");

        assertTrue(salida.contains("Error: No se puede hacer check-out: saldo pendiente de 360.00"));
    }

    @Test
    @DisplayName("Avisa cuando la reserva o el cliente no existen")
    void noExisten() {
        String salida = ejecutar(repositorio,
                "recepcion", "recep123",
                "6", "RES-9999",
                "5", "000",
                "0");

        assertTrue(salida.contains("Error: No existe la reserva RES-9999"));
        assertTrue(salida.contains("Error: No existe un cliente con el documento 000"));
    }

    @Test
    @DisplayName("Si se escribe mal un número o una fecha, vuelve a preguntar")
    void entradasInvalidas() {
        ejecutar(repositorio, "admin", "admin123", "14", "101", "1", "1", "80", "0");

        String salida = ejecutar(repositorio,
                "recepcion", "recep123",
                "abc",
                "4", "10-11-2026", "2026-11-10", "2026-11-13",
                "0");

        assertTrue(salida.contains("Opción no válida"));
        assertTrue(salida.contains("Fecha no válida. Use el formato AAAA-MM-DD"));
        assertTrue(salida.contains("Habitación 101 | SENCILLA"));
    }

    @Test
    @DisplayName("Rechaza datos inválidos como un correo sin '@'")
    void datoInvalido() {
        String salida = ejecutar(repositorio,
                "recepcion", "recep123",
                "1", "Ana", "5566", "", "correo-malo", "Colombiana",
                "2",
                "0");

        assertTrue(salida.contains("Dato inválido: El correo electrónico no es válido"));
        assertTrue(salida.contains("No hay clientes registrados."));
    }

    @Test
    @DisplayName("El administrador puede registrar un empleado nuevo que luego inicia sesión")
    void registrarEmpleado() {
        ejecutar(repositorio,
                "admin", "admin123",
                "17", "Carlos Díaz", "0003", "1", "carlos", "clave456",
                "0");

        String salida = ejecutar(repositorio, "carlos", "clave456", "0");

        assertTrue(salida.contains("Bienvenido(a), Carlos Díaz (RECEPCIONISTA)"));
    }

    @Test
    @DisplayName("El administrador ve el reporte de ocupación e ingresos")
    void reporte() {
        prepararReservaConfirmada();
        // El pago se registra con la fecha de hoy, por eso el segundo reporte abarca un periodo muy amplio
        ejecutar(repositorio, "recepcion", "recep123", "11", "RES-0001", "100", "2", "0");

        String salida = ejecutar(repositorio,
                "admin", "admin123",
                "18", "2026-11-01", "2026-11-30",
                "18", "2000-01-01", "2099-12-31",
                "0");

        assertTrue(salida.contains("REPORTE DE OCUPACIÓN 2026-11-01 a 2026-11-30"));
        assertTrue(salida.contains("Noches ocupadas / disponibles: 3 / 30"));
        assertTrue(salida.contains("Ocupación: 10.00%"));
        assertTrue(salida.contains("Reservas consideradas: 1"));
        assertTrue(salida.contains("Ingresos: 100.00"));
    }

    @Test
    @DisplayName("Cambiar la tarifa no afecta las reservas ya hechas")
    void cambiarTarifa() {
        prepararReservaConfirmada();

        String salida = ejecutar(repositorio,
                "admin", "admin123",
                "15", "101", "200",
                "12", "RES-0001",
                "3",
                "0");

        assertTrue(salida.contains("Tarifa por noche: 120.00"));
        assertTrue(salida.contains("Total: 360.00"));
        assertTrue(salida.contains("tarifa 200.00"));
    }

    @Test
    @DisplayName("Poner una habitación en mantenimiento la saca de las disponibles")
    void mantenimiento() {
        ejecutar(repositorio, "admin", "admin123", "14", "101", "1", "1", "80", "0");

        String salida = ejecutar(repositorio,
                "admin", "admin123",
                "16", "101", "2",
                "4", "2026-11-10", "2026-11-13",
                "0");

        assertTrue(salida.contains("La habitación 101 ahora está MANTENIMIENTO."));
        assertTrue(salida.contains("No hay habitaciones disponibles en esas fechas."));
    }

    @Test
    @DisplayName("Si se acaba la entrada de datos, el programa termina sin errores")
    void finDeLaEntrada() {
        String salida = ejecutar(repositorio, "admin", "admin123");

        assertTrue(salida.contains("Se terminó la entrada de datos"));
        assertNotNull(repositorio.cargar());
    }

    private int contar(String texto, String buscado) {
        int veces = 0;
        int posicion = texto.indexOf(buscado);
        while (posicion >= 0) {
            veces++;
            posicion = texto.indexOf(buscado, posicion + buscado.length());
        }
        return veces;
    }
}
