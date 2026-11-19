package com.airlinespoo.flights.model;

import com.airlinespoo.locations.model.Airport;

import java.time.LocalDate;
import java.time.LocalTime;

public class Flight {
    private int flightId;
    private String flightName;
    private Airport departure;
    private Airport arrival;
    private LocalDate dateDeparture;
    private LocalTime hourDeparture;
    private LocalDate dateArrival;
    private LocalTime hourArrival;
    private Integer airlineId;
    private String airlineName;

    public Flight() {
    }

    public Flight(int flightId,
                  String flightName,
                  Airport departure,
                  Airport arrival,
                  LocalDate dateDeparture,
                  LocalTime hourDeparture,
                  LocalDate dateArrival,
                  LocalTime hourArrival,
                  Integer airlineId,
                  String airlineName) {
        this.flightId = flightId;
        this.flightName = flightName;
        this.departure = departure;
        this.arrival = arrival;
        this.dateDeparture = dateDeparture;
        this.hourDeparture = hourDeparture;
        this.dateArrival = dateArrival;
        this.hourArrival = hourArrival;
        this.airlineId = airlineId;
        this.airlineName = airlineName;
    }

    public Flight(int flightId,
                  String flightName,
                  Airport departure,
                  Airport arrival,
                  LocalDate dateDeparture,
                  LocalTime hourDeparture,
                  LocalDate dateArrival,
                  LocalTime hourArrival) {
        this(flightId, flightName, departure, arrival, dateDeparture, hourDeparture, dateArrival, hourArrival, null, null);
    }

    public Flight(int flightId,
                  String flightName,
                  Airport departure,
                  Airport arrival,
                  String dateDeparture,
                  String hourDeparture,
                  String dateArrival,
                  String hourArrival) {
        this.flightId = flightId;
        this.flightName = flightName;
        this.departure = departure;
        this.arrival = arrival;
        this.dateDeparture = parseDateOrNull(dateDeparture);
        this.hourDeparture = parseTimeOrNull(hourDeparture);
        this.dateArrival = parseDateOrNull(dateArrival);
        this.hourArrival = parseTimeOrNull(hourArrival);
    }

    private LocalDate parseDateOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }

    private LocalTime parseTimeOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalTime.parse(value.trim());
    }

    public int getFlightId() {
        return flightId;
    }

    public void setFlightId(int flightId) {
        this.flightId = flightId;
    }

    public int getId() {
        return flightId;
    }

    public void setId(int id) {
        this.flightId = id;
    }

    public String getFlightName() {
        return flightName;
    }

    public void setFlightName(String flightName) {
        this.flightName = flightName;
    }

    public Airport getDeparture() {
        return departure;
    }

    public void setDeparture(Airport departure) {
        this.departure = departure;
    }

    public Airport getArrival() {
        return arrival;
    }

    public void setArrival(Airport arrival) {
        this.arrival = arrival;
    }

    public LocalDate getDateDeparture() {
        return dateDeparture;
    }

    public void setDateDeparture(LocalDate dateDeparture) {
        this.dateDeparture = dateDeparture;
    }

    public LocalTime getHourDeparture() {
        return hourDeparture;
    }

    public void setHourDeparture(LocalTime hourDeparture) {
        this.hourDeparture = hourDeparture;
    }

    public LocalDate getDateArrival() {
        return dateArrival;
    }

    public void setDateArrival(LocalDate dateArrival) {
        this.dateArrival = dateArrival;
    }

    public LocalTime getHourArrival() {
        return hourArrival;
    }

    public void setHourArrival(LocalTime hourArrival) {
        this.hourArrival = hourArrival;
    }

    public Integer getAirlineId() {
        return airlineId;
    }

    public void setAirlineId(Integer airlineId) {
        this.airlineId = airlineId;
    }

    public String getAirlineName() {
        return airlineName;
    }

    public void setAirlineName(String airlineName) {
        this.airlineName = airlineName;
    }

    @Override
    public String toString() {
        return "Flight{" +
            "flightId=" + flightId +
            ", flightName='" + flightName + '\'' +
            ", departure=" + departure +
            ", arrival=" + arrival +
            ", dateDeparture=" + dateDeparture +
            ", hourDeparture=" + hourDeparture +
            ", dateArrival=" + dateArrival +
            ", hourArrival=" + hourArrival +
            ", airlineId=" + airlineId +
            ", airlineName='" + airlineName + '\'' +
            '}';
    }
}
