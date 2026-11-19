package com.airlinespoo.flights.ui;

import com.airlinespoo.flights.model.Flight;
import com.airlinespoo.flights.service.FlightService;
import com.airlinespoo.locations.model.Airport;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;

public class FlightMenu {

    private final FlightService flightService = new FlightService();
    private final Scanner scanner = new Scanner(System.in);

    public void showMenu() {
        int option;
        do {
            System.out.println("\n--- MENÚ DE VUELOS ---");
            System.out.println("1. Listar todos los vuelos");
            System.out.println("2. Registrar un nuevo vuelo");
            System.out.println("3. Buscar vuelo por ID");
            System.out.println("4. Eliminar vuelo");
            System.out.println("0. Salir");
            System.out.print("Seleccione una opción: ");

            String input = scanner.nextLine().trim();
            try {
                option = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Ingrese un número válido.");
                option = -1;
                continue;
            }

            switch (option) {
                case 1 -> listFlights();
                case 2 -> registerFlight();
                case 3 -> findFlightById();
                case 4 -> deleteFlight();
                case 0 -> System.out.println("Saliendo del menú de vuelos...");
                default -> System.out.println("Opción no válida.");
            }
        } while (option != 0);
    }

    private void listFlights() {
        try {
            List<Flight> flights = flightService.getAllFlights();
            if (flights.isEmpty()) {
                System.out.println("No hay vuelos registrados.");
                return;
            }
            for (Flight f : flights) {
                System.out.printf(
                    "ID: %d | Vuelo: %s | Aerolínea ID: %s (%s) | Salida: %s %s | Llegada: %s %s%n",
                    f.getFlightId(),
                    f.getFlightName(),
                    f.getAirlineId(),
                    f.getAirlineName() != null ? f.getAirlineName() : "N/A",
                    f.getDateDeparture(),
                    f.getHourDeparture(),
                    f.getDateArrival(),
                    f.getHourArrival()
                );
            }
        } catch (RuntimeException e) {
            System.out.println("Error al listar vuelos: " + e.getMessage());
        }
    }

    private void registerFlight() {
        try {
            System.out.print("Código/Nombre del vuelo (máx. 8 caracteres, ej. AV101): ");
            String flightName = scanner.nextLine().trim();

            System.out.print("UUID aeropuerto de salida (o Enter para omitir): ");
            String departureRaw = scanner.nextLine().trim();
            Airport departure = buildAirportIfValid(departureRaw);

            System.out.print("UUID aeropuerto de llegada (o Enter para omitir): ");
            String arrivalRaw = scanner.nextLine().trim();
            Airport arrival = buildAirportIfValid(arrivalRaw);

            LocalDate dateDeparture = readLocalDate("Fecha de salida (YYYY-MM-DD, ej. 2026-11-25): ");
            LocalTime hourDeparture = readLocalTime("Hora de salida (HH:mm o HH:mm:ss, ej. 08:30): ");
            LocalDate dateArrival = readLocalDate("Fecha de llegada (YYYY-MM-DD, ej. 2026-11-25): ");
            LocalTime hourArrival = readLocalTime("Hora de llegada (HH:mm o HH:mm:ss, ej. 10:15): ");

            System.out.print("ID de aerolínea (ej. 100, o Enter para omitir): ");
            String airlineRaw = scanner.nextLine().trim();
            Integer airlineId = airlineRaw.isEmpty() ? null : Integer.parseInt(airlineRaw);

            Flight flight = new Flight();
            flight.setFlightName(flightName);
            flight.setDeparture(departure);
            flight.setArrival(arrival);
            flight.setDateDeparture(dateDeparture);
            flight.setHourDeparture(hourDeparture);
            flight.setDateArrival(dateArrival);
            flight.setHourArrival(hourArrival);
            flight.setAirlineId(airlineId);

            flightService.createFlight(flight);
            System.out.println("¡Vuelo registrado exitosamente!");
        } catch (RuntimeException e) {
            System.out.println("Datos inválidos: " + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println("Error al registrar el vuelo: " + e.getMessage());
        }
    }

    private void findFlightById() {
        try {
            System.out.print("Ingrese el ID del vuelo: ");
            int id = Integer.parseInt(scanner.nextLine().trim());
            Optional<Flight> flightOpt = flightService.getFlightById(id);
            if (flightOpt.isPresent()) {
                Flight f = flightOpt.get();
                System.out.printf(
                    "ID: %d | Vuelo: %s | Aerolínea ID: %s (%s) | Salida: %s %s | Llegada: %s %s%n",
                    f.getFlightId(),
                    f.getFlightName(),
                    f.getAirlineId(),
                    f.getAirlineName() != null ? f.getAirlineName() : "N/A",
                    f.getDateDeparture(),
                    f.getHourDeparture(),
                    f.getDateArrival(),
                    f.getHourArrival()
                );
            } else {
                System.out.println("No se encontró un vuelo con ID " + id);
            }
        } catch (NumberFormatException e) {
            System.out.println("El ID debe ser un número entero.");
        } catch (RuntimeException e) {
            System.out.println("Error al buscar vuelo: " + e.getMessage());
        }
    }

    private void deleteFlight() {
        try {
            System.out.print("Ingrese el ID del vuelo a eliminar: ");
            int id = Integer.parseInt(scanner.nextLine().trim());
            if (flightService.removeFlight(id)) {
                System.out.println("Vuelo eliminado correctamente.");
            }
        } catch (NumberFormatException e) {
            System.out.println("El ID debe ser un número entero.");
        } catch (RuntimeException e) {
            System.out.println("Error al eliminar vuelo: " + e.getMessage());
        }
    }

    private LocalDate readLocalDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            try {
                return LocalDate.parse(value);
            } catch (DateTimeParseException e) {
                System.out.println("Formato de fecha inválido. Usa YYYY-MM-DD (ejemplo: 2026-11-25).");
            }
        }
    }

    private LocalTime readLocalTime(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            try {
                return LocalTime.parse(value);
            } catch (DateTimeParseException e) {
                System.out.println("Formato de hora inválido. Usa HH:mm o HH:mm:ss (ejemplo: 08:30).");
            }
        }
    }

    private Airport buildAirportIfValid(String rawUuid) {
        if (rawUuid == null || rawUuid.isBlank()) {
            return null;
        }
        UUID uuid = UUID.fromString(rawUuid.trim());
        try {
            Constructor<Airport> ctor = Airport.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            Airport airport = ctor.newInstance();
            for (String setter : List.of("setAirportId", "setId", "setUuid")) {
                try {
                    Method m = Airport.class.getMethod(setter, UUID.class);m.invoke(airport, uuid);
                    return airport;
                } catch (NoSuchMethodException ignored) {
                    try {
                        Method mStr = Airport.class.getMethod(setter, String.class);
                        mStr.invoke(airport, uuid.toString());
                        return airport;
                    } catch (NoSuchMethodException ignored2) {
                    }
                }
            }
            return airport;
        } catch (Exception e) {
            return null;
        }
    }
}
