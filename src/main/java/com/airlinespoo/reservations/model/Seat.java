package com.airlinespoo.reservations.model;

import com.airlinespoo.core.util.Validator;

import java.util.Objects;

/**
 * Representa una silla dentro de un vuelo como un objeto de valor (Value Object) inmutable.
 *
 * <p>En la base de datos se almacena en las columnas separadas {@code RowNumber} y {@code SeatNumer}.
 * En el modelo de objetos se encapsula en esta clase inmutable sin setters. Si un pasajero cambia
 * de silla, se reemplaza la instancia de {@code Seat} completa.</p>
 *
 * <p><b>Interpretación de fila y número:</b></p>
 * <ul>
 *   <li>{@code rowNumber}: Fila de la aeronave, representada como texto en mayúsculas de máximo 2 caracteres
 *       (por ejemplo "1", "12", "A"). Se normaliza y convierte a mayúsculas.</li>
 *   <li>{@code seatNumber}: Número o posición de la silla en la fila, entero positivo entre 1 y {@value #MAX_SEAT_NUMBER}.</li>
 * </ul>
 */
public final class Seat implements Comparable<Seat> {

    /**
     * Número máximo permitido para una silla dentro de una fila.
     */
    public static final int MAX_SEAT_NUMBER = 99;

    /**
     * Longitud máxima en caracteres para la fila.
     */
    public static final int MAX_ROW_LENGTH = 2;

    private final String rowNumber;
    private final int seatNumber;

    /**
     * Crea una nueva instancia inmutable de {@code Seat}.
     *
     * @param rowNumber  fila de la aeronave (obligatorio, máx. 2 caracteres, alfanumérico)
     * @param seatNumber número de asiento en la fila (entre 1 y 99)
     * @throws com.airlinespoo.core.exception.ValidationException si alguno de los datos no cumple las reglas
     */
    public Seat(String rowNumber, int seatNumber) {
        String cleanRow = Validator.requireText(rowNumber, "fila", MAX_ROW_LENGTH).toUpperCase();
        this.rowNumber = cleanRow;
        this.seatNumber = (int) Validator.requireInRange(seatNumber, 1, MAX_SEAT_NUMBER, "número de silla");
    }

    /**
     * @return la fila en mayúsculas
     */
    public String getRowNumber() {
        return rowNumber;
    }

    /**
     * @return el número de silla dentro de la fila
     */
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
        return rowNumber + seatNumber;
    }

    @Override
    public int compareTo(Seat other) {
        if (other == null) {
            return 1;
        }
        int rowCompare = compareRows(this.rowNumber, other.rowNumber);
        if (rowCompare != 0) {
            return rowCompare;
        }
        return Integer.compare(this.seatNumber, other.seatNumber);
    }

    /**
     * Compara dos filas dando prioridad numérica si ambas son números, o alfabética en caso contrario.
     */
    private static int compareRows(String r1, String r2) {
        try {
            int n1 = Integer.parseInt(r1);
            int n2 = Integer.parseInt(r2);
            return Integer.compare(n1, n2);
        } catch (NumberFormatException e) {
            return r1.compareTo(r2);
        }
    }
}
