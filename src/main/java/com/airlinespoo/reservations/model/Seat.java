package com.airlinespoo.reservations.model;

import java.util.Objects;
import java.util.Locale;

public class Seat {
    private static final  int MAX_SEAT_NUMBER = 99;
    final private  String rowNumber;
    final private int seatNumber;

    public Seat(String rowNumber, int seatNumber) {
    if (rowNumber == null) {
        throw new IllegalArgumentException("La fila es obligatoria.");
    }

    String normalizedRow = rowNumber.strip().toUpperCase(Locale.ROOT);

    if (!normalizedRow.matches("[A-Z0-9]{1,2}")) {
        throw new IllegalArgumentException(
                "La fila debe tener uno o dos caracteres entre letras y números.");
    }

    if (seatNumber < 1 || seatNumber > MAX_SEAT_NUMBER) {
        throw new IllegalArgumentException(
                "El número de silla debe estar entre 1 y " + MAX_SEAT_NUMBER + ".");
    }

    this.rowNumber = normalizedRow;
    this.seatNumber = seatNumber;
}

    public String getRowNumber() {
        return rowNumber;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    @Override
public boolean equals(Object other) {
    if (!(other instanceof Seat otherSeat)) {
        return false;
    }

    return rowNumber.equals(otherSeat.rowNumber)
            && seatNumber == otherSeat.seatNumber;
}

@Override
public int hashCode() {
    return Objects.hash(rowNumber, seatNumber);
}

    


  

}
