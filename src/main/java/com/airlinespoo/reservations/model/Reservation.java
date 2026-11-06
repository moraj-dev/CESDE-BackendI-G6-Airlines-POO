package com.airlinespoo.reservations.model;

import com.airlinespoo.core.exception.BusinessRuleException;
import com.airlinespoo.core.exception.ValidationException;
import com.airlinespoo.core.model.Identifiable;
import com.airlinespoo.core.util.Validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Representa una reserva aérea en el sistema.
 *
 * <p>Aplica el principio de <b>composición</b>: una reserva es la dueña del ciclo de vida de sus
 * {@link ReservationDetail} (pasajeros con sus sillas asignadas). Un detalle no tiene sentido sin
 * su reserva correspondiente. La reserva encapsula y controla su colección interna de pasajeros,
 * impidiendo la exposición directa de colecciones mutables y garantizando las siguientes reglas internas:</p>
 *
 * <ul>
 *   <li>Al menos {@value #MIN_PASSENGERS} pasajero y como máximo {@value #MAX_PASSENGERS} pasajeros.</li>
 *   <li>No puede haber dos pasajeros con el mismo documento en una misma reserva.</li>
 *   <li>No puede haber dos pasajeros con la misma silla en una misma reserva.</li>
 *   <li>Al eliminar un pasajero, la reserva nunca puede quedar vacía.</li>
 * </ul>
 *
 * <p>Equivale a la tabla {@code tblReservations}.</p>
 */
public class Reservation implements Identifiable<Integer> {

    /**
     * Número mínimo de pasajeros requeridos en una reserva.
     */
    public static final int MIN_PASSENGERS = 1;

    /**
     * Número máximo de pasajeros permitidos en una misma reserva.
     */
    public static final int MAX_PASSENGERS = 9;

    private final int id;
    private final Object customer;
    private Object flight;
    private final List<ReservationDetail> details = new ArrayList<>();

    /**
     * Crea una reserva con su primer pasajero obligatorio.
     *
     * @param id             identificador numérico único positivo
     * @param customer       cliente que realiza la reserva (obligatorio)
     * @param flight         vuelo reservado (obligatorio)
     * @param firstPassenger primer pasajero y detalle inicial (obligatorio)
     * @throws ValidationException   si falta un dato obligatorio
     * @throws BusinessRuleException si no se cumplen las reglas de negocio
     */
    public Reservation(int id, Object customer, Object flight, ReservationDetail firstPassenger) {
        this.id = (int) Validator.requirePositive(id, "id de reserva");
        this.customer = Validator.requireNotNull(customer, "cliente");
        this.flight = Validator.requireNotNull(flight, "vuelo");
        Validator.requireNotNull(firstPassenger, "primer pasajero");
        addDetail(firstPassenger);
    }

    /**
     * Crea una reserva con una lista inicial de pasajeros.
     *
     * @param id             identificador numérico único positivo
     * @param customer       cliente que realiza la reserva (obligatorio)
     * @param flight         vuelo reservado (obligatorio)
     * @param initialDetails lista inicial de detalles (debe tener entre 1 y 9 pasajeros)
     * @throws ValidationException   si falta un dato obligatorio
     * @throws BusinessRuleException si hay datos repetidos o excede la capacidad
     */
    public Reservation(int id, Object customer, Object flight, List<ReservationDetail> initialDetails) {
        this.id = (int) Validator.requirePositive(id, "id de reserva");
        this.customer = Validator.requireNotNull(customer, "cliente");
        this.flight = Validator.requireNotNull(flight, "vuelo");
        Validator.requireNotNull(initialDetails, "lista de pasajeros inicial");

        if (initialDetails.isEmpty()) {
            throw new BusinessRuleException("Una reserva debe tener al menos un pasajero.");
        }
        if (initialDetails.size() > MAX_PASSENGERS) {
            throw new BusinessRuleException(
                    "Una reserva no puede tener más de %d pasajeros.".formatted(MAX_PASSENGERS));
        }

        for (ReservationDetail detail : initialDetails) {
            addDetail(detail);
        }
    }

    @Override
    public Integer getId() {
        return id;
    }

    /**
     * @return el cliente titular de la reserva
     */
    public Object getCustomer() {
        return customer;
    }

    /**
     * @return el vuelo reservado
     */
    public Object getFlight() {
        return flight;
    }

    /**
     * Permite reprogramar o cambiar el vuelo de la reserva.
     *
     * @param flight nuevo vuelo (obligatorio)
     */
    public void setFlight(Object flight) {
        this.flight = Validator.requireNotNull(flight, "vuelo");
    }

    /**
     * Devuelve una vista de solo lectura de los detalles de la reserva.
     *
     * <p>Aplica copia defensiva / vista no modificable para proteger la colección interna de alteraciones externas.</p>
     *
     * @return lista no modificable de {@link ReservationDetail}
     */
    public List<ReservationDetail> getDetails() {
        return Collections.unmodifiableList(new ArrayList<>(details));
    }

    /**
     * Agrega un nuevo pasajero con su silla a la reserva validando las reglas internas.
     *
     * @param detail detalle del pasajero a agregar
     * @throws BusinessRuleException si se supera el cupo máximo, si el documento ya existe o si la silla está duplicada
     */
    public void addDetail(ReservationDetail detail) {
        Validator.requireNotNull(detail, "detalle de pasajero");

        if (details.size() >= MAX_PASSENGERS) {
            throw new BusinessRuleException(
                    "No se pueden agregar más de %d pasajeros a esta reserva.".formatted(MAX_PASSENGERS));
        }

        for (ReservationDetail existing : details) {
            if (existing.getId().equals(detail.getId())) {
                throw new BusinessRuleException(
                        "Ya existe un detalle con el id '%d' en esta reserva.".formatted(detail.getId()));
            }
            if (existing.getPassengerDocument().equalsIgnoreCase(detail.getPassengerDocument())) {
                throw new BusinessRuleException(
                        "El pasajero con documento '%s' ya está registrado en esta reserva."
                                .formatted(detail.getPassengerDocument()));
            }
            if (existing.getSeat().equals(detail.getSeat())) {
                throw new BusinessRuleException(
                        "La silla '%s' ya está asignada a otro pasajero en esta reserva."
                                .formatted(detail.getSeat()));
            }
        }

        details.add(detail);
    }

    /**
     * Elimina un pasajero de la reserva por su identificador de detalle.
     *
     * @param detailId identificador del detalle a remover
     * @return {@code true} si el detalle fue eliminado; {@code false} si no se encontró
     * @throws BusinessRuleException si al eliminar se dejaría la reserva sin pasajeros
     */
    public boolean removeDetail(int detailId) {
        if (details.size() <= MIN_PASSENGERS) {
            throw new BusinessRuleException(
                    "No se puede eliminar el pasajero: la reserva debe tener al menos un pasajero.");
        }
        return details.removeIf(detail -> detail.getId() == detailId);
    }

    /**
     * Elimina un pasajero de la reserva por su documento de identidad.
     *
     * @param passengerDocument documento del pasajero
     * @return {@code true} si el pasajero fue eliminado; {@code false} si no se encontró
     * @throws BusinessRuleException si al eliminar se dejaría la reserva sin pasajeros
     */
    public boolean removePassengerByDocument(String passengerDocument) {
        if (passengerDocument == null) {
            return false;
        }
        if (details.size() <= MIN_PASSENGERS) {
            throw new BusinessRuleException(
                    "No se puede eliminar el pasajero: la reserva debe tener al menos un pasajero.");
        }
        return details.removeIf(detail -> detail.getPassengerDocument().equalsIgnoreCase(passengerDocument.trim()));
    }

    /**
     * Indica si un documento de pasajero ya está registrado dentro de esta reserva.
     *
     * @param document documento buscado
     * @return {@code true} si ya existe
     */
    public boolean hasPassengerWithDocument(String document) {
        if (document == null) return false;
        String clean = document.trim();
        return details.stream().anyMatch(d -> d.getPassengerDocument().equalsIgnoreCase(clean));
    }

    /**
     * Indica si una silla ya está asignada en esta reserva.
     *
     * @param seat silla buscada
     * @return {@code true} si ya está asignada
     */
    public boolean hasSeat(Seat seat) {
        if (seat == null) return false;
        return details.stream().anyMatch(d -> d.getSeat().equals(seat));
    }

    /**
     * Obtiene el conjunto de todas las sillas asignadas en esta reserva.
     *
     * @return conjunto con las sillas de la reserva
     */
    public Set<Seat> getSeats() {
        Set<Seat> seats = new HashSet<>();
        for (ReservationDetail detail : details) {
            seats.add(detail.getSeat());
        }
        return seats;
    }

    /**
     * @return cantidad actual de pasajeros en la reserva
     */
    public int passengerCount() {
        return details.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reservation that = (Reservation) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Reserva #" + id + " [Cliente: " + customer + ", Vuelo: " + flight + ", Pasajeros: " + details.size() + "]";
    }
}
