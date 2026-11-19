package com.airlinespoo.airlines;

import com.airlinespoo.airlines.model.Airline;
import com.airlinespoo.airlines.repository.AirlineRepository;
import com.airlinespoo.airlines.service.AirlineService;
import com.airlinespoo.airlines.ui.AirlineSampleData;
import com.airlinespoo.core.exception.DuplicateException;
import com.airlinespoo.core.exception.NotFoundException;
import com.airlinespoo.core.exception.ValidationException;
import com.airlinespoo.core.util.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AirlineModuleTest {

    private AirlineRepository repository;
    private IdGenerator idGenerator;
    private AirlineService service;

    @BeforeEach
    void setUp() {
        repository = new AirlineRepository();
        idGenerator = new IdGenerator(100, 10);
        service = new AirlineService(repository, idGenerator);
    }

    @Test
    void crearTresAerolineasSeguidasAsignaIds100_110_120() {
        Airline first = service.createAirline("Avianca");
        Airline second = service.createAirline("LATAM");
        Airline third = service.createAirline("Wingo");

        assertEquals(100, first.getId());
        assertEquals(110, second.getId());
        assertEquals(120, third.getId());
    }

    @Test
    void crearAerolineaConNombreRepetidoIgnorandoMayusculasEsRechazado() {
        service.createAirline("Avianca");

        assertThrows(DuplicateException.class, () -> service.createAirline("avianca"));
        assertThrows(DuplicateException.class, () -> service.createAirline("  AVIANCA  "));
        assertEquals(110, idGenerator.peekNext());
    }

    @Test
    void crearAerolineaConNombreVacioOMayorDe50CaracteresEsRechazado() {
        assertThrows(ValidationException.class, () -> service.createAirline(""));
        assertThrows(ValidationException.class, () -> service.createAirline("   "));
        assertThrows(ValidationException.class, () -> service.createAirline(null));

        String fiftyOneChars = "A".repeat(51);
        assertThrows(ValidationException.class, () -> service.createAirline(fiftyOneChars));
        assertEquals(100, idGenerator.peekNext());
    }

    @Test
    void consultarAerolineaConIdInexistenteLanzaNotFound() {
        assertThrows(NotFoundException.class, () -> service.getAirline(999));
    }

    @Test
    void renombrarAerolineaAlMismoNombreSeAcepta() {
        Airline created = service.createAirline("Avianca");

        Airline renamed = service.renameAirline(created.getId(), "avianca");

        assertEquals(created.getId(), renamed.getId());
        assertEquals("avianca", renamed.getName());
    }

    @Test
    void renombrarAerolineaConNombreDeOtraEsRechazado() {
        Airline avianca = service.createAirline("Avianca");
        service.createAirline("Wingo");

        assertThrows(DuplicateException.class, () -> service.renameAirline(avianca.getId(), "wingo"));
    }

    @Test
    void listarTresAerolineasSalenOrdenadasPorNombre() {
        service.createAirline("Wingo");
        service.createAirline("Avianca");
        service.createAirline("LATAM");

        List<Airline> airlines = service.listAirlines();

        assertEquals(List.of("Avianca", "LATAM", "Wingo"),
                airlines.stream().map(Airline::getName).toList());
    }

    @Test
    void buscarPorTextoParcialIgnorandoMayusculasDevuelveCoincidencias() {
        service.createAirline("Avianca");
        service.createAirline("Avior Airlines");
        service.createAirline("Wingo");

        List<Airline> matches = service.searchAirlinesByName("av");

        assertEquals(List.of("Avianca", "Avior Airlines"),
                matches.stream().map(Airline::getName).toList());
    }

    @Test
    void cargarDatosDeEjemploYRegistrarNuevaAerolineaNoRepiteId() {
        AirlineSampleData.load(service);

        assertEquals(3, service.countAirlines());
        Airline extra = service.createAirline("Satena");

        assertTrue(extra.getId() > 120);
        assertEquals(130, extra.getId());
    }

    @Test
    void modificarListaDevueltaPorServicioNoAlteraElRepositorio() {
        service.createAirline("Avianca");
        List<Airline> airlines = service.listAirlines();

        assertThrows(UnsupportedOperationException.class, airlines::clear);
        assertEquals(1, service.countAirlines());
    }

    @Test
    void eliminarAerolineaExistenteLaRemueveDelRepositorio() {
        Airline avianca = service.createAirline("Avianca");

        service.deleteAirline(avianca.getId());

        assertEquals(0, service.countAirlines());
        assertThrows(NotFoundException.class, () -> service.deleteAirline(avianca.getId()));
    }

    @Test
    void setterDeAirlineValidaLongitudYIgualdadPorId() {
        Airline airline = new Airline(100, "Avianca");
        assertThrows(ValidationException.class, () -> airline.setName("   "));
        assertThrows(ValidationException.class, () -> airline.setName("B".repeat(51)));

        Airline sameId = new Airline(100, "Otro Nombre");
        assertEquals(airline, sameId);
        assertEquals(airline.hashCode(), sameId.hashCode());
    }
}
