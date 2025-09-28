Spotless (formato de código)

Objetivo
- Mantener un estilo consistente de Java en todos los módulos sin discusiones en PR.

Qué hace
- Verifica el formato de los `.java` con Google Java Format.
- Quita imports no usados.

Comandos
- Verificar en CI (con ratchet a la rama por defecto): `mvn -ntp -Pci -Dspotless.ratchetFrom=origin/<branch>`
- Verificar local (sin ratchet): `mvn -ntp spotless:check`
- Arreglar automáticamente (local): `mvn -ntp spotless:apply`

Integración en IDE
- Recomendado ejecutar `spotless:apply` antes de commitear.
- También podés configurar el formateador del IDE a Google Java Format para ver el mismo resultado mientras editás.

Dónde está configurado
- Plugin en `pom.xml` (padre): `com.diffplug.spotless:spotless-maven-plugin`.
- Perfil `ci` usa una propiedad `spotless.ratchetFrom` para revisión incremental en CI.
- En GitHub Actions se pasa `-Dspotless.ratchetFrom=origin/${{ github.event.repository.default_branch }}`.
- Se aplica a todos los módulos Java del proyecto.

Fallos comunes y solución
- "No such reference 'origin/<branch>'" al aplicar/chequear localmente:
  - Causa: el ratchet del perfil CI busca esa referencia; no existe localmente.
  - Solución: corré sin el perfil `ci` (ver comandos arriba) o hacé `git fetch origin <branch>` y asegurate de usar el nombre correcto de rama por defecto (main/master/etc.).
- "Spotless Check failed": ejecutá `mvn spotless:apply` y commiteá los cambios.
