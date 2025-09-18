Spotless (formato de código)

Objetivo
- Mantener un estilo consistente de Java en todos los módulos sin discusiones en PR.

Qué hace
- Verifica el formato de los `.java` con Google Java Format.
- Quita imports no usados.

Comandos
- Verificar en CI (con ratchet a origin/main): `mvn -ntp -Pci spotless:check`
- Verificar local (sin ratchet): `mvn -ntp spotless:check`
- Arreglar automáticamente (local): `mvn -ntp spotless:apply`

Integración en IDE
- Recomendado ejecutar `spotless:apply` antes de commitear.
- También podés configurar el formateador del IDE a Google Java Format para ver el mismo resultado mientras editás.

Dónde está configurado
- Plugin en `pom.xml` (padre): `com.diffplug.spotless:spotless-maven-plugin`.
- Perfil `ci` agrega ratchet desde `origin/main` para revisión incremental en CI.
- Se aplica a todos los módulos Java del proyecto.

Fallos comunes y solución
- "No such reference 'origin/main'" al aplicar/chequear localmente:
  - Causa: el ratchet del perfil CI busca esa referencia; no existe localmente.
  - Solución: corré sin el perfil `ci` (ver comandos arriba) o hacé `git fetch origin main`.
- "Spotless Check failed": ejecutá `mvn spotless:apply` y commiteá los cambios.
