package com.airlinespoo.airlines.service;

import com.airlinespoo.airlines.model.Airline;
import com.airlinespoo.airlines.repository.IAirlineRepository;
import com.airlinespoo.core.exception.DuplicateException;
import com.airlinespoo.core.exception.NotFoundException;
import com.airlinespoo.core.util.IdGenerator;
import com.airlinespoo.core.util.Validator;

import java.util.Comparator;
import java.util.List;

/**
 * Reglas de negocio del Módulo 3 (Aerolíneas).
 *
 * <p>Coordina las operaciones sobre {@link Airline}: valida unicidad del nombre,
 * asigna identificadores consecutivos (100, 110, 120...) y delega la persistencia en
 * {@link IAirlineRepository}. No imprime ni lee de la consola.</p>
 */
public class AirlineService {

    /** Orden alfabético por nombre, sin distinguir mayúsculas ni minúsculas. */
    private static final Comparator<Airline> AIRLINE_BY_NAME =
            Comparator.comparing(Airline::getName, String.CASE_INSENSITIVE_ORDER);

    private final IAirlineRepository airlineRepository;
    private final IdGenerator airlineIds;

    /**
     * Crea el servicio de aerolíneas con sus dependencias inyectadas.
     *
     * @param airlineRepository repositorio de aerolíneas
     * @param airlineIds        generador de ids de aerolíneas (arranque 100, paso 10)
     */
    public AirlineService(IAirlineRepository airlineRepository, IdGenerator airlineIds) {
        this.airlineRepository = Validator.requireNotNull(airlineRepository, "repositorio de aerolíneas");
        this.airlineIds = Validator.requireNotNull(airlineIds, "generador de ids de aerolíneas");
    }

    /**
     * Registra una nueva aerolínea (equivalente a {@code uspCrearAerolineas} / {@code INSERT en tblAirlines}).
     *
     * @param name nombre de la aerolínea; obligatorio, máximo 50 caracteres y único
     * @return la aerolínea creada con su identificador asignado
     * @throws com.airlinespoo.core.exception.ValidationException si el nombre no es válido
     * @throws DuplicateException si ya existe una aerolínea con ese nombre (sin distinguir mayúsculas)
     */
    public Airline createAirline(String name) {
        // peekNext() evita consumir el id si el nombre es inválido o duplicado.
        Airline airline = new Airline(airlineIds.peekNext(), name);
        airlineRepository.findByName(airline.getName()).ifPresent(other -> {
            throw new DuplicateException("Ya existe una aerolínea llamada '%s'.".formatted(other.getName()));
        });
        Airline saved = airlineRepository.create(airline);
        airlineIds.next();
        return saved;
    }

    /**
     * Consulta una aerolínea por su identificador ({@code SELECT ... WHERE airlineId = ?}).
     *
     * @param id identificador de la aerolínea
     * @return la aerolínea encontrada
     * @throws NotFoundException si no existe ninguna aerolínea con ese id
     */
    public Airline getAirline(int id) {
        return airlineRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("aerolínea", id));
    }

    /**
     * Lista todas las aerolíneas ordenadas alfabéticamente por nombre
     * ({@code SELECT ... ORDER BY name}).
     *
     * @return lista inmutable de aerolíneas; modificarla no altera el repositorio
     */
    public List<Airline> listAirlines() {
        return airlineRepository.findAll().stream()
                .sorted(AIRLINE_BY_NAME)
                .toList();
    }

    /**
     * Renombra una aerolínea existente ({@code UPDATE tblAirlines SET name = ?}).
     *
     * <p>Permite conservar el mismo nombre (incluso cambiando mayúsculas/minúsculas), pero
     * rechaza si pertenece a otra aerolínea distinta.</p>
     *
     * @param id      identificador de la aerolínea a renombrar
     * @param newName nuevo nombre; obligatorio, máximo 50 caracteres
     * @return la aerolínea actualizada
     * @throws NotFoundException  si la aerolínea no existe
     * @throws DuplicateException si otra aerolínea ya tiene ese nombre
     * @throws com.airlinespoo.core.exception.ValidationException si el nuevo nombre no es válido
     */
    public Airline renameAirline(int id, String newName) {
        Airline existing = getAirline(id);
        String normalized = Validator.requireText(newName, "nombre de la aerolínea", Airline.MAX_NAME_LENGTH);

        airlineRepository.findByName(normalized).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new DuplicateException("Ya existe una aerolínea llamada '%s'.".formatted(other.getName()));
            }
        });

        existing.setName(normalized);
        return airlineRepository.update(existing);
    }

    /**
     * Elimina una aerolínea por su identificador ({@code DELETE ... WHERE airlineId = ?}).
     *
     * @param id identificador de la aerolínea a eliminar
     * @throws NotFoundException si no existe una aerolínea con ese id
     */
    public void deleteAirline(int id) {
        if (!airlineRepository.deleteById(id)) {
            throw new NotFoundException("aerolínea", id);
        }
    }

    /**
     * Busca aerolíneas cuyo nombre contenga el texto indicado, sin distinguir mayúsculas,
     * ordenadas alfabéticamente ({@code SELECT ... WHERE name LIKE ?}).
     *
     * @param query texto parcial a buscar
     * @return lista inmutable de aerolíneas coincidentes
     */
    public List<Airline> searchAirlinesByName(String query) {
        return airlineRepository.searchByName(query).stream()
                .sorted(AIRLINE_BY_NAME)
                .toList();
    }

    /**
     * Cuenta el total de aerolíneas registradas ({@code SELECT COUNT(*)}).
     *
     * @return cantidad total de aerolíneas
     */
    public long countAirlines() {
        return airlineRepository.count();
    }

    /**
     * Sincroniza el generador de ids con el mayor identificador actualmente almacenado
     * para evitar colisiones tras cargar datos iniciales.
     */
    public void syncIdGenerator() {
        int maxId = airlineRepository.findAll().stream()
                .mapToInt(Airline::getId)
                .max()
                .orElse(0);
        if (maxId > 0) {
            airlineIds.syncWith(maxId);
        }
    }
}
