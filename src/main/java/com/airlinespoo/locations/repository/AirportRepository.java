package com.airlinespoo.locations.repository;

import com.airlinespoo.core.repository.InMemoryRepository;
import com.airlinespoo.core.util.Validator;
import com.airlinespoo.locations.model.Airport;
import com.airlinespoo.locations.model.City;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio en memoria de aeropuertos (tabla {@code tblAirports}).
 *
 * <p>La clave del mapa es el {@link UUID} del aeropuerto; {@code UUID} ya trae
 * {@code equals} y {@code hashCode} correctos, por eso funciona bien como clave.</p>
 *
 * <p>Agrega las búsquedas por ciudad y por nombre. No contiene reglas de negocio.</p>
 */
public class AirportRepository extends InMemoryRepository<Airport, UUID> {

    @Override
    protected String entityName() {
        return "aeropuerto";
    }

    /**
     * Lista los aeropuertos de una ciudad ({@code SELECT ... WHERE CityId = ?}).
     *
     * @param city ciudad buscada
     * @return lista con los aeropuertos de esa ciudad (puede estar vacía)
     */
    public List<Airport> findByCity(City city) {
        return values().stream()
            .filter(airport -> airport.getCity().equals(city))
            .toList();
    }

    /**
     * Cuenta los aeropuertos de una ciudad ({@code SELECT COUNT(*) ... WHERE CityId = ?}).
     *
     * @param city ciudad buscada
     * @return cantidad de aeropuertos de esa ciudad
     */
    public long countByCity(City city) {
        return findByCity(city).size();
    }

    /**
     * Indica si una ciudad tiene al menos un aeropuerto ({@code SELECT 1 ... WHERE CityId = ?}).
     * Sirve para impedir eliminar ciudades con aeropuertos.
     *
     * @param city ciudad buscada
     * @return {@code true} si tiene aeropuertos
     */
    public boolean existsByCity(City city) {
        return countByCity(city) > 0;
    }

    /**
     * Indica si ya existe un aeropuerto con ese nombre en esa ciudad, sin distinguir
     * mayúsculas. Sirve para la regla "no dos aeropuertos con el mismo nombre en la misma ciudad".
     *
     * @param name nombre buscado
     * @param city ciudad buscada
     * @return {@code true} si existe
     */
    public boolean existsByNameAndCity(String name, City city) {
        String target = Validator.normalize(name);
        if (target == null) {
            return false;
        }
        return findByCity(city).stream()
            .anyMatch(airport -> airport.getName().equalsIgnoreCase(target));
    }

    /**
     * Busca aeropuertos cuyo nombre contenga el texto, sin distinguir mayúsculas
     * ({@code SELECT ... WHERE name LIKE ?}).
     *
     * @param text parte del nombre
     * @return lista con las coincidencias (puede estar vacía)
     */
    public List<Airport> findByNameContaining(String text) {
        String target = Validator.normalize(text);
        if (target == null) {
            return List.of();
        }
        String lower = target.toLowerCase();
        return values().stream()
            .filter(airport -> airport.getName().toLowerCase().contains(lower))
            .toList();
    }
}
