# Manual técnico — Sistema de Gestión de Hotel

## 1. Introducción

Este documento explica cómo está hecho el sistema por dentro: qué herramientas usa, cómo está
organizado el código, cómo se guardan los datos en la base de datos H2 y cómo se prueba. Está
pensado para quien necesite entender, mantener o ampliar el proyecto.

El sistema resuelve el problema de un hotel mediano (unas 60 habitaciones) que llevaba sus
reservas en hojas de cálculo y libros: evita que se vendan dos reservas para la misma habitación
y fecha, controla el check-in y el check-out, registra pagos y servicios adicionales y genera
reportes de ocupación. Los requisitos están en el documento de análisis (RF1 a RF7 y RNF1 a RNF4).

**Importante:** este proyecto parte de la primera entrega (clases, pruebas y JavaDoc). Las clases de esa
entrega **no se modificaron**: todo lo nuevo (base de datos, menú, manuales y pruebas) se agregó en
archivos nuevos. Los únicos archivos anteriores que cambiaron son `pom.xml` (se agregó la librería H2 y
una forma nueva de ejecutar el programa) y `.gitignore` (se agregaron líneas al final).

## 2. Herramientas usadas

Se escogieron herramientas sencillas y muy comunes:

| Herramienta | Versión | Para qué se usa |
|-------------|---------|-----------------|
| Java (JDK)  | 17 o superior | Lenguaje de programación |
| Maven       | 3.8 o superior | Compilar, descargar librerías y correr las pruebas |
| H2 Database | 2.2.224 | Base de datos (se guarda en un archivo, no hay que instalar nada) |
| JDBC        | (incluido en Java) | Conexión a la base de datos con SQL directo |
| JUnit 5     | 5.10.2 | Pruebas unitarias |
| JavaDoc     | (incluido en el JDK) | Documentación del código |

Cualquier editor sirve (VS Code, IntelliJ IDEA, NetBeans, Eclipse). No se usan frameworks como
Spring o Hibernate: el SQL está escrito a mano y se puede leer completo en un solo archivo.
La interfaz es un menú de consola, que es lo más simple de construir y de probar.

## 3. Estructura del proyecto

Los archivos marcados con **(nuevo)** se agregaron en esta entrega; los demás son los de la primera
entrega y no se modificaron.

```
proyectoHotel/
├── pom.xml                         Configuración de Maven (se agregó H2 y la ejecución "aplicacion")
├── README.md
├── manuales/                       (nuevo) Este manual y el manual de usuario
├── docs/                           JavaDoc generado (abrir docs/index.html)
├── datos/                          (nuevo) Se crea sola al ejecutar: aquí queda la base de datos H2
└── src/
    ├── main/java/com/hotel/
    │   ├── Main.java               Demostración en memoria (primera entrega)
    │   ├── Aplicacion.java         (nuevo) Programa principal con base de datos
    │   ├── Consola.java            (nuevo) Menú de consola
    │   ├── excepciones/            HotelException, HabitacionNoDisponibleException,
    │   │                           AccesoDenegadoException  + PersistenciaException (nuevo)
    │   ├── modelo/                 Clases del negocio (Hotel, Habitacion, Reserva, ...)
    │   └── persistencia/           (nuevo) RepositorioHotel, RepositorioH2,
    │                               HotelPersistente, EmpleadoConClave
    └── test/java/com/hotel/        67 pruebas de la primera entrega + 43 nuevas (110 en total)
```

## 4. Arquitectura

El programa tiene tres capas. Cada una solo habla con la de al lado:

```
  Usuario ──► Consola ──► modelo (Hotel, Reserva, ...) 
                 │
                 └──────► RepositorioHotel ──► RepositorioH2 ──► Base de datos H2
```

- **Consola** (`Consola`, `Aplicacion`): muestra el menú, lee lo que escribe el usuario y llama al
  modelo. No tiene reglas de negocio ni SQL.
- **Modelo** (`com.hotel.modelo`): contiene las clases del diagrama UML y todas las reglas del
  negocio. No sabe nada de la base de datos ni de la pantalla.
- **Persistencia** (`com.hotel.persistencia`): guarda y carga el modelo en H2. La consola solo
  conoce la interfaz `RepositorioHotel`; por eso en las pruebas se puede cambiar la base de datos
  sin tocar la consola.

**Qué pasa cuando el usuario hace una operación** (por ejemplo, crear una reserva): la consola
pide los datos, llama a `hotel.crearReserva(...)`, el modelo valida las reglas (habitación libre,
fechas correctas) y crea la reserva en memoria; después la consola llama a `repositorio.guardar(hotel)`
y los cambios quedan en la base de datos.

## 5. Modelo de clases

Es el diagrama UML del análisis, con estas relaciones:

- `Cliente` y `Empleado` heredan de `Persona` (abstracta).
- `Hotel` contiene sus `Habitacion` (composición) y agrega `Empleado` (agregación).
- `Reserva` une un `Cliente` con una `Habitacion`, y contiene sus `Pago` (composición) y sus
  `ServicioAdicional` (agregación).
- `ReporteOcupacion` es el resultado de `Hotel.generarReporte(...)`.

Ciclo de vida de una reserva:

```
PENDIENTE ──► CONFIRMADA ──► EN_CURSO ──► FINALIZADA
    └────────────┴──► CANCELADA   (solo antes del check-in)
```

## 5.1 Reglas de negocio principales

| Regla | Dónde está |
|-------|------------|
| No hay sobreventa: una habitación no se reserva si otra reserva vigente se cruza con las fechas. La noche de salida queda libre (un huésped puede salir el mismo día en que otro entra). | `Habitacion.estaDisponible`, `Hotel.crearReserva` |
| Una habitación en mantenimiento nunca está disponible. | `Habitacion.estaDisponible` |
| Solo un recepcionista con sesión iniciada hace check-in y check-out. | `Empleado.registrarCheckIn` / `registrarCheckOut` |
| El check-out exige la cuenta saldada. | `Reserva.finalizarEstadia` |
| La tarifa de una reserva queda congelada; cambiar la tarifa de la habitación no la afecta. | `Reserva` (campo `tarifaNoche`) |
| Un pago no puede ser mayor al saldo pendiente. | `Reserva.registrarPago` |
| Las contraseñas no se guardan: solo su hash SHA-256 con una sal aleatoria. | `Empleado` |
| El dinero se maneja con `BigDecimal` a dos decimales. | `Montos`, `Validaciones` |

Las opciones de administrador (agregar habitaciones, cambiar tarifas, mantenimiento, registrar
empleados y ver reportes) se controlan en `Consola.exigirAdministrador()`.

## 6. Base de datos

### 6.1 Tablas

La base de datos se crea sola la primera vez (`CREATE TABLE IF NOT EXISTS`). Son siete tablas:

| Tabla | Columnas principales | Clave primaria |
|-------|---------------------|----------------|
| `hotel` | id, nombre, direccion, telefono | id |
| `empleado` | id, nombre, documento, telefono, email, cargo, usuario, sal, hash_contrasena | id (documento y usuario son únicos) |
| `cliente` | id, nombre, documento, telefono, email, nacionalidad, categoria | id (documento es único) |
| `habitacion` | numero, tipo, capacidad, tarifa_noche, estado | numero |
| `reserva` | codigo, cliente_id, numero_habitacion, empleado_id, fecha_entrada, fecha_salida, tarifa_noche, estado | codigo |
| `servicio_adicional` | reserva_codigo, orden, servicio_id, nombre, precio, cantidad | (reserva_codigo, orden) |
| `pago` | reserva_codigo, id, monto, metodo, fecha | (reserva_codigo, id) |

Relaciones (claves foráneas):

```
cliente 1 ────< reserva >──── 1 habitacion
empleado 0..1 ─< reserva
reserva 1 ────< servicio_adicional
reserva 1 ────< pago
```

Tipos de datos: el dinero es `DECIMAL(12,2)`, las fechas son `DATE`, los textos son `VARCHAR`, y
las enumeraciones (tipo de habitación, estado, cargo, método de pago) se guardan como texto con el
nombre de la constante (por ejemplo `EN_CURSO`).

### 6.2 Dónde queda el archivo

Al ejecutar el programa se crea la carpeta `datos/` junto al proyecto con el archivo
`datos/hotel.mv.db`. La URL de conexión está en `RepositorioH2.URL_ARCHIVO`:

```
jdbc:h2:./datos/hotel      usuario: sa      contraseña: (vacía)
```

Para ver las tablas con una herramienta gráfica, cierre primero el programa (H2 no permite que
dos programas abran el archivo a la vez) y ejecute la consola web de H2:

```bash
java -cp ~/.m2/repository/com/h2database/h2/2.2.224/h2-2.2.224.jar org.h2.tools.Console
```

En el navegador use la URL de conexión con la ruta completa de su carpeta, por ejemplo
`jdbc:h2:C:/proyectos/gestion-hotel/datos/hotel`.

## 7. Cómo funciona la persistencia

Se eligió una forma sencilla de guardar, fácil de entender y de probar.

### 7.1 El problema: guardar sin modificar las clases

Las clases de la primera entrega esconden sus datos (atributos privados) y solo permiten crear los objetos
mediante las operaciones normales del hotel. Eso es bueno para el negocio, pero hace difícil guardarlos y
volver a armarlos. Había dos obstáculos concretos:

1. `Hotel` guarda los clientes pero **no tiene un método para listarlos todos**.
2. `Empleado` guarda la contraseña como hash, pero **no deja leer ese hash**, y solo se puede crear un
   empleado dándole la contraseña en texto plano. Entonces no se puede guardar ni recuperar.

Para no tocar las clases originales, se agregaron dos clases que **heredan** de ellas:

| Clase nueva | Hereda de | Qué agrega |
|-------------|-----------|------------|
| `HotelPersistente` | `Hotel` | Una lista de clientes (`getClientes()`) que se llena cada vez que se registra uno |
| `EmpleadoConClave` | `Empleado` | Guarda su propia sal y hash SHA-256 (`getSalBase64()`, `getHashBase64()`) y valida la contraseña con ellos |

`EmpleadoConClave` funciona así: el constructor de `Empleado` exige una contraseña, por eso se le entrega una
clave interna fija. Al iniciar sesión, `EmpleadoConClave` comprueba la contraseña **real** contra su propio hash
y, si es correcta, llama al `iniciarSesion` original con la clave interna para dejar activa la sesión (así
siguen funcionando el check-in y el check-out). La clave interna por sí sola no sirve para entrar: las
pruebas lo comprueban.

La aplicación debe crear los empleados como `EmpleadoConClave` y el hotel como `HotelPersistente`
(`Consola` ya lo hace). Si se intenta guardar un empleado de otro tipo, se lanza `PersistenciaException`.

### 7.2 Guardar (`guardar`)

1. Abre una conexión y desactiva el guardado automático (`setAutoCommit(false)`).
2. Borra el contenido de todas las tablas (primero las hijas: `pago`, `servicio_adicional`, `reserva`, y luego
   las demás).
3. Vuelve a insertar todo: hotel, empleados, clientes, habitaciones y reservas con sus servicios y pagos.
4. Hace `commit`. Si algo falla en cualquier punto, hace `rollback` y lanza `PersistenciaException`; la
   base de datos queda exactamente como estaba.

### 7.3 Cargar (`cargar`)

Primero se leen las filas de las tablas y después se **vuelve a armar el hotel repitiendo las operaciones
normales**, en este orden:

1. **Habitaciones:** `agregarHabitacion` con su tarifa actual.
2. **Empleados:** se crean con `EmpleadoConClave.restaurar` (con la sal y el hash guardados) y se registran.
3. **Clientes:** `registrarCliente` en orden de id y luego `setCategoria`. El hotel les da los ids 1, 2, 3...
   en orden, que son los mismos con los que se guardaron (el sistema no borra clientes). Si no coinciden, se
   lanza `PersistenciaException`.
4. **Reservas**, en orden de código (`RES-0001`, `RES-0002`...), igual que se crearon:
   - Se cambia un momento la tarifa de la habitación a la que tenía la reserva, se llama a `crearReserva` y se
     devuelve la tarifa actual. Así la reserva conserva su tarifa congelada.
   - Se agregan sus servicios y sus pagos.
   - Se lleva al estado guardado: `confirmar()`, `cancelar()` o, para EN_CURSO y FINALIZADA, se hace el
     check-in (y check-out) con un empleado interno de uso temporal que nunca se registra en el hotel.
5. **Estado de las habitaciones:** al final cada habitación se deja en su estado guardado (disponible,
   ocupada o en mantenimiento). Se hace al final porque el check-in cambia el estado de la habitación y una
   habitación en mantenimiento no deja crear reservas.

Por qué funciona la repetición: una reserva vigente (pendiente, confirmada o en curso) lo estuvo desde que se
creó, así que al repetir las operaciones en el mismo orden nunca aparece un conflicto de fechas que no
existiera antes. Después de cargar, el hotel sigue evitando la sobreventa y sigue numerando las reservas y los
clientes donde había quedado.

**Primera ejecución:** si no hay nada guardado, `Consola` crea un hotel por defecto y dos usuarios:
`admin / admin123` (administrador) y `recepcion / recep123` (recepcionista).

## 8. Instalación y ejecución

Requisitos: JDK 17 o superior y Maven 3.8 o superior (`java -version` y `mvn -version` para
comprobarlo). La primera vez Maven descarga H2 y JUnit, por lo que necesita internet.

```bash
mvn compile exec:java@aplicacion   # ejecuta el programa con base de datos (menú de consola)
mvn test                           # ejecuta las pruebas unitarias
mvn javadoc:javadoc                # genera el JavaDoc en target/site/apidocs
mvn compile exec:java              # demostración en memoria de la primera entrega (Main)
```

También se puede ejecutar `Aplicacion.java` directamente desde el IDE.

## 9. Pruebas

Hay 110 pruebas unitarias con JUnit 5: las 67 de la primera entrega (sin modificar) y 43 nuevas. Las de
persistencia y las de la consola usan una base de datos H2 **en memoria** (`jdbc:h2:mem:...`), distinta para
cada prueba y que desaparece al terminar; por eso no tocan los datos reales de `datos/`.

| Clase de prueba | Pruebas | Qué comprueba |
|-----------------|--------:|---------------|
| `HabitacionTest` | 10 | Disponibilidad por fechas, reservas seguidas, mantenimiento, cambios de estado |
| `ReservaTest` | 11 | Total, servicios, tarifa congelada, confirmar/cancelar, pagos y saldo |
| `PagoTest` | 5 | Pago procesado, comprobante, numeración |
| `ServicioAdicionalTest` | 2 | Subtotal y validaciones |
| `EmpleadoTest` | 12 | Inicio de sesión, permisos por cargo, check-in y check-out |
| `ClienteTest` | 9 | Historial, solicitar reserva, datos de contacto |
| `HotelTest` | 18 | Registro, sobreventa, cancelación, reportes |
| **Subtotal primera entrega** | **67** | |
| `HotelPersistenteTest` | 5 | La lista de clientes y que siga funcionando como un `Hotel` normal |
| `EmpleadoConClaveTest` | 8 | Contraseña, hash y sal, sesión, permisos, empleado restaurado, la clave interna no sirve |
| `RepositorioH2Test` | 12 | Guardar y cargar todo, casos difíciles de reconstruir, sobreventa tras cargar, login tras cargar, rollback si falla |
| `ConsolaTest` | 18 | Programa completo: login, flujo de reserva, errores, roles y que los datos sobrevivan al reinicio |
| **Subtotal nuevas** | **43** | |
| **Total** | **110** | |

Casos difíciles que prueba `RepositorioH2Test`: una reserva finalizada con código mayor pero fechas anteriores a
una en curso en la misma habitación, una reserva creada en línea (sin empleado) a la que luego un empleado le
hace el check-in, una tarifa de habitación que cambió después de reservar, una habitación en mantenimiento, y
guardar varias veces o cargar-guardar-cargar sin que cambie nada.

En `ConsolaTest` se le "escribe" al menú una lista de respuestas (por ejemplo `admin`, `admin123`, `14`, ...) y se
comprueba lo que muestra y lo que quedó guardado. Así se prueba el sistema completo, no solo clases sueltas.

Resultado esperado de `mvn test`: `Tests run: 110, Failures: 0, Errors: 0`.

## 10. Decisiones y limitaciones

- **Las clases anteriores no se modificaron.** A cambio, la base de datos necesita las dos subclases de la
  sección 7 y vuelve a armar el hotel repitiendo operaciones. Una alternativa más directa sería agregar a
  `Hotel`, `Reserva` y `Empleado` unos métodos para restaurar sus datos; no se hizo para respetar la entrega
  anterior.
- **Se guarda todo el hotel en cada cambio.** Es lo más simple y siempre deja los datos coherentes,
  pero con muchísimas reservas (decenas de miles) cada operación y cada arranque tardarían más. Una mejora
  futura sería guardar solo lo que cambió (INSERT/UPDATE por entidad).
- **Los empleados deben ser `EmpleadoConClave`** y el hotel un `HotelPersistente`; si no, no se pueden guardar.
- **No se pueden borrar clientes** (el programa no ofrece esa función); la recarga de ids depende de eso.
- **Un solo programa a la vez.** El archivo H2 se bloquea mientras el programa está abierto; no hay
  acceso simultáneo de varios usuarios (el requisito RNF1 de 20 usuarios concurrentes no está cubierto).
- **Contraseñas:** SHA-256 con sal es suficiente para este proyecto; en producción se usaría
  bcrypt o Argon2. Tampoco hay opción para cambiar contraseñas desde el menú.
- **Fechas:** el sistema no impide reservar en fechas pasadas ni valida que el check-in sea el día
  de entrada.
- **Pagos:** no se conecta con una pasarela de pagos real; `Pago.procesar()` es el punto donde se
  integraría. Al cancelar una reserva no se calculan reembolsos.
- **Nombre del hotel:** se crea como "Hotel Central" y no se puede cambiar desde el menú.

## 11. Problemas frecuentes

| Síntoma | Causa y solución |
|---------|------------------|
| `Database may be already in use` | Hay otro programa (u otra ventana) con el archivo abierto. Ciérrelo e intente de nuevo. |
| `No se pudieron crear las tablas` | Falta la librería H2 o no hay permisos de escritura en la carpeta. Ejecute con Maven (`mvn compile exec:java@aplicacion`). |
| Símbolos raros en lugar de tildes (Windows) | Ejecute `chcp 65001` en la terminal antes de iniciar el programa. |
| Quiero empezar de cero | Cierre el programa y borre la carpeta `datos/`. |
| `mvn` no se reconoce | Maven no está instalado o no está en el PATH. |

## 12. JavaDoc

Todas las clases y métodos públicos están documentados. La documentación generada está en
`docs/index.html`. Para volver a generarla: `mvn javadoc:javadoc`.
