package com.airlinespoo.locations.repository;

import com.airlinespoo.core.exception.AirlinesException;
import com.airlinespoo.core.exception.DuplicateException;
import com.airlinespoo.core.exception.NotFoundException;
import com.airlinespoo.core.util.Validator;
import com.airlinespoo.locations.model.Airport;
import com.airlinespoo.locations.model.City;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de aeropuertos en SQL Server con JDBC (tabla {@code tblAirports}).
 *
 * <p>Implementa {@link IAirportRepository}. Para armar cada
 * {@link Airport} con su {@link City}, las consultas hacen JOIN con {@code tblCities}
 * (la clave foránea {@code CityId}).</p>
 *
 * <p>El UUID lo genera Java al crear el aeropuerto; la columna {@code AirportId}
 * ({@code UNIQUEIDENTIFIER}) lo recibe como texto.</p>
 */
public class AirportRepository implements IAirportRepository {

    private static final String SELECT_AIRPORT =
            "SELECT a.AirportId, a.name, c.CityId, c.name AS cityName "
            + "FROM tblAirports a JOIN tblCities c ON c.CityId = a.CityId";

    @Override
    public Airport create(Airport airport) {
        if (existsById(airport.getId())) {
            throw new DuplicateException("aeropuerto", airport.getId());
        }
        String sql = "INSERT INTO tblAirports (AirportId, name, CityId) VALUES (?, ?, ?)";
        executeUpdate(sql, airport.getId().toString(), airport.getName(), airport.getCity().getId());
        return airport;
    }

    @Override
    public Optional<Airport> findById(UUID id) {
        List<Airport> found = query(SELECT_AIRPORT + " WHERE a.AirportId = ?", id.toString());
        return found.stream().findFirst();
    }

    @Override
    public List<Airport> findAll() {
        return query(SELECT_AIRPORT);
    }

    @Override
    public Airport update(Airport airport) {
        String sql = "UPDATE tblAirports SET name = ?, CityId = ? WHERE AirportId = ?";
        int rows = executeUpdate(sql, airport.getName(), airport.getCity().getId(), airport.getId().toString());
        if (rows == 0) {
            throw new NotFoundException("aeropuerto", airport.getId());
        }
        return airport;
    }

    @Override
    public boolean deleteById(UUID id) {
        return executeUpdate("DELETE FROM tblAirports WHERE AirportId = ?", id.toString()) > 0;
    }

    @Override
    public boolean existsById(UUID id) {
        return queryCount("SELECT COUNT(*) FROM tblAirports WHERE AirportId = ?", id.toString()) > 0;
    }

    @Override
    public long count() {
        return queryCount("SELECT COUNT(*) FROM tblAirports");
    }

    @Override
    public List<Airport> findByCity(City city) {
        return query(SELECT_AIRPORT + " WHERE a.CityId = ?", city.getId());
    }

    @Override
    public long countByCity(City city) {
        return queryCount("SELECT COUNT(*) FROM tblAirports WHERE CityId = ?", city.getId());
    }

    @Override
    public boolean existsByCity(City city) {
        return countByCity(city) > 0;
    }

    @Override
    public boolean existsByNameAndCity(String name, City city) {
        String target = Validator.normalize(name);
        if (target == null) {
            return false;
        }
        String sql = "SELECT COUNT(*) FROM tblAirports WHERE CityId = ? AND LOWER(name) = LOWER(?)";
        return queryCount(sql, city.getId(), target) > 0;
    }

    @Override
    public List<Airport> findByNameContaining(String text) {
        String target = Validator.normalize(text);
        if (target == null) {
            return List.of();
        }
        return query(SELECT_AIRPORT + " WHERE LOWER(a.name) LIKE ?", "%" + target.toLowerCase() + "%");
    }

    // ------------------------------------------------------------------
    // Ayudas privadas para no repetir el manejo de conexiones
    // ------------------------------------------------------------------

    /** Ejecuta un SELECT y convierte cada fila en un {@link Airport} con su {@link City}. */
    private List<Airport> query(String sql, Object... params) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<Airport> result = new ArrayList<>();
                while (rs.next()) {
                    City city = new City(rs.getInt("CityId"), rs.getString("cityName"));
                    UUID id = UUID.fromString(rs.getString("AirportId"));
                    result.add(new Airport(id, rs.getString("name"), city));
                }
                return result;
            }
        } catch (SQLException e) {
            throw databaseError("consultar aeropuertos", e);
        }
    }

    /** Ejecuta un SELECT COUNT(*) y devuelve el número. */
    private long queryCount(String sql, Object... params) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw databaseError("contar aeropuertos", e);
        }
    }

    /** Ejecuta un INSERT, UPDATE o DELETE y devuelve cuántas filas cambió. */
    private int executeUpdate(String sql, Object... params) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setParams(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw databaseError("modificar aeropuertos", e);
        }
    }

    /** Pone los valores en los signos {@code ?} del SQL, en orden. */
    private static void setParams(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    /** Convierte un error de SQL en una excepción del sistema, que el menú sabe mostrar. */
    private static AirlinesException databaseError(String action, SQLException e) {
        return new AirlinesException("Error de base de datos al " + action + ": " + e.getMessage(), e);
    }
}
