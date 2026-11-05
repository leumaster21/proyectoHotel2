# Sistema de Gestión de Hotel

Implementación en Java de la solución planteada en el análisis y diseño orientado a
objetos (habitaciones, reservas y clientes): clases, lógica de negocio, pruebas
unitarias con JUnit 5 y documentación JavaDoc.

## Estructura

```
src/main/java/com/hotel/
├── Main.java                      Demostración por consola del flujo completo
├── excepciones/                   HotelException, HabitacionNoDisponibleException, AccesoDenegadoException
└── modelo/
    ├── Persona (abstracta) ─┬─ Cliente
    │                        └─ Empleado (cargo: RECEPCIONISTA / ADMINISTRADOR)
    ├── Hotel                      Punto de entrada; compone Habitacion, agrega Empleado
    ├── Habitacion                 Disponibilidad por fechas y estado
    ├── Reserva                    Objeto central: noches, servicios, pagos, estados
    ├── Pago                       Pago y comprobante (compuesto por Reserva)
    ├── ServicioAdicional          Desayuno, lavandería, spa…
    ├── ReporteOcupacion           Resultado de Hotel.generarReporte (record inmutable)
    └── enums                      Cargo, TipoHabitacion, EstadoHabitacion, EstadoReserva, MetodoPago
src/test/java/com/hotel/modelo/    67 pruebas unitarias (7 clases)
docs/                              JavaDoc generado (abrir docs/index.html)
```

## Cómo ejecutar

Con Maven (Java 17 o superior):

```bash
mvn test                 # pruebas unitarias
mvn compile exec:java    # demostración (com.hotel.Main)
mvn javadoc:javadoc      # regenera el JavaDoc en target/site/apidocs
```

Sin Maven, solo con el JDK:

```bash
javac -encoding UTF-8 -d out $(find src/main -name "*.java")
java -cp out com.hotel.Main
javadoc -encoding UTF-8 -d docs $(find src/main -name "*.java")
```

## Requisitos funcionales → código

| Requisito | Dónde se implementa |
|-----------|---------------------|
| RF1 Clientes | `Hotel.registrarCliente`, `buscarCliente`, `Persona.actualizarContacto`, `Cliente.consultarHistorial` |
| RF2 Habitaciones | `Hotel.agregarHabitacion`, `Habitacion.cambiarEstado`, `setTarifaNoche` |
| RF3 Reservas sin sobreventa | `Hotel.crearReserva`, `Habitacion.estaDisponible`, `Reserva.confirmar/cancelar`, `Hotel.cancelarReserva` |
| RF4 Check-in / check-out | `Empleado.registrarCheckIn` / `registrarCheckOut` |
| RF5 Servicios y total | `Reserva.agregarServicio`, `calcularTotal`, `ServicioAdicional.calcularSubtotal` |
| RF6 Pagos y comprobante | `Reserva.registrarPago`, `Pago.procesar`, `Pago.generarComprobante` |
| RF7 Reportes | `Hotel.generarReporte` → `ReporteOcupacion` |
| RNF2 Seguridad | Contraseña con hash SHA-256 + sal; permisos por cargo y sesión (`AccesoDenegadoException`) |

## Reglas de negocio y decisiones de diseño

- **Sin sobreventa:** el rango de fechas es semiabierto `[entrada, salida)`; un huésped puede
  salir el mismo día en que otro entra. Solo las reservas PENDIENTE, CONFIRMADA y EN_CURSO
  bloquean la habitación; las canceladas y finalizadas no.
- **Ciclo de vida de la reserva:** `PENDIENTE → CONFIRMADA → EN_CURSO → FINALIZADA`, y
  `CANCELADA` solo desde PENDIENTE o CONFIRMADA.
- **Check-in:** solo un recepcionista con sesión activa, sobre una reserva CONFIRMADA y una
  habitación que pueda ocuparse. **Check-out:** exige la cuenta saldada.
- **Tarifa congelada:** la reserva guarda la tarifa vigente al crearse; cambiar la tarifa de la
  habitación no altera reservas existentes.
- **Dinero:** `BigDecimal` con dos decimales (nunca `double`). Un pago no puede exceder el saldo.
- **Composición / agregación** según el diagrama: `Habitacion` y `Pago` solo se crean desde
  `Hotel` y `Reserva` (constructores de paquete); `Empleado` existe fuera del hotel y se registra.
- **Reportes:** periodo con ambos extremos incluidos; la ocupación cuenta noches de reservas no
  canceladas; los ingresos suman pagos procesados con fecha dentro del periodo.
- El atributo `contraseña` del diagrama se escribe `contrasena` en el código (identificadores ASCII).

## Fuera de alcance

Persistencia en base de datos, interfaz gráfica/web, integración real con la pasarela de pagos
(`Pago.procesar()` es el punto de extensión), política de reembolsos al cancelar, control de
concurrencia entre usuarios (RNF1) y capacidad máxima de huéspedes por reserva.
