package com.airlinespoo.customers.model;
import com.airlinespoo.core.model.Identifiable;
import com.airlinespoo.core.util.Validator;

import java.util.Objects;

public class Country implements Identifiable<Short> {
    //Atributos
    private final Short id;
    private String name;

    //Constructor
    public Country (Short id, String name){
        Validator.requireNotNull(id, "id");
        Validator.requirePositive(id, "id");
        this.id = id;
        setName(name);
    }

    //Métodos
    @Override
    public boolean equals(Object object){
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Country country = (Country) object;
        return Objects.equals(id, country.id);
    }

    @Override
    public int hashcode(){
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
