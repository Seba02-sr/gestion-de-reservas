Spotless (formato de código)

Objetivo
- Mantener un estilo consistente de Java en todos los módulos sin discusiones en PR.

Qué hace
- Verifica el formato de los `.java` con Google Java Format.
- Quita imports no usados.

Comandos
- Verificar (CI/Local): `mvn -ntp spotless:check`
- Arreglar automáticamente: `mvn -ntp spotless:apply`

Integración en IDE
- Recomendado ejecutar `spotless:apply` antes de commitear.
- También podés configurar el formateador del IDE a Google Java Format para ver el mismo resultado mientras editás.

Dónde está configurado
- Plugin en `pom.xml` (padre): `com.diffplug.spotless:spotless-maven-plugin`.
- Se aplica a todos los módulos Java del proyecto.

Fallos comunes y solución
- "Spotless Check failed": ejecutá `mvn spotless:apply` y commiteá los cambios.
