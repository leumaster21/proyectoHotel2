package com.hotel.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Resultado inmutable de {@link Hotel#generarReporte(LocalDate, LocalDate)}
 * (requisito RF7). Es un <em>resultado de operación</em>, no una entidad, según el
 * análisis de sustantivos.
 *
 * @param inicio              primer día del periodo (inclusive)
 * @param fin                 último día del periodo (inclusive)
 * @param totalHabitaciones   habitaciones que tiene el hotel
 * @param nochesDisponibles   noches-habitación disponibles en el periodo
 *                            (habitaciones &times; días)
 * @param nochesOcupadas      noches-habitación ocupadas por reservas no canceladas
 * @param reservasConsideradas reservas no canceladas que tienen al menos una noche en el periodo
 * @param ingresos            suma de los pagos procesados con fecha dentro del periodo
 */
public record ReporteOcupacion(LocalDate inicio, LocalDate fin, int totalHabitaciones,
                               long nochesDisponibles, long nochesOcupadas,
                               int reservasConsideradas, BigDecimal ingresos) {

    /**
     * Calcula el porcentaje de ocupación del periodo.
     *
     * @return noches ocupadas / noches disponibles &times; 100, con dos decimales;
     *         {@code 0.00} si no había noches disponibles
     */
    public BigDecimal porcentajeOcupacion() {
        if (nochesDisponibles == 0) {
            return Montos.CERO;
        }
        return BigDecimal.valueOf(nochesOcupadas)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(nochesDisponibles), 2, RoundingMode.HALF_UP);
    }

    /**
     * Devuelve el reporte en formato de texto legible.
     *
     * @return resumen de varias líneas
     */
    public String resumen() {
        return String.format(
                "REPORTE DE OCUPACIÓN %s a %s%n"
                        + "Habitaciones: %d%n"
                        + "Noches ocupadas / disponibles: %d / %d%n"
                        + "Ocupación: %s%%%n"
                        + "Reservas consideradas: %d%n"
                        + "Ingresos: %s",
                inicio, fin, totalHabitaciones, nochesOcupadas, nochesDisponibles,
                porcentajeOcupacion().toPlainString(), reservasConsideradas, ingresos.toPlainString());
    }
}
