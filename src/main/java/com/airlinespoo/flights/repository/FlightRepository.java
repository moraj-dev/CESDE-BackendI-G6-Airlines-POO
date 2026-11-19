package com.airlinespoo.flights.repository;

import com.airlinespoo.flights.model.Flight;
import com.airlinespoo.locations.model.Airport;
import com.airlinespoo.locations.repository.DatabaseConnection;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FlightRepository {

    private static final String INSERT_FLIGHT_SQL = """
            INSERT INTO dbo.tblFlights (
                FlightName,
                Departure,
                Arrival,
                DateDeparture,
                HourDeparture,
                DateArrival,
                HourArrival,
                AirlineId
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SELECT_ALL_FLIGHTS_SQL = """
            SELECT
                f.FlightId,
                f.FlightName,
                f.Departure,
                f.Arrival,
                f.DateDeparture,
                f.HourDeparture,
                f.DateArrival,
                f.HourArrival,
                f.AirlineId,
                a.name AS AirlineName
            FROM dbo.tblFlights f
            LEFT JOIN dbo.tblAirlines a ON f.AirlineId = a.airlineId
            ORDER BY f.FlightId DESC
            """;

    private static final String SELECT_FLIGHT_BY_ID_SQL = """
            SELECT
                f.FlightId,
                f.FlightName,
                f.Departure,
                f.Arrival,
                f.DateDeparture,
                f.HourDeparture,
                f.DateArrival,
                f.HourArrival,
                f.AirlineId,
                a.name AS AirlineName
            FROM dbo.tblFlights f
            LEFT JOIN dbo.tblAirlines a ON f.AirlineId = a.airlineId
            WHERE f.FlightId = ?
            """;

    private static final String DELETE_FLIGHT_BY_ID_SQL = """
            DELETE FROM dbo.tblFlights
            WHERE FlightId = ?
            """;

    public void save(Flight flight) {
        if (flight == null) {
            throw new IllegalArgumentException("El vuelo no puede ser nulo.");
        }

        UUID departureUuid = extractAirportUuid(flight.getDeparture());
        UUID arrivalUuid = extractAirportUuid(flight.getArrival());

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_FLIGHT_SQL)) {

            ps.setString(1, flight.getFlightName());

            if (departureUuid != null) {
                ps.setString(2, departureUuid.toString());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }

            if (arrivalUuid != null) {
                ps.setString(3, arrivalUuid.toString());
            } else {
                ps.setNull(3, Types.VARCHAR);
            }

            ps.setDate(4, flight.getDateDeparture() != null ? Date.valueOf(flight.getDateDeparture()) : null);
            ps.setTime(5, flight.getHourDeparture() != null ? Time.valueOf(flight.getHourDeparture()) : null);
            ps.setDate(6, flight.getDateArrival() != null ? Date.valueOf(flight.getDateArrival()) : null);
            ps.setTime(7, flight.getHourArrival() != null ? Time.valueOf(flight.getHourArrival()) : null);

            if (flight.getAirlineId() != null) {
                ps.setInt(8, flight.getAirlineId());
            } else {
                ps.setNull(8, Types.INTEGER);
            }

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar el vuelo en dbo.tblFlights: " + e.getMessage(), e);
        }
    }

    public List<Flight> findAll() {
        List<Flight> flights = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL_FLIGHTS_SQL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                flights.add(mapRowToFlight(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar los vuelos en dbo.tblFlights: " + e.getMessage(), e);
        }

        return flights;
    }

    public Optional<Flight> findById(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_FLIGHT_BY_ID_SQL)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToFlight(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar el vuelo con ID " + id + ": " + e.getMessage(), e);
        }

        return Optional.empty();
    }

    public boolean delete(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_FLIGHT_BY_ID_SQL)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar el vuelo con ID " + id + ": " + e.getMessage(), e);
        }
    }

    private Flight mapRowToFlight(ResultSet rs) throws SQLException {
        Flight flight = new Flight();
        flight.setFlightId(rs.getInt("FlightId"));
        flight.setFlightName(rs.getString("FlightName"));

        Date dateDeparture = rs.getDate("DateDeparture");
        Time hourDeparture = rs.getTime("HourDeparture");
        Date dateArrival = rs.getDate("DateArrival");
        Time hourArrival = rs.getTime("HourArrival");

        flight.setDateDeparture(dateDeparture != null ? dateDeparture.toLocalDate() : null);
        flight.setHourDeparture(hourDeparture != null ? hourDeparture.toLocalTime() : null);
        flight.setDateArrival(dateArrival != null ? dateArrival.toLocalDate() : null);
        flight.setHourArrival(hourArrival != null ? hourArrival.toLocalTime() : null);

        int airlineId = rs.getInt("AirlineId");
        flight.setAirlineId(rs.wasNull() ? null : airlineId);
        flight.setAirlineName(rs.getString("AirlineName"));

        return flight;
    }

    private UUID extractAirportUuid(Airport airport) {
        if (airport == null) {
            return null;
        }
        for (String methodName : List.of("getAirportId", "getId", "getUuid")) {
            try {
                Method m = airport.getClass().getMethod(methodName);
                Object val = m.invoke(airport);
                if (val instanceof UUID uuid) {
                    return uuid;
                }
                if (val != null && !val.toString().isBlank()) {
                    return UUID.fromString(val.toString().trim());
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
