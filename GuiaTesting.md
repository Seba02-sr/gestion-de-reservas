Aquí tenés un **informe breve y organizado** para guiar la generación de tests unitarios de tu aplicación Spring Boot, separando claramente **repositorios**, **servicios** y **controladores**, usando JUnit 5, Mockito y H2 cuando corresponda.

---

## 📂 Estructura de carpetas de tests

```text
src/
└── test/
    └── java/
        └── com/tuempresa/campito/
            ├── dao/
            │   └── TransaccionRepositoryTest.java
            ├── service/
            │   └── TransaccionServiceTest.java
            └── controller/
                └── TransaccionControllerTest.java
```

Cada paquete de test refleja exactamente el paquete de producción.

---

## 1. Tests de Repositorios (`dao`)

### Anotaciones y configuración

* **@DataJpaTest**

  * Levanta contexto mínimo de JPA con **H2** embebido.
  * Configura repositorios y `TestEntityManager`.
* **Perfil `test`**: uso de `application-test.properties` si necesitás overrides.

### ¿Qué contiene?

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE) // opcional si querés H2 en memoria
class TransaccionRepositoryTest {

  @Autowired
  private TransaccionRepository repo;

  @Autowired
  private TestEntityManager em;

  @BeforeEach
  void setup() {
    // opcional: poblar datos base en H2
  }

  @Test
  void testGuardarYBuscarPorId() {
    // Arrange: crear y persistir entidad
    Transaccion tx = new Transaccion(...);
    Transaccion saved = repo.save(tx);

    // Act
    Optional<Transaccion> fetched = repo.findById(saved.getId());

    // Assert
    assertTrue(fetched.isPresent());
    assertEquals(tx.getMonto(), fetched.get().getMonto());
  }

  @Test
  void testConsultaDashboard() {
    // Arrange: crear varias transacciones con distintas fechas/tipos
    // Act: invocar repo.findIngresosVsGastos(...)
    // Assert: validar resultados agregados
  }
}
```

* **TestEntityManager** permite persistir entidades sin usar el repo para pruebas de datos complejos.
* Nombrá los métodos `testXxxYyy`.

---

## 2. Tests de Servicios (`service`)

### Anotaciones y configuración

* **@ExtendWith(MockitoExtension.class)**

  * Habilita mocks de Mockito.
* **@Mock** para repositorios.
* **@InjectMocks** para la implementación del servicio.

### ¿Qué contiene?

```java
@ExtendWith(MockitoExtension.class)
class TransaccionServiceTest {

  @Mock
  private DashboardRepository dashboardRepo;

  @InjectMocks
  private TransaccionServiceImpl service;

  @BeforeEach
  void setup() {
    // opcional: inicializar datos comunes
  }

  @Test
  void obtenerDashboardInfo_delegaCorrectamente() {
    // Arrange
    Long espacioId = 1L;
    when(dashboardRepo.findIngresosVsGastos(espacioId))
      .thenReturn(List.of(new IngresoGastoMesDTO("2025-01", BigDecimal.TEN, BigDecimal.ONE)));
    when(dashboardRepo.findDistribucionGastos(espacioId))
      .thenReturn(List.of(new DistribucionGastoDTO("Alimentos", BigDecimal.valueOf(100))));
    when(dashboardRepo.findSaldosAcumulados(espacioId))
      .thenReturn(List.of(new SaldoAcumuladoMesDTO("2025-01", BigDecimal.valueOf(9))));

    // Act
    DashboardInfoDTO dto = service.obtenerDashboardInfo(espacioId);

    // Assert
    assertNotNull(dto);
    assertEquals(1, dto.ingresosVsGastos().size());
    verify(dashboardRepo).findIngresosVsGastos(espacioId);
  }
}
```

* **Arrange–Act–Assert**: separación clara de fases.
* Verificá interacciones con `verify(...)` cuando importe asegurar llamadas al repo.

---

## 3. Tests de Controladores (`controller`)

### Anotaciones y configuración

* **@WebMvcTest(TransaccionController.class)**

  * Levanta contexto de Spring MVC, sin cargar todo el backend.
* **@MockBean** para el servicio.
* **MockMvc** para simular peticiones HTTP.

### ¿Qué contiene?

```java
@WebMvcTest(TransaccionController.class)
class TransaccionControllerTest {

  @Autowired
  private MockMvc mvc;

  @MockBean
  private TransaccionService transaccionService;

  @Test
  void getDashboardInfo_ok() throws Exception {
    // Arrange
    DashboardInfoDTO dto = new DashboardInfoDTO(
      List.of(new IngresoGastoMesDTO("2025-01", BigDecimal.TEN, BigDecimal.ONE)),
      List.of(new DistribucionGastoDTO("Alimentos", BigDecimal.valueOf(100))),
      List.of(new SaldoAcumuladoMesDTO("2025-01", BigDecimal.valueOf(9)))
    );
    when(transaccionService.obtenerDashboardInfo(1L)).thenReturn(dto);

    // Act & Assert
    mvc.perform(get("/api/transacciones/dashboardInfo")
          .param("espacioTrabajoId", "1")
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.ingresosVsGastos[0].mes").value("2025-01"))
      .andExpect(jsonPath("$.distribucionGastos[0].motivo").value("Alimentos"));
  }
}
```

* Usá **`jsonPath`** para validar el JSON de respuesta.
* Probá casos de error: parámetro faltante o servicio lanza excepción → status 4xx/5xx.

---

## 📌 Buenas prácticas generales

* **Nombres claros**: `<Clase>Test` y métodos `debenDescribirLoQuePrueban()`.
* **Cobertura**: apuntá a > 80% en lógica crítica.
* **Aislamiento**: cada test configura solo lo que necesita (mockeo/repositorio inmembrado).
* **Datos de prueba mínimos**: no poblar de más; solo lo necesario para el caso.
* **Uso de perfiles**: `application-test.properties` para tests de integración (JPA).
* **Tests independientes**: cada test debe poder correr solo y en cualquier orden.

---

Con esta guía tendrás la **plantilla y estructura** necesaria para que otra IA genere tus tests de forma limpia, profesional y alineada a las mejores prácticas de ingeniería de software.
