package com.airlinespoo.reservations.service;

import com.airlinespoo.core.exception.BusinessRuleException;
import com.airlinespoo.core.exception.NotFoundException;
import com.airlinespoo.core.exception.ValidationException;
import com.airlinespoo.core.util.IdGenerator;
import com.airlinespoo.core.util.Validator;
import com.airlinespoo.reservations.model.ManifestEntry;
import com.airlinespoo.reservations.model.Reservation;
import com.airlinespoo.reservations.model.ReservationDetail;
import com.airlinespoo.reservations.model.Seat;
import com.airlinespoo.reservations.repository.ReservationRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Servicio de negocio que gestiona las operaciones del módulo de Reservas y Pasajeros.
 *
 * <p>Aplica las reglas de negocio del sistema que involucran múltiples objetos y colecciones:</p>
 * <ul>
 *   <li><b>Regla central de sillas:</b> Una silla no puede asignarse a dos pasajeros en el mismo vuelo.</li>
 *   <li><b>Regla de unicidad interna:</b> Un pasajero (por documento) no puede repetirse en la misma reserva.</li>
 *   <li><b>Capacidad mínima y máxima:</b> Cada reserva debe tener entre 1 y 9 pasajeros.</li>
 *   <li><b>Manifiesto de vuelo:</b> Listado ordenado por silla de todos los pasajeros confirmados para un vuelo.</li>
 *   <li><b>Integridad referencial:</b> Verificación de existencia de reservas por cliente y por vuelo.</li>
 * </ul>
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

    private final ReservationRepository reservationRepository;
    private final IdGenerator reservationIdGenerator;
    private final IdGenerator detailIdGenerator;

    /**
     * Construye el servicio mediante inyección de dependencias manual.
     *
     * @param reservationRepository repositorio de reservas
     * @param reservationIdGenerator generador de identificadores para reservas
     * @param detailIdGenerator      generador de identificadores para detalles de reserva
     */
    public ReservationService(ReservationRepository reservationRepository,
                              IdGenerator reservationIdGenerator,
                              IdGenerator detailIdGenerator) {
        this.reservationRepository = Validator.requireNotNull(reservationRepository, "repositorio de reservas");
        this.reservationIdGenerator = Validator.requireNotNull(reservationIdGenerator, "generador de ids de reserva");
        this.detailIdGenerator = Validator.requireNotNull(detailIdGenerator, "generador de ids de detalle");
    }

    /**
     * Crea una reserva para un único pasajero inicial.
     *
     * @param customer cliente titular de la reserva
     * @param flight   vuelo reservado
     * @param fullName nombre completo del pasajero
     * @param document documento del pasajero
     * @param seat     silla asignada
     * @return la reserva creada
     */
    public Reservation createReservation(Object customer, Object flight, String fullName, String document, Seat seat) {
        return createReservation(customer, flight, List.of(new PassengerRequest(fullName, document, seat)));
    }

    /**
     * Crea una reserva con uno o varios pasajeros iniciales.
     *
     * @param customer         cliente titular
     * @param flight           vuelo reservado
     * @param passengerRequests lista de solicitudes de pasajeros (entre 1 y 9)
     * @return la reserva persistida
     * @throws ValidationException   si faltan datos obligatorios
     * @throws BusinessRuleException si alguna silla ya está ocupada en el vuelo o hay duplicados
     */
    public Reservation createReservation(Object customer, Object flight, List<PassengerRequest> passengerRequests) {
        Validator.requireNotNull(customer, "cliente");
        Validator.requireNotNull(flight, "vuelo");
        Validator.requireNotNull(passengerRequests, "lista de pasajeros");

        if (passengerRequests.isEmpty()) {
            throw new BusinessRuleException("Una reserva debe tener al menos un pasajero.");
        }
        if (passengerRequests.size() > Reservation.MAX_PASSENGERS) {
            throw new BusinessRuleException(
                    "Una reserva no puede tener más de %d pasajeros.".formatted(Reservation.MAX_PASSENGERS));
        }

        // Validar que ninguna silla solicitada esté ocupada en el vuelo
        for (PassengerRequest req : passengerRequests) {
            if (isSeatOccupied(flight, req.seat())) {
                throw new BusinessRuleException(
                        "La silla '%s' ya está ocupada en este vuelo por otra reserva.".formatted(req.seat()));
            }
        }

        int reservationId = reservationIdGenerator.next();
        List<ReservationDetail> details = new ArrayList<>();
        for (PassengerRequest req : passengerRequests) {
            int detailId = detailIdGenerator.next();
            details.add(new ReservationDetail(detailId, req.fullName(), req.document(), req.seat()));
        }

        Reservation reservation = new Reservation(reservationId, customer, flight, details);
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
     * @throws BusinessRuleException si la silla está ocupada en el vuelo o se superan los límites
     */
    public ReservationDetail addPassenger(int reservationId, String passengerFullName, String passengerDocument, Seat seat) {
        Reservation reservation = getReservation(reservationId);
        Validator.requireNotNull(seat, "silla");

        // Regla: la silla no puede estar ocupada en ese mismo vuelo
        if (isSeatOccupied(reservation.getFlight(), seat)) {
            throw new BusinessRuleException(
                    "La silla '%s' ya está ocupada en este vuelo.".formatted(seat));
        }

        int detailId = detailIdGenerator.next();
        ReservationDetail detail = new ReservationDetail(detailId, passengerFullName, passengerDocument, seat);
        reservation.addDetail(detail);

        reservationRepository.update(reservation);
        return detail;
    }

    /**
     * Consulta una reserva por su identificador.
     *
     * @param reservationId id de la reserva
     * @return la reserva encontrada
     * @throws NotFoundException si no existe
     */
    public Reservation getReservation(int reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("reserva", reservationId));
    }

    /**
     * Lista todas las reservas del sistema ordenadas por identificador.
     *
     * @return copia de todas las reservas
     */
    public List<Reservation> listReservations() {
        return reservationRepository.findAllOrderedById();
    }

    /**
     * Lista todos los pasajeros y sillas de una reserva dada.
     *
     * @param reservationId id de la reserva
     * @return lista inmutable de detalles de la reserva
     */
    public List<ReservationDetail> listPassengers(int reservationId) {
        return getReservation(reservationId).getDetails();
    }

    /**
     * Cambia la silla de un pasajero en una reserva dada.
     *
     * @param reservationId     id de la reserva
     * @param passengerDocument documento del pasajero
     * @param newSeat           nueva silla solicitada
     * @throws BusinessRuleException si la nueva silla ya está ocupada en el vuelo
     */
    public void changeSeat(int reservationId, String passengerDocument, Seat newSeat) {
        Reservation reservation = getReservation(reservationId);
        Validator.requireNotNull(newSeat, "nueva silla");
        String cleanDoc = Validator.requireText(passengerDocument, "documento del pasajero");

        ReservationDetail targetDetail = reservation.getDetails().stream()
                .filter(d -> d.getPassengerDocument().equalsIgnoreCase(cleanDoc))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "No existe un pasajero con documento '%s' en la reserva #%d.".formatted(cleanDoc, reservationId)));

        // Si ya tiene asignada esa misma silla, no hay conflicto
        if (targetDetail.getSeat().equals(newSeat)) {
            return;
        }

        // Verificar que ningún otro pasajero de este vuelo tenga esa silla
        List<Reservation> flightReservations = reservationRepository.findByFlight(reservation.getFlight());
        for (Reservation otherRes : flightReservations) {
            for (ReservationDetail otherDetail : otherRes.getDetails()) {
                if (otherDetail.getSeat().equals(newSeat)) {
                    throw new BusinessRuleException(
                            "La silla '%s' ya está ocupada en este vuelo.".formatted(newSeat));
                }
            }
        }

        targetDetail.setSeat(newSeat);
        reservationRepository.update(reservation);
    }

    /**
     * Actualiza los datos de un pasajero existente.
     *
     * @param reservationId id de la reserva
     * @param detailId      id del detalle
     * @param newFullName   nuevo nombre
     * @param newDocument   nuevo documento
     * @param newSeat       nueva silla
     */
    public void updatePassenger(int reservationId, int detailId, String newFullName, String newDocument, Seat newSeat) {
        Reservation reservation = getReservation(reservationId);
        ReservationDetail targetDetail = reservation.getDetails().stream()
                .filter(d -> d.getId() == detailId)
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "No existe el detalle con id '%d' en la reserva #%d.".formatted(detailId, reservationId)));

        if (!targetDetail.getSeat().equals(newSeat)) {
            changeSeat(reservationId, targetDetail.getPassengerDocument(), newSeat);
        }

        // Si el documento cambia, validar que no choque con otro pasajero de la reserva
        if (!targetDetail.getPassengerDocument().equalsIgnoreCase(newDocument.trim())) {
            boolean duplicate = reservation.getDetails().stream()
                    .anyMatch(d -> d.getId() != detailId && d.getPassengerDocument().equalsIgnoreCase(newDocument.trim()));
            if (duplicate) {
                throw new BusinessRuleException(
                        "Ya existe otro pasajero con el documento '%s' en esta reserva.".formatted(newDocument));
            }
            targetDetail.setPassengerDocument(newDocument);
        }

        targetDetail.setPassengerFullName(newFullName);
        reservationRepository.update(reservation);
    }

    /**
     * Elimina un pasajero de la reserva por su documento.
     *
     * @param reservationId     id de la reserva
     * @param passengerDocument documento del pasajero a retirar
     * @throws BusinessRuleException si al eliminar se dejara la reserva vacía
     */
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

    /**
     * Cancela y elimina una reserva completa junto con todos sus detalles.
     *
     * <p>Al eliminarse la reserva, todas sus sillas asignadas quedan automáticamente libres para el vuelo.</p>
     *
     * @param reservationId identificador de la reserva a cancelar
     */
    public void cancelReservation(int reservationId) {
        if (!reservationRepository.existsById(reservationId)) {
            throw new NotFoundException("reserva", reservationId);
        }
        reservationRepository.deleteById(reservationId);
    }

    /**
     * Cambia el vuelo de una reserva existente, asegurando que todas sus sillas estén libres en el nuevo vuelo.
     *
     * @param reservationId id de la reserva
     * @param newFlight     nuevo vuelo de destino
     * @throws BusinessRuleException si alguna de las sillas de los pasajeros está ocupada en el nuevo vuelo
     */
    public void changeReservationFlight(int reservationId, Object newFlight) {
        Reservation reservation = getReservation(reservationId);
        Validator.requireNotNull(newFlight, "nuevo vuelo");

        for (ReservationDetail detail : reservation.getDetails()) {
            if (isSeatOccupied(newFlight, detail.getSeat())) {
                throw new BusinessRuleException(
                        "La silla '%s' no está disponible en el nuevo vuelo solicitado.".formatted(detail.getSeat()));
            }
        }

        reservation.setFlight(newFlight);
        reservationRepository.update(reservation);
    }

    /**
     * Consulta todas las reservas asociadas a un cliente.
     *
     * @param customer cliente a consultar
     * @return lista de reservas del cliente
     */
    public List<Reservation> listReservationsByCustomer(Object customer) {
        return reservationRepository.findByCustomer(customer);
    }

    /**
     * Consulta todas las reservas registradas para un vuelo dado.
     *
     * @param flight vuelo a consultar
     * @return lista de reservas del vuelo
     */
    public List<Reservation> listReservationsByFlight(Object flight) {
        return reservationRepository.findByFlight(flight);
    }

    /**
     * Comprueba si un cliente tiene alguna reserva registrada.
     *
     * @param customer cliente a verificar
     * @return {@code true} si tiene reservas
     */
    public boolean customerHasReservations(Object customer) {
        return reservationRepository.existsByCustomer(customer);
    }

    /**
     * Comprueba si un vuelo tiene alguna reserva registrada.
     *
     * @param flight vuelo a verificar
     * @return {@code true} si tiene reservas
     */
    public boolean flightHasReservations(Object flight) {
        return reservationRepository.existsByFlight(flight);
    }

    /**
     * Determina si una silla específica ya se encuentra ocupada por cualquier pasajero en un vuelo.
     *
     * @param flight vuelo a inspeccionar
     * @param seat   silla a consultar
     * @return {@code true} si la silla está ocupada; {@code false} si está libre
     */
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

    /**
     * Lista todas las sillas actualmente ocupadas en un vuelo, ordenadas de menor a mayor por fila y número.
     *
     * @param flight vuelo a consultar
     * @return lista ordenada sin duplicados de sillas ocupadas
     */
    public List<Seat> listOccupiedSeats(Object flight) {
        List<Reservation> flightReservations = reservationRepository.findByFlight(flight);
        Set<Seat> occupied = new TreeSet<>();
        for (Reservation reservation : flightReservations) {
            occupied.addAll(reservation.getSeats());
        }
        return new ArrayList<>(occupied);
    }

    /**
     * Cuenta la cantidad total de pasajeros confirmados en un vuelo dado entre todas sus reservas.
     *
     * @param flight vuelo a contabilizar
     * @return total de pasajeros
     */
    public int countPassengersByFlight(Object flight) {
        List<Reservation> flightReservations = reservationRepository.findByFlight(flight);
        int total = 0;
        for (Reservation reservation : flightReservations) {
            total += reservation.passengerCount();
        }
        return total;
    }

    /**
     * Genera el manifiesto oficial de pasajeros para un vuelo, ordenado por la silla asignada.
     *
     * @param flight vuelo a consultar
     * @return lista ordenada por silla de {@link ManifestEntry}
     */
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
}
