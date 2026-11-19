package com.airlinespoo.airlines.model;

import com.airlinespoo.core.model.Identifiable;
import com.airlinespoo.core.util.Validator;

/**
 * Aerolínea registrada en el sistema (tabla {@code tblAirlines}).
 *
 * <p>El identificador lo asigna el programa mediante {@code IdGenerator}
 * (arranca en 100 y avanza de 10 en 10: 100, 110, 120...) y no cambia después
 * de creada la aerolínea.</p>
 */
public class Airline implements Identifiable<Integer> {

    /** Longitud máxima del nombre, equivalente a {@code VARCHAR(50)} en la base de datos. */
    public static final int MAX_NAME_LENGTH = 50;

    private final int id;
    private String name;

    /**
     * Crea una aerolínea válida.
     *
     * @param id   identificador entregado por el generador; debe ser mayor que cero
     * @param name nombre de la aerolínea; obligatorio, máximo 50 caracteres
     * @throws com.airlinespoo.core.exception.ValidationException si algún dato no es válido
     */
    public Airline(int id, String name) {
        this.id = (int) Validator.requirePositive(id, "id de la aerolínea");
        this.name = validateName(name);
    }

    /**
     * @return el identificador de la aerolínea; nunca cambia
     */
    @Override
    public Integer getId() {
        return id;
    }

    /**
     * @return el nombre de la aerolínea, ya normalizado
     */
    public String getName() {
        return name;
    }

    /**
     * Cambia el nombre de la aerolínea. Valida con las mismas reglas que el constructor.
     *
     * @param name nuevo nombre; obligatorio, máximo 50 caracteres
     * @throws com.airlinespoo.core.exception.ValidationException si el nombre no es válido
     */
    public void setName(String name) {
        this.name = validateName(name);
    }

    /**
     * Único punto de validación y normalización del nombre de la aerolínea.
     */
    private static String validateName(String name) {
        return Validator.requireText(name, "nombre de la aerolínea", MAX_NAME_LENGTH);
    }

    /**
     * Dos aerolíneas son iguales si tienen el mismo identificador.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Airline other)) {
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
