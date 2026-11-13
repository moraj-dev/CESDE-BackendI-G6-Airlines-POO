package com.airlinespoo.customers.repository;

import com.airlinespoo.core.repository.InMemoryRepository;
import com.airlinespoo.customers.model.DocumentType;

public class DocumentTypeRepository extends InMemoryRepository<DocumentType, String> {
    //Como en Country, hereda el mapa CRUD básico
    @Override
    protected String entityName (){
        return "Tipo de Documento";
    }
}
