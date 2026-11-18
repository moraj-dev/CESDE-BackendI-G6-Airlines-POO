package com.airlinespoo.flights.repository;

import com.airlinespoo.flights.model.Flight;
import com.airlinespoo.core.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FlightRepository {

    public void save(Flight flight) {
        String sql = "INSERT INTO tblFlights (flightNumber, departureTime, arrivalTime, price, availableSeats) VALUES (?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DatabaseConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, flight.getFlightNumber());
            pstmt.setString(2, flight.getDepartureTime());
            pstmt.setString(3, flight.getArrivalTime());
            pstmt.setDouble(4, flight.getPrice());
            pstmt.setInt(5, flight.getAvailableSeats());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    public List<Flight> findAll() {
        List<Flight> flights = new ArrayList<>();
        String sql = "SELECT * FROM tblFlights";
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                Flight flight = new Flight();
                flight.setId(rs.getInt("id"));
                flight.setFlightNumber(rs.getString("flightNumber"));
                flight.setDepartureTime(rs.getString("departureTime"));
                flight.setArrivalTime(rs.getString("arrivalTime"));
                flight.setPrice(rs.getDouble("price"));
                flight.setAvailableSeats(rs.getInt("availableSeats"));
                flights.add(flight);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        return flights;
    }

    public Optional<Flight> findById(int id) {
        String sql = "SELECT * FROM tblFlights WHERE id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                Flight flight = new Flight();
                flight.setId(rs.getInt("id"));
                flight.setFlightNumber(rs.getString("flightNumber"));
                flight.setDepartureTime(rs.getString("departureTime"));
                flight.setArrivalTime(rs.getString("arrivalTime"));
                flight.setPrice(rs.getDouble("price"));
                flight.setAvailableSeats(rs.getInt("availableSeats"));
                return Optional.of(flight);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        return Optional.empty();
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM tblFlights WHERE id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DatabaseConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
}
