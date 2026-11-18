package com.airlinespoo.app;

import com.airlinespoo.flights.service.FlightService;
import com.airlinespoo.flights.ui.FlightMenu;

public final class Main {

    private Main() {

    }

    public static void main(String[] args) {
        System.out.println("=== Airlines-poo ===");
        System.out.println("Iniciando módulo de gestión de vuelos...");


        FlightService flightService = new FlightService();
        FlightMenu flightMenu = new FlightMenu(flightService);


        flightMenu.showMenu();
    }
}
