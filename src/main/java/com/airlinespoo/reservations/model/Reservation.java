package com.airlinespoo.reservations.model;

import com.airlinespoo.core.exception.BusinessRuleException;
import com.airlinespoo.core.exception.ValidationException;
import com.airlinespoo.core.model.Identifiable;
import com.airlinespoo.core.util.Validator;
import com.airlinespoo.customers.model.Customer;

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
 * {@link ReservationDetail} (pasajeros con sus sillas asignadas). La reserva encapsula su colección
 * interna y garantiza las reglas de integridad de pasajeros y sillas dentro de la misma reserva.</p>
 *
 * <p>Equivale a la tabla {@code tblReservations}.</p>
 */
public class Reservation implements Identifiable<Integer> {

    public static final int MIN_PASSENGERS = 1;
    public static final int MAX_PASSENGERS = 9;

    private final int id;
    private final Customer customer;
    private Object flight;
    private final List<ReservationDetail> details = new ArrayList<>();

    /**
     * Crea una reserva con su primer pasajero obligatorio.
     *
     * @param id             identificador numérico único positivo
     * @param customer       cliente titular (Módulo 1, obligatorio)
     * @param flight         vuelo reservado (obligatorio)
     * @param firstPassenger primer pasajero y detalle inicial (obligatorio)
     */
    public Reservation(int id, Customer customer, Object flight, ReservationDetail firstPassenger) {
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
     * @param customer       cliente titular (Módulo 1, obligatorio)
     * @param flight         vuelo reservado (obligatorio)
     * @param initialDetails lista inicial de detalles (entre 1 y 9 pasajeros)
     */
    public Reservation(int id, Customer customer, Object flight, List<ReservationDetail> initialDetails) {
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
     * @return el cliente titular de la reserva (entidad del Módulo 1)
     */
    public Customer getCustomer() {
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
     * @return lista no modificable de {@link ReservationDetail}
     */
    public List<ReservationDetail> getDetails() {
        return Collections.unmodifiableList(new ArrayList<>(details));
    }

    /**
     * Agrega un nuevo pasajero con su silla a la reserva validando las reglas internas.
     *
     * @param detail detalle del pasajero a agregar
     */
    public void addDetail(ReservationDetail detail) {
        Validator.requireNotNull(detail, "detalle de pasajero");

        if (details.size() >= MAX_PASSENGERS) {
            throw new BusinessRuleException(
                    "No se pueden agregar más de %d pasajeros a esta reserva.".formatted(MAX_PASSENGERS));
        }

        for (ReservationDetail existing : details) {
            if (existing.getId() == detail.getId()) {
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
                                .formatted(ManifestEntry.formatSeat(detail.getSeat())));
            }
        }

        details.add(detail);
    }

    /**
     * Reemplaza un detalle existente por una nueva instancia inmutable validando unicidad.
     *
     * @param detailId  identificador del detalle a reemplazar
     * @param newDetail nuevo detalle con el mismo id
     */
    public void replaceDetail(int detailId, ReservationDetail newDetail) {
        Validator.requireNotNull(newDetail, "detalle de pasajero");
        int targetIndex = -1;
        for (int i = 0; i < details.size(); i++) {
            ReservationDetail current = details.get(i);
            if (current.getId() == detailId) {
                targetIndex = i;
            } else {
                if (current.getPassengerDocument().equalsIgnoreCase(newDetail.getPassengerDocument())) {
                    throw new BusinessRuleException(
                            "El pasajero con documento '%s' ya está registrado en esta reserva."
                                    .formatted(newDetail.getPassengerDocument()));
                }
                if (current.getSeat().equals(newDetail.getSeat())) {
                    throw new BusinessRuleException(
                            "La silla '%s' ya está asignada a otro pasajero en esta reserva."
                                    .formatted(ManifestEntry.formatSeat(newDetail.getSeat())));
                }
            }
        }
        if (targetIndex < 0) {
            throw new BusinessRuleException("No se encontró el detalle #%d en la reserva.".formatted(detailId));
        }
        details.set(targetIndex, newDetail);
    }

    /**
     * Elimina un pasajero de la reserva por su identificador de detalle.
     *
     * @param detailId identificador del detalle a remover
     * @return {@code true} si el detalle fue eliminado
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
     * @return {@code true} si el pasajero fue eliminado
     */
    public boolean removePassengerByDocument(String passengerDocument) {
        if (passengerDocument == null) return false;
        if (details.size() <= MIN_PASSENGERS) {
            throw new BusinessRuleException(
                    "No se puede eliminar el pasajero: la reserva debe tener al menos un pasajero.");
        }
        return details.removeIf(detail -> detail.getPassengerDocument().equalsIgnoreCase(passengerDocument.trim()));
    }

    public boolean hasPassengerWithDocument(String document) {
        if (document == null) return false;
        String clean = document.trim();
        return details.stream().anyMatch(d -> d.getPassengerDocument().equalsIgnoreCase(clean));
    }

    public boolean hasSeat(Seat seat) {
        if (seat == null) return false;
        return details.stream().anyMatch(d -> d.getSeat().equals(seat));
    }

    public Set<Seat> getSeats() {
        Set<Seat> seats = new HashSet<>();
        for (ReservationDetail detail : details) {
            seats.add(detail.getSeat());
        }
        return seats;
    }

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
        return "Reserva #" + id + " [Cliente: " + customer.getFullName() + " (" + customer.getId() + ")"
                + ", Vuelo: " + flight + ", Pasajeros: " + details.size() + "]";
    }
}
