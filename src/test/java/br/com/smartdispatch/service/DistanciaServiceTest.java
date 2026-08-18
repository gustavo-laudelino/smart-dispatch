package br.com.smartdispatch.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DistanciaServiceTest {

    private final DistanciaService distanciaService = new DistanciaService();

    @Test
    void deveRetornarZeroParaCoordenadasIguais() {

        // Arrange
        Double latitude = -22.905;
        Double longitude = -47.060;

        // Act
        double resultado = distanciaService.calcularEmKm(latitude, longitude, latitude, longitude);

        // Assert
        assertEquals(0.0, resultado, 0.0001);
    }

    @Test
    void deveCalcularDistanciaConhecidaPelaFormulaDeHaversine() {

        // Act
        double resultado = distanciaService.calcularEmKm(0.0, 0.0, 0.0, 1.0);

        // Assert
        assertEquals(111.195, resultado, 0.001);
    }

    @Test
    void deveLancarIllegalArgumentQuandoAlgumaCoordenadaForNula() {

        assertIllegalArgumentCoordenadasObrigatorias(null, -47.0, -22.9, -47.0);
        assertIllegalArgumentCoordenadasObrigatorias(-22.9, null, -22.9, -47.0);
        assertIllegalArgumentCoordenadasObrigatorias(-22.9, -47.0, null, -47.0);
        assertIllegalArgumentCoordenadasObrigatorias(-22.9, -47.0, -22.9, null);
    }

    @Test
    void deveLancarIllegalArgumentParaLatitudeForaDoIntervalo() {

        assertIllegalArgumentLatitudeInvalida(-90.1, -47.0, -22.9, -47.0);
        assertIllegalArgumentLatitudeInvalida(90.1, -47.0, -22.9, -47.0);
        assertIllegalArgumentLatitudeInvalida(-22.9, -47.0, -90.1, -47.0);
        assertIllegalArgumentLatitudeInvalida(-22.9, -47.0, 90.1, -47.0);
    }

    @Test
    void deveLancarIllegalArgumentParaLongitudeForaDoIntervalo() {

        assertIllegalArgumentLongitudeInvalida(-22.9, -180.1, -22.9, -47.0);
        assertIllegalArgumentLongitudeInvalida(-22.9, 180.1, -22.9, -47.0);
        assertIllegalArgumentLongitudeInvalida(-22.9, -47.0, -22.9, -180.1);
        assertIllegalArgumentLongitudeInvalida(-22.9, -47.0, -22.9, 180.1);
    }

    // ---------------------------------------------------------------
    // Fixtures auxiliares
    // ---------------------------------------------------------------

    private void assertIllegalArgumentCoordenadasObrigatorias(
            Double latitudeOrigem, Double longitudeOrigem, Double latitudeDestino, Double longitudeDestino
    ) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> distanciaService.calcularEmKm(
                        latitudeOrigem, longitudeOrigem, latitudeDestino, longitudeDestino
                )
        );

        assertEquals(
                "As coordenadas de origem e destino são obrigatórias.",
                exception.getMessage()
        );
    }

    private void assertIllegalArgumentLatitudeInvalida(
            Double latitudeOrigem, Double longitudeOrigem, Double latitudeDestino, Double longitudeDestino
    ) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> distanciaService.calcularEmKm(
                        latitudeOrigem, longitudeOrigem, latitudeDestino, longitudeDestino
                )
        );

        assertEquals("Latitude inválida.", exception.getMessage());
    }

    private void assertIllegalArgumentLongitudeInvalida(
            Double latitudeOrigem, Double longitudeOrigem, Double latitudeDestino, Double longitudeDestino
    ) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> distanciaService.calcularEmKm(
                        latitudeOrigem, longitudeOrigem, latitudeDestino, longitudeDestino
                )
        );

        assertEquals("Longitude inválida.", exception.getMessage());
    }
}
