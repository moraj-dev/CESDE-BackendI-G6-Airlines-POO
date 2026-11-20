package com.airlinespoo.web;

import com.airlinespoo.airlines.model.Airline;
import com.airlinespoo.airlines.service.AirlineService;
import com.airlinespoo.core.exception.AirlinesException;
import com.airlinespoo.core.exception.BusinessRuleException;
import com.airlinespoo.core.exception.DuplicateException;
import com.airlinespoo.core.exception.NotFoundException;
import com.airlinespoo.core.exception.ValidationException;
import com.airlinespoo.core.util.Validator;
import com.airlinespoo.customers.model.Country;
import com.airlinespoo.customers.model.Customer;
import com.airlinespoo.customers.model.DocumentType;
import com.airlinespoo.customers.service.CustomerService;
import com.airlinespoo.flights.model.Flight;
import com.airlinespoo.flights.service.FlightService;
import com.airlinespoo.locations.model.Airport;
import com.airlinespoo.locations.service.LocationService;
import com.airlinespoo.reservations.model.Reservation;
import com.airlinespoo.reservations.model.Seat;
import com.airlinespoo.reservations.service.ReservationService;
import com.airlinespoo.reservations.service.ReservationService.PassengerRequest;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * Servidor HTTP ligero en Java 21 puro ({@link HttpServer}) que sirve los archivos
 * estáticos de {@code ./web} y expone la API REST de los 5 módulos de Airlines-poo.
 */
public class AirlinesWebServer {

    private final int port;
    private final Path webRoot;
    private final CustomerService customerService;
    private final LocationService locationService;
    private final AirlineService airlineService;
    private final FlightService flightService;
    private final ReservationService reservationService;

    private HttpServer server;

    public AirlinesWebServer(int port,
                             CustomerService customerService,
                             LocationService locationService,
                             AirlineService airlineService,
                             FlightService flightService,
                             ReservationService reservationService) {
        this.port = port;
        this.webRoot = Path.of("web").toAbsolutePath().normalize();
        this.customerService = Validator.requireNotNull(customerService, "servicio de clientes");
        this.locationService = Validator.requireNotNull(locationService, "servicio de ubicaciones");
        this.airlineService = Validator.requireNotNull(airlineService, "servicio de aerolíneas");
        this.flightService = Validator.requireNotNull(flightService, "servicio de vuelos");
        this.reservationService = Validator.requireNotNull(reservationService, "servicio de reservas");
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/", this::handleApi);
        server.createContext("/", this::handleStatic);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // ------------------------------------------------------------------
    // Despachador principal de /api/
    // ------------------------------------------------------------------

    private void handleApi(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod().toUpperCase();
            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());

            if (path.equals("/api/dashboard")) {
                handleDashboard(exchange, method);
            } else if (path.equals("/api/countries") || path.startsWith("/api/countries/")) {
                handleCountries(exchange, method, path);
            } else if (path.equals("/api/document-types") || path.startsWith("/api/document-types/")) {
                handleDocumentTypes(exchange, method, path);
            } else if (path.equals("/api/customers") || path.startsWith("/api/customers/")) {
                handleCustomers(exchange, method, path, query);
            } else if (path.equals("/api/cities") || path.startsWith("/api/cities/")) {
                handleCities(exchange, method, path, query);
            } else if (path.equals("/api/airports") || path.startsWith("/api/airports/")) {
                handleAirports(exchange, method, path, query);
            } else if (path.equals("/api/airlines") || path.startsWith("/api/airlines/")) {
                handleAirlines(exchange, method, path, query);
            } else if (path.equals("/api/flights") || path.startsWith("/api/flights/")) {
                handleFlights(exchange, method, path, query);
            } else if (path.equals("/api/reservations") || path.startsWith("/api/reservations/")) {
                handleReservations(exchange, method, path, query);
            } else {
                sendJson(exchange, 404, JsonUtil.mapOf("error", "Ruta API no encontrada: " + path, "type", "NotFoundException"));
            }
        } catch (Throwable ex) {
            handleException(exchange, ex);
        }
    }

    // ------------------------------------------------------------------
    // Módulo 1: Países, Tipos de Documento y Clientes
    // ------------------------------------------------------------------

    private void handleCountries(HttpExchange ex, String method, String path) throws IOException {
        String idPart = extractTail(path, "/api/countries");
        if (idPart == null) {
            switch (method) {
                case "GET" -> sendJson(ex, 200, customerService.listCountries());
                case "POST" -> {
                    Map<String, Object> body = readJsonBody(ex);
                    short id = JsonUtil.requireShort(body, "id");
                    String name = JsonUtil.requireString(body, "name");
                    Country country = new Country(id, name);
                    customerService.createCountry(country);
                    sendJson(ex, 201, country);
                }
                default -> methodNotAllowed(ex);
            }
            return;
        }
        short id = parseShortId(idPart, "país");
        switch (method) {
            case "GET" -> sendJson(ex, 200, customerService.getCountry(id));
            case "PUT" -> {
                Map<String, Object> body = readJsonBody(ex);
                Country updated = new Country(id, JsonUtil.requireString(body, "name"));
                customerService.updateCountry(updated);
                sendJson(ex, 200, updated);
            }
            case "DELETE" -> {
                customerService.deleteCountry(id);
                sendJson(ex, 200, JsonUtil.mapOf("deleted", true, "id", id));
            }
            default -> methodNotAllowed(ex);
        }
    }

    private void handleDocumentTypes(HttpExchange ex, String method, String path) throws IOException {
        String idPart = extractTail(path, "/api/document-types");
        if (idPart == null) {
            switch (method) {
                case "GET" -> sendJson(ex, 200, customerService.listDocumentTypes());
                case "POST" -> {
                    Map<String, Object> body = readJsonBody(ex);
                    DocumentType dt = new DocumentType(
                            JsonUtil.requireString(body, "id"),
                            JsonUtil.requireString(body, "description")
                    );
                    customerService.createDocumentType(dt);
                    sendJson(ex, 201, dt);
                }
                default -> methodNotAllowed(ex);
            }
            return;
        }
        String id = urlDecode(idPart);
        switch (method) {
            case "GET" -> sendJson(ex, 200, customerService.getDocumentType(id));
            case "PUT" -> {
                Map<String, Object> body = readJsonBody(ex);
                DocumentType updated = new DocumentType(id, JsonUtil.requireString(body, "description"));
                customerService.updateDocumentType(updated);
                sendJson(ex, 200, updated);
            }
            case "DELETE" -> {
                customerService.deleteDocumentType(id);
                sendJson(ex, 200, JsonUtil.mapOf("deleted", true, "id", id));
            }
            default -> methodNotAllowed(ex);
        }
    }

    private void handleCustomers(HttpExchange ex, String method, String path, Map<String, String> query) throws IOException {
        String idPart = extractTail(path, "/api/customers");
        if (idPart == null) {
            switch (method) {
                case "GET" -> {
                    if (query.containsKey("countryId")) {
                        short cid = parseShortId(query.get("countryId"), "país");
                        sendJson(ex, 200, customerService.listCustomerByCountry(cid));
                    } else if (query.containsKey("typeId")) {
                        sendJson(ex, 200, customerService.listCustomerByDocumentType(query.get("typeId")));
                    } else if (query.containsKey("lastName")) {
                        sendJson(ex, 200, customerService.searchCustomerByLastName(query.get("lastName")));
                    } else {
                        sendJson(ex, 200, customerService.listCustomer());
                    }
                }
                case "POST" -> {
                    Map<String, Object> body = readJsonBody(ex);
                    Customer customer = buildCustomerFromBody(JsonUtil.requireString(body, "id"), body);
                    customerService.createCustomer(customer);
                    sendJson(ex, 201, customer);
                }
                default -> methodNotAllowed(ex);
            }
            return;
        }
        String id = urlDecode(idPart);
        switch (method) {
            case "GET" -> sendJson(ex, 200, customerService.getCustomer(id));
            case "PUT" -> {
                Map<String, Object> body = readJsonBody(ex);
                Customer updated = buildCustomerFromBody(id, body);
                customerService.updateCustomer(updated);
                sendJson(ex, 200, customerService.getCustomer(id));
            }
            case "DELETE" -> {
                customerService.deleteCustomer(id);
                sendJson(ex, 200, JsonUtil.mapOf("deleted", true, "id", id));
            }
            default -> methodNotAllowed(ex);
        }
    }

    private Customer buildCustomerFromBody(String id, Map<String, Object> body) {
        String firstName = JsonUtil.requireString(body, "firstName");
        String lastName = JsonUtil.requireString(body, "lastName");
        String docTypeId = JsonUtil.requireString(body, "documentTypeId");
        String phone = JsonUtil.requireString(body, "telephoneNumber");
        short countryId = JsonUtil.requireShort(body, "countryId");

        DocumentType docType = customerService.getDocumentType(docTypeId);
        Country country = customerService.getCountry(countryId);

        Customer customer = new Customer(id, firstName, lastName, docType, phone, country);
        customer.setEmail(JsonUtil.getString(body, "email"));
        customer.setAddress(JsonUtil.getString(body, "address"));
        return customer;
    }

    // ------------------------------------------------------------------
    // Módulo 2: Ciudades y Aeropuertos
    // ------------------------------------------------------------------

    private void handleCities(HttpExchange ex, String method, String path, Map<String, String> query) throws IOException {
        String idPart = extractTail(path, "/api/cities");
        if (idPart == null) {
            switch (method) {
                case "GET" -> {
                    if (query.containsKey("q")) {
                        sendJson(ex, 200, locationService.searchCitiesByName(query.get("q")));
                    } else {
                        sendJson(ex, 200, locationService.listCities());
                    }
                }
                case "POST" -> {
                    Map<String, Object> body = readJsonBody(ex);
                    sendJson(ex, 201, locationService.createCity(JsonUtil.requireString(body, "name")));
                }
                default -> methodNotAllowed(ex);
            }
            return;
        }
        int id = parseIntId(idPart, "ciudad");
        switch (method) {
            case "GET" -> sendJson(ex, 200, locationService.getCity(id));
            case "PUT" -> {
                Map<String, Object> body = readJsonBody(ex);
                sendJson(ex, 200, locationService.updateCity(id, JsonUtil.requireString(body, "name")));
            }
            case "DELETE" -> {
                locationService.deleteCity(id);
                sendJson(ex, 200, JsonUtil.mapOf("deleted", true, "id", id));
            }
            default -> methodNotAllowed(ex);
        }
    }

    private void handleAirports(HttpExchange ex, String method, String path, Map<String, String> query) throws IOException {
        String idPart = extractTail(path, "/api/airports");
        if (idPart == null) {
            switch (method) {
                case "GET" -> {
                    if (query.containsKey("cityId")) {
                        int cityId = parseIntId(query.get("cityId"), "ciudad");
                        sendJson(ex, 200, locationService.listAirportsByCity(cityId));
                    } else if (query.containsKey("q")) {
                        sendJson(ex, 200, locationService.searchAirportsByName(query.get("q")));
                    } else {
                        sendJson(ex, 200, locationService.listAirports());
                    }
                }
                case "POST" -> {
                    Map<String, Object> body = readJsonBody(ex);
                    String name = JsonUtil.requireString(body, "name");
                    int cityId = JsonUtil.requireInt(body, "cityId");
                    sendJson(ex, 201, locationService.createAirport(name, cityId));
                }
                default -> methodNotAllowed(ex);
            }
            return;
        }
        UUID id = parseUuid(idPart, "aeropuerto");
        switch (method) {
            case "GET" -> sendJson(ex, 200, locationService.getAirport(id));
            case "PUT" -> {
                Map<String, Object> body = readJsonBody(ex);
                String name = JsonUtil.requireString(body, "name");
                int cityId = JsonUtil.requireInt(body, "cityId");
                sendJson(ex, 200, locationService.updateAirport(id, name, cityId));
            }
            case "DELETE" -> {
                locationService.deleteAirport(id);
                sendJson(ex, 200, JsonUtil.mapOf("deleted", true, "id", id));
            }
            default -> methodNotAllowed(ex);
        }
    }

    // ------------------------------------------------------------------
    // Módulo 3: Aerolíneas
    // ------------------------------------------------------------------

    private void handleAirlines(HttpExchange ex, String method, String path, Map<String, String> query) throws IOException {
        String idPart = extractTail(path, "/api/airlines");
        if (idPart == null) {
            switch (method) {
                case "GET" -> {
                    if (query.containsKey("q")) {
                        sendJson(ex, 200, airlineService.searchAirlinesByName(query.get("q")));
                    } else {
                        sendJson(ex, 200, airlineService.listAirlines());
                    }
                }
                case "POST" -> {
                    Map<String, Object> body = readJsonBody(ex);
                    sendJson(ex, 201, airlineService.createAirline(JsonUtil.requireString(body, "name")));
                }
                default -> methodNotAllowed(ex);
            }
            return;
        }
        int id = parseIntId(idPart, "aerolínea");
        switch (method) {
            case "GET" -> sendJson(ex, 200, airlineService.getAirline(id));
            case "PUT" -> {
                Map<String, Object> body = readJsonBody(ex);
                sendJson(ex, 200, airlineService.renameAirline(id, JsonUtil.requireString(body, "name")));
            }
            case "DELETE" -> {
                airlineService.deleteAirline(id);
                sendJson(ex, 200, JsonUtil.mapOf("deleted", true, "id", id));
            }
            default -> methodNotAllowed(ex);
        }
    }

    // ------------------------------------------------------------------
    // Dashboard en vivo
    // ------------------------------------------------------------------

    private void handleDashboard(HttpExchange ex, String method) throws IOException {
        if (!"GET".equals(method)) {
            methodNotAllowed(ex);
            return;
        }
        List<Flight> flights = new ArrayList<>(flightService.getAllFlights());
        flights.sort((a, b) -> {
            int cmp = java.util.Comparator.nullsLast(LocalDate::compareTo)
                    .compare(a.getDateDeparture(), b.getDateDeparture());
            if (cmp != 0) return cmp;
            return java.util.Comparator.nullsLast(LocalTime::compareTo)
                    .compare(a.getHourDeparture(), b.getHourDeparture());
        });

        List<Map<String, Object>> upcomingFlights = new ArrayList<>();
        for (Flight f : flights) {
            upcomingFlights.add(flightToMap(f));
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalCustomers", customerService.countCustomers());
        stats.put("totalCountries", customerService.listCountries().size());
        stats.put("totalDocumentTypes", customerService.listDocumentTypes().size());
        stats.put("totalCities", locationService.listCities().size());
        stats.put("totalAirports", locationService.listAirports().size());
        stats.put("totalAirlines", airlineService.countAirlines());
        stats.put("totalFlights", flights.size());
        stats.put("totalReservations", reservationService.listReservations().size());
        stats.put("upcomingFlights", upcomingFlights);
        sendJson(ex, 200, stats);
    }

    // ------------------------------------------------------------------
    // Módulo 4: Vuelos + Reporte + Sillas/Manifiesto
    // ------------------------------------------------------------------

    private void handleFlights(HttpExchange ex, String method, String path, Map<String, String> query) throws IOException {
        if (path.equals("/api/flights/report")) {
            if (!"GET".equals(method)) {
                methodNotAllowed(ex);
                return;
            }
            int airlineId = parseIntId(query.get("airlineId"), "aerolínea");
            int year = parseIntId(query.get("year"), "año");
            int month = parseIntId(query.get("month"), "mes");
            List<Map<String, Object>> report = new ArrayList<>();
            for (Flight f : flightService.getFlightsByAirlineAndMonth(airlineId, year, month)) {
                report.add(flightToMap(f));
            }
            sendJson(ex, 200, report);
            return;
        }

        String tail = extractTail(path, "/api/flights");
        if (tail == null) {
            switch (method) {
                case "GET" -> {
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (Flight f : flightService.getAllFlights()) {
                        list.add(flightToMap(f));
                    }
                    sendJson(ex, 200, list);
                }
                case "POST" -> {
                    Map<String, Object> body = readJsonBody(ex);
                    String flightName = JsonUtil.requireString(body, "flightName");
                    UUID depId = JsonUtil.requireUuid(body, "departureId");
                    UUID arrId = JsonUtil.requireUuid(body, "arrivalId");
                    LocalDate depDate = JsonUtil.requireDate(body, "dateDeparture");
                    LocalTime depTime = JsonUtil.requireTime(body, "hourDeparture");
                    LocalDate arrDate = JsonUtil.requireDate(body, "dateArrival");
                    LocalTime arrTime = JsonUtil.requireTime(body, "hourArrival");
                    int airlineId = JsonUtil.requireInt(body, "airlineId");

                    Airport departure = locationService.getAirport(depId);
                    Airport arrival = locationService.getAirport(arrId);
                    Airline airline = airlineService.getAirline(airlineId);

                    Flight flight = new Flight(0, flightName, departure, arrival, depDate, depTime, arrDate, arrTime, airline);
                    sendJson(ex, 201, flightToMap(flightService.createFlight(flight)));
                }
                default -> methodNotAllowed(ex);
            }
            return;
        }

        String[] parts = tail.split("/");
        int flightId = parseIntId(parts[0], "vuelo");

        if (parts.length == 2 && "seats".equals(parts[1])) {
            if (!"GET".equals(method)) {
                methodNotAllowed(ex);
                return;
            }
            Flight flight = flightService.getFlightOrThrow(flightId);
            List<Map<String, Object>> seats = new ArrayList<>();
            for (Seat s : reservationService.listOccupiedSeats(flight)) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("row", s.getRowNumber());
                m.put("column", s.getSeatNumber());
                m.put("rowNumber", s.getRowNumber());
                m.put("seatNumber", s.getSeatNumber());
                m.put("label", ManifestEntryLabel(s));
                m.put("code", ManifestEntryLabel(s));
                seats.add(m);
            }
            sendJson(ex, 200, seats);
            return;
        }

        if (parts.length == 2 && "manifest".equals(parts[1])) {
            if (!"GET".equals(method)) {
                methodNotAllowed(ex);
                return;
            }
            Flight flight = flightService.getFlightOrThrow(flightId);
            sendJson(ex, 200, reservationService.getFlightManifest(flight));
            return;
        }

        if (parts.length == 1) {
            switch (method) {
                case "GET" -> sendJson(ex, 200, flightToMap(flightService.getFlightOrThrow(flightId)));
                case "DELETE" -> {
                    Flight flight = flightService.getFlightOrThrow(flightId);
                    if (reservationService.flightHasReservations(flight)) {
                        throw new BusinessRuleException("No se puede eliminar el vuelo #" + flightId + " porque tiene reservas asociadas.");
                    }
                    flightService.removeFlight(flightId);
                    sendJson(ex, 200, JsonUtil.mapOf("deleted", true, "id", flightId));
                }
                default -> methodNotAllowed(ex);
            }
            return;
        }

        sendJson(ex, 404, JsonUtil.mapOf("error", "Ruta de vuelos no encontrada.", "type", "NotFoundException"));
    }

    private Map<String, Object> flightToMap(Flight fl) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", fl.getFlightId());
        m.put("flightId", fl.getFlightId());
        m.put("flightName", fl.getFlightName());
        m.put("departure", fl.getDeparture());
        m.put("arrival", fl.getArrival());
        m.put("dateDeparture", fl.getDateDeparture());
        m.put("hourDeparture", fl.getHourDeparture());
        m.put("dateArrival", fl.getDateArrival());
        m.put("hourArrival", fl.getHourArrival());
        m.put("airlineId", fl.getAirlineId());
        m.put("airlineName", fl.getAirlineName());
        m.put("airline", fl.getAirline());
        m.put("occupiedSeatsCount", reservationService.countPassengersByFlight(fl));
        return m;
    }

    private static String ManifestEntryLabel(Seat s) {
        return s.getRowNumber() + "-" + s.getSeatNumber();
    }

    // ------------------------------------------------------------------
    // Módulo 5: Reservas y Pasajeros
    // ------------------------------------------------------------------

    private void handleReservations(HttpExchange ex, String method, String path, Map<String, String> query) throws IOException {
        String tail = extractTail(path, "/api/reservations");
        if (tail == null) {
            switch (method) {
                case "GET" -> {
                    if (query.containsKey("customerId")) {
                        sendJson(ex, 200, reservationService.listReservationsByCustomer(query.get("customerId")));
                    } else if (query.containsKey("flightId")) {
                        int fid = parseIntId(query.get("flightId"), "vuelo");
                        sendJson(ex, 200, reservationService.listReservationsByFlight(fid));
                    } else {
                        sendJson(ex, 200, reservationService.listReservations());
                    }
                }
                case "POST" -> {
                    Map<String, Object> body = readJsonBody(ex);
                    String customerId = JsonUtil.requireString(body, "customerId");
                    int flightId = JsonUtil.requireInt(body, "flightId");
                    List<Map<String, Object>> rawPassengers = JsonUtil.getObjectList(body, "passengers");

                    List<PassengerRequest> requests = new ArrayList<>();
                    for (Map<String, Object> p : rawPassengers) {
                        requests.add(new PassengerRequest(
                                JsonUtil.requireString(p, "fullName"),
                                JsonUtil.requireString(p, "document"),
                                parseSeatFromBody(p)
                        ));
                    }
                    Reservation created = reservationService.createReservation(customerId, flightId, requests);
                    sendJson(ex, 201, created);
                }
                default -> methodNotAllowed(ex);
            }
            return;
        }

        String[] parts = tail.split("/");
        int resId = parseIntId(parts[0], "reserva");

        if (parts.length == 1) {
            switch (method) {
                case "GET" -> sendJson(ex, 200, reservationService.getReservation(resId));
                case "DELETE" -> {
                    reservationService.cancelReservation(resId);
                    sendJson(ex, 200, JsonUtil.mapOf("deleted", true, "id", resId));
                }
                default -> methodNotAllowed(ex);
            }
            return;
        }

        if (parts.length == 2 && "flight".equals(parts[1])) {
            if (!"PUT".equals(method)) {
                methodNotAllowed(ex);
                return;
            }
            Map<String, Object> body = readJsonBody(ex);
            int newFlightId = JsonUtil.requireInt(body, "flightId");
            reservationService.changeReservationFlight(resId, newFlightId);
            sendJson(ex, 200, reservationService.getReservation(resId));
            return;
        }

        if (parts.length >= 2 && "passengers".equals(parts[1])) {
            if (parts.length == 2) {
                switch (method) {
                    case "GET" -> sendJson(ex, 200, reservationService.listPassengers(resId));
                    case "POST" -> {
                        Map<String, Object> body = readJsonBody(ex);
                        Seat seat = parseSeatFromBody(body);
                        reservationService.addPassenger(
                                resId,
                                JsonUtil.requireString(body, "fullName"),
                                JsonUtil.requireString(body, "document"),
                                seat
                        );
                        sendJson(ex, 201, reservationService.getReservation(resId));
                    }
                    case "PUT" -> {
                        Map<String, Object> body = readJsonBody(ex);
                        Seat newSeat = parseSeatFromBody(body);
                        Integer detailId = JsonUtil.getInt(body, "detailId");
                        if (detailId != null) {
                            reservationService.updatePassenger(
                                    resId,
                                    detailId,
                                    JsonUtil.requireString(body, "fullName"),
                                    JsonUtil.requireString(body, "document"),
                                    newSeat
                            );
                        } else {
                            reservationService.changeSeat(resId, JsonUtil.requireString(body, "document"), newSeat);
                        }
                        sendJson(ex, 200, reservationService.getReservation(resId));
                    }
                    default -> methodNotAllowed(ex);
                }
                return;
            }

            if (parts.length == 3 && "PUT".equals(method)) {
                int detailId = parseIntId(parts[2], "detalle");
                Map<String, Object> body = readJsonBody(ex);
                Seat newSeat = parseSeatFromBody(body);
                reservationService.updatePassenger(
                        resId,
                        detailId,
                        JsonUtil.requireString(body, "fullName"),
                        JsonUtil.requireString(body, "document"),
                        newSeat
                );
                sendJson(ex, 200, reservationService.getReservation(resId));
                return;
            }

            if (parts.length == 3 && "DELETE".equals(method)) {
                String passengerDoc = urlDecode(parts[2]);
                reservationService.removePassenger(resId, passengerDoc);
                sendJson(ex, 200, reservationService.getReservation(resId));
                return;
            }
        }

        sendJson(ex, 404, JsonUtil.mapOf("error", "Ruta de reservas no encontrada.", "type", "NotFoundException"));
    }

    private static Seat parseSeatFromBody(Map<String, Object> body) {
        String row = JsonUtil.getString(body, "rowNumber");
        if (row == null || row.isBlank()) {
            row = JsonUtil.requireString(body, "row");
        }
        Integer col = JsonUtil.getInt(body, "seatNumber");
        if (col == null) {
            col = JsonUtil.requireInt(body, "column");
        }
        return new Seat(row, col);
    }

    // ------------------------------------------------------------------
    // Archivos estáticos en ./web
    // ------------------------------------------------------------------

    private void handleStatic(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String rawPath = exchange.getRequestURI().getPath();
        String relPath = (rawPath == null || rawPath.equals("/") || rawPath.isBlank())
                ? "index.html"
                : rawPath.replaceFirst("^/+", "");

        Path target = webRoot.resolve(relPath).normalize();
        if (!target.startsWith(webRoot)) {
            sendText(exchange, 403, "Acceso denegado", "text/plain; charset=UTF-8");
            return;
        }
        if (Files.isDirectory(target)) {
            target = target.resolve("index.html");
        }
        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            sendJson(exchange, 404, JsonUtil.mapOf(
                    "error", "Recurso no encontrado: " + rawPath,
                    "type", "NotFoundException"
            ));
            return;
        }

        byte[] bytes = Files.readAllBytes(target);
        exchange.getResponseHeaders().set("Content-Type", contentTypeFor(target.getFileName().toString()));
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String contentTypeFor(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".html")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }

    // ------------------------------------------------------------------
    // Manejo de excepciones y helpers HTTP
    // ------------------------------------------------------------------

    private void handleException(HttpExchange exchange, Throwable ex) throws IOException {
        int status = 500;
        if (ex instanceof ValidationException || ex instanceof BusinessRuleException || ex instanceof IllegalArgumentException) {
            status = 400;
        } else if (ex instanceof NotFoundException) {
            status = 404;
        } else if (ex instanceof DuplicateException) {
            status = 409;
        } else if (ex instanceof AirlinesException || ex instanceof RuntimeException) {
            SQLException sqlEx = findSqlCause(ex);
            if (sqlEx != null) {
                int code = sqlEx.getErrorCode();
                if (code == 547 || code == 2627 || code == 2601) {
                    status = 409;
                }
            } else if (ex.getMessage() != null && (ex.getMessage().contains("PRIMARY KEY")
                    || ex.getMessage().contains("UNIQUE KEY")
                    || ex.getMessage().contains("duplicate key")
                    || ex.getMessage().contains("REFERENCE constraint"))) {
                status = 409;
            }
        }
        String message = ex.getMessage() != null ? ex.getMessage() : "Error interno del servidor.";
        sendJson(exchange, status, JsonUtil.mapOf("error", message, "type", ex.getClass().getSimpleName()));
    }

    private static SQLException findSqlCause(Throwable ex) {
        Throwable curr = ex;
        while (curr != null) {
            if (curr instanceof SQLException sql) {
                return sql;
            }
            curr = curr.getCause();
        }
        return null;
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void methodNotAllowed(HttpExchange exchange) throws IOException {
        sendJson(exchange, 405, JsonUtil.mapOf("error", "Método HTTP no permitido.", "type", "MethodNotAllowed"));
    }

    private static void sendJson(HttpExchange exchange, int status, Object payload) throws IOException {
        byte[] bytes = JsonUtil.toJson(payload).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendText(HttpExchange exchange, int status, String text, String contentType) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static Map<String, Object> readJsonBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            String raw = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return JsonUtil.parseObject(raw);
        }
    }

    private static String extractTail(String path, String prefix) {
        if (path.equals(prefix)) return null;
        if (!path.startsWith(prefix + "/")) return null;
        String tail = path.substring(prefix.length() + 1);
        return tail.isBlank() ? null : tail;
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> map = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) return map;
        for (String pair : rawQuery.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String k = urlDecode(pair.substring(0, idx));
                String v = urlDecode(pair.substring(idx + 1));
                map.put(k, v);
            }
        }
        return map;
    }

    private static String urlDecode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    private static int parseIntId(String raw, String entity) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("El identificador de " + entity + " es obligatorio.");
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("El identificador de " + entity + " debe ser numérico.");
        }
    }

    private static short parseShortId(String raw, String entity) {
        int val = parseIntId(raw, entity);
        if (val < Short.MIN_VALUE || val > Short.MAX_VALUE) {
            throw new ValidationException("El identificador de " + entity + " está fuera de rango.");
        }
        return (short) val;
    }

    private static UUID parseUuid(String raw, String entity) {
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("El UUID de " + entity + " no es válido.");
        }
    }
}
