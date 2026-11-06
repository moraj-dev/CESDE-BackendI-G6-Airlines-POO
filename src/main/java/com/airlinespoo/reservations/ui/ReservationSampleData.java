package com.airlinespoo.reservations.ui;

import com.airlinespoo.core.util.IdGenerator;
import com.airlinespoo.reservations.model.Seat;
import com.airlinespoo.reservations.service.ReservationService;
import com.airlinespoo.reservations.service.ReservationService.PassengerRequest;

import java.util.List;

/**
 * Cargador de datos de ejemplo para el módulo de Reservas y Pasajeros.
 *
 * <p>Carga 4 reservas de muestra con entre 1 y 3 pasajeros cada una, garantizando que no
 * existan sillas repetidas dentro del mismo vuelo.</p>
 */
public final class ReservationSampleData {

    private ReservationSampleData() {
        // Clase utilitaria de carga inicial
    }

    /**
     * Carga 4 reservas de prueba en el servicio y sincroniza los generadores de ids.
     *
     * @param service          servicio de reservas
     * @param resIdGenerator   generador de ids de reservas
     * @param detailIdGenerator generador de ids de detalles
     */
    public static void load(ReservationService service, IdGenerator resIdGenerator, IdGenerator detailIdGenerator) {
        // Vuelo 101 (por ej. BOG-MDE)
        service.createReservation(
                "CLI-1001",
                101,
                List.of(
                        new PassengerRequest("Carlos Mendoza", "1020304050", new Seat("1", 1)),
                        new PassengerRequest("Laura Gómez", "1020304051", new Seat("1", 2))
                )
        );

        service.createReservation(
                "CLI-1002",
                101,
                List.of(
                        new PassengerRequest("Andrés Restrepo", "71234567", new Seat("2", 1))
                )
        );

        // Vuelo 202 (por ej. MDE-CTG)
        service.createReservation(
                "CLI-1003",
                202,
                List.of(
                        new PassengerRequest("Sofía Vergara", "43000111", new Seat("12", 2)),
                        new PassengerRequest("Mateo Londoño", "100200300", new Seat("12", 3)),
                        new PassengerRequest("Camila Duque", "98765432", new Seat("14", 1))
                )
        );

        // Vuelo 303 (por ej. CLO-BOG)
        service.createReservation(
                "CLI-1001",
                303,
                List.of(
                        new PassengerRequest("Carlos Mendoza", "1020304050", new Seat("5", 1))
                )
        );
    }
}
