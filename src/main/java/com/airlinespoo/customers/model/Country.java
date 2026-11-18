package com.airlinespoo.customers.model;
import com.airlinespoo.core.exception.ValidationException;
import com.airlinespoo.core.model.Identifiable;
import com.airlinespoo.core.util.Validator;

import java.util.Objects;

public class Country implements Identifiable<Short> {
    //Atributos
    private final Short id; //se declara final porque no tiene setter
    private String name;

    //Constructor
    public Country (Short id, String name){
        Validator.requireNotNull(id, "id");
        Validator.requirePositive(id, "id");
        this.id = id;
        setName(name);
    }

    //Métodos

    public static void validCountry (short id, String name){
        if (id <= 0){
            throw new ValidationException("El ID del país debe ser un número positivo");
        }

        String validName = Validator.requireText(name, "Nombre del país", 50);
        if (validName.trim().length() < 2) {
            throw new ValidationException("El nombre del país debe tener al menos dos caracteres");
        }
    }
    @Override
    public boolean equals(Object object){
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Country country = (Country) object;
        return Objects.equals (this.id, country.id);
    }

    @Override
    public int hashCode(){
        return Objects.hash(id);
    }

    @Override
    public String toString(){
        return "País [" + id + " - " + name + "]";
    }

    //Getter
    @Override
    public Short getId() {
        return id;
    }

    public String getName(){
        return name;
    }

    //Setter
    public void setName (String name){
       this.name = Validator.requireText(name, "Nombre del País", 20);
    }
}
