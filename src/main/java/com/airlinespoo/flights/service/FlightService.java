package com.airlinespoo.flights.service;

import com.airlinespoo.airlines.model.Airline;
import com.airlinespoo.airlines.service.AirlineService;
import com.airlinespoo.core.exception.BusinessRuleException;
import com.airlinespoo.core.exception.DuplicateException;
import com.airlinespoo.core.exception.NotFoundException;
import com.airlinespoo.core.util.Validator;
import com.airlinespoo.flights.model.Flight;
import com.airlinespoo.flights.repository.FlightRepository;
import com.airlinespoo.locations.model.Airport;
import com.airlinespoo.locations.service.LocationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de negocio para la gestión de vuelos (Módulo 4).
 */
public class FlightService {

    public static final int MAX_FLIGHT_NAME_LENGTH = 8;

    private final FlightRepository flightRepository;
    private final LocationService locationService;
    private final AirlineService airlineService;

    public FlightService() {
        this(new FlightRepository(), null, null);
    }

    public FlightService(FlightRepository flightRepository,
                         LocationService locationService,
                         AirlineService airlineService) {
        this.flightRepository = Validator.requireNotNull(flightRepository, "repositorio de vuelos");
        this.locationService = locationService;
        this.airlineService = airlineService;
    }

    public Flight createFlight(Flight flight) {
        Validator.requireNotNull(flight, "vuelo");

        String cleanName = Validator.requireText(
                flight.getFlightName(),
                "código del vuelo",
                MAX_FLIGHT_NAME_LENGTH
        ).toUpperCase();
        flight.setFlightName(cleanName);

        if (flightRepository.findByFlightName(cleanName).isPresent()) {
            throw new DuplicateException("Ya existe un vuelo con el código '" + cleanName + "'.");
        }

        Airport departure = Validator.requireNotNull(flight.getDeparture(), "aeropuerto de salida");
        Airport arrival = Validator.requireNotNull(flight.getArrival(), "aeropuerto de llegada");

        if (locationService != null) {
            departure = locationService.getAirport(departure.getId());
            arrival = locationService.getAirport(arrival.getId());
            flight.setDeparture(departure);
            flight.setArrival(arrival);
        }

        if (departure.getId().equals(arrival.getId())) {
            throw new BusinessRuleException("El aeropuerto de salida y el de llegada no pueden ser el mismo.");
        }

        Validator.requireNotNull(flight.getDateDeparture(), "fecha de salida");
        Validator.requireNotNull(flight.getHourDeparture(), "hora de salida");
        Validator.requireNotNull(flight.getDateArrival(), "fecha de llegada");
        Validator.requireNotNull(flight.getHourArrival(), "hora de llegada");

        LocalDateTime departureDt = LocalDateTime.of(flight.getDateDeparture(), flight.getHourDeparture());
        LocalDateTime arrivalDt = LocalDateTime.of(flight.getDateArrival(), flight.getHourArrival());
        if (arrivalDt.isBefore(departureDt)) {
            throw new BusinessRuleException("La fecha y hora de llegada no pueden ser anteriores a la salida.");
        }

        if (airlineService != null) {
            Integer airlineId = flight.getAirlineId();
            Validator.requireNotNull(airlineId, "aerolínea del vuelo");
            Airline airline = airlineService.getAirline(airlineId);
            flight.setAirline(airline);
        }

        return flightRepository.save(flight);
    }

    public List<Flight> getAllFlights() {
        return flightRepository.findAll();
    }

    public Optional<Flight> getFlightById(int id) {
        return flightRepository.findById(id);
    }

    public Flight getFlightOrThrow(int id) {
        return flightRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("vuelo", id));
    }

    public List<Flight> getFlightsByAirlineAndMonth(int airlineId, int year, int month) {
        Validator.requireInRange(month, 1, 12, "mes");
        Validator.requireInRange(year, 2000, 2100, "año");
        if (airlineService != null) {
            airlineService.getAirline(airlineId);
        }
        return flightRepository.findByAirlineAndMonth(airlineId, year, month);
    }

    public boolean removeFlight(int id) {
        if (!flightRepository.delete(id)) {
            throw new NotFoundException("vuelo", id);
        }
        return true;
    }
}
