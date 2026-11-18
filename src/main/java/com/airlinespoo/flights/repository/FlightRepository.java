package com.airlinespoo.flights.repository;

import com.airlinespoo.flights.model.Flight;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FlightRepository {


    private final List<Flight> flights = new ArrayList<>();


    public Flight save(Flight flight) {
        flights.add(flight);
        return flight;
    }


    public List<Flight> findAll() {
        return new ArrayList<>(flights);
    }


    public Optional<Flight> findById(int id) {
        return flights.stream()
            .filter(f -> f.getId() == id)
            .findFirst();
    }


    public boolean delete(int id) {
        return flights.removeIf(f -> f.getId() == id);
    }
}
