package com.airlinespoo.locations.service;

import com.airlinespoo.core.exception.BusinessRuleException;
import com.airlinespoo.core.exception.DuplicateException;
import com.airlinespoo.core.exception.NotFoundException;
import com.airlinespoo.core.util.IdGenerator;
import com.airlinespoo.core.util.Validator;
import com.airlinespoo.locations.model.Airport;
import com.airlinespoo.locations.model.City;
import com.airlinespoo.locations.repository.AirportRepository;
import com.airlinespoo.locations.repository.CityRepository;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Reglas de negocio de ciudades y aeropuertos (Módulo 2 · Ubicaciones).
 *
 * <p>Coordina: pide datos a los repositorios, comprueba las reglas y lanza excepciones.
 * No imprime ni lee de la consola.</p>
 *
 * <p>Recibe sus dependencias por el constructor (inyección de dependencias manual):
 * no crea los repositorios ni el generador, se los entregan.</p>
 */
public class LocationService {

    /** Orden alfabético por nombre, sin distinguir mayúsculas. */
    private static final Comparator<City> CITY_BY_NAME =
        Comparator.comparing(City::getName, String.CASE_INSENSITIVE_ORDER);
    private static final Comparator<Airport> AIRPORT_BY_NAME =
        Comparator.comparing(Airport::getName, String.CASE_INSENSITIVE_ORDER);

    private final CityRepository cityRepository;
    private final AirportRepository airportRepository;
    private final IdGenerator cityIds;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param cityRepository    repositorio de ciudades
     * @param airportRepository repositorio de aeropuertos
     * @param cityIds           generador de ids de ciudades (arranque 1, paso 1)
     */
    public LocationService(CityRepository cityRepository, AirportRepository airportRepository,
                           IdGenerator cityIds) {
        this.cityRepository = cityRepository;
        this.airportRepository = airportRepository;
        this.cityIds = cityIds;
    }

    // ------------------------------------------------------------------
    // Ciudades
    // ------------------------------------------------------------------

    /**
     * Registra una ciudad nueva (INSERT en {@code tblCities}).
     *
     * @param name nombre de la ciudad
     * @return la ciudad creada, con su id
     * @throws com.airlinespoo.core.exception.ValidationException si el nombre no es válido
     * @throws DuplicateException si ya existe una ciudad con ese nombre (sin distinguir mayúsculas)
     */
    public City createCity(String name) {
        // peekNext() consulta el id sin gastarlo: si el nombre es inválido o repetido,
        // el número no se pierde y los ids siguen siendo consecutivos.
        City city = new City(cityIds.peekNext(), name);
        cityRepository.findByName(city.getName()).ifPresent(other -> {
            throw new DuplicateException("Ya existe una ciudad llamada '%s'.".formatted(other.getName()));
        });
        cityRepository.create(city);
        cityIds.next();
        return city;
    }

    /**
     * Consulta una ciudad por su id.
     *
     * @param id id de la ciudad
     * @return la ciudad
     * @throws NotFoundException si no existe
     */
    public City getCity(int id) {
        return cityRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("ciudad", id));
    }

    /**
     * Lista todas las ciudades ordenadas por nombre.
     *
     * @return copia de la lista de ciudades
     */
    public List<City> listCities() {
        return cityRepository.findAll().stream()
            .sorted(CITY_BY_NAME)
            .toList();
    }

    /**
     * Cambia el nombre de una ciudad (UPDATE ... SET name).
     * Puede quedar igual a su propio nombre, pero no al de otra ciudad.
     *
     * @param id      id de la ciudad
     * @param newName nombre nuevo
     * @return la ciudad actualizada
     * @throws NotFoundException  si la ciudad no existe
     * @throws DuplicateException si otra ciudad ya tiene ese nombre
     */
    public City updateCity(int id, String newName) {
        City city = getCity(id);
        cityRepository.findByName(newName)
            .filter(other -> !other.equals(city))
            .ifPresent(other -> {
                throw new DuplicateException("Ya existe una ciudad llamada '%s'.".formatted(other.getName()));
            });
        city.setName(newName);
        return cityRepository.update(city);
    }

    /**
     * Elimina una ciudad, solo si no tiene aeropuertos (efecto de la clave foránea).
     *
     * @param id id de la ciudad
     * @throws NotFoundException     si la ciudad no existe
     * @throws BusinessRuleException si la ciudad tiene aeropuertos
     */
    public void deleteCity(int id) {
        City city = getCity(id);
        if (airportRepository.existsByCity(city)) {
            throw new BusinessRuleException(
                "No se puede eliminar '%s' porque tiene aeropuertos registrados.".formatted(city.getName()));
        }
        cityRepository.deleteById(id);
    }

    /**
     * Busca ciudades cuyo nombre contenga el texto (coincidencia parcial).
     *
     * @param text parte del nombre
     * @return ciudades encontradas, ordenadas por nombre
     */
    public List<City> searchCitiesByName(String text) {
        return cityRepository.findByNameContaining(text).stream()
            .sorted(CITY_BY_NAME)
            .toList();
    }

    // ------------------------------------------------------------------
    // Aeropuertos
    // ------------------------------------------------------------------

    /**
     * Registra un aeropuerto nuevo (INSERT en {@code tblAirports}). El UUID se genera al crearlo.
     *
     * @param name   nombre del aeropuerto
     * @param cityId id de la ciudad donde está
     * @return el aeropuerto creado
     * @throws NotFoundException  si la ciudad no existe
     * @throws DuplicateException si ya hay un aeropuerto con ese nombre en esa ciudad
     */
    public Airport createAirport(String name, int cityId) {
        City city = getCity(cityId);
        Airport airport = new Airport(name, city);
        if (airportRepository.existsByNameAndCity(airport.getName(), city)) {
            throw new DuplicateException("Ya existe el aeropuerto '%s' en %s."
                .formatted(airport.getName(), city.getName()));
        }
        return airportRepository.create(airport);
    }

    /**
     * Consulta un aeropuerto por su UUID.
     *
     * @param id UUID del aeropuerto
     * @return el aeropuerto
     * @throws NotFoundException si no existe
     */
    public Airport getAirport(UUID id) {
        return airportRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("aeropuerto", id));
    }

    /**
     * Lista todos los aeropuertos ordenados por nombre.
     *
     * @return copia de la lista de aeropuertos
     */
    public List<Airport> listAirports() {
        return airportRepository.findAll().stream()
            .sorted(AIRPORT_BY_NAME)
            .toList();
    }

    /**
     * Cambia el nombre y la ciudad de un aeropuerto (UPDATE ... SET name, CityId).
     *
     * @param id        UUID del aeropuerto
     * @param newName   nombre nuevo
     * @param newCityId id de la ciudad nueva (puede ser la misma)
     * @return el aeropuerto actualizado
     * @throws NotFoundException  si el aeropuerto o la ciudad no existen
     * @throws DuplicateException si otro aeropuerto de esa ciudad ya tiene ese nombre
     */
    public Airport updateAirport(UUID id, String newName, int newCityId) {
        Airport airport = getAirport(id);
        City newCity = getCity(newCityId);
        String name = normalizeForSearch(newName);
        boolean taken = airportRepository.findByCity(newCity).stream()
            .filter(other -> !other.equals(airport))
            .anyMatch(other -> other.getName().equalsIgnoreCase(name));
        if (taken) {
            throw new DuplicateException("Ya existe el aeropuerto '%s' en %s."
                .formatted(name, newCity.getName()));
        }
        airport.setName(newName);
        airport.setCity(newCity);
        return airportRepository.update(airport);
    }

    /**
     * Elimina un aeropuerto. Por ahora se permite; en la integración se impedirá si tiene vuelos.
     *
     * @param id UUID del aeropuerto
     * @throws NotFoundException si no existe
     */
    public void deleteAirport(UUID id) {
        getAirport(id);
        airportRepository.deleteById(id);
    }

    /**
     * Lista los aeropuertos de una ciudad, ordenados por nombre.
     *
     * @param cityId id de la ciudad
     * @return aeropuertos de esa ciudad
     * @throws NotFoundException si la ciudad no existe
     */
    public List<Airport> listAirportsByCity(int cityId) {
        City city = getCity(cityId);
        return airportRepository.findByCity(city).stream()
            .sorted(AIRPORT_BY_NAME)
            .toList();
    }

    /**
     * Busca aeropuertos cuyo nombre contenga el texto (coincidencia parcial).
     *
     * @param text parte del nombre
     * @return aeropuertos encontrados, ordenados por nombre
     */
    public List<Airport> searchAirportsByName(String text) {
        return airportRepository.findByNameContaining(text).stream()
            .sorted(AIRPORT_BY_NAME)
            .toList();
    }

    /**
     * Cuenta los aeropuertos de una ciudad.
     *
     * @param cityId id de la ciudad
     * @return cantidad de aeropuertos
     * @throws NotFoundException si la ciudad no existe
     */
    public long countAirportsByCity(int cityId) {
        return airportRepository.countByCity(getCity(cityId));
    }

    /**
     * Limpia espacios sobrantes para comparar nombres; si es {@code null}, devuelve texto vacío.
     */
    private static String normalizeForSearch(String text) {
        String normalized = Validator.normalize(text);
        return normalized == null ? "" : normalized;
    }
}
