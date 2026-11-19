package com.airlinespoo.flights.repository;

import com.airlinespoo.core.DatabaseConnection;
import com.airlinespoo.flights.model.Flight;
import com.airlinespoo.locations.model.Airport;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
                FlightId,
                FlightName,
                Departure,
                Arrival,
                DateDeparture,
                HourDeparture,
                DateArrival,
                HourArrival,
                AirlineId
            FROM dbo.tblFlights
            ORDER BY FlightId DESC
            """;

    private static final String SELECT_FLIGHT_BY_ID_SQL = """
            SELECT
                FlightId,
                FlightName,
                Departure,
                Arrival,
                DateDeparture,
                HourDeparture,
                DateArrival,
                HourArrival,
                AirlineId
            FROM dbo.tblFlights
            WHERE FlightId = ?
            """;

    private static final String DELETE_FLIGHT_BY_ID_SQL = """
            DELETE FROM dbo.tblFlights
            WHERE FlightId = ?
            """;

    public void save(Flight flight) {
        if (flight == null) {
            throw new IllegalArgumentException("El vuelo no puede ser nulo.");
        }

        LocalDateTime depDateTime = parseDateTime(flight.getDepartureTime(), LocalDate.now(), LocalTime.of(8, 0));
        LocalDateTime arrDateTime = parseDateTime(flight.getArrivalTime(), depDateTime.toLocalDate(), LocalTime.of(10, 0));

        UUID departureUuid = extractAirportUuid(flight.getOriginAirport());
        UUID arrivalUuid = extractAirportUuid(flight.getDestinationAirport());

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_FLIGHT_SQL)) {

            ps.setString(1, flight.getFlightNumber());

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

            ps.setDate(4, Date.valueOf(depDateTime.toLocalDate()));
            ps.setTime(5, Time.valueOf(depDateTime.toLocalTime()));
            ps.setDate(6, Date.valueOf(arrDateTime.toLocalDate()));
            ps.setTime(7, Time.valueOf(arrDateTime.toLocalTime()));
            ps.setNull(8, Types.INTEGER);

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
        flight.setId(rs.getInt("FlightId"));
        flight.setFlightNumber(rs.getString("FlightName"));

        Date dateDeparture = rs.getDate("DateDeparture");
        Time hourDeparture = rs.getTime("HourDeparture");
        Date dateArrival = rs.getDate("DateArrival");
        Time hourArrival = rs.getTime("HourArrival");

        flight.setDepartureTime(formatDateAndTime(dateDeparture, hourDeparture));
        flight.setArrivalTime(formatDateAndTime(dateArrival, hourArrival));

        return flight;
    }

    private String formatDateAndTime(Date date, Time time) {
        String d = date != null ? date.toLocalDate().toString() : "";
        String t = time != null ? time.toLocalTime().toString() : "";
        return (d + " " + t).trim();
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

    private LocalDateTime parseDateTime(String raw, LocalDate defaultDate, LocalTime defaultTime) {
        if (raw == null || raw.isBlank()) {
            return LocalDateTime.of(defaultDate, defaultTime);
        }

        String value = raw.trim();

        Locale localeEsCo = new Locale("es", "CO");
        List<DateTimeFormatter> timeFormatters = List.of(
            DateTimeFormatter.ofPattern("hh:mm a", Locale.US),
            DateTimeFormatter.ofPattern("h:mm a", Locale.US),
            DateTimeFormatter.ofPattern("hh:mm a", localeEsCo),
            DateTimeFormatter.ofPattern("h:mm a", localeEsCo),
            DateTimeFormatter.ISO_LOCAL_TIME
        );

        for (DateTimeFormatter fmt : timeFormatters) {
            try {
                LocalTime parsedTime = LocalTime.parse(value.toUpperCase(Locale.ROOT), fmt);
                return LocalDateTime.of(defaultDate, parsedTime);
            } catch (DateTimeParseException ignored) {
            }
        }

        for (DateTimeFormatter dtFmt : List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        )) {
            try {
                return LocalDateTime.parse(value, dtFmt);
            } catch (DateTimeParseException ignored) {
            }
        }

        try {
            return LocalDateTime.of(LocalDate.parse(value), defaultTime);
        } catch (DateTimeParseException ignored) {
        }

        return LocalDateTime.of(defaultDate, defaultTime);
    }
}
