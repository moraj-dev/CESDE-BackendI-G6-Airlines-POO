package com.airlinespoo.airlines.ui;

import com.airlinespoo.airlines.model.Airline;
import com.airlinespoo.airlines.service.AirlineService;
import com.airlinespoo.core.exception.AirlinesException;
import com.airlinespoo.core.ui.ConsoleInput;
import com.airlinespoo.core.ui.Menu;
import com.airlinespoo.core.util.Validator;

import java.util.List;

/**
 * Menú de consola del Módulo 3 (Aerolíneas).
 *
 * <p>Lee datos mediante {@link ConsoleInput}, delega la lógica en {@link AirlineService}
 * y captura las excepciones del sistema para mostrar mensajes claros en español sin
 * interrumpir la ejecución.</p>
 */
public class AirlineMenu implements Menu {

    private static final int EXIT = 0;
    private static final int LAST_OPTION = 6;
    private static final int MIN_AIRLINE_ID = 1;
    private static final int MAX_AIRLINE_ID = Integer.MAX_VALUE;

    private final AirlineService service;
    private final ConsoleInput input;

    /**
     * Crea el menú de aerolíneas con sus dependencias.
     *
     * @param service servicio de aerolíneas
     * @param input   lector de consola compartido de la aplicación
     */
    public AirlineMenu(AirlineService service, ConsoleInput input) {
        this.service = Validator.requireNotNull(service, "servicio de aerolíneas");
        this.input = Validator.requireNotNull(input, "lector de consola");
    }

    @Override
    public String title() {
        return "Aerolíneas";
    }

    @Override
    public void run() {
        int option;
        do {
            printOptions();
            option = input.readInt("Elige una opción", EXIT, LAST_OPTION);
            if (option != EXIT) {
                execute(option);
            }
        } while (option != EXIT);
    }

    private void printOptions() {
        System.out.println();
        System.out.println("=== " + title() + " ===");
        System.out.println("1. Registrar aerolínea");
        System.out.println("2. Consultar aerolínea por id");
        System.out.println("3. Renombrar aerolínea");
        System.out.println("4. Listar aerolíneas");
        System.out.println("5. Buscar aerolínea por nombre");
        System.out.println("6. Eliminar aerolínea");
        System.out.println("0. Volver");
    }

    private void execute(int option) {
        try {
            switch (option) {
                case 1 -> registerAirline();
                case 2 -> findAirlineById();
                case 3 -> renameAirline();
                case 4 -> listAirlines();
                case 5 -> searchAirlinesByName();
                case 6 -> deleteAirline();
                default -> System.out.println("Opción no reconocida.");
            }
        } catch (AirlinesException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void registerAirline() {
        String name = input.readRequiredText("Nombre de la aerolínea");
        Airline created = service.createAirline(name);
        System.out.println("Aerolínea registrada: " + created);
    }

    private void findAirlineById() {
        int id = input.readInt("Id de la aerolínea", MIN_AIRLINE_ID, MAX_AIRLINE_ID);
        Airline airline = service.getAirline(id);
        System.out.println("Aerolínea encontrada: " + airline);
    }

    private void renameAirline() {
        List<Airline> airlines = service.listAirlines();
        if (airlines.isEmpty()) {
            System.out.println("No hay aerolíneas registradas para renombrar.");
            return;
        }
        printNumberedAirlines(airlines);
        int choice = input.readInt("Selecciona el número de la aerolínea", 1, airlines.size());
        Airline selected = airlines.get(choice - 1);
        String newName = input.readRequiredText("Nuevo nombre de la aerolínea");
        Airline updated = service.renameAirline(selected.getId(), newName);
        System.out.println("Aerolínea actualizada: " + updated);
    }

    private void listAirlines() {
        List<Airline> airlines = service.listAirlines();
        if (airlines.isEmpty()) {
            System.out.println("No hay aerolíneas registradas.");
            return;
        }
        System.out.println("--- Aerolíneas (" + service.countAirlines() + ") ---");
        for (Airline airline : airlines) {
            System.out.println("  " + airline);
        }
    }

    private void searchAirlinesByName() {
        String query = input.readRequiredText("Texto a buscar en el nombre");
        List<Airline> matches = service.searchAirlinesByName(query);
        if (matches.isEmpty()) {
            System.out.println("No se encontraron aerolíneas con ese criterio.");
            return;
        }
        System.out.println("--- Resultados ---");
        for (Airline airline : matches) {
            System.out.println("  " + airline);
        }
    }

    private void deleteAirline() {
        List<Airline> airlines = service.listAirlines();
        if (airlines.isEmpty()) {
            System.out.println("No hay aerolíneas registradas para eliminar.");
            return;
        }
        printNumberedAirlines(airlines);
        int choice = input.readInt("Selecciona el número de la aerolínea a eliminar", 1, airlines.size());
        Airline selected = airlines.get(choice - 1);
        if (!input.confirm("¿Seguro que deseas eliminar '" + selected.getName() + "'?")) {
            System.out.println("Operación cancelada.");
            return;
        }
        service.deleteAirline(selected.getId());
        System.out.println("Aerolínea eliminada: " + selected);
    }

    private static void printNumberedAirlines(List<Airline> airlines) {
        for (int i = 0; i < airlines.size(); i++) {
            System.out.println((i + 1) + ". " + airlines.get(i));
        }
    }
}
