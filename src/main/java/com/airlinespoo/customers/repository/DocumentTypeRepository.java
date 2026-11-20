package com.airlinespoo.customers.repository;

import com.airlinespoo.core.repository.InMemoryRepository;
import com.airlinespoo.customers.model.DocumentType;
import com.airlinespoo.locations.repository.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DocumentTypeRepository extends InMemoryRepository<DocumentType, String> {

    // Metodo para la conexión a la base de datos
    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getConnection();
    }

    //Como en Country, hereda el mapa CRUD básico
    @Override
    protected String entityName (){
        return "Tipo de Documento";
    }

    @Override
    public DocumentType create(DocumentType documentType) {
        String sql = "INSERT INTO dbo.tblDocumentsType (TypeId, Description) VALUES (?, ?)";
        try (Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, documentType.getId());
            pstmt.setString(2, documentType.getDescription());
            pstmt.executeUpdate();
            System.out.println("✅ Tipo de documento guardado con éxito");

        } catch (SQLException e){
            throw new RuntimeException("❌ Error al registrar el tipo de documento " + e.getMessage());
        }
        return documentType;
    }

    @Override
    public Optional<DocumentType> findById(String id) {
        String sql = "SELECT TypeId, Description FROM dbo.tblDocumentsType WHERE TypeId = ?";
        try (Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()){
                if (rs.next()){
                    return Optional.of(new DocumentType(rs.getString("TypeId"), rs.getString("Description")));
                }
            }
        } catch (SQLException e){
            throw new RuntimeException("❌ Error al buscar el tipo de documento " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<DocumentType> findAll() {
        List<DocumentType> documentTypes = new ArrayList<>();
        String sql = "SELECT TypeId, Description FROM dbo.tblDocumentsType ORDER BY TypeId";
        try (Connection conn = getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)){

            while (rs.next()){
                documentTypes.add(new DocumentType(rs.getString("TypeId"), rs.getString("Description")));
            }
        }  catch (SQLException e){
            throw new RuntimeException("❌ Error al listar los tipo de documentos " + e.getMessage());
        }
        return documentTypes;
    }

    @Override
    public DocumentType update(DocumentType documentType) {
        String sql = "UPDATE dbo.tblDocumentsType SET Description = ? WHERE TypeId = ?";
        try (Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, documentType.getDescription());
            pstmt.setString(2, documentType.getId());
            pstmt.executeUpdate();
            System.out.println("✅ Tipo de documento actualizado con éxito");

        } catch (SQLException e){
            throw new RuntimeException("❌ Error al actualizar el tipo de documento " + e.getMessage());
        }
        return documentType;
    }

    @Override
    public boolean deleteById(String id) {
        String sql = "DELETE FROM dbo.tblDocumentsType WHERE TypeId = ?";
        try (Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, id);
            int rowsAffected = pstmt.executeUpdate();
            return (rowsAffected > 0);

        } catch (SQLException e){
            throw new RuntimeException("❌ Error al eliminar tipo de documento " + e.getMessage());
        }
    }

    @Override
    public boolean existsById(String id) {
        String sql = "SELECT 1 FROM dbo.tblDocumentsType WHERE TypeId = ?";
        try (Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()){
                return rs.next();
            }
        } catch (SQLException e){
            throw new RuntimeException("❌ Error al verificar si existe el tipo de documento " + e.getMessage());
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM dbo.tblDocumentsType";
        try (Connection conn = getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)){

            if (rs.next()){
                return rs.getLong(1);
            }
        } catch (SQLException e){
            throw new RuntimeException("❌ Error al contar los documentos " + e.getMessage());
        }
        return 0;
    }
}
