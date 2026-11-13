package com.airlinespoo.locations.repository;

import com.airlinespoo.core.repository.InMemoryRepository;
import com.airlinespoo.core.util.Validator;
import com.airlinespoo.locations.model.City;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio en memoria de ciudades (tabla {@code tblCities}).
 *
 * <p>Hereda de {@link InMemoryRepository} las operaciones comunes ({@code create},
 * {@code findById}, {@code findAll}, {@code update}, {@code deleteById},
 * {@code existsById}, {@code count}) y agrega las búsquedas por nombre.</p>
 *
 * <p>No contiene reglas de negocio ni lanza "no encontrado": eso lo decide el servicio.</p>
 */
public class CityRepository extends InMemoryRepository<City, Integer> {

    @Override
    protected String entityName() {
        return "ciudad";
    }

    /**
     * Busca una ciudad por su nombre exacto, sin distinguir mayúsculas
     * ({@code SELECT ... WHERE name = ?}).
     *
     * @param name nombre buscado
     * @return la ciudad, o vacío si no existe
     */
    public Optional<City> findByName(String name) {
        String target = Validator.normalize(name);
        if (target == null) {
            return Optional.empty();
        }
        for (City city : values()) {
            if (city.getName().equalsIgnoreCase(target)) {
                return Optional.of(city);
            }
        }
        return Optional.empty();
    }

    /**
     * Indica si ya existe una ciudad con ese nombre, sin distinguir mayúsculas
     * ({@code SELECT 1 ... WHERE name = ?}).
     *
     * @param name nombre buscado
     * @return {@code true} si existe
     */
    public boolean existsByName(String name) {
        return findByName(name).isPresent();
    }

    /**
     * Busca ciudades cuyo nombre contenga el texto, sin distinguir mayúsculas
     * ({@code SELECT ... WHERE name LIKE ?}).
     *
     * @param text parte del nombre
     * @return lista con las coincidencias (puede estar vacía)
     */
    public List<City> findByNameContaining(String text) {
        String target = Validator.normalize(text);
        if (target == null) {
            return List.of();
        }
        String lower = target.toLowerCase();
        return values().stream()
            .filter(city -> city.getName().toLowerCase().contains(lower))
            .toList();
    }
}
