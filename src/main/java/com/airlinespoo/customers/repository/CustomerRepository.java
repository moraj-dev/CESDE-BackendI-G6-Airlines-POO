package com.airlinespoo.customers.repository;

import com.airlinespoo.core.repository.InMemoryRepository;
import com.airlinespoo.customers.model.Customer;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

public class CustomerRepository extends InMemoryRepository<Customer,String> {

    @Override
    protected String entityName (){
        return "Cliente";
    }

    //Busqueda por email
    public Optional<Customer> findByEmail (String email){
        for (Customer customer : findAll()){
            if (customer.getEmail() != null && customer.getEmail().equalsIgnoreCase(email)){
                return Optional.of(customer);
            }
        }
        return Optional.empty();
    }

    //Busqueda por apellido
    public List<Customer> searchByLastName (String lastNamePart){
        List<Customer> result = new ArrayList<>();
        String searchLower = lastNamePart.toLowerCase();

        for (Customer customer : findAll()){
            if (customer.getLastName().toLowerCase().contains(searchLower)){
                result.add(customer);
            }
        }
        return result;
    }

    //Busqueda por País
    public List<Customer> findByCountry (Short countryId){
        List<Customer> result = new ArrayList<>();
        for (Customer customer : findAll()){
            if (customer.getCountry().getId().equals(countryId)){
                result.add(customer);
            }
        }
        return result;
    }

    //Busqueda por Tipo de documento
    public List<Customer> findByDocumentType (String documentTypeId){
        List<Customer> result = new ArrayList<>();
        for (Customer customer : findAll()){
            if (customer.getDocumentType().getId().equalsIgnoreCase(documentTypeId));
            result.add(customer);
        }
        return result;
    }
}
