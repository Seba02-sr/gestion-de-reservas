# reservas-svc — decisiones de diseño

Servicio de reservas sobre MongoDB. Se accede por el API Gateway (`http://localhost:8080/reservas/...`) y su
Swagger está en el Swagger unificado (`http://localhost:8080/swagger-ui.html`, elegir `reservas-svc`); el servicio
ya no publica el puerto 8082 en el host. Los paths de esta sección son los del servicio: por el gateway llevan
el prefijo `/reservas` (p. ej. `PATCH /reservas/reservas/{id}/estado`). La
colección `habitacion` es una **réplica** que se actualiza solo con los mensajes de RabbitMQ de
gestion-svc (la fuente de verdad); por HTTP las habitaciones son de solo lectura.

### Regla de confirmación de pagos (el enunciado se contradice)
- Slide 12: la reserva queda CONFIRMADA "cuando se paga **al menos la mitad**".
- Slide 14: "cuando la reserva tiene **al menos un pago** pasa a estado CONFIRMADA".
- **Se implementó la slide 14** (≥ 1 pago). La regla está aislada en un único método,
  `ReservaPolicy.pagoAlcanzaParaConfirmar`; pasar a "la mitad" es cambiar esa línea
  (`totalPagado ≥ precioTotal / 2`) y su test.

### Estados
`RESERVADA` (al crear) → `CONFIRMADA` (primer pago) → `FINALIZADA` / `ADEUDADA`; `CANCELADA` (solo
sin pagos); `BLOQUEADA` (el dueño bloquea fechas con `POST /reservas/bloqueos`); `CERRADA` (llega
por el evento `CERRAR_HOTEL`). El enunciado también menciona REALIZADA y EFECTUADA (slide 12): no se
implementaron, se usan los estados de la slide 14.
- `PATCH /reservas/{id}/estado` acepta solo `CANCELADA` (desde RESERVADA/BLOQUEADA sin pagos) y
  `FINALIZADA` (desde RESERVADA/CONFIRMADA/ADEUDADA). `FINALIZADA` exige el review del dueño
  (`PUT /review-dueno`) **y** el pago completo; si falta algo la reserva queda `ADEUDADA` (se puede
  reintentar). Desde RESERVADA no hay pagos, así que queda ADEUDADA.
- Todo cambio actualiza la `Reserva` y la copia embebida (`ReservaSimple`) en la habitación.
- `PUT /review-cliente` solo después del check-out (el día del check-out ya vale).

### Disponibilidad
Una habitación está **ocupada** para `[checkIn, checkOut)` si tiene una reserva **no CANCELADA** con
`reserva.checkIn < checkOut pedido` **y** (`reserva.checkOut` es null **o** `reserva.checkOut >
checkIn pedido`). Dos estadías pueden encadenarse el mismo día (sale uno, entra otro) y el `null`
cubre las reservas abiertas (CERRADA). La fórmula de la slide 15 está mal redactada (compara la
entrada con la entrada y usa extremos cerrados); el razonamiento está comentado en
`Disponibilidad.java`, donde conviven la versión Java y la de Mongo (un test las compara).

### Reserva concurrente: operación atómica, no transacción
Dos pedidos simultáneos no pueden quedarse con las mismas fechas. Se resuelve con **un `findAndModify`
sobre el documento de la habitación** cuyo filtro incluye "no hay reservas que se superpongan" y que
hace el `$push` de la reserva en la misma operación: Mongo garantiza atomicidad por documento, así
que solo un pedido modifica el documento. Se eligió sobre una transacción porque el Mongo del
compose es standalone (las transacciones piden replica set) y porque, al estar las reservas
embebidas en la habitación, alcanza con un documento. Orden: primero se toma el cupo y después se
guarda la `Reserva`; si el guardado falla, se libera el cupo. Un corte en el medio deja a lo sumo
fechas bloqueadas de más (seguro), nunca una doble reserva. Hay un test con 10 hilos contra un Mongo
real. Los pagos y el resto de los cambios de estado son lectura-modificación-escritura de una
`Reserva` (no compiten por fechas).

### Consistencia eventual
reservas-svc y gestion-svc tienen bases distintas y se enteran por mensajes: entre el cambio en
gestion-svc y su réplica puede pasar un instante (p. ej. una habitación recién creada tarda en
aparecer). `CREAR` y `CERRAR_HOTEL` son idempotentes (índice único + `upsert`, y condición atómica
"no tiene ya una reserva CERRADA"), así que un mensaje repetido no duplica nada. Las reservas ya
existentes de un hotel que cierra no se cancelan automáticamente.

### Fechas
Los check-in/check-out son días (`yyyy-MM-dd`) y se guardan como la medianoche de ese día en
`America/Argentina/Buenos_Aires` (`app.zone`). "Hoy" sale de un `Clock`; para probar a mano se puede
simular la fecha con `APP_FECHA_ACTUAL` (ver CLAUDE.md).

### Llamadas a otros servicios (Etapa 4)
`POST /reservas` valida contra las fuentes de verdad por OpenFeign antes de tomar el cupo: el huésped en user-svc (404 si no existe; se guardan su nombre y email) y la disponibilidad de la habitación en gestion-svc (409 si no está disponible). Si alguno no responde, tarda más que el timeout o su circuit breaker (Resilience4j) está abierto, la respuesta es **503** con un mensaje claro. Detalle y trade-offs en `CLAUDE.md`.

### Geo
`hotel.ubicacion` es un punto GeoJSON `[longitud, latitud]` (x = longitud, y = latitud) con índice
`2dsphere` creado al arrancar. La búsqueda por cercanía usa `$geoWithin` + `$centerSphere`
(combinable con los demás filtros).
