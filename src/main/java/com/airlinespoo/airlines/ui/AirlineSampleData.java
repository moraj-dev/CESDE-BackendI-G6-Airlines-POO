package com.airlinespoo.airlines.ui;

import com.airlinespoo.airlines.service.AirlineService;
import com.airlinespoo.core.util.Validator;

/**
 * Datos de ejemplo del Módulo 3 (Aerolíneas).
 *
 * <p>Carga 3 aerolíneas iniciales y sincroniza el generador de ids para que los
 * registros posteriores no repitan identificadores.</p>
 */
public final class AirlineSampleData {

    private AirlineSampleData() {
        // Clase utilitaria: no se instancia.
    }

    /**
     * Carga las aerolíneas de muestra si aún no hay registros en el servicio.
     *
     * @param service servicio de aerolíneas donde se cargan los datos
     */
    public static void load(AirlineService service) {
        Validator.requireNotNull(service, "servicio de aerolíneas");
        if (service.countAirlines() > 0) {
            service.syncIdGenerator();
            return;
        }
        service.createAirline("Avianca");
        service.createAirline("LATAM Airlines");
        service.createAirline("Wingo");
        service.syncIdGenerator();
    }
}
