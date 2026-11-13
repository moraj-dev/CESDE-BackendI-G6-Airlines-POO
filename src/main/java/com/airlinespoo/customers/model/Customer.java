package com.airlinespoo.customers.model;

import com.airlinespoo.core.exception.ValidationException;
import com.airlinespoo.core.model.Identifiable;
import com.airlinespoo.core.util.Validator;
import java.util.Objects;

public class Customer implements Identifiable<String> {

    //Atributos
    private final String id;
    private String firstName;
    private String lastName;
    private DocumentType documentType;
    private String telephoneNumber;
    private  String email;
    private Country country;
    private String address;

    //Constructor
    public Customer (String id, String firstName, String lastName, DocumentType documentType, String telephoneNumber, Country country ){
        this.id = Validator.requireText(id, "documento", 20);

        setFirstName(firstName);
        setLastName(lastName);
        setDocumentType(documentType);
        setTelephoneNumber(telephoneNumber);
        setCountry(country);
    }

    //Métodos
    @Override
    public boolean equals (Object object){
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Customer customer = (Customer) object;
        return Objects.equals(id, customer.id);
    }

    @Override
    public int hashCode (){
        return Objects.hash(id);
    }

    @Override
    public String toString(){
        return "Cliente [" + id + " - " + getFullName() + "]";
    }

    //Getters
    @Override
    public String getId(){
        return id;
    }

    public String getFirstName(){
        return firstName;
    }

    public String getLastName(){
        return lastName;
    }

    //Metodo que devuelve el nombre completo
    public String getFullName(){
        return firstName + " " + lastName;
    }

    public DocumentType getDocumentType(){
        return documentType;
    }

    public Country getCountry(){
        return country;
    }

    public String getTelephoneNumber(){
        return telephoneNumber;
    }

    public String getEmail(){
        return email;
    }

    public String getAddress(){
        return address;
    }

    //Setters
    public void setFirstName(String firstName){
        this.firstName = Validator.requireText(firstName, "Nombre", 30);
    }

    public void setLastName (String lastName){
        this.lastName = Validator.requireText(lastName, "Apellido", 30);
    }

    public void setDocumentType (DocumentType documentType){
        this.documentType = Validator.requireNotNull(documentType, "Tipo de Documento");
    }

    public void  setCountry(Country country){
        this.country = Validator.requireNotNull(country, "País");
    }

    public void setTelephoneNumber (String telephoneNumber){
        String validPhone = Validator.requireText(telephoneNumber, " Teléfono", 15);

        //Validación de longitud mínima.
        if (validPhone.length() < 7) {
            throw new ValidationException("El número de teléfono debe tener al menos 7 dígitos.");
        }

        //Verificación del número de telefono, con opción de que contenga un '+' al inicio
        if (!validPhone.matches("^\\+?[0-9]+$")){
            throw new ValidationException("El teléfono solo debe contener digitos, opcionalmente un '+' al inicio");
        }
        this.telephoneNumber = validPhone;
    }

    public void setEmail(String email){
        String normalizedEmail = Validator.normalize(email);
        if (normalizedEmail != null && !normalizedEmail.isEmpty()){
            Validator.requireMaxLength(normalizedEmail, 80, "correo");

            //Validación del formato del correo
            if(!normalizedEmail.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")){
                throw new ValidationException("El formato del correo no es válido");
            }
            this.email = normalizedEmail;
        }else{
            this.email = null;
        }
    }

    public void setAddress (String address){
        String normalizedAddress = Validator.normalize(address);
        if (normalizedAddress != null && !normalizedAddress.isEmpty()){
            Validator.requireMaxLength(normalizedAddress, 40, "Dirección");
            this.address = normalizedAddress;
        }else {
            this.address = null;
        }
    }







}
