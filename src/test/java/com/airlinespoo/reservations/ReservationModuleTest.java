package com.airlinespoo.reservations;

import com.airlinespoo.core.exception.BusinessRuleException;
import com.airlinespoo.core.exception.NotFoundException;
import com.airlinespoo.core.exception.ValidationException;
import com.airlinespoo.core.util.IdGenerator;
import com.airlinespoo.customers.CustomerSampleData;
import com.airlinespoo.customers.repository.CountryRepository;
import com.airlinespoo.customers.repository.CustomerRepository;
import com.airlinespoo.customers.repository.DocumentTypeRepository;
import com.airlinespoo.customers.service.CustomerService;
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
 * Pruebas unitarias e integración con Módulo 1 (Clientes) para el Módulo 5 (Reservas).
 */
class ReservationModuleTest {

    private CustomerService customerService;
    private ReservationRepository repository;
    private ReservationService service;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(
                new CountryRepository(),
                new DocumentTypeRepository(),
                new CustomerRepository()
        );
        CustomerSampleData.load(customerService);

        repository = new ReservationRepository();
        service = new ReservationService(
                repository,
                customerService,
                new IdGenerator(1, 1),
                new IdGenerator(1, 1)
        );
    }

    @Test
    @DisplayName("Caso 1: Crear reserva sin pasajeros -> Rechazo")
    void testCreateReservationWithoutPassengers() {
        assertThrows(BusinessRuleException.class, () ->
                service.createReservation("101010", 101, List.of())
        );
    }

    @Test
    @DisplayName("Caso 2: Crear reserva con cliente inexistente en Módulo 1 -> NotFoundException")
    void testCreateReservationWithNonExistentCustomer() {
        assertThrows(NotFoundException.class, () ->
                service.createReservation("999999", 101, "Carlos", "102030", new Seat("12", 2))
        );
    }

    @Test
    @DisplayName("Caso 3: Crear reserva con vuelo nulo -> ValidationException")
    void testCreateReservationWithNullFlight() {
        assertThrows(ValidationException.class, () ->
                service.createReservation("101010", null, "Carlos", "102030", new Seat("12", 2))
        );
    }

    @Test
    @DisplayName("Caso 5: Agregar dos pasajeros con el mismo documento en una reserva -> Rechazo")
    void testDuplicatePassengerDocumentInSameReservation() {
        Reservation reservation = service.createReservation("101010", 101, "Ana Pérez", "101010", new Seat("1", 1));
        assertThrows(BusinessRuleException.class, () ->
                service.addPassenger(reservation.getId(), "Ana Clon", "101010", new Seat("1", 2))
        );
    }

    @Test
    @DisplayName("Caso 6: Asignar la silla 12-2 a dos pasajeros de la misma reserva -> Rechazo")
    void testAssignSameSeatToTwoPassengersInSameReservation() {
        Seat seat12B = new Seat("12", 2);
        Reservation reservation = service.createReservation("101010", 101, "Pasajero 1", "DOC1", seat12B);
        assertThrows(BusinessRuleException.class, () ->
                service.addPassenger(reservation.getId(), "Pasajero 2", "DOC2", seat12B)
        );
    }

    @Test
    @DisplayName("Caso 7: Asignar la silla 12-2 a un pasajero de otra reserva del mismo vuelo -> Rechazo")
    void testAssignSameSeatToPassengerInDifferentReservationSameFlight() {
        Seat seat12B = new Seat("12", 2);
        service.createReservation("101010", 101, "Pasajero 1", "DOC1", seat12B);

        assertThrows(BusinessRuleException.class, () ->
                service.createReservation("202020", 101, "Pasajero 2", "DOC2", seat12B)
        );
    }

    @Test
    @DisplayName("Caso 8: Asignar la silla 12-2 en dos vuelos distintos -> Se acepta")
    void testAssignSameSeatInTwoDifferentFlights() {
        Seat seat12B = new Seat("12", 2);
        Reservation resFlight1 = service.createReservation("101010", 101, "Pasajero 1", "DOC1", seat12B);
        Reservation resFlight2 = service.createReservation("202020", 202, "Pasajero 2", "DOC2", seat12B);

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
        Reservation reservation = service.createReservation("101010", 101, passengers);
        assertEquals(9, reservation.passengerCount());

        assertThrows(BusinessRuleException.class, () ->
                service.addPassenger(reservation.getId(), "Pasajero 10", "DOC10", new Seat("2", 1))
        );
    }

    @Test
    @DisplayName("Caso 10: Quitar el único pasajero -> Rechazo")
    void testRemoveOnlyPassengerFromReservation() {
        Reservation reservation = service.createReservation("101010", 101, "Pasajero Solo", "DOC1", new Seat("1", 1));
        assertThrows(BusinessRuleException.class, () ->
                service.removePassenger(reservation.getId(), "DOC1")
        );
    }

    @Test
    @DisplayName("Caso 11: Cancelar una reserva y volver a usar su silla -> La silla queda libre")
    void testCancelReservationAndReuseSeat() {
        Seat seat12B = new Seat("12", 2);
        Reservation reservation = service.createReservation("101010", 101, "Pasajero", "DOC1", seat12B);
        assertTrue(service.isSeatOccupied(101, seat12B));

        service.cancelReservation(reservation.getId());
        assertFalse(service.isSeatOccupied(101, seat12B));

        Reservation newRes = service.createReservation("202020", 101, "Nuevo Pasajero", "DOC2", seat12B);
        assertNotNull(newRes);
    }

    @Test
    @DisplayName("Caso 12: Ver manifiesto con 5 pasajeros -> Salen ordenados por silla")
    void testManifestOrderedBySeat() {
        service.createReservation("101010", 101, List.of(
                new PassengerRequest("Pasajero C", "DOC3", new Seat("10", 1)),
                new PassengerRequest("Pasajero A", "DOC1", new Seat("1", 2)),
                new PassengerRequest("Pasajero B", "DOC2", new Seat("2", 1))
        ));

        service.createReservation("202020", 101, List.of(
                new PassengerRequest("Pasajero E", "DOC5", new Seat("12", 3)),
                new PassengerRequest("Pasajero D", "DOC4", new Seat("1", 1))
        ));

        List<ManifestEntry> manifest = service.getFlightManifest(101);
        assertEquals(5, manifest.size());

        assertEquals("1-1", manifest.get(0).seat().toString());
        assertEquals("1-2", manifest.get(1).seat().toString());
        assertEquals("2-1", manifest.get(2).seat().toString());
        assertEquals("10-1", manifest.get(3).seat().toString());
        assertEquals("12-3", manifest.get(4).seat().toString());
    }

    @Test
    @DisplayName("Caso 13: Intentar modificar la lista que devuelve la reserva desde afuera -> No afecta la reserva")
    void testDefensiveCopyOfDetailsList() {
        Reservation reservation = service.createReservation("101010", 101, "Pasajero", "DOC1", new Seat("1", 1));
        List<ReservationDetail> details = reservation.getDetails();

        assertThrows(UnsupportedOperationException.class, () ->
                details.add(new ReservationDetail(99, "Infiltrado", "DOC99", new Seat("1", 2)))
        );
        assertEquals(1, reservation.passengerCount());
    }
}
