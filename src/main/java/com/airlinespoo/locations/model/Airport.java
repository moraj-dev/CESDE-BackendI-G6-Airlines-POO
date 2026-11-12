package com.airlinespoo.locations.model;

import com.airlinespoo.core.model.Identifiable;
import com.airlinespoo.core.util.Validator;

import java.util.UUID;

/**
 * Aeropuerto ubicado en una ciudad (tabla {@code tblAirports}).
 *
 * <p>El id es un {@link UUID}: no se numera, se genera aleatoriamente y no cambia.</p>
 *
 * <p>Hay dos formas de construirlo:</p>
 * <ul>
 *   <li>Aeropuerto <b>nuevo</b>: {@code new Airport(nombre, ciudad)} genera el UUID.</li>
 *   <li>Aeropuerto <b>existente</b>: {@code new Airport(id, nombre, ciudad)} recibe un UUID
 *       que ya existe (por ejemplo, el que más adelante vendrá de la base de datos).</li>
 * </ul>
 *
 * <p>En la base de datos la ciudad es opcional; en el negocio es obligatoria, porque un
 * aeropuerto siempre está en algún lugar y los vuelos necesitan saber dónde.</p>
 */
public class Airport implements Identifiable<UUID> {

    /** Longitud máxima del nombre, igual que {@code VARCHAR(50)} en la base de datos. */
    public static final int MAX_NAME_LENGTH = 50;

    private final UUID id;
    private String name;
    private City city;

    /**
     * Crea un aeropuerto <b>nuevo</b> y le genera su UUID.
     *
     * @param name nombre del aeropuerto; obligatorio, máximo 50 caracteres
     * @param city ciudad donde está; obligatoria
     * @throws com.airlinespoo.core.exception.ValidationException si algún dato no es válido
     */
    public Airport(String name, City city) {
        this(UUID.randomUUID(), name, city);
    }

    /**
     * Crea un aeropuerto que <b>ya existe</b> y ya tiene su UUID.
     *
     * @param id   identificador existente; obligatorio
     * @param name nombre del aeropuerto; obligatorio, máximo 50 caracteres
     * @param city ciudad donde está; obligatoria
     * @throws com.airlinespoo.core.exception.ValidationException si algún dato no es válido
     */
    public Airport(UUID id, String name, City city) {
        this.id = Validator.requireNotNull(id, "id del aeropuerto");
        this.name = validateName(name);
        this.city = validateCity(city);
    }

    /**
     * @return el UUID del aeropuerto; nunca cambia
     */
    @Override
    public UUID getId() {
        return id;
    }

    /**
     * @return el nombre del aeropuerto, ya normalizado
     */
    public String getName() {
        return name;
    }

    /**
     * Cambia el nombre del aeropuerto. Valida igual que el constructor.
     *
     * @param name nombre nuevo; obligatorio, máximo 50 caracteres
     * @throws com.airlinespoo.core.exception.ValidationException si el nombre no es válido
     */
    public void setName(String name) {
        this.name = validateName(name);
    }

    /**
     * @return la ciudad donde está el aeropuerto
     */
    public City getCity() {
        return city;
    }

    /**
     * Cambia la ciudad del aeropuerto. No acepta {@code null}.
     *
     * @param city ciudad nueva; obligatoria
     * @throws com.airlinespoo.core.exception.ValidationException si la ciudad es {@code null}
     */
    public void setCity(City city) {
        this.city = validateCity(city);
    }

    /**
     * Único lugar donde se valida y normaliza el nombre (quita espacios sobrantes).
     */
    private static String validateName(String name) {
        return Validator.requireText(name, "nombre del aeropuerto", MAX_NAME_LENGTH);
    }

    /**
     * Único lugar donde se valida la ciudad.
     */
    private static City validateCity(City city) {
        return Validator.requireNotNull(city, "ciudad del aeropuerto");
    }

    /**
     * Dos aeropuertos son iguales si tienen el mismo UUID.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Airport other)) {
            return false;
        }
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "%s (%s)".formatted(name, city.getName());
    }
}
