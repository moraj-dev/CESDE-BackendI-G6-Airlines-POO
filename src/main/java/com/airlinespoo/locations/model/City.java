package com.airlinespoo.locations.model;

import com.airlinespoo.core.model.Identifiable;
import com.airlinespoo.core.util.Validator;

/**
 * Ciudad donde se ubican los aeropuertos (tabla {@code tblCities}).
 *
 * <p>El id lo asigna el programa con un {@code IdGenerator} (arranca en 1 y sube de 1 en 1)
 * y no cambia después de creada la ciudad.</p>
 *
 * <p>Regla de normalización: el nombre se guarda sin espacios sobrantes. La comparación
 * sin distinguir mayúsculas ("Cali" y "cali") la hace el servicio, porque una ciudad sola
 * no conoce a las demás.</p>
 */
public class City implements Identifiable<Integer> {

    /** Longitud máxima del nombre, igual que {@code VARCHAR(20)} en la base de datos. */
    public static final int MAX_NAME_LENGTH = 20;

    private final int id;
    private String name;

    /**
     * Crea una ciudad válida.
     *
     * @param id   identificador entregado por el generador; debe ser mayor que cero
     * @param name nombre de la ciudad; obligatorio, máximo 20 caracteres
     * @throws com.airlinespoo.core.exception.ValidationException si algún dato no es válido
     */
    public City(int id, String name) {
        this.id = (int) Validator.requirePositive(id, "id de la ciudad");
        this.name = validateName(name);
    }

    /**
     * @return el identificador de la ciudad; nunca cambia
     */
    @Override
    public Integer getId() {
        return id;
    }

    /**
     * @return el nombre de la ciudad, ya normalizado
     */
    public String getName() {
        return name;
    }

    /**
     * Cambia el nombre de la ciudad. Valida igual que el constructor.
     *
     * @param name nombre nuevo; obligatorio, máximo 20 caracteres
     * @throws com.airlinespoo.core.exception.ValidationException si el nombre no es válido
     */
    public void setName(String name) {
        this.name = validateName(name);
    }

    /**
     * Único lugar donde se valida y normaliza el nombre (quita espacios sobrantes).
     */
    private static String validateName(String name) {
        return Validator.requireText(name, "nombre de la ciudad", MAX_NAME_LENGTH);
    }

    /**
     * Dos ciudades son iguales si tienen el mismo id.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof City other)) {
            return false;
        }
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "%d - %s".formatted(id, name);
    }
}
