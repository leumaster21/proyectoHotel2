/**
 * Capa de persistencia: guarda y carga el hotel en una base de datos H2 mediante JDBC.
 * La aplicación solo usa la interfaz {@link com.hotel.persistencia.RepositorioHotel};
 * {@link com.hotel.persistencia.RepositorioH2} es la implementación con base de datos.
 *
 * <p>Las clases de la primera entrega (Hotel, Empleado, Reserva, ...) no se modificaron.
 * Para poder guardarlas se agregaron dos clases que las extienden:
 * {@link com.hotel.persistencia.HotelPersistente} y
 * {@link com.hotel.persistencia.EmpleadoConClave}.</p>
 */
package com.hotel.persistencia;
