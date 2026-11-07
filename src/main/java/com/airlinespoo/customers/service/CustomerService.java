package com.airlinespoo.customers.service;

import com.airlinespoo.core.exception.BusinessRuleException;
import com.airlinespoo.core.exception.DuplicateException;
import com.airlinespoo.core.exception.NotFoundException;
import com.airlinespoo.customers.model.Country;
import com.airlinespoo.customers.model.Customer;
import com.airlinespoo.customers.model.DocumentType;
import com.airlinespoo.customers.repository.CountryRepository;
import com.airlinespoo.customers.repository.CustomerRepository;
import com.airlinespoo.customers.repository.DocumentTypeRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class CustomerService {

    private final CountryRepository countryRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private  final CustomerRepository customerRepository;

    //Constructor
    //Recibe los repositorios ya creados
    public CustomerService (CountryRepository countryRepository, DocumentTypeRepository documentTypeRepository,
                            CustomerRepository customerRepository){
        this.countryRepository = countryRepository;
        this.documentTypeRepository = documentTypeRepository;
        this.customerRepository = customerRepository;
    }

    //METODOS PARA COUNTRY

    //Crear un país nuevo
    public void createCountry (Country country){
        countryRepository.create(country);
    }

    //Busca un país por id
    public Country getCountry (Short id){
        return countryRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("No se encontró el país con id: " + id));
    }

    //Devuelve la lista de paises por orden alfabético
    public List<Country> listCountries(){
        List<Country> countries = countryRepository.findAll();
        countries.sort(Comparator.comparing(Country::getName));
        return countries;
    }

    //Actualiza los datos de un país existente
    public void updateCountry(Country country){
        countryRepository.update(country);
    }

    //Elimina un país existente, realizando primero la verificación de si hay un usuario relacionado con este
    public void deleteCountry(Short id){
        if(!customerRepository.findByCountry(id).isEmpty()){
            throw new BusinessRuleException("No se puede eliminar el país porque tiene clientes asociados");
        }
        countryRepository.deleteById(id);

    }

    //METODOS PARA DOCUMENTTYPE

    //Guarda un nuevo tipo de documento
    public void createDocumentType (DocumentType documentType){
        documentTypeRepository.create(documentType);
    }

    //Consulta por tipo de documento
    public DocumentType getDocumentType (String id){
        return documentTypeRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("No se encontró el tipo de Documento"));
    }

    //Lista todos los tipos de documento
    public List<DocumentType> listDocumentTypes(){
        return documentTypeRepository.findAll();
    }

    //Actualiza la descripción de un tipo de documento.
    public void updateDocumentType(DocumentType documentType){
        documentTypeRepository.update(documentType);
    }

    //Elimina un tipo de documento, realizando la validación de que no hayan clientes asociados a este
    public void deleteDocumentType(String id){
        if (!customerRepository.findByDocumentType(id).isEmpty()){
            throw new BusinessRuleException("No se puede eliminar el tipo de documento porque existen clientes asociados a este");
        }
        documentTypeRepository.deleteById(id);
    }


    //METODOS PARA CUSTOMER

    //Registra clientes aplicando reglas de negocio
    public void createCustomer(Customer customer){
        //Se reutilizan getCountry y getDocumentType-El metodo se interrumpe si alguno de los dos no existe (NotFoundException)
        getCountry(customer.getCountry().getId());
        getDocumentType(customer.getDocumentType().getId());

        //Valida unicidad de correo
        if (customer.getEmail() != null){
            if (customerRepository.findByEmail(customer.getEmail()).isPresent()){
                throw new DuplicateException("Ya existe un cliente registrado con este correo: " + customer.getEmail());
            }
        }
        //Guardar: El repositorio evalúa que la cédula (id) no este repetida
        customerRepository.create(customer);
    }

    //Busca un cliente por documento de identidad
    public  Customer getCustomer(String id){
        return customerRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("No se encontró el cliente con documento: " + id));
    }

    //Lista de clientes ordenada por apellido y nombre
    public List<Customer> listCustomer(){
        List<Customer> customers = customerRepository.findAll();
        customers.sort(Comparator.comparing(Customer::getLastName).thenComparing(Customer::getFirstName));
        return customers;
    }

    //Actualiza los datos de un cliente
    public void updateCustomer (Customer customerUpdates){
        //Valida que un cliente exista
        Customer existing = getCustomer(customerUpdates.getId());

        //Comprueba que el nuevo pais y tipo de documento existan
        getCountry(customerUpdates.getCountry().getId());
        getDocumentType(customerUpdates.getDocumentType().getId());

        //Verificar que el nuevo correo no pertenezca a un cliente existente
        if (customerUpdates.getEmail() != null){
            Optional<Customer> withEmail = customerRepository.findByEmail(customerUpdates.getEmail());
            if (withEmail.isPresent() && !withEmail.get().getId().equals(existing.getId())){
                throw new DuplicateException("El correo ya está registrado a nombre de otro cliente");
            }
        }

        //Actualiza los demas datos permitidos excepto el id
        existing.setFirstName(customerUpdates.getFirstName());
        existing.setLastName(customerUpdates.getLastName());
        existing.setDocumentType(customerUpdates.getDocumentType());
        existing.setCountry(customerUpdates.getCountry());
        existing.setTelephoneNumber(customerUpdates.getTelephoneNumber());
        existing.setEmail(customerUpdates.getEmail());
        existing.setAddress(customerUpdates.getAddress());

        //Guarda el cliente (objeto) modificado
        customerRepository.update(existing);
    }

    //Eliminar cliente por documento
    public void deleteCustomer(String id){
        customerRepository.deleteById(id);
    }


    //BUSQUEDAS COMPLEMENTARIAS

    //Busca un cliente por correo. No lanza excepcion si no lo encuentra
    public Optional<Customer> getCustomerByEmail(String email){
        return customerRepository.findByEmail(email);
    }

    //Busqueda de clientes por apellido con coincidencia parcial
    public List<Customer> searchCustomerByLastName(String lastName){
        return customerRepository.searchByLastName(lastName);
    }

    //Filtro de clientes por país, validando que exista el pais
    public List<Customer> listCustomerByCountry (Short countryId){
        getCountry(countryId);
        return customerRepository.findByCountry(countryId);
    }

    //Filtro de clientes por tipo de documento, validando que exista el tipo de documento
    public List<Customer> listCustomerByDocumentType (String documentTypeId){
        getDocumentType(documentTypeId);
        return customerRepository.findByDocumentType(documentTypeId);
    }

    //Devuelve el numero total de clientes registrados
    public long countCustomers(){
        return customerRepository.count();
    }






}
