package com.airlinespoo.airlines.repository;

import com.airlinespoo.airlines.model.Airline;
import com.airlinespoo.core.exception.AirlinesException;
import com.airlinespoo.core.exception.DuplicateException;
import com.airlinespoo.core.exception.NotFoundException;
import com.airlinespoo.core.util.Validator;
import com.airlinespoo.locations.repository.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de aerolíneas en SQL Server con JDBC (tabla {@code tblAirlines}).
 *
 * <p>Implementa {@link IAirlineRepository}. Cada método abre su conexión mediante
 * {@link DatabaseConnection}, ejecuta la sentencia SQL correspondiente y la cierra
 * automáticamente con {@code try-with-resources}.</p>
 *
 * <p>El identificador lo genera SQL Server ({@code IDENTITY(100,10)}): {@link #create(Airline)}
 * recupera la clave generada tras el {@code INSERT} y devuelve la aerolínea con ese id.</p>
 */
public class AirlineJdbcRepository implements IAirlineRepository {

    private static final int SQL_PRIMARY_KEY_VIOLATION = 2627;
    private static final int SQL_UNIQUE_INDEX_VIOLATION = 2601;

    private static final String SELECT_AIRLINE = "SELECT AirlineId, Name FROM tblAirlines";

    @Override
    public Airline create(Airline airline) {
        Validator.requireNotNull(airline, "aerolínea");
        String sql = "INSERT INTO tblAirlines (Name) VALUES (?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, airline.getName());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Airline(keys.getInt(1), airline.getName());
                }
                throw new AirlinesException("No se pudo obtener el id generado para la aerolínea.");
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == SQL_PRIMARY_KEY_VIOLATION || e.getErrorCode() == SQL_UNIQUE_INDEX_VIOLATION) {
                throw new DuplicateException("Ya existe una aerolínea llamada '%s'.".formatted(airline.getName()));
            }
            throw databaseError("guardar la aerolínea", e);
        }
    }

    @Override
    public Optional<Airline> findById(Integer id) {
        if (id == null) {
            return Optional.empty();
        }
        List<Airline> found = query(SELECT_AIRLINE + " WHERE AirlineId = ?", id);
        return found.stream().findFirst();
    }

    @Override
    public List<Airline> findAll() {
        return query(SELECT_AIRLINE + " ORDER BY AirlineId");
    }

    @Override
    public Airline update(Airline airline) {
        Validator.requireNotNull(airline, "aerolínea");
        String sql = "UPDATE tblAirlines SET Name = ? WHERE AirlineId = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, airline.getName());
            ps.setInt(2, airline.getId());
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new NotFoundException("aerolínea", airline.getId());
            }
            return airline;
        } catch (SQLException e) {
            if (e.getErrorCode() == SQL_PRIMARY_KEY_VIOLATION || e.getErrorCode() == SQL_UNIQUE_INDEX_VIOLATION) {
                throw new DuplicateException("Ya existe una aerolínea llamada '%s'.".formatted(airline.getName()));
            }
            throw databaseError("actualizar la aerolínea", e);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        if (id == null) {
            return false;
        }
        return executeUpdate("DELETE FROM tblAirlines WHERE AirlineId = ?", id) > 0;
    }

    @Override
    public boolean existsById(Integer id) {
        return findById(id).isPresent();
    }

    @Override
    public long count() {
        return queryCount("SELECT COUNT(*) FROM tblAirlines");
    }

    @Override
    public Optional<Airline> findByName(String name) {
        String target = Validator.normalize(name);
        if (target == null || target.isEmpty()) {
            return Optional.empty();
        }
        List<Airline> found = query(SELECT_AIRLINE + " WHERE LOWER(Name) = LOWER(?)", target);
        return found.stream().findFirst();
    }

    @Override
    public boolean existsByName(String name) {
        return findByName(name).isPresent();
    }

    @Override
    public List<Airline> searchByName(String queryText) {
        String target = Validator.normalize(queryText);
        if (target == null || target.isEmpty()) {
            return List.of();
        }
        return query(SELECT_AIRLINE + " WHERE LOWER(Name) LIKE ?", "%" + target.toLowerCase() + "%");
    }

    private List<Airline> query(String sql, Object... params) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<Airline> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(new Airline(rs.getInt("AirlineId"), rs.getString("Name")));
                }
                return result;
            }
        } catch (SQLException e) {
            throw databaseError("consultar aerolíneas", e);
        }
    }

    private long queryCount(String sql, Object... params) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw databaseError("contar aerolíneas", e);
        }
    }

    private int executeUpdate(String sql, Object... params) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setParams(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw databaseError("modificar aerolíneas", e);
        }
    }

    private static void setParams(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    private static AirlinesException databaseError(String action, SQLException e) {
        return new AirlinesException("Error de base de datos al " + action + ": " + e.getMessage(), e);
    }
}
