# Manual de usuario — Sistema de Gestión de Hotel

## 1. ¿Para qué sirve?

El sistema permite al hotel llevar en un solo lugar sus habitaciones, clientes y reservas. Con él se puede:

- Registrar clientes y habitaciones.
- Consultar qué habitaciones están libres en unas fechas.
- Crear, confirmar y cancelar reservas, sin que se vendan dos reservas para la misma habitación y fecha.
- Hacer el check-in y el check-out de los huéspedes.
- Agregar servicios extra (desayuno, lavandería, spa) y registrar pagos con su comprobante.
- Ver un reporte de ocupación e ingresos.

Todo lo que se hace **queda guardado**: al cerrar y volver a abrir el programa, los datos siguen ahí.

## 2. Qué necesita para usarlo

- Java 17 o superior instalado.
- Maven instalado (o un editor como IntelliJ, VS Code o NetBeans con el proyecto abierto).
- Una terminal (CMD, PowerShell, Terminal de macOS o de Linux).

## 3. Cómo iniciar el programa

1. Abra una terminal dentro de la carpeta del proyecto (la que contiene el archivo `pom.xml`).
2. Escriba:

   ```
   mvn compile exec:java@aplicacion
   ```

3. Espere unos segundos. Aparecerá la pantalla de ingreso:

   ```
   === Hotel Central - Sistema de gestión ===
   Usuario:
   ```

Si usa un editor (IDE), abra el archivo `Aplicacion.java` y presione **Run**.

> **Windows:** si ve símbolos extraños en lugar de tildes, escriba `chcp 65001` en la terminal y vuelva a iniciar.

## 4. Ingresar al sistema

La **primera vez** que se ejecuta, el sistema crea dos usuarios:

| Usuario | Contraseña | Cargo |
|---------|------------|-------|
| `admin` | `admin123` | Administrador |
| `recepcion` | `recep123` | Recepcionista |

Escriba el usuario, presione Enter, escriba la contraseña y presione Enter. Tiene 3 intentos; si
falla las tres veces, el programa se cierra.

**Qué puede hacer cada cargo:**

| Opción | Recepcionista | Administrador |
|--------|:-------------:|:-------------:|
| Clientes, habitaciones, reservas, servicios y pagos (opciones 1 a 13) | Sí | Sí |
| Check-in y check-out (opciones 8 y 9) | Sí | No |
| Agregar habitaciones, cambiar tarifas, mantenimiento, registrar empleados y reportes (opciones 14 a 18) | No | Sí |

Si usa una opción que no le corresponde, el sistema muestra un mensaje de error y no pasa nada más.

## 5. El menú

Después de ingresar aparece el menú. Se elige escribiendo el **número** de la opción y presionando Enter.

```
----------- MENÚ -----------
 1. Registrar cliente
 2. Listar clientes
 3. Ver habitaciones
 4. Buscar habitaciones disponibles
 5. Crear reserva
 6. Confirmar reserva
 7. Cancelar reserva
 8. Check-in
 9. Check-out
10. Agregar servicio a una reserva
11. Registrar pago
12. Ver una reserva
13. Listar reservas
--- Solo administrador ---
14. Agregar habitación
15. Cambiar tarifa de una habitación
16. Poner/quitar habitación de mantenimiento
17. Registrar empleado
18. Reporte de ocupación e ingresos
 0. Salir
```

**Reglas para escribir los datos:**
- Las **fechas** se escriben así: `AAAA-MM-DD`, por ejemplo `2026-11-10`.
- Los **valores de dinero** se escriben con punto para los decimales, por ejemplo `80.50`.
- Los **códigos de reserva** se ven así: `RES-0001`.
- Cuando se pide elegir entre varias opciones (tipo de habitación, método de pago), se escribe el número de la lista.
- Si escribe algo mal, el sistema lo avisa y vuelve a preguntar.

### Descripción de cada opción

| N.º | Opción | Qué le pide | Qué hace |
|----:|--------|-------------|----------|
| 1 | Registrar cliente | Nombre, documento, teléfono y correo (se pueden omitir con Enter), nacionalidad | Crea el cliente y le asigna un número |
| 2 | Listar clientes | Nada | Muestra todos los clientes |
| 3 | Ver habitaciones | Nada | Muestra número, tipo, capacidad, tarifa y estado de cada habitación |
| 4 | Buscar habitaciones disponibles | Fecha de entrada y de salida | Muestra las habitaciones libres en esas fechas |
| 5 | Crear reserva | Documento del cliente, número de habitación, fechas | Crea la reserva (queda **PENDIENTE**) y muestra el total |
| 6 | Confirmar reserva | Código de la reserva | La pasa a **CONFIRMADA** |
| 7 | Cancelar reserva | Código de la reserva | La pasa a **CANCELADA** y libera la habitación |
| 8 | Check-in | Código de la reserva | Llegada del huésped: la reserva queda **EN_CURSO** y la habitación **OCUPADA** |
| 9 | Check-out | Código de la reserva | Salida del huésped: la reserva queda **FINALIZADA** y la habitación **DISPONIBLE** (la cuenta debe estar pagada) |
| 10 | Agregar servicio | Código, nombre del servicio, precio por unidad, cantidad | Suma el servicio al total de la reserva |
| 11 | Registrar pago | Código, monto y método (efectivo, tarjeta o transferencia) | Registra el pago y muestra el comprobante |
| 12 | Ver una reserva | Código de la reserva | Muestra cliente, habitación, fechas, servicios, pagos, total y saldo |
| 13 | Listar reservas | Nada | Muestra todas las reservas con su estado |
| 14 | Agregar habitación | Número, tipo, capacidad, tarifa por noche | Crea la habitación (solo administrador) |
| 15 | Cambiar tarifa | Número de habitación y nueva tarifa | Cambia la tarifa. Las reservas ya hechas **conservan** la tarifa anterior |
| 16 | Mantenimiento | Número de habitación y nuevo estado | Pone la habitación en mantenimiento (no se puede reservar) o la vuelve a dejar disponible |
| 17 | Registrar empleado | Nombre, documento, cargo, usuario, contraseña (mínimo 6 caracteres) | Crea un usuario nuevo |
| 18 | Reporte | Fecha inicial y final | Muestra noches ocupadas, porcentaje de ocupación, reservas e ingresos del periodo |
| 0 | Salir | Nada | Cierra la sesión y el programa |

## 6. Guías paso a paso

### 6.1 Preparar el hotel (administrador, una sola vez)

Entre como `admin` y agregue las habitaciones con la opción **14**:

```
Elija una opción: 14
Número de habitación: 102
Tipo de habitación:
  1. SENCILLA
  2. DOBLE
  3. SUITE
Elija un número: 2
Capacidad (huéspedes): 2
Tarifa por noche: 120
Habitación 102 agregada.
```

Repita para cada habitación. Con la opción **3** puede ver la lista completa:

```
Habitación 101 | SENCILLA | capacidad 1 | tarifa 80.00 | DISPONIBLE
Habitación 102 | DOBLE | capacidad 2 | tarifa 120.00 | DISPONIBLE
Habitación 201 | SUITE | capacidad 4 | tarifa 250.00 | DISPONIBLE
```

### 6.2 Atender una reserva completa (recepcionista)

Entre como `recepcion`.

**Paso 1 — Registrar al cliente** (opción 1), si todavía no existe:

```
Nombre completo: Ana Pérez
Documento: 5566
Teléfono (Enter para omitir): 3001112233
Correo (Enter para omitir): ana@correo.com
Nacionalidad: Colombiana
Cliente registrado con id 1.
```

**Paso 2 — Ver qué hay libre** (opción 4):

```
Fecha de entrada (AAAA-MM-DD): 2026-11-10
Fecha de salida (AAAA-MM-DD): 2026-11-13
Habitación 101 | SENCILLA | capacidad 1 | tarifa 80.00
Habitación 102 | DOBLE | capacidad 2 | tarifa 120.00
Habitación 201 | SUITE | capacidad 4 | tarifa 250.00
```

**Paso 3 — Crear la reserva** (opción 5):

```
Documento del cliente: 5566
Número de habitación: 102
Fecha de entrada (AAAA-MM-DD): 2026-11-10
Fecha de salida (AAAA-MM-DD): 2026-11-13
Reserva creada: RES-0001 (3 noches, total 360.00). Estado: PENDIENTE
```

Anote el código (`RES-0001`): lo necesitará en los pasos siguientes.

**Paso 4 — Confirmar** (opción 6): escriba el código. Muestra `Reserva RES-0001 confirmada.`

**Paso 5 — Check-in cuando llega el huésped** (opción 8): escriba el código. Muestra
`Check-in realizado. Habitación 102 ocupada.`

**Paso 6 — Agregar servicios** (opción 10):

```
Código de la reserva (ej. RES-0001): RES-0001
Nombre del servicio (ej. Desayuno): Desayuno
Precio por unidad: 15
Cantidad: 3
Servicio agregado. Nuevo total: 405.00
```

**Paso 7 — Cobrar** (opción 11). Puede pagar todo de una vez o en partes:

```
Código de la reserva (ej. RES-0001): RES-0001
Total: 405.00 | Pagado: 0.00 | Saldo: 405.00
Monto a pagar: 100
Método de pago:
  1. EFECTIVO
  2. TARJETA
  3. TRANSFERENCIA
Elija un número: 2

COMPROBANTE DE PAGO RES-0001-P1
Cliente: Ana Pérez
Habitación: 102
Monto: 100.00
Método: TARJETA
Fecha: 2026-11-04
Saldo pendiente: 305.00
```

Repita hasta que el saldo sea `0.00`.

**Paso 8 — Check-out** (opción 9) cuando el huésped se va. El sistema solo lo permite si el saldo
es 0. Muestra `Check-out realizado. Habitación 102 disponible.`

Puede revisar toda la cuenta en cualquier momento con la opción **12**:

```
Reserva RES-0001 - EN_CURSO
Cliente: Ana Pérez (doc. 5566)
Habitación: 102 DOBLE
Fechas: 2026-11-10 a 2026-11-13 (3 noches)
Tarifa por noche: 120.00
  Servicio: Desayuno x3 = 45.00
  Pago 1: 100.00 TARJETA (2026-11-04)
Total: 405.00 | Pagado: 100.00 | Saldo: 305.00
```

### 6.3 Cancelar una reserva

Use la opción **7** con el código. Solo se pueden cancelar reservas que todavía no tienen check-in.
Al cancelar, la habitación queda libre otra vez para esas fechas.

### 6.4 Cuando una habitación ya está reservada

Si intenta reservar una habitación en fechas que se cruzan con otra reserva, el sistema lo impide:

```
Error: La habitación 101 no está disponible entre 2026-11-12 y 2026-11-14
```

Use la opción 4 para ver qué otras habitaciones están libres. Un huésped puede **entrar el mismo día en
que otro sale** (la noche de salida queda libre).

### 6.5 Ver el reporte (administrador)

Opción **18**, con la fecha inicial y la final (ambas se incluyen):

```
REPORTE DE OCUPACIÓN 2026-11-01 a 2026-11-30
Habitaciones: 3
Noches ocupadas / disponibles: 3 / 90
Ocupación: 3.33%
Reservas consideradas: 1
Ingresos: 100.00
```

Los **ingresos** suman los pagos registrados en esas fechas. La **ocupación** cuenta las noches de las
reservas que no están canceladas.

### 6.6 Poner una habitación en mantenimiento (administrador)

Opción **16**, escriba el número de la habitación y elija `1` (DISPONIBLE) o `2` (MANTENIMIENTO).
Una habitación en mantenimiento no aparece en las búsquedas ni se puede reservar. No se puede poner
en mantenimiento una habitación ocupada.

## 7. Mensajes de error frecuentes

| Mensaje | Qué significa | Qué hacer |
|---------|---------------|-----------|
| `Usuario o contraseña incorrectos.` | Datos de ingreso equivocados | Revise mayúsculas y vuelva a escribir |
| `Esta opción es solo para el administrador` | Su usuario es recepcionista | Pida al administrador que lo haga |
| `Solo un recepcionista puede registrar check-in y check-out` | Intentó hacer check-in con el usuario administrador | Entre con un usuario recepcionista |
| `No existe la reserva RES-xxxx` | El código está mal escrito | Use la opción 13 para ver los códigos |
| `No existe un cliente con el documento ...` | El cliente no está registrado | Regístrelo con la opción 1 |
| `La habitación N no está disponible entre ...` | Ya hay una reserva en esas fechas | Elija otra habitación u otras fechas |
| `No se puede hacer check-out: saldo pendiente de ...` | La cuenta no está pagada | Registre el pago (opción 11) |
| `El pago (...) excede el saldo pendiente (...)` | Intentó pagar más de lo que se debe | Escriba un monto menor o igual al saldo |
| `No se puede confirmar / cancelar / hacer check-in una reserva en estado ...` | La reserva no está en el estado correcto para esa acción | Revise el estado con la opción 12 |
| `Ya existe un cliente con el documento ...` | Cliente repetido | Búsquelo con la opción 2 |
| `Fecha no válida. Use el formato AAAA-MM-DD` | La fecha está mal escrita | Escríbala como `2026-11-10` |
| `Dato inválido: El correo electrónico no es válido` | El correo no tiene `@` | Corrija el correo |

## 8. Dónde se guardan los datos

Los datos se guardan automáticamente en la carpeta `datos/` del proyecto (archivo `hotel.mv.db`).

- **Copia de seguridad:** cierre el programa y copie la carpeta `datos/` a un lugar seguro. Para
  restaurarla, reemplace la carpeta con la copia.
- **Empezar de cero:** cierre el programa y borre la carpeta `datos/`. Al iniciar de nuevo se crearán los
  usuarios por defecto y se perderán todos los datos.
- **No abra el programa dos veces a la vez:** el archivo queda bloqueado por la primera ventana.

## 9. Preguntas frecuentes

**¿Puedo cambiar mi contraseña?** Todavía no existe esa opción. El administrador puede registrar un
usuario nuevo con la opción 17.

**¿Cómo salgo del programa?** Con la opción `0`. Los datos ya están guardados desde cada operación.

**Escribí mal el número de una opción, ¿se daña algo?** No. El sistema dice `Opción no válida` y
muestra el menú de nuevo.

**¿Puedo borrar un cliente o una habitación?** No, por ahora el sistema no tiene esa función. Una
habitación se puede poner en mantenimiento para que no se use.

**¿Puedo reservar en una fecha pasada?** El sistema no lo impide; revise bien las fechas antes de confirmar.
