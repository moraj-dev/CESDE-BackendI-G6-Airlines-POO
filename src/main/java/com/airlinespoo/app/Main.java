package com.airlinespoo.app;

import com.airlinespoo.core.ui.ConsoleInput;
import com.airlinespoo.customers.CustomerSampleData;
import com.airlinespoo.customers.repository.CountryRepository;
import com.airlinespoo.customers.repository.CustomerRepository;
import com.airlinespoo.customers.repository.DocumentTypeRepository;
import com.airlinespoo.customers.service.CustomerService;
import com.airlinespoo.customers.ui.CustomerMenu;

/**
 * Punto de entrada de Airlines-poo.
 *
 * <p>Esta es una clase <b>provisional</b> que entrega el docente para que el proyecto se
 * pueda ejecutar desde el primer día. El estudiante del Módulo 3 la reemplaza al final
 * por la versión que ensambla todos los módulos (lector de consola, generadores de ids,
 * repositorios, servicios, datos de ejemplo y menús) y arranca el menú principal.</p>
 */
public final class Main {

    private Main() {
        // Clase de arranque: no se instancia.
    }

    /**
     * Arranca la aplicación.
     *
     * @param args argumentos de la línea de comandos (no se usan)
     */
    public static void main(String[] args) {
        System.out.println("=== Airlines-poo ===");
        System.out.println("Proyecto base listo. El kit base (paquete core) está disponible.");
        System.out.println("Cada módulo se integra en develop mediante Pull Request.");
        // TODO (Módulo 3): ensamblar aquí lector, generadores, repositorios, servicios y menús.

        //MENU MODULO 1-CUSTOMER
        //Instancia de la herramienta de consola
        ConsoleInput console = new ConsoleInput();

        //Instancia de los repositorios de la base de datos en memoria
        CountryRepository countryRepository = new CountryRepository();
        DocumentTypeRepository documentTypeRepository = new DocumentTypeRepository();
        CustomerRepository customerRepository = new CustomerRepository();

        //Instancia del servicio
        CustomerService customerService = new CustomerService(
            countryRepository,
            documentTypeRepository,
            customerRepository);

        //Carga los datos de prueba
        CustomerSampleData.load (customerService);

        //Instancia el menú del módulo 1. Customer
        CustomerMenu customerMenu = new CustomerMenu (customerService, console);

        //Correr la interfaz
        customerMenu.run();

        System.out.println("Sistema cerrado");
    }
}

