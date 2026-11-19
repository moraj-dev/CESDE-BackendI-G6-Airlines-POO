package com.airlinespoo.reservations.model;

import java.util.Comparator;
import java.util.Objects;

/**
 * Registro inmutable que representa una línea del manifiesto de pasajeros de un vuelo.
 *
 * @param reservationId     identificador de la reserva
 * @param passengerFullName nombre completo del pasajero
 * @param passengerDocument documento de identidad del pasajero
 * @param seat              silla asignada en la aeronave
 */
public record ManifestEntry(
        int reservationId,
        String passengerFullName,
        String passengerDocument,
        Seat seat
) implements Comparable<ManifestEntry> {

    /**
     * Comparador compartido para ordenar sillas por fila (numérica o alfabética) y número de silla.
     */
    public static final Comparator<Seat> SEAT_COMPARATOR = (s1, s2) -> {
        if (s1 == s2) return 0;
        if (s1 == null) return -1;
        if (s2 == null) return 1;
        int rowCompare = compareRows(s1.getRowNumber(), s2.getRowNumber());
        if (rowCompare != 0) {
            return rowCompare;
        }
        return Integer.compare(s1.getSeatNumber(), s2.getSeatNumber());
    };

    public ManifestEntry {
        Objects.requireNonNull(passengerFullName, "El nombre del pasajero es obligatorio.");
        Objects.requireNonNull(passengerDocument, "El documento del pasajero es obligatorio.");
        Objects.requireNonNull(seat, "La silla es obligatoria.");
    }

    /**
     * Formatea una silla en texto legible (ej. "12-2").
     *
     * @param seat silla a formatear
     * @return representación en texto
     */
    public static String formatSeat(Seat seat) {
        if (seat == null) return "";
        return seat.getRowNumber() + "-" + seat.getSeatNumber();
    }

    @Override
    public int compareTo(ManifestEntry other) {
        if (other == null) return 1;
        int seatCompare = SEAT_COMPARATOR.compare(this.seat, other.seat);
        if (seatCompare != 0) {
            return seatCompare;
        }
        return this.passengerFullName.compareToIgnoreCase(other.passengerFullName);
    }

    private static int compareRows(String r1, String r2) {
        try {
            return Integer.compare(Integer.parseInt(r1), Integer.parseInt(r2));
        } catch (NumberFormatException e) {
            return r1.compareTo(r2);
        }
    }
}
