package com.airlinespoo.reservations.repository;

import com.airlinespoo.core.model.Identifiable;
import com.airlinespoo.core.repository.InMemoryRepository;
import com.airlinespoo.core.util.Validator;
import com.airlinespoo.reservations.model.Reservation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Repositorio en memoria para la gestión y persistencia de reservas aéreas.
 *
 * <p>Extiende de {@link InMemoryRepository} implementando el contrato CRUD del sistema para
 * la entidad {@link Reservation} con clave primaria entera. Los detalles (pasajeros) no tienen
 * un repositorio independiente debido al principio de composición: residen y se gestionan
 * exclusivamente a través de la reserva titular.</p>
 *
 * <p>Proporciona las consultas complementarias especificadas en el mapa CRUD:</p>
 * <ul>
 *   <li>Búsqueda de reservas por cliente ({@link #findByCustomer(Object)}).</li>
 *   <li>Búsqueda de reservas por vuelo ({@link #findByFlight(Object)}).</li>
 *   <li>Comprobación de existencia de reservas por cliente y por vuelo para integridad referencial.</li>
 * </ul>
 */
public class ReservationRepository extends InMemoryRepository<Reservation, Integer> {

    @Override
    protected String entityName() {
        return "reserva";
    }

    /**
     * Busca todas las reservas asociadas a un cliente específico.
     *
     * @param customer objeto cliente o identificador del cliente
     * @return lista copia de reservas asociadas al cliente, o vacía si no tiene
     */
    public List<Reservation> findByCustomer(Object customer) {
        Validator.requireNotNull(customer, "cliente a consultar");
        List<Reservation> results = new ArrayList<>();
        for (Reservation reservation : values()) {
            if (matchesCustomer(reservation, customer)) {
                results.add(reservation);
            }
        }
        return results;
    }

    /**
     * Busca todas las reservas asociadas a un vuelo específico.
     *
     * @param flight objeto vuelo o identificador numérico del vuelo
     * @return lista copia de reservas asociadas al vuelo, o vacía si no tiene
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
     * <p>Utilizado para validar la regla de integridad: no eliminar clientes con reservas.</p>
     *
     * @param customer cliente a verificar
     * @return {@code true} si tiene al menos una reserva
     */
    public boolean existsByCustomer(Object customer) {
        if (customer == null) return false;
        return values().stream().anyMatch(r -> matchesCustomer(r, customer));
    }

    /**
     * Determina si existen reservas registradas para un vuelo dado.
     *
     * <p>Utilizado para validar la regla de integridad: no eliminar vuelos con reservas.</p>
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

    /**
     * Comprueba si una reserva corresponde a un criterio de cliente dado (objeto o id).
     */
    private boolean matchesCustomer(Reservation reservation, Object target) {
        Object current = reservation.getCustomer();
        if (Objects.equals(current, target)) {
            return true;
        }
        if (target instanceof String targetId) {
            if (current instanceof Identifiable<?> ident && Objects.equals(ident.getId(), targetId)) {
                return true;
            }
            return current.toString().equalsIgnoreCase(targetId);
        }
        if (current instanceof Identifiable<?> currentIdent && target instanceof Identifiable<?> targetIdent) {
            return Objects.equals(currentIdent.getId(), targetIdent.getId());
        }
        return false;
    }

    /**
     * Comprueba si una reserva corresponde a un criterio de vuelo dado (objeto o id).
     */
    private boolean matchesFlight(Reservation reservation, Object target) {
        Object current = reservation.getFlight();
        if (Objects.equals(current, target)) {
            return true;
        }
        if (target instanceof Integer targetId) {
            if (current instanceof Identifiable<?> ident && Objects.equals(ident.getId(), targetId)) {
                return true;
            }
            if (current instanceof Number number && number.intValue() == targetId) {
                return true;
            }
        }
        if (current instanceof Identifiable<?> currentIdent && target instanceof Identifiable<?> targetIdent) {
            return Objects.equals(currentIdent.getId(), targetIdent.getId());
        }
        return false;
    }
}
