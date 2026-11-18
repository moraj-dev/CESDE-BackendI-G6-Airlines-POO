package com.airlinespoo.flights.service;

import com.airlinespoo.flights.model.Flight;
import com.airlinespoo.flights.repository.FlightRepository;

import java.util.List;
import java.util.Optional;

public class FlightService {

    private final FlightRepository flightRepository = new FlightRepository();

    public void createFlight(Flight flight) {
        // Aquí puedes agregar validaciones de negocio si lo deseas en el futuro
        flightRepository.save(flight);
    }

    public List<Flight> getAllFlights() {
        return flightRepository.findAll();
    }

    public Optional<Flight> getFlightById(int id) {
        return flightRepository.findById(id);
    }

    public boolean removeFlight(int id) {
        boolean deleted = flightRepository.delete(id);
        if (!deleted) {
            throw new IllegalArgumentException("No se encontró un vuelo con el ID especificado.");
        }
        return true;
    }
}
