package com.airlinespoo.flights.ui;

import com.airlinespoo.airlines.model.Airline;
import com.airlinespoo.airlines.service.AirlineService;
import com.airlinespoo.core.exception.AirlinesException;
import com.airlinespoo.core.ui.ConsoleInput;
import com.airlinespoo.core.ui.Menu;
import com.airlinespoo.core.util.Validator;
import com.airlinespoo.flights.model.Flight;
import com.airlinespoo.flights.service.FlightService;
import com.airlinespoo.locations.model.Airport;
import com.airlinespoo.locations.service.LocationService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Menú de interacción en consola para el Módulo 4 (Vuelos).
 */
public class FlightMenu implements Menu {

    private final FlightService flightService;
    private final LocationService locationService;
    private final AirlineService airlineService;
    private final ConsoleInput input;

    public FlightMenu(FlightService flightService,
                      LocationService locationService,
                      AirlineService airlineService,
                      ConsoleInput input) {
        this.flightService = Validator.requireNotNull(flightService, "servicio de vuelos");
        this.locationService = Validator.requireNotNull(locationService, "servicio de ubicaciones");
        this.airlineService = Validator.requireNotNull(airlineService, "servicio de aerolíneas");
        this.input = Validator.requireNotNull(input, "lector de consola");
    }

    @Override
    public String title() {
        return "Gestión de Vuelos";
    }

    @Override
    public void run() {
        boolean exit = false;
        while (!exit) {
            System.out.println("\n=================================================");
            System.out.println("              MÓDULO 4: VUELOS                   ");
            System.out.println("=================================================");
            System.out.println("1. Listar todos los vuelos");
            System.out.println("2. Registrar nuevo vuelo");
            System.out.println("3. Buscar vuelo por ID");
            System.out.println("4. Reporte de vuelos por aerolínea y mes");
            System.out.println("5. Eliminar vuelo");
            System.out.println("0. Volver");

            int option = input.readInt("Seleccione una opción", 0, 5);
            switch (option) {
                case 1 -> listFlights();
                case 2 -> createFlight();
                case 3 -> findFlightById();
                case 4 -> reportByAirlineAndMonth();
                case 5 -> removeFlight();
                case 0 -> exit = true;
            }
        }
    }

    private void listFlights() {
        System.out.println("\n--- LISTA DE VUELOS ---");
        try {
            List<Flight> flights = flightService.getAllFlights();
            if (flights.isEmpty()) {
                System.out.println("No hay vuelos registrados.");
            } else {
                flights.forEach(this::printFlight);
            }
        } catch (AirlinesException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
        input.pause();
    }

    private void createFlight() {
        System.out.println("\n--- REGISTRAR NUEVO VUELO ---");
        try {
            List<Airport> airports = locationService.listAirports();
            if (airports.size() < 2) {
                System.out.println("Se necesitan al menos 2 aeropuertos registrados en Ubicaciones.");
                input.pause();
                return;
            }

            List<Airline> airlines = airlineService.listAirlines();
            if (airlines.isEmpty()) {
                System.out.println("Se necesita al menos 1 aerolínea registrada en Aerolíneas.");
                input.pause();
                return;
            }

            String flightName = input.readRequiredText("Código del vuelo (ej. AV101, máx 8 caracteres)");
            Airport departure = pickAirport("Seleccione el aeropuerto de SALIDA", airports);
            Airport arrival = pickAirport("Seleccione el aeropuerto de LLEGADA", airports);
            LocalDate dateDeparture = input.readDate("Fecha de salida");
            LocalTime hourDeparture = input.readTime("Hora de salida");
            LocalDate dateArrival = input.readDate("Fecha de llegada");
            LocalTime hourArrival = input.readTime("Hora de llegada");
            Airline airline = pickAirline(airlines);

            Flight flight = new Flight(
                    0, flightName, departure, arrival,
                    dateDeparture, hourDeparture, dateArrival, hourArrival, airline
            );
            Flight created = flightService.createFlight(flight);
            System.out.println("Vuelo registrado exitosamente:");
            printFlight(created);
        } catch (AirlinesException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
        input.pause();
    }

    private void findFlightById() {
        System.out.println("\n--- BUSCAR VUELO POR ID ---");
        try {
            int id = input.readInt("ID del vuelo", 1, Integer.MAX_VALUE);
            Flight flight = flightService.getFlightOrThrow(id);
            printFlight(flight);
        } catch (AirlinesException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
        input.pause();
    }

    private void reportByAirlineAndMonth() {
        System.out.println("\n--- REPORTE DE VUELOS POR AEROLÍNEA Y MES ---");
        try {
            List<Airline> airlines = airlineService.listAirlines();
            if (airlines.isEmpty()) {
                System.out.println("No hay aerolíneas registradas.");
                input.pause();
                return;
            }
            Airline airline = pickAirline(airlines);
            int year = input.readInt("Año (ej. 2026)", 2000, 2100);
            int month = input.readInt("Mes (1-12)", 1, 12);

            List<Flight> flights = flightService.getFlightsByAirlineAndMonth(airline.getId(), year, month);
            System.out.printf("%nVuelos de %s en %04d-%02d: %d encontrado(s)%n",
                    airline.getName(), year, month, flights.size());
            if (flights.isEmpty()) {
                System.out.println("No hay vuelos programados para ese período.");
            } else {
                flights.forEach(this::printFlight);
            }
        } catch (AirlinesException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
        input.pause();
    }

    private void removeFlight() {
        System.out.println("\n--- ELIMINAR VUELO ---");
        try {
            int id = input.readInt("ID del vuelo a eliminar", 1, Integer.MAX_VALUE);
            if (input.confirm("¿Seguro que deseas eliminar el vuelo #" + id + "?")) {
                flightService.removeFlight(id);
                System.out.println("Vuelo eliminado correctamente.");
            }
        } catch (AirlinesException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
        input.pause();
    }

    private Airport pickAirport(String prompt, List<Airport> airports) {
        System.out.println("\n" + prompt + ":");
        for (int i = 0; i < airports.size(); i++) {
            Airport a = airports.get(i);
            String city = a.getCity() != null ? a.getCity().getName() : "Sin ciudad";
            System.out.printf("  %d. %s (%s)%n", i + 1, a.getName(), city);
        }
        int choice = input.readInt("Número de aeropuerto", 1, airports.size());
        return airports.get(choice - 1);
    }

    private Airline pickAirline(List<Airline> airlines) {
        System.out.println("\nSeleccione la aerolínea:");
        for (int i = 0; i < airlines.size(); i++) {
            Airline a = airlines.get(i);
            System.out.printf("  %d. [%d] %s%n", i + 1, a.getId(), a.getName());
        }
        int choice = input.readInt("Número de aerolínea", 1, airlines.size());
        return airlines.get(choice - 1);
    }

    private void printFlight(Flight f) {
        String dep = f.getDeparture() != null ? f.getDeparture().getName() : "Sin origen";
        String arr = f.getArrival() != null ? f.getArrival().getName() : "Sin destino";
        String air = f.getAirlineName() != null ? f.getAirlineName() : "Sin aerolínea";
        System.out.printf("  #%d [%s] | %s -> %s | Salida: %s %s | Llegada: %s %s | Aerolínea: %s%n",
                f.getFlightId(), f.getFlightName(), dep, arr,
                f.getDateDeparture(), f.getHourDeparture(),
                f.getDateArrival(), f.getHourArrival(), air);
    }
}
