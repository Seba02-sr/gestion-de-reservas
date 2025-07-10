// package edu.utn.frsf.isi.dan.user.mapper;

// import org.mapstruct.Mapper;
// import org.mapstruct.factory.Mappers;

// import edu.utn.frsf.isi.dan.user.model.Huesped;
// import edu.utn.frsf.isi.dan.user.model.TarjetaCredito;

// @Mapper
// public class HuespedMapper {
//     HuespedMapper INSTANCE = Mappers.getMapper(HuespedMapper.class);

//     @Mapping(target = "tarjetaCredito", source = "record")
//     Huesped toHuesped(HuespedRecord record);

//     default List<TarjetaCredito> mapTarjetas(HuespedRecord record) {
//         if(record.numeroCC() == null) {
//             return List.of();
//         }

//         TarjetaCredito tarjeta = TarjetaCredito.builder()
//             .numero(record.numeroCC())
//             .nombreTitular(record.nombreTitular())
//             .fechaVencimiento(record.fechaVencimientoCC())
//             .cvc(record.cvcCC())
//             .esPrincipal(record.esPrincipalCC())
//             .banco(Banco.builder().id(record.idBanco()).build())
//             .build();

//         return List.of(tarjeta);
//     }
// }
