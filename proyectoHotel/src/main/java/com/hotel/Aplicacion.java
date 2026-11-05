package com.hotel;

import com.hotel.persistencia.RepositorioH2;
import com.hotel.persistencia.RepositorioHotel;

/**
 * Programa principal del Sistema de Gestión de Hotel con base de datos. Abre la base de
 * datos H2 (archivo {@code datos/hotel.mv.db}) y arranca el menú de consola.
 *
 * <p>La clase {@link Main} de la primera entrega no se modificó; sigue siendo una
 * demostración en memoria, sin base de datos.</p>
 */
public final class Aplicacion {

    private Aplicacion() {
    }

    /**
     * Inicia la aplicación.
     *
     * @param args no se usan
     */
    public static void main(String[] args) {
        RepositorioHotel repositorio = new RepositorioH2(RepositorioH2.URL_ARCHIVO);
        Consola consola = new Consola(repositorio, System.in, System.out);
        consola.iniciar();
    }
}
