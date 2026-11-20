package com.airlinespoo.web;

import com.airlinespoo.airlines.model.Airline;
import com.airlinespoo.core.exception.ValidationException;
import com.airlinespoo.customers.model.Country;
import com.airlinespoo.customers.model.Customer;
import com.airlinespoo.customers.model.DocumentType;
import com.airlinespoo.flights.model.Flight;
import com.airlinespoo.locations.model.Airport;
import com.airlinespoo.locations.model.City;
import com.airlinespoo.reservations.model.ManifestEntry;
import com.airlinespoo.reservations.model.Reservation;
import com.airlinespoo.reservations.model.ReservationDetail;
import com.airlinespoo.reservations.model.Seat;

import java.lang.reflect.Array;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Utilidad 100% Java estándar para parsear y serializar JSON,
 * incluyendo tipos temporales, UUID, colecciones y entidades del dominio.
 */
public final class JsonUtil {

    private JsonUtil() {
    }

    // ------------------------------------------------------------------
    // Serialización a JSON
    // ------------------------------------------------------------------

    public static String toJson(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value);
        return sb.toString();
    }

    private static void writeValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String s) {
            writeString(sb, s);
        } else if (value instanceof Boolean || value instanceof Number) {
            sb.append(value);
        } else if (value instanceof LocalDate || value instanceof LocalTime || value instanceof UUID) {
            writeString(sb, value.toString());
        } else if (value instanceof Map<?, ?> map) {
            writeMap(sb, map);
        } else if (value instanceof Collection<?> col) {
            writeCollection(sb, col);
        } else if (value.getClass().isArray()) {
            writeArray(sb, value);
        } else if (value instanceof Country c) {
            writeMap(sb, mapOf("id", c.getId(), "name", c.getName()));
        } else if (value instanceof DocumentType dt) {
            writeMap(sb, mapOf("id", dt.getId(), "description", dt.getDescription()));
        } else if (value instanceof Customer cu) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", cu.getId());
            m.put("firstName", cu.getFirstName());
            m.put("lastName", cu.getLastName());
            m.put("fullName", cu.getFullName());
            m.put("documentType", cu.getDocumentType());
            m.put("telephoneNumber", cu.getTelephoneNumber());
            m.put("email", cu.getEmail());
            m.put("country", cu.getCountry());
            m.put("address", cu.getAddress());
            writeMap(sb, m);
        } else if (value instanceof City city) {
            writeMap(sb, mapOf("id", city.getId(), "name", city.getName()));
        } else if (value instanceof Airport ap) {
            writeMap(sb, mapOf("id", ap.getId(), "name", ap.getName(), "city", ap.getCity()));
        } else if (value instanceof Airline al) {
            writeMap(sb, mapOf("id", al.getId(), "name", al.getName()));
        } else if (value instanceof Flight fl) {
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
            writeMap(sb, m);
        } else if (value instanceof Seat seat) {
            writeMap(sb, mapOf(
                    "rowNumber", seat.getRowNumber(),
                    "seatNumber", seat.getSeatNumber(),
                    "code", ManifestEntry.formatSeat(seat)
            ));
        } else if (value instanceof ReservationDetail rd) {
            writeMap(sb, mapOf(
                    "id", rd.getId(),
                    "passengerFullName", rd.getPassengerFullName(),
                    "passengerDocument", rd.getPassengerDocument(),
                    "seat", rd.getSeat()
            ));
        } else if (value instanceof Reservation res) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", res.getId());
            m.put("customer", res.getCustomer());
            m.put("flight", res.getFlight());
            m.put("passengerCount", res.passengerCount());
            m.put("details", res.getDetails());
            writeMap(sb, m);
        } else if (value instanceof ManifestEntry me) {
            writeMap(sb, mapOf(
                    "reservationId", me.reservationId(),
                    "passengerFullName", me.passengerFullName(),
                    "passengerDocument", me.passengerDocument(),
                    "seat", me.seat()
            ));
        } else {
            writeString(sb, value.toString());
        }
    }

    private static void writeMap(StringBuilder sb, Map<?, ?> map) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            writeString(sb, String.valueOf(entry.getKey()));
            sb.append(':');
            writeValue(sb, entry.getValue());
        }
        sb.append('}');
    }

    private static void writeCollection(StringBuilder sb, Collection<?> col) {
        sb.append('[');
        boolean first = true;
        for (Object item : col) {
            if (!first) sb.append(',');
            first = false;
            writeValue(sb, item);
        }
        sb.append(']');
    }

    private static void writeArray(StringBuilder sb, Object array) {
        sb.append('[');
        int len = Array.getLength(array);
        for (int i = 0; i < len; i++) {
            if (i > 0) sb.append(',');
            writeValue(sb, Array.get(array, i));
        }
        sb.append(']');
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
                }
            }
        }
        sb.append('"');
    }

    public static Map<String, Object> mapOf(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return map;
    }

    // ------------------------------------------------------------------
    // Parsing de JSON
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        Object parsed = new Parser(json).parseValue();
        if (parsed instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new ValidationException("Se esperaba un objeto JSON.");
    }

    public static Object parse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return new Parser(json).parseValue();
    }

    // Helpers de lectura tipada desde Map<String, Object>

    public static String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val == null ? null : String.valueOf(val);
    }

    public static String requireString(Map<String, Object> map, String key) {
        String val = getString(map, key);
        if (val == null || val.isBlank()) {
            throw new ValidationException("El campo '" + key + "' es obligatorio.");
        }
        return val;
    }

    public static Integer getInt(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return null;
        if (val instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(val.toString().trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("El campo '" + key + "' debe ser un número entero válido.");
        }
    }

    public static int requireInt(Map<String, Object> map, String key) {
        Integer val = getInt(map, key);
        if (val == null) {
            throw new ValidationException("El campo '" + key + "' es obligatorio.");
        }
        return val;
    }

    public static short requireShort(Map<String, Object> map, String key) {
        int val = requireInt(map, key);
        if (val < Short.MIN_VALUE || val > Short.MAX_VALUE) {
            throw new ValidationException("El campo '" + key + "' está fuera de rango para Short.");
        }
        return (short) val;
    }

    public static UUID requireUuid(Map<String, Object> map, String key) {
        String raw = requireString(map, key);
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("El campo '" + key + "' debe ser un UUID válido.");
        }
    }

    public static LocalDate requireDate(Map<String, Object> map, String key) {
        String raw = requireString(map, key);
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException("El campo '" + key + "' debe tener formato de fecha YYYY-MM-DD.");
        }
    }

    public static LocalTime requireTime(Map<String, Object> map, String key) {
        String raw = requireString(map, key);
        try {
            return LocalTime.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException("El campo '" + key + "' debe tener formato de hora HH:MM.");
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getObjectList(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return List.of();
        if (!(val instanceof List<?> rawList)) {
            throw new ValidationException("El campo '" + key + "' debe ser una lista JSON.");
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : rawList) {
            if (item instanceof Map<?, ?> itemMap) {
                result.add((Map<String, Object>) itemMap);
            } else {
                throw new ValidationException("Cada elemento de '" + key + "' debe ser un objeto JSON.");
            }
        }
        return result;
    }

    private static final class Parser {
        private final String src;
        private int pos = 0;

        Parser(String src) {
            this.src = src;
        }

        Object parseValue() {
            skipWhitespace();
            if (pos >= src.length()) {
                throw new ValidationException("JSON vacío o incompleto.");
            }
            char c = src.charAt(pos);
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't', 'f' -> parseBoolean();
                case 'n' -> parseNull();
                default -> parseNumber();
            };
        }

        Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object val = parseValue();
                map.put(key, val);
                skipWhitespace();
                if (peek(',')) {
                    pos++;
                } else if (peek('}')) {
                    pos++;
                    return map;
                } else {
                    throw new ValidationException("JSON inválido cerca de la posición " + pos);
                }
            }
        }

        List<Object> parseArray() {
            expect('[');
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                pos++;
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                if (peek(',')) {
                    pos++;
                } else if (peek(']')) {
                    pos++;
                    return list;
                } else {
                    throw new ValidationException("Arreglo JSON inválido cerca de la posición " + pos);
                }
            }
        }

        String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (pos < src.length()) {
                char c = src.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (pos >= src.length()) {
                        throw new ValidationException("Secuencia de escape incompleta en JSON.");
                    }
                    char esc = src.charAt(pos++);
                    switch (esc) {
                        case '"', '\\', '/' -> sb.append(esc);
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (pos + 4 > src.length()) {
                                throw new ValidationException("Escape unicode incompleto en JSON.");
                            }
                            sb.append((char) Integer.parseInt(src.substring(pos, pos + 4), 16));
                            pos += 4;
                        }
                        default -> throw new ValidationException("Escape inválido en JSON: \\" + esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new ValidationException("Cadena JSON sin cerrar.");
        }

        Boolean parseBoolean() {
            if (src.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (src.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw new ValidationException("Booleano inválido en JSON.");
        }

        Object parseNull() {
            if (src.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw new ValidationException("Valor inválido en JSON cerca de la posición " + pos);
        }

        Number parseNumber() {
            int start = pos;
            if (peek('-')) pos++;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
            boolean isDecimal = false;
            if (peek('.')) {
                isDecimal = true;
                pos++;
                while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
            }
            if (peek('e') || peek('E')) {
                isDecimal = true;
                pos++;
                if (peek('+') || peek('-')) pos++;
                while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
            }
            String num = src.substring(start, pos);
            try {
                if (isDecimal) return Double.parseDouble(num);
                long val = Long.parseLong(num);
                if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) {
                    return (int) val;
                }
                return val;
            } catch (NumberFormatException e) {
                throw new ValidationException("Número inválido en JSON: " + num);
            }
        }

        void skipWhitespace() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
                pos++;
            }
        }

        boolean peek(char c) {
            return pos < src.length() && src.charAt(pos) == c;
        }

        void expect(char c) {
            if (pos >= src.length() || src.charAt(pos) != c) {
                throw new ValidationException("Se esperaba '" + c + "' en JSON cerca de la posición " + pos);
            }
            pos++;
        }
    }
}
