package com.airlinespoo.locations.repository;

import com.airlinespoo.core.repository.Repository;
import com.airlinespoo.locations.model.Airport;
import com.airlinespoo.locations.model.City;

import java.util.List;
import java.util.UUID;

/**
 * Contrato del repositorio de aeropuertos (tabla {@code tblAirports}).
 *
 * <p>Hereda las operaciones CRUD de {@link Repository} y agrega las búsquedas por ciudad
 * y por nombre. La implementa {@link AirportRepository}, que guarda los datos en SQL Server
 * con JDBC.</p>
 */
public interface IAirportRepository extends Repository<Airport, UUID> {

    /**
     * Lista los aeropuertos de una ciudad ({@code SELECT ... WHERE CityId = ?}).
     *
     * @param city ciudad buscada
     * @return lista con los aeropuertos de esa ciudad (puede estar vacía)
     */
    List<Airport> findByCity(City city);

    /**
     * Cuenta los aeropuertos de una ciudad ({@code SELECT COUNT(*) ... WHERE CityId = ?}).
     *
     * @param city ciudad buscada
     * @return cantidad de aeropuertos de esa ciudad
     */
    long countByCity(City city);

    /**
     * Indica si una ciudad tiene al menos un aeropuerto ({@code SELECT 1 ... WHERE CityId = ?}).
     *
     * @param city ciudad buscada
     * @return {@code true} si tiene aeropuertos
     */
    boolean existsByCity(City city);

    /**
     * Indica si ya existe un aeropuerto con ese nombre en esa ciudad, sin distinguir mayúsculas.
     *
     * @param name nombre buscado
     * @param city ciudad buscada
     * @return {@code true} si existe
     */
    boolean existsByNameAndCity(String name, City city);

    /**
     * Busca aeropuertos cuyo nombre contenga el texto, sin distinguir mayúsculas
     * ({@code SELECT ... WHERE name LIKE ?}).
     *
     * @param text parte del nombre
     * @return lista con las coincidencias (puede estar vacía)
     */
    List<Airport> findByNameContaining(String text);
}
