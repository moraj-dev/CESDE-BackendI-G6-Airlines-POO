package com.airlinespoo.reservations;

import com.airlinespoo.core.exception.BusinessRuleException;
import com.airlinespoo.core.exception.ValidationException;
import com.airlinespoo.core.util.IdGenerator;
import com.airlinespoo.reservations.model.ManifestEntry;
import com.airlinespoo.reservations.model.Reservation;
import com.airlinespoo.reservations.model.ReservationDetail;
import com.airlinespoo.reservations.model.Seat;
import com.airlinespoo.reservations.repository.ReservationRepository;
import com.airlinespoo.reservations.service.ReservationService;
import com.airlinespoo.reservations.service.ReservationService.PassengerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para el Módulo 5 (Reservas y Pasajeros).
 * Valida los 13 casos de prueba requeridos en la guía docente.
 */
class ReservationModuleTest {

    private ReservationRepository repository;
    private IdGenerator reservationIdGen;
    private IdGenerator detailIdGen;
    private ReservationService service;

    @BeforeEach
    void setUp() {
        repository = new ReservationRepository();
        reservationIdGen = new IdGenerator(1, 1);
        detailIdGen = new IdGenerator(1, 1);
        service = new ReservationService(repository, reservationIdGen, detailIdGen);
    }

    @Test
    @DisplayName("Caso 1: Crear reserva sin pasajeros -> Rechazo")
    void testCreateReservationWithoutPassengers() {
        assertThrows(BusinessRuleException.class, () ->
                service.createReservation("CLI-01", 101, List.of())
        );
    }

    @Test
    @DisplayName("Caso 2 & 3: Crear reserva con cliente o vuelo inexistente / nulo -> Rechazo")
    void testCreateReservationWithNullCustomerOrFlight() {
        assertThrows(ValidationException.class, () ->
                service.createReservation(null, 101, "Carlos", "102030", new Seat("12", 2))
        );
        assertThrows(ValidationException.class, () ->
                service.createReservation("CLI-01", null, "Carlos", "102030", new Seat("12", 2))
        );
    }

    @Test
    @DisplayName("Caso 5: Agregar dos pasajeros con el mismo documento en una reserva -> El segundo es rechazado")
    void testDuplicatePassengerDocumentInSameReservation() {
        Reservation reservation = service.createReservation("CLI-01", 101, "Carlos", "102030", new Seat("1", 1));
        assertThrows(BusinessRuleException.class, () ->
                service.addPassenger(reservation.getId(), "Carlos Clon", "102030", new Seat("1", 2))
        );
    }

    @Test
    @DisplayName("Caso 6: Asignar la silla 12B a dos pasajeros de la misma reserva -> Rechazo")
    void testAssignSameSeatToTwoPassengersInSameReservation() {
        Seat seat12B = new Seat("12", 2);
        Reservation reservation = service.createReservation("CLI-01", 101, "Pasajero 1", "DOC1", seat12B);
        assertThrows(BusinessRuleException.class, () ->
                service.addPassenger(reservation.getId(), "Pasajero 2", "DOC2", seat12B)
        );
    }

    @Test
    @DisplayName("Caso 7: Asignar la silla 12B a un pasajero de otra reserva del mismo vuelo -> Rechazo")
    void testAssignSameSeatToPassengerInDifferentReservationSameFlight() {
        Seat seat12B = new Seat("12", 2);
        service.createReservation("CLI-01", 101, "Pasajero 1", "DOC1", seat12B);

        assertThrows(BusinessRuleException.class, () ->
                service.createReservation("CLI-02", 101, "Pasajero 2", "DOC2", seat12B)
        );
    }

    @Test
    @DisplayName("Caso 8: Asignar la silla 12B en dos vuelos distintos -> Se acepta")
    void testAssignSameSeatInTwoDifferentFlights() {
        Seat seat12B = new Seat("12", 2);
        Reservation resFlight1 = service.createReservation("CLI-01", 101, "Pasajero 1", "DOC1", seat12B);
        Reservation resFlight2 = service.createReservation("CLI-02", 202, "Pasajero 2", "DOC2", seat12B);

        assertNotNull(resFlight1);
        assertNotNull(resFlight2);
        assertNotEquals(resFlight1.getId(), resFlight2.getId());
    }

    @Test
    @DisplayName("Caso 9: Agregar más pasajeros del máximo permitido (9) -> Rechazo")
    void testExceedMaxPassengersLimit() {
        List<PassengerRequest> passengers = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            passengers.add(new PassengerRequest("Pasajero " + i, "DOC" + i, new Seat("1", i)));
        }
        Reservation reservation = service.createReservation("CLI-01", 101, passengers);
        assertEquals(9, reservation.passengerCount());

        assertThrows(BusinessRuleException.class, () ->
                service.addPassenger(reservation.getId(), "Pasajero 10", "DOC10", new Seat("2", 1))
        );
    }

    @Test
    @DisplayName("Caso 10: Quitar el único pasajero -> Rechazo")
    void testRemoveOnlyPassengerFromReservation() {
        Reservation reservation = service.createReservation("CLI-01", 101, "Pasajero Solo", "DOC1", new Seat("1", 1));
        assertThrows(BusinessRuleException.class, () ->
                service.removePassenger(reservation.getId(), "DOC1")
        );
    }

    @Test
    @DisplayName("Caso 11: Cancelar una reserva y volver a usar su silla -> La silla queda libre")
    void testCancelReservationAndReuseSeat() {
        Seat seat12B = new Seat("12", 2);
        Reservation reservation = service.createReservation("CLI-01", 101, "Pasajero", "DOC1", seat12B);
        assertTrue(service.isSeatOccupied(101, seat12B));

        service.cancelReservation(reservation.getId());
        assertFalse(service.isSeatOccupied(101, seat12B));

        // Ahora debe permitirse crear una nueva reserva con esa misma silla
        Reservation newRes = service.createReservation("CLI-02", 101, "Nuevo Pasajero", "DOC2", seat12B);
        assertNotNull(newRes);
    }

    @Test
    @DisplayName("Caso 12: Ver manifiesto con 5 pasajeros -> Salen ordenados por silla")
    void testManifestOrderedBySeat() {
        service.createReservation("CLI-01", 101, List.of(
                new PassengerRequest("Pasajero C", "DOC3", new Seat("10", 1)),
                new PassengerRequest("Pasajero A", "DOC1", new Seat("1", 2)),
                new PassengerRequest("Pasajero B", "DOC2", new Seat("2", 1))
        ));

        service.createReservation("CLI-02", 101, List.of(
                new PassengerRequest("Pasajero E", "DOC5", new Seat("12", 3)),
                new PassengerRequest("Pasajero D", "DOC4", new Seat("1", 1))
        ));

        List<ManifestEntry> manifest = service.getFlightManifest(101);
        assertEquals(5, manifest.size());

        // Debe estar ordenado por silla: 1-1, 1-2, 2-1, 10-1, 12-3
        assertEquals("11", manifest.get(0).seat().toString());
        assertEquals("12", manifest.get(1).seat().toString());
        assertEquals("21", manifest.get(2).seat().toString());
        assertEquals("101", manifest.get(3).seat().toString());
        assertEquals("123", manifest.get(4).seat().toString());
    }

    @Test
    @DisplayName("Caso 13: Intentar modificar la lista que devuelve la reserva desde afuera -> No afecta la reserva")
    void testDefensiveCopyOfDetailsList() {
        Reservation reservation = service.createReservation("CLI-01", 101, "Pasajero", "DOC1", new Seat("1", 1));
        List<ReservationDetail> details = reservation.getDetails();

        assertThrows(UnsupportedOperationException.class, () ->
                details.add(new ReservationDetail(99, "Infiltrado", "DOC99", new Seat("1", 2)))
        );
        assertEquals(1, reservation.passengerCount());
    }
}
