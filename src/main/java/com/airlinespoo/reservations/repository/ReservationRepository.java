package com.airlinespoo.reservations.repository;

import com.airlinespoo.core.model.Identifiable;
import com.airlinespoo.core.repository.InMemoryRepository;
import com.airlinespoo.core.util.Validator;
import com.airlinespoo.customers.model.Customer;
import com.airlinespoo.flights.model.Flight;
import com.airlinespoo.reservations.model.Reservation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Repositorio en memoria para la gestión y persistencia de reservas aéreas.
 *
 * <p>Extiende de {@link InMemoryRepository} para {@link Reservation} con clave primaria entera.</p>
 */
public class ReservationRepository extends InMemoryRepository<Reservation, Integer> {

    @Override
    protected String entityName() {
        return "reserva";
    }

    /**
     * Busca todas las reservas asociadas a un cliente específico.
     *
     * @param customer cliente a consultar
     * @return lista copia de reservas asociadas al cliente
     */
    public List<Reservation> findByCustomer(Customer customer) {
        Validator.requireNotNull(customer, "cliente a consultar");
        return findByCustomerId(customer.getId());
    }

    /**
     * Busca todas las reservas asociadas a un documento de cliente.
     *
     * @param customerId documento/id del cliente
     * @return lista copia de reservas asociadas al cliente
     */
    public List<Reservation> findByCustomerId(String customerId) {
        Validator.requireText(customerId, "documento del cliente");
        List<Reservation> results = new ArrayList<>();
        for (Reservation reservation : values()) {
            if (reservation.getCustomer().getId().equalsIgnoreCase(customerId.trim())) {
                results.add(reservation);
            }
        }
        return results;
    }

    /**
     * Busca todas las reservas asociadas a un vuelo específico.
     *
     * @param flight objeto vuelo o identificador numérico del vuelo
     * @return lista copia de reservas asociadas al vuelo
     */
    public List<Reservation> findByFlight(Object flight) {
        Validator.requireNotNull(flight, "vuelo a consultar");
        List<Reservation> results = new ArrayList<>();
        for (Reservation reservation : values()) {
            if (matchesFlight(reservation, flight)) {
                results.add(reservation);
            }
        }
        return results;
    }

    /**
     * Determina si existen reservas registradas para un cliente dado.
     *
     * @param customerId documento/id del cliente
     * @return {@code true} si tiene al menos una reserva
     */
    public boolean existsByCustomer(String customerId) {
        if (customerId == null || customerId.isBlank()) return false;
        String cleanId = customerId.trim();
        return values().stream().anyMatch(r -> r.getCustomer().getId().equalsIgnoreCase(cleanId));
    }

    /**
     * Determina si existen reservas registradas para un vuelo dado.
     *
     * @param flight vuelo a verificar
     * @return {@code true} si tiene al menos una reserva
     */
    public boolean existsByFlight(Object flight) {
        if (flight == null) return false;
        return values().stream().anyMatch(r -> matchesFlight(r, flight));
    }

    /**
     * Retorna todas las reservas ordenadas por su identificador.
     *
     * @return copia ordenada de todas las reservas
     */
    public List<Reservation> findAllOrderedById() {
        List<Reservation> list = values();
        list.sort(Comparator.comparing(Reservation::getId));
        return list;
    }

    private boolean matchesFlight(Reservation reservation, Object target) {
        Object current = reservation.getFlight();
        if (Objects.equals(current, target)) {
            return true;
        }
        Integer currentId = extractFlightId(current);
        Integer targetId = extractFlightId(target);
        if (currentId != null && targetId != null) {
            return currentId.equals(targetId);
        }
        if (current instanceof Identifiable<?> currentIdent && target instanceof Identifiable<?> targetIdent) {
            return Objects.equals(currentIdent.getId(), targetIdent.getId());
        }
        return false;
    }

    private static Integer extractFlightId(Object flightObj) {
        if (flightObj instanceof Flight flight) {
            return flight.getId();
        }
        if (flightObj instanceof Number number) {
            return number.intValue();
        }
        if (flightObj instanceof Identifiable<?> identifiable && identifiable.getId() instanceof Number number) {
            return number.intValue();
        }
        return null;
    }
}
