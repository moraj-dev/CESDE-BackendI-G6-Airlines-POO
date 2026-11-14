package com.airlinespoo.reservations.model;

public class ReservationDetail {
    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_DOCUMENT_LENGTH = 20;

    private final int id;
    private final String passengerFullName;
    private final String passengerDocument;
    private final Seat seat;

    public ReservationDetail(int id, String passengerFullName,
                             String passengerDocument, Seat seat) {
        if (id < 1) {
            throw new IllegalArgumentException("El ID del detalle debe ser positivo.");
        }

        if (passengerFullName == null || passengerFullName.isBlank()) {
            throw new IllegalArgumentException("El nombre del pasajero es obligatorio.");
        }

        String name = passengerFullName.strip();
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("El nombre admite máximo 50 caracteres.");
        }

        if (passengerDocument == null || passengerDocument.isBlank()) {
            throw new IllegalArgumentException("El documento del pasajero es obligatorio.");
        }

        String document = passengerDocument.strip();
        if (document.length() > MAX_DOCUMENT_LENGTH || !document.matches("[A-Za-z0-9]+")) {
            throw new IllegalArgumentException(
                    "El documento admite máximo 20 caracteres y solo puede tener letras de A a Z y dígitos.");
        }

        if (seat == null) {
            throw new IllegalArgumentException("La silla del pasajero es obligatoria.");
        }

        this.id = id;
        this.passengerFullName = name;
        this.passengerDocument = document;
        this.seat = seat;
    }

    
}
