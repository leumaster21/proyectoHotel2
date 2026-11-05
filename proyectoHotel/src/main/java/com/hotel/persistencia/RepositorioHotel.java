package com.hotel.persistencia;

/**
 * Contrato para guardar y cargar el hotel. La aplicación solo conoce esta interfaz;
 * la implementación real con base de datos es {@link RepositorioH2}.
 * Tener una interfaz permite probar la consola sin necesidad de una base de datos.
 */
public interface RepositorioHotel {

    /**
     * Carga el hotel completo desde el almacenamiento.
     *
     * @return el hotel con todos sus datos, o {@code null} si todavía no hay nada guardado
     */
    HotelPersistente cargar();

    /**
     * Guarda el estado actual del hotel completo (habitaciones, clientes, empleados,
     * reservas, servicios y pagos).
     *
     * @param hotel hotel a guardar; todos sus empleados deben ser {@link EmpleadoConClave}
     */
    void guardar(HotelPersistente hotel);
}
