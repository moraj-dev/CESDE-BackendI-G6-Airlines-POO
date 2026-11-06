package com.airlinespoo.reservations.model;

import java.util.Objects;

/**
 * Registro inmutable que representa una línea del manifiesto de pasajeros de un vuelo.
 *
 * <p>Agrupa los datos esenciales del pasajero para el abordaje y control de vuelo:
 * identificador de la reserva, nombre completo, documento y silla asignada.</p>
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

    public ManifestEntry {
        Objects.requireNonNull(passengerFullName, "El nombre del pasajero es obligatorio.");
        Objects.requireNonNull(passengerDocument, "El documento del pasajero es obligatorio.");
        Objects.requireNonNull(seat, "La silla es obligatoria.");
    }

    @Override
    public int compareTo(ManifestEntry other) {
        if (other == null) return 1;
        int seatCompare = this.seat.compareTo(other.seat);
        if (seatCompare != 0) {
            return seatCompare;
        }
        return this.passengerFullName.compareToIgnoreCase(other.passengerFullName);
    }
}
