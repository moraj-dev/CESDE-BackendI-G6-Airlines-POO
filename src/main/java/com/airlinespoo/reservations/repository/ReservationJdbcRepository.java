package com.airlinespoo.reservations.repository;

import com.airlinespoo.core.exception.AirlinesException;
import com.airlinespoo.core.exception.DuplicateException;
import com.airlinespoo.core.exception.NotFoundException;
import com.airlinespoo.core.exception.ValidationException;
import com.airlinespoo.core.util.Validator;
import com.airlinespoo.customers.model.Customer;
import com.airlinespoo.customers.service.CustomerService;
import com.airlinespoo.flights.service.FlightService;
import com.airlinespoo.locations.repository.DatabaseConnection;
import com.airlinespoo.reservations.model.Reservation;
import com.airlinespoo.reservations.model.ReservationDetail;
import com.airlinespoo.reservations.model.Seat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * Repositorio de reservas en SQL Server con JDBC
 * (tablas {@code tblReservations} y {@code tblDetailsReservation}).
 *
 * <p>Implementa {@link IReservationRepository}. Las operaciones compuestas
 * ({@link #create}, {@link #update}, {@link #deleteById}) usan transacciones SQL
 * para garantizar consistencia entre la reserva y sus detalles de pasajeros.</p>
 */
public class ReservationJdbcRepository implements IReservationRepository {

    private static final int SQL_PRIMARY_KEY_VIOLATION = 2627;
    private static final int SQL_UNIQUE_INDEX_VIOLATION = 2601;

    private static final String SELECT_RESERVATIONS =
            "SELECT ReservationId, CustomerId, FlightId FROM tblReservations";
    private static final String SELECT_DETAILS_BY_RESERVATION =
            "SELECT DetailId, ReservationId, PassangerFullname, PassagerDocument, SeatNumer, RowNumber "
                    + "FROM tblDetailsReservation WHERE ReservationId = ? ORDER BY DetailId";
    private static final String SELECT_ALL_DETAILS =
            "SELECT DetailId, ReservationId, PassangerFullname, PassagerDocument, SeatNumer, RowNumber "
                    + "FROM tblDetailsReservation ORDER BY ReservationId, DetailId";

    private final Function<String, Customer> customerLookup;
    private final IntFunction<Object> flightLookup;

    /**
     * Construye el repositorio JDBC integrando los servicios de clientes y vuelos.
     *
     * @param customerService servicio de clientes (Módulo 1) para reconstruir el titular
     * @param flightService   servicio de vuelos (Módulo 4) para reconstruir el vuelo (opcional)
     */
    public ReservationJdbcRepository(CustomerService customerService, FlightService flightService) {
        this(
                Validator.requireNotNull(customerService, "servicio de clientes")::getCustomer,
                flightService != null
                        ? flightId -> flightService.getFlightById(flightId)
                        .<Object>map(f -> f)
                        .orElse(flightId)
                        : Integer::valueOf
        );
    }

    /**
     * Construye el repositorio JDBC resolviendo el vuelo como identificador entero.
     *
     * @param customerService servicio de clientes (Módulo 1)
     */
    public ReservationJdbcRepository(CustomerService customerService) {
        this(customerService, null);
    }

    /**
     * Construye el repositorio JDBC con funciones personalizadas para resolver cliente y vuelo.
     *
     * @param customerLookup función que resuelve un {@link Customer} a partir de {@code CustomerId}
     * @param flightLookup   función que resuelve el objeto vuelo a partir de {@code FlightId}
     */
    public ReservationJdbcRepository(Function<String, Customer> customerLookup, IntFunction<Object> flightLookup) {
        this.customerLookup = Validator.requireNotNull(customerLookup, "resolvedor de clientes");
        this.flightLookup = Validator.requireNotNull(flightLookup, "resolvedor de vuelos");
    }

    @Override
    public Reservation create(Reservation reservation) {
        Validator.requireNotNull(reservation, "reserva");
        int flightId = requireFlightId(reservation.getFlight());

        String insertResSql = "INSERT INTO tblReservations (ReservationId, CustomerId, FlightId) VALUES (?, ?, ?)";
        String insertDetailSql = "INSERT INTO tblDetailsReservation "
                + "(DetailId, ReservationId, PassangerFullname, PassagerDocument, SeatNumer, RowNumber) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement psRes = con.prepareStatement(insertResSql)) {
                    psRes.setInt(1, reservation.getId());
                    psRes.setString(2, reservation.getCustomer().getId());
                    psRes.setInt(3, flightId);
                    psRes.executeUpdate();
                }

                insertDetails(con, insertDetailSql, reservation);
                con.commit();
                return reservation;
            } catch (SQLException e) {
                rollbackQuietly(con);
                if (isDuplicateKeyError(e)) {
                    throw new DuplicateException("reserva", reservation.getId());
                }
                throw databaseError("guardar la reserva", e);
            }
        } catch (SQLException e) {
            throw databaseError("abrir conexión para guardar la reserva", e);
        }
    }

    @Override
    public Optional<Reservation> findById(Integer id) {
        if (id == null) {
            return Optional.empty();
        }
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_RESERVATIONS + " WHERE ReservationId = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                int reservationId = rs.getInt("ReservationId");
                String customerId = rs.getString("CustomerId");
                int flightId = rs.getInt("FlightId");

                List<ReservationDetail> details = loadDetailsForReservation(con, reservationId);
                return Optional.of(buildReservation(reservationId, customerId, flightId, details));
            }
        } catch (SQLException e) {
            throw databaseError("buscar la reserva #" + id, e);
        }
    }

    @Override
    public List<Reservation> findAll() {
        return findAllOrderedById();
    }

    @Override
    public List<Reservation> findAllOrderedById() {
        return queryReservations(SELECT_RESERVATIONS + " ORDER BY ReservationId");
    }

    @Override
    public Reservation update(Reservation reservation) {
        Validator.requireNotNull(reservation, "reserva");
        int flightId = requireFlightId(reservation.getFlight());

        String updateResSql = "UPDATE tblReservations SET CustomerId = ?, FlightId = ? WHERE ReservationId = ?";
        String deleteDetailsSql = "DELETE FROM tblDetailsReservation WHERE ReservationId = ?";
        String insertDetailSql = "INSERT INTO tblDetailsReservation "
                + "(DetailId, ReservationId, PassangerFullname, PassagerDocument, SeatNumer, RowNumber) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                int updatedRows;
                try (PreparedStatement psRes = con.prepareStatement(updateResSql)) {
                    psRes.setString(1, reservation.getCustomer().getId());
                    psRes.setInt(2, flightId);
                    psRes.setInt(3, reservation.getId());
                    updatedRows = psRes.executeUpdate();
                }
                if (updatedRows == 0) {
                    rollbackQuietly(con);
                    throw new NotFoundException("reserva", reservation.getId());
                }

                try (PreparedStatement psDel = con.prepareStatement(deleteDetailsSql)) {
                    psDel.setInt(1, reservation.getId());
                    psDel.executeUpdate();
                }

                insertDetails(con, insertDetailSql, reservation);
                con.commit();
                return reservation;
            } catch (SQLException e) {
                rollbackQuietly(con);
                throw databaseError("actualizar la reserva #" + reservation.getId(), e);
            }
        } catch (SQLException e) {
            throw databaseError("abrir conexión para actualizar la reserva", e);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        if (id == null) {
            return false;
        }
        String deleteDetailsSql = "DELETE FROM tblDetailsReservation WHERE ReservationId = ?";
        String deleteResSql = "DELETE FROM tblReservations WHERE ReservationId = ?";

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement psDetails = con.prepareStatement(deleteDetailsSql)) {
                    psDetails.setInt(1, id);
                    psDetails.executeUpdate();
                }
                int deletedRows;
                try (PreparedStatement psRes = con.prepareStatement(deleteResSql)) {
                    psRes.setInt(1, id);
                    deletedRows = psRes.executeUpdate();
                }
                con.commit();
                return deletedRows > 0;
            } catch (SQLException e) {
                rollbackQuietly(con);
                throw databaseError("eliminar la reserva #" + id, e);
            }
        } catch (SQLException e) {
            throw databaseError("abrir conexión para eliminar la reserva", e);
        }
    }

    @Override
    public boolean existsById(Integer id) {
        if (id == null) {
            return false;
        }
        return queryExists("SELECT 1 FROM tblReservations WHERE ReservationId = ?", id);
    }

    @Override
    public long count() {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM tblReservations");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw databaseError("contar reservas", e);
        }
    }

    @Override
    public List<Reservation> findByCustomer(Customer customer) {
        Validator.requireNotNull(customer, "cliente a consultar");
        return findByCustomerId(customer.getId());
    }

    @Override
    public List<Reservation> findByCustomerId(String customerId) {
        String cleanId = Validator.requireText(customerId, "documento del cliente");
        return queryReservations(
                SELECT_RESERVATIONS + " WHERE LOWER(CustomerId) = LOWER(?) ORDER BY ReservationId",
                cleanId
        );
    }

    @Override
    public List<Reservation> findByFlight(Object flight) {
        Validator.requireNotNull(flight, "vuelo a consultar");
        Integer flightId = ReservationRepository.extractFlightId(flight);
        if (flightId == null) {
            return List.of();
        }
        return queryReservations(
                SELECT_RESERVATIONS + " WHERE FlightId = ? ORDER BY ReservationId",
                flightId
        );
    }

    @Override
    public boolean existsByCustomer(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return false;
        }
        return queryExists(
                "SELECT 1 FROM tblReservations WHERE LOWER(CustomerId) = LOWER(?)",
                customerId.trim()
        );
    }

    @Override
    public boolean existsByFlight(Object flight) {
        if (flight == null) {
            return false;
        }
        Integer flightId = ReservationRepository.extractFlightId(flight);
        if (flightId == null) {
            return false;
        }
        return queryExists("SELECT 1 FROM tblReservations WHERE FlightId = ?", flightId);
    }

    // ------------------------------------------------------------------
    // Ayudas privadas de mapeo y manejo de JDBC
    // ------------------------------------------------------------------

    private void insertDetails(Connection con, String sql, Reservation reservation) throws SQLException {
        try (PreparedStatement psDetail = con.prepareStatement(sql)) {
            for (ReservationDetail detail : reservation.getDetails()) {
                psDetail.setInt(1, detail.getId());
                psDetail.setInt(2, reservation.getId());
                psDetail.setString(3, detail.getPassengerFullName());
                psDetail.setString(4, detail.getPassengerDocument());
                psDetail.setShort(5, (short) detail.getSeat().getSeatNumber());
                psDetail.setString(6, detail.getSeat().getRowNumber());
                psDetail.addBatch();
            }
            psDetail.executeBatch();
        }
    }

    private List<Reservation> queryReservations(String sql, Object... params) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setParams(ps, params);

            List<ReservationRow> rows = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new ReservationRow(
                            rs.getInt("ReservationId"),
                            rs.getString("CustomerId"),
                            rs.getInt("FlightId")
                    ));
                }
            }
            if (rows.isEmpty()) {
                return List.of();
            }

            Map<Integer, List<ReservationDetail>> detailsByResId = loadAllDetailsGrouped(con);
            List<Reservation> result = new ArrayList<>(rows.size());
            for (ReservationRow row : rows) {
                List<ReservationDetail> details = detailsByResId.getOrDefault(row.reservationId(), List.of());
                result.add(buildReservation(row.reservationId(), row.customerId(), row.flightId(), details));
            }
            return result;
        } catch (SQLException e) {
            throw databaseError("consultar reservas", e);
        }
    }

    private List<ReservationDetail> loadDetailsForReservation(Connection con, int reservationId) throws SQLException {
        List<ReservationDetail> details = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(SELECT_DETAILS_BY_RESERVATION)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    details.add(mapDetail(rs));
                }
            }
        }
        return details;
    }

    private Map<Integer, List<ReservationDetail>> loadAllDetailsGrouped(Connection con) throws SQLException {
        Map<Integer, List<ReservationDetail>> grouped = new LinkedHashMap<>();
        try (PreparedStatement ps = con.prepareStatement(SELECT_ALL_DETAILS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int reservationId = rs.getInt("ReservationId");
                grouped.computeIfAbsent(reservationId, k -> new ArrayList<>()).add(mapDetail(rs));
            }
        }
        return grouped;
    }

    private ReservationDetail mapDetail(ResultSet rs) throws SQLException {
        int detailId = rs.getInt("DetailId");
        String fullName = rs.getString("PassangerFullname");
        String document = rs.getString("PassagerDocument");
        int seatNumber = rs.getInt("SeatNumer");
        String rawRow = rs.getString("RowNumber");
        String rowNumber = rawRow == null ? "" : rawRow.trim();
        return new ReservationDetail(detailId, fullName, document, new Seat(rowNumber, seatNumber));
    }

    private Reservation buildReservation(int reservationId, String customerId, int flightId, List<ReservationDetail> details) {
        Customer customer = customerLookup.apply(customerId);
        Object flight = flightLookup.apply(flightId);
        return new Reservation(reservationId, customer, flight, details);
    }

    private boolean queryExists(String sql, Object... params) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw databaseError("verificar existencia de reserva", e);
        }
    }

    private static int requireFlightId(Object flight) {
        Integer flightId = ReservationRepository.extractFlightId(flight);
        if (flightId == null) {
            throw new ValidationException("El vuelo de la reserva debe tener un identificador numérico válido.");
        }
        return flightId;
    }

    private static void setParams(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    private static boolean isDuplicateKeyError(SQLException e) {
        return e.getErrorCode() == SQL_PRIMARY_KEY_VIOLATION || e.getErrorCode() == SQL_UNIQUE_INDEX_VIOLATION;
    }

    private static void rollbackQuietly(Connection con) {
        try {
            con.rollback();
        } catch (SQLException ignored) {
            // Se conserva la excepción original.
        }
    }

    private static AirlinesException databaseError(String action, SQLException e) {
        return new AirlinesException("Error en base de datos al " + action + ": " + e.getMessage(), e);
    }

    private record ReservationRow(int reservationId, String customerId, int flightId) {
    }
}
