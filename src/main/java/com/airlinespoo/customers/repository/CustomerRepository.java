package com.airlinespoo.customers.repository;

import com.airlinespoo.core.repository.InMemoryRepository;
import com.airlinespoo.customers.model.Customer;
import com.airlinespoo.customers.model.Country;
import com.airlinespoo.customers.model.DocumentType;
import com.airlinespoo.locations.repository.DatabaseConnection;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

public class CustomerRepository extends InMemoryRepository<Customer,String> {

    //Metodo de conexión a la base de datos
    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getConnection();
    }

    @Override
    protected String entityName (){
        return "Cliente";
    }

    @Override
    public Customer create(Customer customer) {
        String sql = "INSERT INTO dbo.tblCustomers (customerId, FirtName, Lastname, TypeId, TelephoneNumber, Email, CountryId, Address)" +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, customer.getId());
            pstmt.setString(2, customer.getFirstName());
            pstmt.setString(3, customer.getLastName());
            pstmt.setString(4, customer.getDocumentType() != null ? customer.getDocumentType().getId() : null);
            pstmt.setString(5, customer.getTelephoneNumber());
            pstmt.setString(6, customer.getEmail());
            pstmt.setShort(7, customer.getCountry() != null ? customer.getCountry().getId() : 0);
            pstmt.setString(8, customer.getAddress());

            pstmt.executeUpdate();
            System.out.println("✅ Cliente guardado con éxito");

        } catch (SQLException e) {
            throw new RuntimeException("Error al registrar el cliente: " + e.getMessage());
        }
        return customer;
    }

    @Override
    public Optional<Customer> findById(String id) {
        String sql = "SELECT c.customerId, c.FirtName, c.Lastname, c.TelephoneNumber, c.Email, c.Address, " +
            "d.TypeId, d.Description AS Doc, " +
            "co.CountryId, co.Name AS CountryName " +
            "FROM dbo.tblCustomers c " +
            "INNER JOIN dbo.tblDocumentsType d ON c.TypeId = d.TypeId " +
            "INNER JOIN dbo.tblCountries co ON c.CountryId = co.CountryId " +
            "WHERE c.customerId = ?";

        try (Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()){
                if (rs.next()){
                    DocumentType documentType = new DocumentType(rs.getString("TypeId"), rs.getString("Doc"));
                    Country country = new Country(rs.getShort("CountryId"), rs.getString("CountryName"));

                    Customer customer = new Customer(
                        rs.getString("customerId"),
                        rs.getString("FirtName"),
                        rs.getString("Lastname"),
                        documentType,
                        rs.getString("TelephoneNumber"),
                        country
                    );
                    customer.setEmail(rs.getString("Email"));
                    customer.setAddress(rs.getString("Address"));

                    return Optional.of(customer);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("❌ Error al buscar cliente por ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Customer> findAll() {
        List<Customer> customers = new ArrayList<>();
        String sql = "SELECT c.customerId, c.FirtName, c.Lastname, c.TelephoneNumber, c.Email, c.Address, " +
            "d.TypeId, d.Description AS Doc, " +
            "co.CountryId, co.Name AS CountryName " +
            "FROM dbo.tblCustomers c " +
            "INNER JOIN dbo.tblDocumentsType d ON c.TypeId = d.TypeId " +
            "INNER JOIN dbo.tblCountries co ON c.CountryId = co.CountryId";

        try (Connection conn = getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)){

            while (rs.next()){
                DocumentType documentType = new DocumentType(rs.getString("TypeId"), rs.getString("Doc"));
                Country country = new Country(rs.getShort("CountryId"), rs.getString("CountryName"));

                Customer customer = new Customer(
                    rs.getString("customerId"),
                    rs.getString("FirtName"),
                    rs.getString("Lastname"),
                    documentType,
                    rs.getString("TelephoneNumber"),
                    country
                );
                customer.setEmail(rs.getString("Email"));
                customer.setAddress(rs.getString("Address"));

                customers.add(customer);
            }
        } catch (SQLException e) {
            throw new RuntimeException("❌ Error al listar clientes por ID: " + e.getMessage());
        }
        return customers;
    }

    @Override
    public Customer update(Customer customer) {
        String sql ="UPDATE dbo.tblCustomers SET FirtName = ?, Lastname = ?, TypeId = ?, TelephoneNumber = ?, Email = ?, CountryId = ?, Address = ? " +
                    "WHERE customerId = ?";

        try (Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, customer.getFirstName());
            pstmt.setString(2,customer.getLastName());
            pstmt.setString(3, customer.getDocumentType().getId());
            pstmt.setString(4, customer.getTelephoneNumber());
            pstmt.setString(5, customer.getEmail());
            pstmt.setShort(6, customer.getCountry().getId());
            pstmt.setString(7, customer.getAddress());
            pstmt.setString(8, customer.getId());

            pstmt.executeUpdate();
            System.out.println("✅ El cliente actualizado exitosamente");

        } catch (SQLException e) {
            throw new RuntimeException("❌ Error al actualizar el cliente: " + e.getMessage());
        }
        return customer;
    }

    @Override
    public boolean deleteById(String id) {
        String sql = "DELETE FROM dbo.tblCustomers WHERE customerId = ?";
        try (Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, id);
            int rowsAffected = pstmt.executeUpdate();
            return (rowsAffected > 0);

        } catch (SQLException e) {
            throw new RuntimeException("❌ Error al eliminar el cliente: " + e.getMessage());
        }
    }

    @Override
    public boolean existsById(String id) {
        String sql = "SELECT 1 FROM dbo.tblCustomers WHERE customerId = ?";
        try (Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()){
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("❌ Error al verificar la existencia del cliente " + e.getMessage());

        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM dbo.tblCustomers";
        try (Connection conn = getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)){

            if (rs.next()){
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("❌ Error al contar los clientes: " + e.getMessage());
        }
        return 0;
    }

    //Busqueda por email
    public Optional<Customer> findByEmail (String email){
        String sql = "SELECT c.customerId, c.FirtName, c.Lastname, c.TelephoneNumber, c.Email, c.Address, " +
            "d.TypeId, d.Description AS Doc, " +
            "co.CountryId, co.Name AS CountryName " +
            "FROM dbo.tblCustomers c " +
            "INNER JOIN dbo.tblDocumentsType d ON c.TypeId = d.TypeId " +
            "INNER JOIN dbo.tblCountries co ON c.CountryId = co.CountryId " +
            "WHERE c.Email = ?";

        try (Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()){
                if (rs.next()){
                    DocumentType documentType = new DocumentType(rs.getString("TypeId"), rs.getString("Doc"));
                    Country country = new Country(rs.getShort("CountryId"), rs.getString("CountryName"));

                    Customer customer = new Customer(
                        rs.getString("customerId"),
                        rs.getString("FirtName"),
                        rs.getString("Lastname"),
                        documentType,
                        rs.getString("TelephoneNumber"),
                        country
                    );
                    customer.setEmail(rs.getString("Email"));
                    customer.setAddress(rs.getString("Address"));

                    return Optional.of(customer);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("❌ Error al buscar cliente por email: " + e.getMessage());
        }
        return Optional.empty();
    }

    //Busqueda por apellido
    public List<Customer> searchByLastName (String lastNamePart){
        List<Customer> customers = new ArrayList<>();
        String sql = "SELECT c.customerId, c.FirtName, c.Lastname, c.TelephoneNumber, c.Email, c.Address, " +
                    "d.TypeId, d.Description AS Doc, " +
                    "co.CountryId, co.Name AS CountryName " +
                    "FROM dbo.tblCustomers c " +
                    "INNER JOIN dbo.tblDocumentsType d ON c.TypeId = d.TypeId " +
                    "INNER JOIN dbo.tblCountries co ON c.CountryId = co.CountryId " +
                    "WHERE c.LastName = ?";

        try (Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, "%" + lastNamePart + "%");
            try (ResultSet rs = pstmt.executeQuery()){
                while (rs.next()){
                    DocumentType documentType = new DocumentType(rs.getString("TypeId"), rs.getString("Doc"));
                    Country country = new Country(rs.getShort("CountryId"), rs.getString("CountryName"));

                    Customer customer = new Customer(
                        rs.getString("customerId"),
                        rs.getString("FirtName"),
                        rs.getString("Lastname"),
                        documentType,
                        rs.getString("TelephoneNumber"),
                        country
                    );
                    customer.setEmail(rs.getString("Email"));
                    customer.setAddress(rs.getString("Address"));

                    customers.add(customer);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("❌ Error al buscar cliente por apellido: " + e.getMessage());
        }
        return customers;
    }

    //Busqueda por País
    public List<Customer> findByCountry (Short countryId){
        List<Customer> customers = new ArrayList<>();
        String sql = "SELECT c.customerId, c.FirtName, c.Lastname, c.TelephoneNumber, c.Email, c.Address,  " +
                    "d.TypeId, d.Description AS Doc, " +
                    "co.CountryId, co.Name AS CountryName " +
                    "FROM dbo.tblCustomers c " +
                    "INNER JOIN dbo.tblDocumentsType d ON c.TypeId = d.TypeId " +
                    "INNER JOIN dbo.tblCountries co ON c.CountryId = co.CountryId " +
                    "WHERE c.CountryId = ?";

        try (Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setShort(1, countryId);
            try (ResultSet rs = pstmt.executeQuery()){
                while (rs.next()){
                    DocumentType documentType = new DocumentType(rs.getString("TypeId"), rs.getString("Doc"));
                    Country country = new Country(rs.getShort("CountryId"), rs.getString("CountryName"));

                    Customer customer = new Customer(
                        rs.getString("customerId"),
                        rs.getString("FirtName"),
                        rs.getString("Lastname"),
                        documentType,
                        rs.getString("TelephoneNumber"),
                        country
                    );
                    customer.setEmail(rs.getString("Email"));
                    customer.setAddress(rs.getString("Address"));

                    customers.add(customer);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("❌ Error al buscar clientes por país: " + e.getMessage());
        }
        return customers;
    }

    //Busqueda por Tipo de documento
    public List<Customer> findByDocumentType (String documentTypeId){
        List<Customer> customers = new ArrayList<>();
        String sql = "SELECT c.customerId, c.FirtName, c.Lastname, c.TelephoneNumber, c.Email, c.Address, " +
            "d.TypeId, d.Description AS DocDesc, " +
            "co.CountryId, co.Name AS CountryName " +
            "FROM dbo.tblCustomers c " +
            "INNER JOIN dbo.tblDocumentsType d ON c.TypeId = d.TypeId " +
            "INNER JOIN dbo.tblCountries co ON c.CountryId = co.CountryId " +
            "WHERE c.TypeId = ?";

        try (Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, documentTypeId);
            try (ResultSet rs = pstmt.executeQuery()){
                while (rs.next()){
                    DocumentType documentType = new DocumentType(rs.getString("TypeId"), rs.getString("Doc"));
                    Country country = new Country(rs.getShort("CountryId"), rs.getString("CountryName"));

                    Customer customer = new Customer(
                        rs.getString("customerId"),
                        rs.getString("FirtName"),
                        rs.getString("Lastname"),
                        documentType,
                        rs.getString("TelephoneNumber"),
                        country
                    );
                    customer.setEmail(rs.getString("Email"));
                    customer.setAddress(rs.getString("Address"));

                    customers.add(customer);
                }
            }
        } catch (SQLException e) {
            throw  new RuntimeException("❌ Error al buscar clientes por tipo de documento: " + e.getMessage());
        }
        return customers;
    }
}
