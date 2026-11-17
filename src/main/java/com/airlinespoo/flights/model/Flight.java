package com.airlinespoo.flights.model;

import com.airlinespoo.locations.model.Airport;

public class Flight {
    private int id;
    private String flightNumber;
    private Airport originAirport;
    private Airport destinationAirport;
    private String departureTime;
    private String arrivalTime;
    private double price;
    private int availableSeats;

    public Flight() {
    }

    public Flight(int id, String flightNumber, Airport originAirport, Airport destinationAirport,
                  String departureTime, String arrivalTime, double price, int availableSeats) {
        this.id = id;
        this.flightNumber = flightNumber;
        this.originAirport = originAirport;
        this.destinationAirport = destinationAirport;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.price = price;
        this.availableSeats = availableSeats;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public Airport getOriginAirport() {
        return originAirport;
    }

    public void setOriginAirport(Airport originAirport) {
        this.originAirport = originAirport;
    }

    public Airport getDestinationAirport() {
        return destinationAirport;
    }

    public void setDestinationAirport(Airport destinationAirport) {
        this.destinationAirport = destinationAirport;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }

    public String getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(String arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    @Override
    public String toString() {
        return "Flight{" +
            "id=" + id +
            ", flightNumber='" + flightNumber + '\'' +
            ", originAirport=" + originAirport +
            ", destinationAirport=" + destinationAirport +
            ", departureTime='" + departureTime + '\'' +
            ", arrivalTime='" + arrivalTime + '\'' +
            ", price=" + price +
            ", availableSeats=" + availableSeats +
            '}';
    }
}
