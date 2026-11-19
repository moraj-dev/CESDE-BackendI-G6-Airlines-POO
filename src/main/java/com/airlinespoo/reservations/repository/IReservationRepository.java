package com.airlinespoo.reservations.repository;

import com.airlinespoo.core.repository.Repository;
import com.airlinespoo.customers.model.Customer;
import com.airlinespoo.reservations.model.Reservation;

import java.util.List;

/**
 * Contrato de almacenamiento para las reservas aéreas.
 *
 * <p>Permite intercambiar la implementación en memoria ({@link ReservationRepository})
 * por la implementación en SQL Server con JDBC ({@link ReservationJdbcRepository})
 * sin modificar {@code ReservationService} ni {@code ReservationMenu}.</p>
 */
public interface IReservationRepository extends Repository<Reservation, Integer> {

    List<Reservation> findByCustomer(Customer customer);

    List<Reservation> findByCustomerId(String customerId);

    List<Reservation> findByFlight(Object flight);

    boolean existsByCustomer(String customerId);

    boolean existsByFlight(Object flight);

    List<Reservation> findAllOrderedById();
}
