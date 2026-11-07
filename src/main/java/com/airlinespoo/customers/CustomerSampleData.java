package com.airlinespoo.customers;

import com.airlinespoo.customers.model.Country;
import com.airlinespoo.customers.model.Customer;
import com.airlinespoo.customers.model.DocumentType;
import com.airlinespoo.customers.service.CustomerService;

//Clase utilitaria para cargar los datos de prueba
public class CustomerSampleData {

    //Prueba: Cargar 5 paises, 4 tipos de documento y 3 clientes en los repositorios

    public static void load (CustomerService service){

        //1. Crear y guardar 5 paises
        Country col = new Country((short) 57, "Colombia");
        Country arg = new Country((short) 54, "Argentina");
        Country mex = new Country((short) 52, "Mexico");
        Country usa = new Country((short) 1, "Estados Unidos");
        Country esp = new Country((short) 34, "España");

        service.createCountry(col);
        service.createCountry(arg);
        service.createCountry(mex);
        service.createCountry(usa);
        service.createCountry(esp);

        //2. Crear y guardar 4 tipos de documento
        DocumentType cc = new DocumentType("CC", "Cédula de Ciudadanía");
        DocumentType ti = new DocumentType("TI", "Tarjeta de Identidad");
        DocumentType ce = new DocumentType("CE", "Cédula de Extranjería");
        DocumentType pa = new DocumentType("PA", "Pasaporte");

        service.createDocumentType(cc);
        service.createDocumentType(ti);
        service.createDocumentType(ce);
        service.createDocumentType(pa);

        //3. Crear y guardar 3 clientes de muestra

        // Cliente 1: Con todos los datos opcionales completos
        Customer c1 = new Customer("101010", "Ana", "Pérez", cc, "3001112233", col);
        c1.setEmail("ana.perez@correo.com");
        c1.setAddress("Calle 10 # 20-30");

        // Cliente 2: Sin datos opcionales (para probar que los nulos se manejan bien)
        Customer c2 = new Customer("202020", "Luis", "Gómez", ce, "3109998877", arg);

        // Cliente 3: Solo con el correo opcional
        Customer c3 = new Customer("303030", "María", "López", pa, "+34600123456", esp);
        c3.setEmail("maria.lopez@correo.com");

        service.createCustomer(c1);
        service.createCustomer(c2);
        service.createCustomer(c3);

        System.out.println("✅ Datos de ejemplo del Módulo 1 (Clientes) cargados.");
    }
}
