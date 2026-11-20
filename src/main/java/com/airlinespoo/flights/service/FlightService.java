package com.airlinespoo.flights.service;

import com.airlinespoo.flights.model.Flight;
import com.airlinespoo.flights.repository.FlightRepository;

import java.util.List;
import java.util.Optional;

public class FlightService {

    private final FlightRepository flightRepository = new FlightRepository();

    public void createFlight(Flight flight) {
        if (flight == null) {
            throw new IllegalArgumentException("El vuelo no puede ser nulo.");
        }
        if (flight.getFlightName() == null || flight.getFlightName().isBlank()) {
            throw new IllegalArgumentException("El nombre/código del vuelo es obligatorio.");
        }
        if (flight.getFlightName().length() > 8) {
            throw new IllegalArgumentException("El nombre del vuelo no puede superar 8 caracteres.");
        }
        if (flight.getDateDeparture() == null || flight.getDateArrival() == null) {
            throw new IllegalArgumentException("Las fechas de salida y llegada son obligatorias.");
        }
        if (flight.getHourDeparture() == null || flight.getHourArrival() == null) {
            throw new IllegalArgumentException("Las horas de salida y llegada son obligatorias.");
        }

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
