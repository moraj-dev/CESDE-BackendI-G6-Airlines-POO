package com.airlinespoo.reservations.model;

import com.airlinespoo.core.util.Validator;

import java.util.Objects;

/**
 * Representa una silla dentro de un vuelo como un objeto de valor (Value Object) inmutable.
 *
 * <p>En la base de datos se almacena en las columnas {@code RowNumber} y {@code SeatNumer}.</p>
 */
public final class Seat implements Comparable<Seat> {

    public static final int MAX_SEAT_NUMBER = 99;
    public static final int MAX_ROW_LENGTH = 2;

    private final String rowNumber;
    private final int seatNumber;

    /**
     * Crea una nueva instancia inmutable de {@code Seat}.
     *
     * @param rowNumber  fila de la aeronave (obligatorio, máx. 2 caracteres, alfanumérico)
     * @param seatNumber número de asiento en la fila (entre 1 y 99)
     */
    public Seat(String rowNumber, int seatNumber) {
        this.rowNumber = Validator.requireText(rowNumber, "fila", MAX_ROW_LENGTH).toUpperCase();
        this.seatNumber = (int) Validator.requireInRange(seatNumber, 1, MAX_SEAT_NUMBER, "número de silla");
    }

    public String getRowNumber() {
        return rowNumber;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Seat seat = (Seat) o;
        return seatNumber == seat.seatNumber && Objects.equals(rowNumber, seat.rowNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rowNumber, seatNumber);
    }

    @Override
    public String toString() {
        return rowNumber + "-" + seatNumber;
    }

    @Override
    public int compareTo(Seat other) {
        if (other == null) return 1;
        int rowCompare = compareRows(this.rowNumber, other.rowNumber);
        if (rowCompare != 0) return rowCompare;
        return Integer.compare(this.seatNumber, other.seatNumber);
    }

    private static int compareRows(String r1, String r2) {
        try {
            return Integer.compare(Integer.parseInt(r1), Integer.parseInt(r2));
        } catch (NumberFormatException e) {
            return r1.compareTo(r2);
        }
    }
}
