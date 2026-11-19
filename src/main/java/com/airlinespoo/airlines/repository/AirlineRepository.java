package com.airlinespoo.airlines.repository;

import com.airlinespoo.airlines.model.Airline;
import com.airlinespoo.core.repository.InMemoryRepository;
import com.airlinespoo.core.util.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio en memoria de aerolíneas (tabla {@code tblAirlines}).
 *
 * <p>Hereda de {@link InMemoryRepository} (que utiliza un {@code LinkedHashMap} para
 * búsquedas directas por id y conserva el orden de inserción) e implementa
 * {@link IAirlineRepository}.</p>
 */
public class AirlineRepository extends InMemoryRepository<Airline, Integer> implements IAirlineRepository {

    @Override
    protected String entityName() {
        return "aerolínea";
    }

    @Override
    public Optional<Airline> findByName(String name) {
        String target = Validator.normalize(name);
        if (target == null || target.isEmpty()) {
            return Optional.empty();
        }
        for (Airline airline : values()) {
            if (airline.getName().equalsIgnoreCase(target)) {
                return Optional.of(airline);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean existsByName(String name) {
        return findByName(name).isPresent();
    }

    @Override
    public List<Airline> searchByName(String query) {
        String target = Validator.normalize(query);
        if (target == null || target.isEmpty()) {
            return List.of();
        }
        String lowerQuery = target.toLowerCase();
        List<Airline> matches = new ArrayList<>();
        for (Airline airline : values()) {
            if (airline.getName().toLowerCase().contains(lowerQuery)) {
                matches.add(airline);
            }
        }
        return matches;
    }
}
