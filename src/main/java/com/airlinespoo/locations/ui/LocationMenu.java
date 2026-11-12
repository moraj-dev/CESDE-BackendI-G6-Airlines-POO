package com.airlinespoo.locations.ui;

import com.airlinespoo.core.exception.AirlinesException;
import com.airlinespoo.core.ui.ConsoleInput;
import com.airlinespoo.core.ui.Menu;
import com.airlinespoo.locations.model.Airport;
import com.airlinespoo.locations.model.City;
import com.airlinespoo.locations.service.LocationService;

import java.util.List;

/**
 * Menú de consola del Módulo 2 (Ubicaciones): ciudades y aeropuertos.
 *
 * <p>Pide datos con {@link ConsoleInput}, llama a {@link LocationService} y muestra
 * los resultados. Captura las excepciones del sistema y muestra un mensaje en español:
 * un dato mal digitado nunca cierra el programa.</p>
 */
public class LocationMenu implements Menu {

    private static final int EXIT = 0;
    private static final int LAST_OPTION = 7;

    private final LocationService service;
    private final ConsoleInput input;

    /**
     * Crea el menú con sus dependencias.
     *
     * @param service servicio de ubicaciones
     * @param input   lector de consola compartido de la aplicación
     */
    public LocationMenu(LocationService service, ConsoleInput input) {
        this.service = service;
        this.input = input;
    }

    @Override
    public String title() {
        return "Ubicaciones (ciudades y aeropuertos)";
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
        System.out.println("1. Registrar ciudad");
        System.out.println("2. Registrar aeropuerto");
        System.out.println("3. Listar ciudades");
        System.out.println("4. Listar aeropuertos por ciudad");
        System.out.println("5. Buscar aeropuerto por nombre");
        System.out.println("6. Renombrar ciudad o aeropuerto");
        System.out.println("7. Eliminar ciudad o aeropuerto");
        System.out.println("0. Volver");
    }

    /**
     * Ejecuta la opción elegida. Si el servicio lanza una excepción del sistema,
     * muestra su mensaje y vuelve al menú.
     */
    private void execute(int option) {
        try {
            switch (option) {
                case 1 -> registerCity();
                case 2 -> registerAirport();
                case 3 -> listCities();
                case 4 -> listAirportsByCity();
                case 5 -> searchAirports();
                case 6 -> rename();
                case 7 -> delete();
                default -> System.out.println("Opción no válida.");
            }
        } catch (AirlinesException e) {
            System.out.println("No se pudo completar: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Opciones
    // ------------------------------------------------------------------

    private void registerCity() {
        String name = input.readRequiredText("Nombre de la ciudad");
        City city = service.createCity(name);
        System.out.println("Ciudad registrada: " + city);
    }

    private void registerAirport() {
        City city = chooseCity();
        if (city == null) {
            return;
        }
        String name = input.readRequiredText("Nombre del aeropuerto");
        Airport airport = service.createAirport(name, city.getId());
        System.out.println("Aeropuerto registrado: " + airport);
    }

    private void listCities() {
        List<City> cities = service.listCities();
        if (cities.isEmpty()) {
            System.out.println("No hay ciudades registradas.");
            return;
        }
        System.out.println("Ciudades:");
        for (City city : cities) {
            System.out.println("  " + city.getName() + " (" + service.countAirportsByCity(city.getId())
                + " aeropuerto(s))");
        }
    }

    private void listAirportsByCity() {
        City city = chooseCity();
        if (city == null) {
            return;
        }
        List<Airport> airports = service.listAirportsByCity(city.getId());
        if (airports.isEmpty()) {
            System.out.println(city.getName() + " no tiene aeropuertos registrados.");
            return;
        }
        System.out.println("Aeropuertos de " + city.getName() + ":");
        for (Airport airport : airports) {
            System.out.println("  " + airport.getName());
        }
    }

    private void searchAirports() {
        String text = input.readRequiredText("Texto a buscar en el nombre");
        List<Airport> found = service.searchAirportsByName(text);
        if (found.isEmpty()) {
            System.out.println("No se encontraron aeropuertos con \"" + text + "\".");
            return;
        }
        System.out.println("Resultados:");
        for (Airport airport : found) {
            System.out.println("  " + airport + "  · id: " + airport.getId());
        }
    }

    private void rename() {
        boolean isCity = chooseCityOrAirport("renombrar");
        if (isCity) {
            City city = chooseCity();
            if (city == null) {
                return;
            }
            String newName = input.readRequiredText("Nuevo nombre de la ciudad");
            System.out.println("Ciudad actualizada: " + service.updateCity(city.getId(), newName));
        } else {
            Airport airport = chooseAirport();
            if (airport == null) {
                return;
            }
            String newName = input.readRequiredText("Nuevo nombre del aeropuerto");
            int cityId = airport.getCity().getId();
            if (input.confirm("¿Cambiar también la ciudad?")) {
                City newCity = chooseCity();
                if (newCity == null) {
                    return;
                }
                cityId = newCity.getId();
            }
            System.out.println("Aeropuerto actualizado: " + service.updateAirport(airport.getId(), newName, cityId));
        }
    }

    private void delete() {
        boolean isCity = chooseCityOrAirport("eliminar");
        if (isCity) {
            City city = chooseCity();
            if (city != null && input.confirm("¿Seguro que quieres eliminar " + city.getName() + "?")) {
                service.deleteCity(city.getId());
                System.out.println("Ciudad eliminada.");
            }
        } else {
            Airport airport = chooseAirport();
            if (airport != null && input.confirm("¿Seguro que quieres eliminar " + airport.getName() + "?")) {
                service.deleteAirport(airport.getId());
                System.out.println("Aeropuerto eliminado.");
            }
        }
    }

    // ------------------------------------------------------------------
    // Ayudas para elegir de una lista numerada
    // ------------------------------------------------------------------

    /**
     * Pregunta si la acción es sobre una ciudad (1) o un aeropuerto (2).
     *
     * @return {@code true} si eligió ciudad
     */
    private boolean chooseCityOrAirport(String action) {
        System.out.println("¿Qué quieres " + action + "?  1. Ciudad   2. Aeropuerto");
        return input.readInt("Elige", 1, 2) == 1;
    }

    /**
     * Muestra las ciudades numeradas y pide el número (no el id interno).
     *
     * @return la ciudad elegida, o {@code null} si no hay ciudades
     */
    private City chooseCity() {
        List<City> cities = service.listCities();
        if (cities.isEmpty()) {
            System.out.println("Primero registra una ciudad.");
            return null;
        }
        for (int i = 0; i < cities.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + cities.get(i).getName());
        }
        int number = input.readInt("Número de la ciudad", 1, cities.size());
        return cities.get(number - 1);
    }

    /**
     * Muestra los aeropuertos numerados y pide el número (no el UUID).
     *
     * @return el aeropuerto elegido, o {@code null} si no hay aeropuertos
     */
    private Airport chooseAirport() {
        List<Airport> airports = service.listAirports();
        if (airports.isEmpty()) {
            System.out.println("No hay aeropuertos registrados.");
            return null;
        }
        for (int i = 0; i < airports.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + airports.get(i));
        }
        int number = input.readInt("Número del aeropuerto", 1, airports.size());
        return airports.get(number - 1);
    }
}
