package com.hotel.persistencia;

import com.hotel.modelo.Cliente;
import com.hotel.modelo.Hotel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Hotel que además recuerda la lista de sus clientes.
 *
 * <p>La clase {@link Hotel} original guarda los clientes, pero no ofrece ningún método
 * para listarlos todos, y la base de datos necesita esa lista para guardarlos.
 * En lugar de modificar {@code Hotel}, esta clase hereda de ella y anota cada cliente
 * que se registra.</p>
 */
public class HotelPersistente extends Hotel {

    private final List<Cliente> clientes = new ArrayList<>();

    /**
     * Crea un hotel vacío.
     *
     * @param nombre    nombre del hotel
     * @param direccion dirección
     * @param telefono  teléfono
     */
    public HotelPersistente(String nombre, String direccion, String telefono) {
        super(nombre, direccion, telefono);
    }

    /**
     * Registra un cliente igual que {@link Hotel#registrarCliente} y lo agrega a la lista.
     *
     * @param nombre       nombre completo
     * @param documento    documento de identidad
     * @param telefono     teléfono (opcional)
     * @param email        correo (opcional)
     * @param nacionalidad nacionalidad
     * @return el cliente creado
     */
    @Override
    public Cliente registrarCliente(String nombre, String documento, String telefono,
                                    String email, String nacionalidad) {
        Cliente cliente = super.registrarCliente(nombre, documento, telefono, email, nacionalidad);
        clientes.add(cliente);
        return cliente;
    }

    /**
     * Devuelve todos los clientes registrados, en orden de registro.
     *
     * @return lista de solo lectura
     */
    public List<Cliente> getClientes() {
        return Collections.unmodifiableList(clientes);
    }
}
