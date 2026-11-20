package com.airlinespoo.customers.repository;

import com.airlinespoo.core.repository.InMemoryRepository;
import com.airlinespoo.customers.model.Country;
import com.airlinespoo.locations.repository.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class CountryRepository extends InMemoryRepository<Country, Short> {

    //Metodo para la conexión a la base de datos
    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getConnection();
    }

    //Al heredar Country a InmemoryRepository ya se tiene todo el mapa CRUD básico
    @Override
    protected String entityName(){
        return "País";
    }

    @Override
    public Country create(Country country) {
        String sql = "INSERT INTO dbo.tblCountries (CountryId, Name) VALUES (?, ?)";
        try (Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setShort (1, country.getId());
            pstmt.setString(2, country.getName());
            pstmt.executeUpdate();
            System.out.println("✅ País guardado con éxito");
        } catch (SQLException e) {
            throw new RuntimeException("❌ Error al registrar el país " + e.getMessage());
        }
        return country;
    }

    @Override
    public Optional<Country> findById(Short id) {
        String sql = "SELECT CountryId, Name FROM dbo.tblCountries WHERE CountryId = ?";
        try (Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setShort (1, id);
            try (ResultSet rs = pstmt.executeQuery()){
                if (rs.next()){
                    return Optional.of(new Country(rs.getShort("CountryId"), rs.getString("Name")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("❌ Error al buscar el país por ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Country> findAll() {
        List<Country> countries = new ArrayList<>();
        String sql = "SELECT CountryId, Name FROM dbo.tblCountries ORDER BY Name";
        try (Connection conn = getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)){

            while (rs.next()){
                countries.add(new Country(rs.getShort("CountryId"), rs.getString("Name")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("❌ Error al listar los paises: " + e.getMessage());
        }
        return countries;
    }

    @Override
    public Country update(Country country) {
        String sql = "UPDATE dbo.tblCountries SET Name = ? WHERE CountryId = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, country.getName());
            pstmt.setShort(2, country.getId());
            pstmt.executeUpdate();
            System.out.println("✅ País actualizao con éxito");
        } catch (SQLException e){
            throw new RuntimeException("❌ Error al actualizar el país: " + e.getMessage());
        }
        return country;
    }


    @Override
    public boolean deleteById(Short id) {
        String sql = "DELETE FROM dbo.tblCountries WHERE CountryId = ?";
        try (Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setShort(1, id);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e){
            throw new RuntimeException("❌ Error al eliminar el país: " + e.getMessage());
        }
    }

    @Override
    public boolean existsById(Short id) {
        String sql = "SELECT 1 FROM dbo.tblCountries WHERE CountryId = ?";
        try (Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setShort(1, id);
            try (ResultSet rs = pstmt.executeQuery()){
                return rs.next();
            }
        }catch (SQLException e){
            throw new RuntimeException("❌ Error al verificar existencia del país: " + e.getMessage());
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM dbo.tblCountries";
        try (Connection conn = getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)){

            if (rs.next()){
                return rs.getLong(1);
            }
        } catch (SQLException e){
            throw new RuntimeException("❌ Error al contar países: " + e.getMessage());
        }
        return 0;
    }
}
