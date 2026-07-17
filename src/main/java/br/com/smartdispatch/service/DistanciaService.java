package br.com.smartdispatch.service;

import org.springframework.stereotype.Service;

@Service
public class DistanciaService {

    private static final double RAIO_TERRA_KM = 6371.0088;

    public double calcularEmKm(
            Double latitudeOrigem,
            Double longitudeOrigem,
            Double latitudeDestino,
            Double longitudeDestino
    ) {
        validarCoordenadas(
                latitudeOrigem,
                longitudeOrigem,
                latitudeDestino,
                longitudeDestino
        );

        double latitudeOrigemRad = Math.toRadians(latitudeOrigem);
        double latitudeDestinoRad = Math.toRadians(latitudeDestino);

        double diferencaLatitude =
                Math.toRadians(latitudeDestino - latitudeOrigem);

        double diferencaLongitude =
                Math.toRadians(longitudeDestino - longitudeOrigem);

        double a =
                Math.pow(Math.sin(diferencaLatitude / 2), 2)
                        + Math.cos(latitudeOrigemRad)
                        * Math.cos(latitudeDestinoRad)
                        * Math.pow(Math.sin(diferencaLongitude / 2), 2);

        double c = 2 * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(1 - a)
        );

        return RAIO_TERRA_KM * c;
    }

    private void validarCoordenadas(
            Double latitudeOrigem,
            Double longitudeOrigem,
            Double latitudeDestino,
            Double longitudeDestino
    ) {
        if (
                latitudeOrigem == null
                        || longitudeOrigem == null
                        || latitudeDestino == null
                        || longitudeDestino == null
        ) {
            throw new IllegalArgumentException(
                    "As coordenadas de origem e destino são obrigatórias."
            );
        }

        if (
                latitudeOrigem < -90
                        || latitudeOrigem > 90
                        || latitudeDestino < -90
                        || latitudeDestino > 90
        ) {
            throw new IllegalArgumentException("Latitude inválida.");
        }

        if (
                longitudeOrigem < -180
                        || longitudeOrigem > 180
                        || longitudeDestino < -180
                        || longitudeDestino > 180
        ) {
            throw new IllegalArgumentException("Longitude inválida.");
        }
    }
}