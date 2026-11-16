package com.airlinespoo.reservations.model;

import com.airlinespoo.core.exception.ValidationException;
import com.airlinespoo.core.model.Identifiable;
import com.airlinespoo.core.util.Validator;

import java.util.Objects;

/**
 * Representa el detalle de una reserva aérea, asociando a un pasajero con su silla asignada.
 *
 * <p>Equivale a la tabla {@code tblDetailsReservation}.</p>
 */
public class ReservationDetail implements Identifiable<Integer> {

    public static final int MAX_PASSENGER_NAME_LENGTH = 50;
    public static final int MAX_PASSENGER_DOCUMENT_LENGTH = 20;

    private final int id;
    private String passengerFullName;
    private String passengerDocument;
    private Seat seat;

    /**
     * Crea un nuevo detalle de reserva validando todos sus datos.
     *
     * @param id                identificador numérico positivo único
     * @param passengerFullName nombre completo del pasajero (obligatorio, máx. 50)
     * @param passengerDocument documento del pasajero (obligatorio, máx. 20, sin espacios)
     * @param seat              silla asignada (obligatoria)
     */
    public ReservationDetail(int id, String passengerFullName, String passengerDocument, Seat seat) {
        this.id = (int) Validator.requirePositive(id, "id de detalle");
        setPassengerFullName(passengerFullName);
        setPassengerDocument(passengerDocument);
        setSeat(seat);
    }

    @Override
    public Integer getId() {
        return id;
    }

    public String getPassengerFullName() {
        return passengerFullName;
    }

    public void setPassengerFullName(String passengerFullName) {
        this.passengerFullName = Validator.requireText(passengerFullName, "nombre completo del pasajero", MAX_PASSENGER_NAME_LENGTH);
    }

    public String getPassengerDocument() {
        return passengerDocument;
    }

    public void setPassengerDocument(String passengerDocument) {
        String cleanDoc = Validator.requireText(passengerDocument, "documento del pasajero", MAX_PASSENGER_DOCUMENT_LENGTH);
        if (cleanDoc.contains(" ")) {
            throw new ValidationException("El documento del pasajero no puede contener espacios.");
        }
        if (!cleanDoc.matches("^[a-zA-Z0-9]+$")) {
            throw new ValidationException("El documento del pasajero solo puede contener letras y dígitos.");
        }
        this.passengerDocument = cleanDoc;
    }

    public Seat getSeat() {
        return seat;
    }

    public void setSeat(Seat seat) {
        this.seat = Validator.requireNotNull(seat, "silla");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReservationDetail that = (ReservationDetail) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Detalle #" + id + " [" + passengerFullName + " (" + passengerDocument + ") - Silla " + seat + "]";
    }
}
