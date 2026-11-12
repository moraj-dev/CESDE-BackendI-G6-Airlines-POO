package com.airlinespoo.locations.ui;

import com.airlinespoo.locations.model.City;
import com.airlinespoo.locations.service.LocationService;

/**
 * Carga los datos de ejemplo del Módulo 2: 5 ciudades y 6 aeropuertos
 * (Bogotá tiene dos). El Módulo 4 los usa para sus vuelos de prueba.
 *
 * <p>Los datos entran por el servicio, así que pasan por las mismas reglas que los del
 * usuario y el generador de ids avanza solo: no hace falta sincronizarlo.</p>
 */
public final class LocationSampleData {

    private LocationSampleData() {
        // Clase utilitaria: no se instancia.
    }

    /**
     * Registra las ciudades y aeropuertos de ejemplo.
     *
     * @param service servicio de ubicaciones donde se cargan los datos
     */
    public static void load(LocationService service) {
        City bogota = service.createCity("Bogotá");
        City medellin = service.createCity("Medellín");
        City rionegro = service.createCity("Rionegro");
        City cali = service.createCity("Cali");
        City cartagena = service.createCity("Cartagena");

        service.createAirport("Aeropuerto Internacional El Dorado", bogota.getId());
        service.createAirport("Aeropuerto Guaymaral", bogota.getId());
        service.createAirport("Aeropuerto Olaya Herrera", medellin.getId());
        service.createAirport("Aeropuerto Internacional José María Córdova", rionegro.getId());
        service.createAirport("Aeropuerto Internacional Alfonso Bonilla Aragón", cali.getId());
        service.createAirport("Aeropuerto Internacional Rafael Núñez", cartagena.getId());
    }
}
