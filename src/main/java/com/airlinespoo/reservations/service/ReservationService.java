package com.airlinespoo.reservations.service;

import com.airlinespoo.core.exception.BusinessRuleException;
import com.airlinespoo.core.exception.NotFoundException;
import com.airlinespoo.core.exception.ValidationException;
import com.airlinespoo.core.util.IdGenerator;
import com.airlinespoo.core.util.Validator;
import com.airlinespoo.customers.model.Customer;
import com.airlinespoo.customers.service.CustomerService;
import com.airlinespoo.flights.model.Flight;
import com.airlinespoo.flights.service.FlightService;
import com.airlinespoo.reservations.model.ManifestEntry;
import com.airlinespoo.reservations.model.Reservation;
import com.airlinespoo.reservations.model.ReservationDetail;
import com.airlinespoo.reservations.model.Seat;
import com.airlinespoo.reservations.repository.IReservationRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Servicio de negocio que gestiona las operaciones del módulo de Reservas y Pasajeros.
 *
 * <p>Coordina las validaciones con el Módulo 1 ({@link CustomerService}) y aplica la regla
 * central de sillas libres en el vuelo, gestión de pasajeros, cancelaciones y manifiesto.</p>
 */
public class ReservationService {

    /**
     * Datos para la solicitud de registro de un pasajero.
     *
     * @param fullName nombre completo del pasajero
     * @param document documento de identidad
     * @param seat     silla deseada
     */
    public record PassengerRequest(String fullName, String document, Seat seat) {
        public PassengerRequest {
            Validator.requireNotNull(fullName, "nombre del pasajero");
            Validator.requireNotNull(document, "documento del pasajero");
            Validator.requireNotNull(seat, "silla");
        }
    }

    private final IReservationRepository reservationRepository;
    private final CustomerService customerService;
    private final FlightService flightService;
    private final IdGenerator reservationIdGenerator;
    private final IdGenerator detailIdGenerator;

    /**
     * Construye el servicio mediante inyección de dependencias manual.
     *
     * @param reservationRepository  repositorio de reservas
     * @param customerService        servicio del Módulo 1 para validar clientes existentes
     * @param reservationIdGenerator generador de ids para reservas
     * @param detailIdGenerator      generador de ids para detalles
     */
    public ReservationService(IReservationRepository reservationRepository,
                              CustomerService customerService,
                              IdGenerator reservationIdGenerator,
                              IdGenerator detailIdGenerator) {
        this(reservationRepository, customerService, null, reservationIdGenerator, detailIdGenerator);
    }

    /**
     * Construye el servicio integrado con Módulo 1 ({@link CustomerService}) y Módulo 4 ({@link FlightService}).
     *
     * @param reservationRepository  repositorio de reservas
     * @param customerService        servicio del Módulo 1 para validar clientes existentes
     * @param flightService          servicio del Módulo 4 para validar vuelos existentes (opcional)
     * @param reservationIdGenerator generador de ids para reservas
     * @param detailIdGenerator      generador de ids para detalles
     */
    public ReservationService(IReservationRepository reservationRepository,
                              CustomerService customerService,
                              FlightService flightService,
                              IdGenerator reservationIdGenerator,
                              IdGenerator detailIdGenerator) {
        this.reservationRepository = Validator.requireNotNull(reservationRepository, "repositorio de reservas");
        this.customerService = Validator.requireNotNull(customerService, "servicio de clientes");
        this.flightService = flightService;
        this.reservationIdGenerator = Validator.requireNotNull(reservationIdGenerator, "generador de ids de reserva");
        this.detailIdGenerator = Validator.requireNotNull(detailIdGenerator, "generador de ids de detalle");
    }

    /**
     * Crea una reserva para un único pasajero inicial validando la existencia del cliente en Módulo 1.
     *
     * @param customerId documento del cliente titular
     * @param flight     vuelo reservado
     * @param fullName   nombre completo del pasajero
     * @param document   documento del pasajero
     * @param seat       silla asignada
     * @return la reserva creada
     */
    public Reservation createReservation(String customerId, Object flight, String fullName, String document, Seat seat) {
        return createReservation(customerId, flight, List.of(new PassengerRequest(fullName, document, seat)));
    }

    /**
     * Crea una reserva con uno o varios pasajeros iniciales validando la existencia del cliente.
     *
     * @param customerId        documento del cliente titular en Módulo 1
     * @param flight            vuelo reservado
     * @param passengerRequests lista de solicitudes de pasajeros (entre 1 y 9)
     * @return la reserva persistida
     */
    public Reservation createReservation(String customerId, Object flight, List<PassengerRequest> passengerRequests) {
        String cleanCustomerId = Validator.requireText(customerId, "documento del cliente");
        Customer customer = customerService.getCustomer(cleanCustomerId);

        Object resolvedFlight = resolveFlight(flight);
        Validator.requireNotNull(passengerRequests, "lista de pasajeros");

        if (passengerRequests.isEmpty()) {
            throw new BusinessRuleException("Una reserva debe tener al menos un pasajero.");
        }
        if (passengerRequests.size() > Reservation.MAX_PASSENGERS) {
            throw new BusinessRuleException(
                    "Una reserva no puede tener más de %d pasajeros.".formatted(Reservation.MAX_PASSENGERS));
        }

        // Regla central: validar que ninguna silla solicitada esté ocupada en el vuelo
        for (PassengerRequest req : passengerRequests) {
            if (isSeatOccupied(resolvedFlight, req.seat())) {
                throw new BusinessRuleException(
                        "La silla '%s' ya está ocupada en este vuelo por otra reserva."
                                .formatted(ManifestEntry.formatSeat(req.seat())));
            }
        }

        int reservationId = reservationIdGenerator.next();
        List<ReservationDetail> details = new ArrayList<>();
        for (PassengerRequest req : passengerRequests) {
            int detailId = detailIdGenerator.next();
            details.add(new ReservationDetail(detailId, req.fullName(), req.document(), req.seat()));
        }

        Reservation reservation = new Reservation(reservationId, customer, resolvedFlight, details);
        return reservationRepository.create(reservation);
    }

    /**
     * Agrega un nuevo pasajero a una reserva existente.
     *
     * @param reservationId     id de la reserva
     * @param passengerFullName nombre del pasajero
     * @param passengerDocument documento del pasajero
     * @param seat              silla asignada
     * @return el detalle de reserva creado
     */
    public ReservationDetail addPassenger(int reservationId, String passengerFullName, String passengerDocument, Seat seat) {
        Reservation reservation = getReservation(reservationId);
        Validator.requireNotNull(seat, "silla");

        if (isSeatOccupied(reservation.getFlight(), seat)) {
            throw new BusinessRuleException(
                    "La silla '%s' ya está ocupada en este vuelo.".formatted(ManifestEntry.formatSeat(seat)));
        }

        int detailId = detailIdGenerator.next();
        ReservationDetail detail = new ReservationDetail(detailId, passengerFullName, passengerDocument, seat);
        reservation.addDetail(detail);

        reservationRepository.update(reservation);
        return detail;
    }

    public Reservation getReservation(int reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("reserva", reservationId));
    }

    public List<Reservation> listReservations() {
        return reservationRepository.findAllOrderedById();
    }

    public List<ReservationDetail> listPassengers(int reservationId) {
        return getReservation(reservationId).getDetails();
    }

    public void changeSeat(int reservationId, String passengerDocument, Seat newSeat) {
        Reservation reservation = getReservation(reservationId);
        Validator.requireNotNull(newSeat, "nueva silla");
        String cleanDoc = Validator.requireText(passengerDocument, "documento del pasajero");

        ReservationDetail targetDetail = reservation.getDetails().stream()
                .filter(d -> d.getPassengerDocument().equalsIgnoreCase(cleanDoc))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "No existe un pasajero con documento '%s' en la reserva #%d.".formatted(cleanDoc, reservationId)));

        if (targetDetail.getSeat().equals(newSeat)) {
            return;
        }

        List<Reservation> flightReservations = reservationRepository.findByFlight(reservation.getFlight());
        for (Reservation otherRes : flightReservations) {
            for (ReservationDetail otherDetail : otherRes.getDetails()) {
                if (otherDetail.getSeat().equals(newSeat)) {
                    throw new BusinessRuleException(
                            "La silla '%s' ya está ocupada en este vuelo.".formatted(ManifestEntry.formatSeat(newSeat)));
                }
            }
        }

        ReservationDetail updated = new ReservationDetail(
                targetDetail.getId(),
                targetDetail.getPassengerFullName(),
                targetDetail.getPassengerDocument(),
                newSeat
        );
        reservation.replaceDetail(targetDetail.getId(), updated);
        reservationRepository.update(reservation);
    }

    public void updatePassenger(int reservationId, int detailId, String newFullName, String newDocument, Seat newSeat) {
        Reservation reservation = getReservation(reservationId);
        ReservationDetail targetDetail = reservation.getDetails().stream()
                .filter(d -> d.getId() == detailId)
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "No existe el detalle con id '%d' en la reserva #%d.".formatted(detailId, reservationId)));

        if (!targetDetail.getSeat().equals(newSeat) && isSeatOccupied(reservation.getFlight(), newSeat)) {
            throw new BusinessRuleException(
                    "La silla '%s' ya está ocupada en este vuelo.".formatted(ManifestEntry.formatSeat(newSeat)));
        }

        ReservationDetail updated = new ReservationDetail(detailId, newFullName, newDocument, newSeat);
        reservation.replaceDetail(detailId, updated);
        reservationRepository.update(reservation);
    }

    public void removePassenger(int reservationId, String passengerDocument) {
        Reservation reservation = getReservation(reservationId);
        String cleanDoc = Validator.requireText(passengerDocument, "documento del pasajero");

        boolean removed = reservation.removePassengerByDocument(cleanDoc);
        if (!removed) {
            throw new NotFoundException(
                    "No se encontró un pasajero con documento '%s' en la reserva #%d.".formatted(cleanDoc, reservationId));
        }

        reservationRepository.update(reservation);
    }

    public void cancelReservation(int reservationId) {
        if (!reservationRepository.existsById(reservationId)) {
            throw new NotFoundException("reserva", reservationId);
        }
        reservationRepository.deleteById(reservationId);
    }

    public void changeReservationFlight(int reservationId, Object newFlight) {
        Reservation reservation = getReservation(reservationId);
        Object resolvedFlight = resolveFlight(newFlight);

        for (ReservationDetail detail : reservation.getDetails()) {
            if (isSeatOccupied(resolvedFlight, detail.getSeat())) {
                throw new BusinessRuleException(
                        "La silla '%s' no está disponible en el nuevo vuelo solicitado."
                                .formatted(ManifestEntry.formatSeat(detail.getSeat())));
            }
        }

        reservation.setFlight(resolvedFlight);
        reservationRepository.update(reservation);
    }

    public List<Reservation> listReservationsByCustomer(String customerId) {
        Customer customer = customerService.getCustomer(customerId);
        return reservationRepository.findByCustomer(customer);
    }

    public List<Reservation> listReservationsByFlight(Object flight) {
        return reservationRepository.findByFlight(flight);
    }

    public boolean customerHasReservations(String customerId) {
        return reservationRepository.existsByCustomer(customerId);
    }

    public boolean flightHasReservations(Object flight) {
        return reservationRepository.existsByFlight(flight);
    }

    public boolean isSeatOccupied(Object flight, Seat seat) {
        if (flight == null || seat == null) return false;
        List<Reservation> flightReservations = reservationRepository.findByFlight(flight);
        for (Reservation reservation : flightReservations) {
            if (reservation.hasSeat(seat)) {
                return true;
            }
        }
        return false;
    }

    public List<Seat> listOccupiedSeats(Object flight) {
        List<Reservation> flightReservations = reservationRepository.findByFlight(flight);
        Set<Seat> occupied = new TreeSet<>(ManifestEntry.SEAT_COMPARATOR);
        for (Reservation reservation : flightReservations) {
            occupied.addAll(reservation.getSeats());
        }
        return new ArrayList<>(occupied);
    }

    public int countPassengersByFlight(Object flight) {
        List<Reservation> flightReservations = reservationRepository.findByFlight(flight);
        int total = 0;
        for (Reservation reservation : flightReservations) {
            total += reservation.passengerCount();
        }
        return total;
    }

    public List<ManifestEntry> getFlightManifest(Object flight) {
        List<Reservation> flightReservations = reservationRepository.findByFlight(flight);
        List<ManifestEntry> manifest = new ArrayList<>();

        for (Reservation res : flightReservations) {
            for (ReservationDetail detail : res.getDetails()) {
                manifest.add(new ManifestEntry(
                        res.getId(),
                        detail.getPassengerFullName(),
                        detail.getPassengerDocument(),
                        detail.getSeat()
                ));
            }
        }

        Collections.sort(manifest);
        return manifest;
    }

    private Object resolveFlight(Object flightOrId) {
        Validator.requireNotNull(flightOrId, "vuelo");
        if (flightService == null) {
            return flightOrId;
        }
        Integer flightId = null;
        if (flightOrId instanceof Flight flight) {
            flightId = flight.getId();
        } else if (flightOrId instanceof Number number) {
            flightId = number.intValue();
        }
        if (flightId == null) {
            return flightOrId;
        }
        int id = flightId;
        return flightService.getFlightById(id)
                .orElseThrow(() -> new NotFoundException("vuelo", id));
    }
}
