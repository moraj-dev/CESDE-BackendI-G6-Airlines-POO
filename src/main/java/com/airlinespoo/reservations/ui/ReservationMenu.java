package com.airlinespoo.reservations.ui;

import com.airlinespoo.core.exception.AirlinesException;
import com.airlinespoo.core.ui.ConsoleInput;
import com.airlinespoo.core.ui.Menu;
import com.airlinespoo.reservations.model.ManifestEntry;
import com.airlinespoo.reservations.model.Reservation;
import com.airlinespoo.reservations.model.ReservationDetail;
import com.airlinespoo.reservations.model.Seat;
import com.airlinespoo.reservations.service.ReservationService;
import com.airlinespoo.reservations.service.ReservationService.PassengerRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Menú de interacción en consola para el módulo de Reservas y Pasajeros.
 *
 * <p>Implementa {@link Menu} y canaliza todas las entradas de usuario a través de
 * {@link ConsoleInput}, capturando las excepciones de la aplicación y presentando respuestas
 * estructuradas y tablas formateadas para los manifiestos de vuelo.</p>
 */
public class ReservationMenu implements Menu {

    private final ReservationService reservationService;
    private final ConsoleInput input;

    /**
     * Construye el menú de reservas.
     *
     * @param reservationService servicio de negocio de reservas
     * @param input              lector de consola compartido
     */
    public ReservationMenu(ReservationService reservationService, ConsoleInput input) {
        this.reservationService = reservationService;
        this.input = input;
    }

    @Override
    public String title() {
        return "Gestión de Reservas y Pasajeros";
    }

    @Override
    public void run() {
        boolean exit = false;
        while (!exit) {
            System.out.println();
            System.out.println("=================================================");
            System.out.println("       MÓDULO DE RESERVAS Y PASAJEROS            ");
            System.out.println("=================================================");
            System.out.println("1. Registrar nueva reserva");
            System.out.println("2. Agregar pasajero a una reserva");
            System.out.println("3. Quitar pasajero de una reserva");
            System.out.println("4. Cambiar silla de un pasajero");
            System.out.println("5. Cancelar reserva completa");
            System.out.println("6. Consultar reservas por cliente");
            System.out.println("7. Listar todas las reservas");
            System.out.println("8. Ver sillas ocupadas de un vuelo");
            System.out.println("9. Ver manifiesto de pasajeros de un vuelo");
            System.out.println("0. Volver");
            System.out.println("=================================================");

            int option = input.readInt("Seleccione una opción", 0, 9);
            switch (option) {
                case 1 -> createReservationFlow();
                case 2 -> addPassengerFlow();
                case 3 -> removePassengerFlow();
                case 4 -> changeSeatFlow();
                case 5 -> cancelReservationFlow();
                case 6 -> listByCustomerFlow();
                case 7 -> listAllReservationsFlow();
                case 8 -> listOccupiedSeatsFlow();
                case 9 -> showFlightManifestFlow();
                case 0 -> exit = true;
                default -> System.out.println("Opción no válida.");
            }
        }
    }

    private void createReservationFlow() {
        System.out.println("\n--- REGISTRAR NUEVA RESERVA ---");
        try {
            String customer = input.readRequiredText("Identificador o documento del cliente titular");
            int flightId = input.readInt("Identificador numérico del vuelo", 1, 999999);

            int passengerCount = input.readInt("Cantidad de pasajeros a registrar", 1, Reservation.MAX_PASSENGERS);
            List<PassengerRequest> passengerRequests = new ArrayList<>();

            for (int i = 1; i <= passengerCount; i++) {
                System.out.printf("%n[Datos del Pasajero %d de %d]%n", i, passengerCount);
                String fullName = input.readRequiredText("Nombre completo");
                String document = input.readRequiredText("Documento de identidad");
                Seat seat = readSeatFromConsole();
                passengerRequests.add(new PassengerRequest(fullName, document, seat));
            }

            Reservation reservation = reservationService.createReservation(customer, flightId, passengerRequests);
            System.out.println("\n¡Reserva registrada con éxito!");
            printReservationSummary(reservation);
        } catch (AirlinesException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
        input.pause();
    }

    private void addPassengerFlow() {
        System.out.println("\n--- AGREGAR PASAJERO A RESERVA ---");
        try {
            int reservationId = input.readInt("Identificador de la reserva", 1, 999999);
            String fullName = input.readRequiredText("Nombre completo del pasajero");
            String document = input.readRequiredText("Documento de identidad");
            Seat seat = readSeatFromConsole();

            ReservationDetail detail = reservationService.addPassenger(reservationId, fullName, document, seat);
            System.out.println("\n¡Pasajero agregado con éxito!");
            System.out.println("Detalle generado: " + detail);
        } catch (AirlinesException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
        input.pause();
    }

    private void removePassengerFlow() {
        System.out.println("\n--- QUITAR PASAJERO DE RESERVA ---");
        try {
            int reservationId = input.readInt("Identificador de la reserva", 1, 999999);
            String document = input.readRequiredText("Documento del pasajero a retirar");

            if (input.confirm("¿Está seguro de remover a este pasajero de la reserva?")) {
                reservationService.removePassenger(reservationId, document);
                System.out.println("Pasajero retirado exitosamente de la reserva.");
            } else {
                System.out.println("Operación cancelada.");
            }
        } catch (AirlinesException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
        input.pause();
    }

    private void changeSeatFlow() {
        System.out.println("\n--- CAMBIAR SILLA DE PASAJERO ---");
        try {
            int reservationId = input.readInt("Identificador de la reserva", 1, 999999);
            String document = input.readRequiredText("Documento del pasajero");
            System.out.println("Ingrese la nueva silla:");
            Seat newSeat = readSeatFromConsole();

            reservationService.changeSeat(reservationId, document, newSeat);
            System.out.println("¡Silla actualizada con éxito a: " + newSeat + "!");
        } catch (AirlinesException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
        input.pause();
    }

    private void cancelReservationFlow() {
        System.out.println("\n--- CANCELAR RESERVA COMPLETA ---");
        try {
            int reservationId = input.readInt("Identificador de la reserva a cancelar", 1, 999999);
            Reservation reservation = reservationService.getReservation(reservationId);
            printReservationSummary(reservation);

            if (input.confirm("¿Confirma la cancelación total de esta reserva y la liberación de todas sus sillas?")) {
                reservationService.cancelReservation(reservationId);
                System.out.println("Reserva #" + reservationId + " cancelada exitosamente.");
            } else {
                System.out.println("Cancelación abortada.");
            }
        } catch (AirlinesException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
        input.pause();
    }

    private void listByCustomerFlow() {
        System.out.println("\n--- CONSULTAR RESERVAS POR CLIENTE ---");
        try {
            String customer = input.readRequiredText("Identificador o documento del cliente");
            List<Reservation> reservations = reservationService.listReservationsByCustomer(customer);
            if (reservations.isEmpty()) {
                System.out.println("No se encontraron reservas registradas para el cliente: " + customer);
            } else {
                System.out.printf("Se encontraron %d reserva(s):%n", reservations.size());
                for (Reservation res : reservations) {
                    printReservationSummary(res);
                }
            }
        } catch (AirlinesException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
        input.pause();
    }

    private void listAllReservationsFlow() {
        System.out.println("\n--- LISTADO GENERAL DE RESERVAS ---");
        List<Reservation> all = reservationService.listReservations();
        if (all.isEmpty()) {
            System.out.println("No hay reservas registradas en el sistema.");
        } else {
            System.out.printf("Total de reservas: %d%n", all.size());
            for (Reservation res : all) {
                printReservationSummary(res);
            }
        }
        input.pause();
    }

    private void listOccupiedSeatsFlow() {
        System.out.println("\n--- VER SILLAS OCUPADAS DE UN VUELO ---");
        try {
            int flightId = input.readInt("Identificador numérico del vuelo", 1, 999999);
            List<Seat> occupied = reservationService.listOccupiedSeats(flightId);
            if (occupied.isEmpty()) {
                System.out.println("Todas las sillas están disponibles para el vuelo #" + flightId);
            } else {
                System.out.printf("Sillas ocupadas en el vuelo #%d (%d sillas):%n", flightId, occupied.size());
                System.out.println(String.join(", ", occupied.stream().map(Seat::toString).toList()));
            }
        } catch (AirlinesException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
        input.pause();
    }

    private void showFlightManifestFlow() {
        System.out.println("\n--- MANIFIESTO DE PASAJEROS DE VUELO ---");
        try {
            int flightId = input.readInt("Identificador numérico del vuelo", 1, 999999);
            List<ManifestEntry> manifest = reservationService.getFlightManifest(flightId);

            if (manifest.isEmpty()) {
                System.out.println("No hay pasajeros confirmados para el vuelo #" + flightId + ".");
            } else {
                System.out.println("\n" + "=".repeat(78));
                System.out.printf("%35s #%d%n", "MANIFIESTO OFICIAL DE PASAJEROS - VUELO", flightId);
                System.out.println("=".repeat(78));
                System.out.printf("| %-6s | %-16s | %-32s | %-11s |%n", "Silla", "Documento", "Nombre del Pasajero", "Reserva #");
                System.out.println("|-" + "-".repeat(6) + "-|-" + "-".repeat(16) + "-|-" + "-".repeat(32) + "-|-" + "-".repeat(11) + "-|");
                for (ManifestEntry entry : manifest) {
                    System.out.printf("| %-6s | %-16s | %-32s | %-11d |%n",
                            entry.seat(),
                            entry.passengerDocument(),
                            entry.passengerFullName(),
                            entry.reservationId());
                }
                System.out.println("=".repeat(78));
                System.out.printf("Total de pasajeros confirmados a bordo: %d%n", manifest.size());
            }
        } catch (AirlinesException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
        input.pause();
    }

    private Seat readSeatFromConsole() {
        String row = input.readRequiredText("Fila (ej. 1, 12, A)");
        int number = input.readInt("Número de asiento", 1, Seat.MAX_SEAT_NUMBER);
        return new Seat(row, number);
    }

    private void printReservationSummary(Reservation res) {
        System.out.println("-------------------------------------------------");
        System.out.printf("Reserva #%d | Cliente: %s | Vuelo: %s%n", res.getId(), res.getCustomer(), res.getFlight());
        System.out.println("Pasajeros asignados:");
        for (ReservationDetail detail : res.getDetails()) {
            System.out.printf("  * Silla %-4s -> %s (Doc: %s)%n",
                    detail.getSeat(), detail.getPassengerFullName(), detail.getPassengerDocument());
        }
    }
}
