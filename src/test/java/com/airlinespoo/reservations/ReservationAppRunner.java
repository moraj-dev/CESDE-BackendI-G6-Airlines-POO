package com.airlinespoo.reservations;

import com.airlinespoo.core.ui.ConsoleInput;
import com.airlinespoo.core.util.IdGenerator;
import com.airlinespoo.customers.CustomerSampleData;
import com.airlinespoo.customers.repository.CountryRepository;
import com.airlinespoo.customers.repository.CustomerRepository;
import com.airlinespoo.customers.repository.DocumentTypeRepository;
import com.airlinespoo.customers.service.CustomerService;
import com.airlinespoo.reservations.repository.ReservationRepository;
import com.airlinespoo.reservations.service.ReservationService;
import com.airlinespoo.reservations.ui.ReservationMenu;
import com.airlinespoo.reservations.ui.ReservationSampleData;

/**
 * Lanzador interactivo local para probar el Módulo 5 (Reservas) integrado con el Módulo 1 (Clientes)
 * sin modificar la clase {@code app.Main} del proyecto.
 */
public final class ReservationAppRunner {

    private ReservationAppRunner() {
        // Lanzador de consola
    }

    /**
     * Inicia el menú de reservas por consola con datos reales de clientes y reservas precargados.
     *
     * @param args argumentos de consola (no se usan)
     */
    public static void main(String[] args) {
        ConsoleInput input = new ConsoleInput();

        // 1. Inicializar Módulo 1 (Clientes)
        CustomerService customerService = new CustomerService(
                new CountryRepository(),
                new DocumentTypeRepository(),
                new CustomerRepository()
        );
        CustomerSampleData.load(customerService);

        // 2. Inicializar Módulo 5 (Reservas)
        ReservationRepository reservationRepository = new ReservationRepository();
        ReservationService reservationService = new ReservationService(
                reservationRepository,
                customerService,
                new IdGenerator(1, 1),
                new IdGenerator(1, 1)
        );
        ReservationSampleData.load(reservationService);
        System.out.println("✅ Datos de ejemplo del Módulo 5 (Reservas) cargados.");

        // 3. Ejecutar el menú interactivo
        ReservationMenu menu = new ReservationMenu(reservationService, customerService, input);
        menu.run();

        System.out.println("\nPrueba interactiva finalizada.");
    }
}
