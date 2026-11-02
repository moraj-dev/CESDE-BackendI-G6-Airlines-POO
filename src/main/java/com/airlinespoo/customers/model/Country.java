package com.airlinespoo.customers.model;
import com.airlinespoo.core.model.Identifiable;
import com.airlinespoo.core.util.Validator;

import java.util.Objects;

public class Country implements Identifiable<Short> {
    //Atributos
    private final Short id;
    private String name;

    public boolean equals(Object object){
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Country country = (Country) object;
        return Objects.equals(id, country.id);
    }

    public Country (Short id, String name){
        Validator.requireNotNull(id, "id");
        Validator.requirePositive(id, "id");
        this.id = id;
        setName(name);
    }

    public int hashcode(){
        return Objects.hash(id);
    }

    public String toString(){
        return "País [" + id + " - " + name + "]";
    }

    //Getter
    public Short getId() {
        return id;
    }

    public String getName(){
        return name;
    }

    //Setter
    public void setName (String name){
       this.name = Validator.requireText(name, "nombre del país", 20)
    }
}
