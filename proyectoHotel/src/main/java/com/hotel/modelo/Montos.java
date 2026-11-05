package com.hotel.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utilidades internas para manejar dinero con {@link BigDecimal}.
 * Todos los montos del sistema se normalizan a dos decimales.
 */
final class Montos {

    /** Cero con dos decimales. */
    static final BigDecimal CERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private Montos() {
    }

    /**
     * Redondea un monto a dos decimales (HALF_UP).
     *
     * @param valor monto a normalizar
     * @return monto con escala 2
     */
    static BigDecimal normalizar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
