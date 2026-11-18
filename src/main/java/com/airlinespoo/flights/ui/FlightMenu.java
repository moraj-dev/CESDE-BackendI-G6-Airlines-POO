package com.airlinespoo.flights.ui;

import com.airlinespoo.flights.model.Flight;
import com.airlinespoo.flights.service.FlightService;
import com.airlinespoo.locations.model.Airport;
import com.airlinespoo.locations.model.City;

import java.util.Scanner;
import java.util.UUID;

public class FlightMenu {
    private final FlightService flightService;
    private final Scanner scanner;

    public FlightMenu(FlightService flightService) {
        this.flightService = flightService;
        this.scanner = new Scanner(System.in);
    }

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

            option = scanner.nextInt();
            scanner.nextLine();

            switch (option) {
                case 1:
                    listFlights();
                    break;
                case 2:
                    createFlight();
                    break;
                case 3:
                    findFlightById();
                    break;
                case 4:
                    removeFlight();
                    break;
                case 0:
                    System.out.println("Saliendo del módulo de vuelos...");
                    break;
                default:
                    System.out.println("Opción inválida. Intente de nuevo.");
            }
        } while (option != 0);
    }

    private void listFlights() {
        System.out.println("\n--- LISTA DE VUELOS ---");
        var flights = flightService.getAllFlights();
        if (flights.isEmpty()) {
            System.out.println("No hay vuelos registrados.");
        } else {
            for (Flight f : flights) {
                System.out.println(f);
            }
        }
    }

    private void createFlight() {
        System.out.println("\n--- REGISTRAR NUEVO VUELO ---");

        // El ID no se pide por consola ya que es autoincrementable (IDENTITY) en la base de datos.
        int id = 0;

        System.out.print("Número de vuelo (ej. LA-402): ");
        String flightNumber = scanner.nextLine();

        System.out.print("Hora de salida: ");
        String departureTime = scanner.nextLine();

        System.out.print("Hora de llegada: ");
        String arrivalTime = scanner.nextLine();

        System.out.print("Precio: ");
        double price = scanner.nextDouble();

        System.out.print("Asientos disponibles: ");
        int seats = scanner.nextInt();
        scanner.nextLine();

        City cityMedellin = new City(1, "Medellín");
        City cityBogota = new City(2, "Bogotá");

        Airport dummyOrigin = new Airport(UUID.randomUUID(), "Aeropuerto Internacional José María Córdova", cityMedellin);
        Airport dummyDestination = new Airport(UUID.randomUUID(), "Aeropuerto El Dorado", cityBogota);

        try {
            Flight newFlight = new Flight(id, flightNumber, dummyOrigin, dummyDestination, departureTime, arrivalTime, price, seats);
            flightService.createFlight(newFlight);
            System.out.println("¡Vuelo registrado exitosamente!");
        } catch (Exception e) {
            System.out.println("Error al registrar el vuelo: " + e.getMessage());
        }
    }

    private void findFlightById() {
        System.out.print("\nIngrese el ID del vuelo a buscar: ");
        int id = scanner.nextInt();
        var flight = flightService.getFlightById(id);
        if (flight.isPresent()) {
            System.out.println("Vuelo encontrado: " + flight.get());
        } else {
            System.out.println("No se encontró ningún vuelo con ese ID.");
        }
    }

    private void removeFlight() {
        System.out.print("\nIngrese el ID del vuelo a eliminar: ");
        int id = scanner.nextInt();
        try {
            flightService.removeFlight(id);
            System.out.println("Vuelo eliminado correctamente.");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
