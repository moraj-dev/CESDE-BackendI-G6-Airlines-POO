package com.airlinespoo.airlines.repository;

import com.airlinespoo.airlines.model.Airline;
import com.airlinespoo.core.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de almacenamiento para aerolíneas ({@code tblAirlines}).
 *
 * <p>Extiende el contrato CRUD común ({@link Repository}) y agrega las consultas
 * específicas por nombre.</p>
 */
public interface IAirlineRepository extends Repository<Airline, Integer> {

    /**
     * Busca una aerolínea por su nombre exacto, sin distinguir mayúsculas ni minúsculas
     * ({@code SELECT ... WHERE LOWER(name) = LOWER(?)}).
     *
     * @param name nombre a buscar
     * @return la aerolínea encontrada, o {@link Optional#empty()} si no existe
     */
    Optional<Airline> findByName(String name);

    /**
     * Indica si existe una aerolínea con ese nombre exacto, sin distinguir mayúsculas
     * ({@code SELECT 1 ... WHERE LOWER(name) = LOWER(?)}).
     *
     * @param name nombre a verificar
     * @return {@code true} si ya existe una aerolínea con ese nombre
     */
    boolean existsByName(String name);

    /**
     * Busca aerolíneas cuyo nombre contenga el texto indicado, sin distinguir mayúsculas
     * ({@code SELECT ... WHERE LOWER(name) LIKE ?}).
     *
     * @param query texto parcial a buscar
     * @return copia de las aerolíneas coincidentes
     */
    List<Airline> searchByName(String query);
}
