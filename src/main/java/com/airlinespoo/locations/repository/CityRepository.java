package com.airlinespoo.locations.repository;

import com.airlinespoo.core.exception.AirlinesException;
import com.airlinespoo.core.exception.NotFoundException;
import com.airlinespoo.core.util.Validator;
import com.airlinespoo.locations.model.City;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de ciudades en SQL Server con JDBC (tabla {@code tblCities}).
 *
 * <p>Implementa {@link ICityRepository}. Cada método abre su conexión con
 * {@link DatabaseConnection}, ejecuta un SQL y la cierra (try-with-resources).</p>
 *
 * <p>El id lo genera el motor ({@code IDENTITY(1,1)}): {@link #create(City)} lo lee
 * después del INSERT y devuelve la ciudad con ese id.</p>
 */
public class CityRepository implements ICityRepository {

    private static final String SELECT_CITY = "SELECT CityId, name FROM tblCities";

    /**
     * Inserta la ciudad (INSERT). Se ignora el id que trae el objeto: el motor genera uno nuevo.
     *
     * @param city ciudad a guardar
     * @return una ciudad nueva con el id que asignó la base de datos
     */
    @Override
    public City create(City city) {
        String sql = "INSERT INTO tblCities (name) VALUES (?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, city.getName());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new City(keys.getInt(1), city.getName());
            }
        } catch (SQLException e) {
            throw databaseError("guardar la ciudad", e);
        }
    }

    @Override
    public Optional<City> findById(Integer id) {
        List<City> found = query(SELECT_CITY + " WHERE CityId = ?", id);
        return found.stream().findFirst();
    }

    @Override
    public List<City> findAll() {
        return query(SELECT_CITY + " ORDER BY CityId");
    }

    @Override
    public City update(City city) {
        String sql = "UPDATE tblCities SET name = ? WHERE CityId = ?";
        int rows = executeUpdate(sql, city.getName(), city.getId());
        if (rows == 0) {
            throw new NotFoundException("ciudad", city.getId());
        }
        return city;
    }

    @Override
    public boolean deleteById(Integer id) {
        return executeUpdate("DELETE FROM tblCities WHERE CityId = ?", id) > 0;
    }

    @Override
    public boolean existsById(Integer id) {
        return findById(id).isPresent();
    }

    @Override
    public long count() {
        return queryCount("SELECT COUNT(*) FROM tblCities");
    }

    @Override
    public Optional<City> findByName(String name) {
        String target = Validator.normalize(name);
        if (target == null) {
            return Optional.empty();
        }
        List<City> found = query(SELECT_CITY + " WHERE LOWER(name) = LOWER(?)", target);
        return found.stream().findFirst();
    }

    @Override
    public boolean existsByName(String name) {
        return findByName(name).isPresent();
    }

    @Override
    public List<City> findByNameContaining(String text) {
        String target = Validator.normalize(text);
        if (target == null) {
            return List.of();
        }
        return query(SELECT_CITY + " WHERE LOWER(name) LIKE ?", "%" + target.toLowerCase() + "%");
    }

    // ------------------------------------------------------------------
    // Ayudas privadas para no repetir el manejo de conexiones
    // ------------------------------------------------------------------

    /** Ejecuta un SELECT y convierte cada fila en una {@link City}. */
    private List<City> query(String sql, Object... params) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<City> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(new City(rs.getInt("CityId"), rs.getString("name")));
                }
                return result;
            }
        } catch (SQLException e) {
            throw databaseError("consultar ciudades", e);
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
            throw databaseError("contar ciudades", e);
        }
    }

    /** Ejecuta un INSERT, UPDATE o DELETE y devuelve cuántas filas cambió. */
    private int executeUpdate(String sql, Object... params) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setParams(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw databaseError("modificar ciudades", e);
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
