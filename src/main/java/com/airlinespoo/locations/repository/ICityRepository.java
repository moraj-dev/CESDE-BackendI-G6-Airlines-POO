package com.airlinespoo.locations.repository;

import com.airlinespoo.core.repository.Repository;
import com.airlinespoo.locations.model.City;

import java.util.List;
import java.util.Optional;

/**
 * Contrato del repositorio de ciudades (tabla {@code tblCities}).
 *
 * <p>Hereda las operaciones CRUD de {@link Repository} y agrega las búsquedas por nombre.
 * La implementa {@link CityRepository}, que guarda los datos en SQL Server con JDBC.
 * El servicio solo conoce esta interfaz, así que no sabe ni le importa dónde se guardan
 * los datos.</p>
 */
public interface ICityRepository extends Repository<City, Integer> {

    /**
     * Busca una ciudad por su nombre exacto, sin distinguir mayúsculas
     * ({@code SELECT ... WHERE name = ?}).
     *
     * @param name nombre buscado
     * @return la ciudad, o vacío si no existe
     */
    Optional<City> findByName(String name);

    /**
     * Indica si ya existe una ciudad con ese nombre, sin distinguir mayúsculas.
     *
     * @param name nombre buscado
     * @return {@code true} si existe
     */
    boolean existsByName(String name);

    /**
     * Busca ciudades cuyo nombre contenga el texto, sin distinguir mayúsculas
     * ({@code SELECT ... WHERE name LIKE ?}).
     *
     * @param text parte del nombre
     * @return lista con las coincidencias (puede estar vacía)
     */
    List<City> findByNameContaining(String text);
}
