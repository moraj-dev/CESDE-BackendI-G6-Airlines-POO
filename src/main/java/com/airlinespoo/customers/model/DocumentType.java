package com.airlinespoo.customers.model;

import com.airlinespoo.core.model.Identifiable;
import com.airlinespoo.core.util.Validator;
import com.airlinespoo.core.exception.ValidationException;
import java.util.Objects;

public class DocumentType implements Identifiable<String> {

    //Atributos
    private final String id;
    private String description;

    //Constructor
    public DocumentType (String id, String description){
        String normalizedId = Validator.requireText(id, "id").toUpperCase();

        if (normalizedId.length() != 2){
            throw new ValidationException("El campo 'id' debe tener 2 caracteres");
        }
        this.id = normalizedId;

        setDescription (description);
    }

    //Metodos
    @Override
    public boolean equals (Object object){
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        DocumentType that = (DocumentType) object;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode (){
        return Objects.hash(id);
    }

    @Override
    public String toString (){
        return "Tipo Documento [" + id + " - " + description + "]";
    }

    //Getter
    @Override
    public String getId (){
        return id;
    }

    public String getDescription(){
        return description;
    }

    //Setter
    public void setDescription (String description){
        this.description = Validator.requireText(description, "descripcion", 20);
    }

}
