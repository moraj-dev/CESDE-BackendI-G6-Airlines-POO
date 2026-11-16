package com.airlinespoo.reservations.ui;

import com.airlinespoo.reservations.model.Seat;
import com.airlinespoo.reservations.service.ReservationService;
import com.airlinespoo.reservations.service.ReservationService.PassengerRequest;

import java.util.List;

/**
 * Cargador de datos de ejemplo para el módulo de Reservas y Pasajeros.
 *
 * <p>Utiliza los clientes cargados por {@code CustomerSampleData} en el Módulo 1
 * (documentos 101010, 202020, 303030) con 4 reservas de 1 a 3 pasajeros cada una.</p>
 */
public final class ReservationSampleData {

    private ReservationSampleData() {
        // Clase utilitaria de carga inicial
    }

    /**
     * Carga 4 reservas de prueba en el servicio.
     *
     * @param service servicio de reservas
     */
    public static void load(ReservationService service) {
        // Reserva 1: Cliente 101010 (Ana Pérez) en Vuelo 101 (2 pasajeros)
        service.createReservation(
                "101010",
                101,
                List.of(
                        new PassengerRequest("Ana Pérez", "101010", new Seat("1", 1)),
                        new PassengerRequest("Carlos Pérez", "1020304051", new Seat("1", 2))
                )
        );

        // Reserva 2: Cliente 202020 (Luis Gómez) en Vuelo 101 (1 pasajero)
        service.createReservation(
                "202020",
                101,
                List.of(
                        new PassengerRequest("Luis Gómez", "202020", new Seat("2", 1))
                )
        );

        // Reserva 3: Cliente 303030 (María López) en Vuelo 202 (3 pasajeros)
        service.createReservation(
                "303030",
                202,
                List.of(
                        new PassengerRequest("María López", "303030", new Seat("12", 2)),
                        new PassengerRequest("Mateo Londoño", "100200300", new Seat("12", 3)),
                        new PassengerRequest("Camila Duque", "98765432", new Seat("14", 1))
                )
        );

        // Reserva 4: Cliente 101010 (Ana Pérez) en Vuelo 303 (1 pasajero)
        service.createReservation(
                "101010",
                303,
                List.of(
                        new PassengerRequest("Ana Pérez", "101010", new Seat("5", 1))
                )
        );
    }
}
