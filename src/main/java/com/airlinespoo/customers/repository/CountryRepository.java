package com.airlinespoo.customers.repository;

import com.airlinespoo.core.repository.InMemoryRepository;
import com.airlinespoo.customers.model.Country;

public class CountryRepository extends InMemoryRepository<Country, Short> {
    //Al heredar Country a InmemoryRepository ya se tiene todo el mapa CRUD básico
    @Override
    protected String entityName(){
        return "País";
    }
}
