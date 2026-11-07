package edu.utn.frsf.isi.dan.gestion.demo;

import edu.utn.frsf.isi.dan.gestion.model.Amenity;
import java.util.List;
import java.util.Map;

/**
 * Catálogo de los datos de demo: hoteles ficticios en ciudades argentinas (coordenadas reales del
 * centro de cada ciudad), tarifas por tipo de habitación y habitaciones.
 *
 * <p>Los tipos de habitación son los que siembra {@code
 * docker/config/postgres/initdb/01_schema.sql}: 1 SINGLE (1 huésped), 2 DOBLE INDIVIDUAL (2), 3
 * DOBLE (2), 5 TRIPLE (3), 6 CUADRUPLE (4), 7 DOBLE SUPERIOR (2).
 */
final class DemoCatalogo {

  private DemoCatalogo() {}

  /** Una habitación: piso, número y tipo. */
  record Hab(int piso, int numero, int tipo) {}

  record HotelDemo(
      String nombre,
      String cuitBase,
      String domicilio,
      double latitud,
      double longitud,
      String telefono,
      String correo,
      int categoria,
      List<Amenity> amenities,
      List<Hab> habitaciones) {}

  /** Precio por noche (ARS) de la tarifa común de cada tipo de habitación. */
  static final Map<Integer, Double> TARIFAS_POR_TIPO =
      Map.of(
          1, 28_000.0,
          2, 42_000.0,
          3, 48_000.0,
          5, 64_000.0,
          6, 85_000.0,
          7, 72_000.0);

  static final List<HotelDemo> HOTELES =
      List.of(
          new HotelDemo(
              "Hotel Costanera Santa Fe",
              "3071000001",
              "Bv. Gálvez 1500, Santa Fe",
              -31.6333,
              -60.7000,
              "+54 342 455-1000",
              "reservas@costanera-demo.example",
              4,
              List.of(
                  Amenity.WIFI,
                  Amenity.PILETA,
                  Amenity.GIMNASIO,
                  Amenity.RESTAURANTE,
                  Amenity.ESTACIONAMIENTO,
                  Amenity.AIRE_ACONDICIONADO),
              List.of(
                  new Hab(1, 101, 2),
                  new Hab(1, 102, 3),
                  new Hab(2, 201, 3),
                  new Hab(2, 202, 5),
                  new Hab(3, 301, 6))),
          new HotelDemo(
              "Gran Hotel Monumento Rosario",
              "3071000002",
              "Av. Belgrano 700, Rosario",
              -32.9468,
              -60.6393,
              "+54 341 440-2000",
              "reservas@monumento-demo.example",
              5,
              List.of(
                  Amenity.WIFI,
                  Amenity.SPA,
                  Amenity.PILETA,
                  Amenity.GIMNASIO,
                  Amenity.BAR,
                  Amenity.RESTAURANTE,
                  Amenity.SERVICIO_HABITACIONES,
                  Amenity.TRANSPORTE_AEROPUERTO,
                  Amenity.AIRE_ACONDICIONADO),
              List.of(
                  new Hab(1, 101, 3),
                  new Hab(2, 201, 7),
                  new Hab(2, 202, 7),
                  new Hab(3, 301, 5),
                  new Hab(4, 401, 6),
                  new Hab(5, 501, 6))),
          new HotelDemo(
              "Hotel Sierras de Córdoba",
              "3071000003",
              "Av. Colón 400, Córdoba",
              -31.4201,
              -64.1888,
              "+54 351 423-3000",
              "reservas@sierras-demo.example",
              3,
              List.of(
                  Amenity.WIFI,
                  Amenity.ESTACIONAMIENTO,
                  Amenity.TV_CABLE,
                  Amenity.AIRE_ACONDICIONADO),
              List.of(
                  new Hab(1, 101, 1), new Hab(1, 102, 2), new Hab(2, 201, 3), new Hab(2, 202, 5))),
          new HotelDemo(
              "Hotel Obelisco Buenos Aires",
              "3071000004",
              "Av. Corrientes 1200, Buenos Aires",
              -34.6037,
              -58.3816,
              "+54 11 4382-4000",
              "reservas@obelisco-demo.example",
              4,
              List.of(
                  Amenity.WIFI,
                  Amenity.GIMNASIO,
                  Amenity.BAR,
                  Amenity.SALA_REUNIONES,
                  Amenity.LIMPIEZA_DIARIA,
                  Amenity.TV_CABLE,
                  Amenity.AIRE_ACONDICIONADO),
              List.of(
                  new Hab(1, 101, 1),
                  new Hab(2, 201, 3),
                  new Hab(2, 202, 3),
                  new Hab(3, 301, 7),
                  new Hab(4, 401, 5),
                  new Hab(5, 501, 6))),
          new HotelDemo(
              "Hotel Viñedos de Mendoza",
              "3071000005",
              "Av. San Martín 900, Mendoza",
              -32.8895,
              -68.8458,
              "+54 261 429-5000",
              "reservas@vinedos-demo.example",
              5,
              List.of(
                  Amenity.WIFI,
                  Amenity.PISCINA_DESCUBIERTA,
                  Amenity.SPA,
                  Amenity.SAUNA,
                  Amenity.RESTAURANTE,
                  Amenity.BAR,
                  Amenity.ESTACIONAMIENTO),
              List.of(
                  new Hab(1, 101, 3),
                  new Hab(1, 102, 7),
                  new Hab(2, 201, 7),
                  new Hab(2, 202, 5),
                  new Hab(3, 301, 6))),
          new HotelDemo(
              "Hostería Lago Nahuel Huapi",
              "3071000006",
              "Av. Bustillo km 2, San Carlos de Bariloche",
              -41.1335,
              -71.3103,
              "+54 294 442-6000",
              "reservas@nahuelhuapi-demo.example",
              2,
              List.of(Amenity.WIFI, Amenity.CALENTADOR, Amenity.ESTACIONAMIENTO),
              List.of(
                  new Hab(1, 101, 2), new Hab(1, 102, 3), new Hab(2, 201, 5), new Hab(2, 202, 6))));
}
