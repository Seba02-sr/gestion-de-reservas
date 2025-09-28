Revisión de dependencias (Dependency Review)

Objetivo
- Avisar en los PR cuando se agregan o actualizan dependencias con vulnerabilidades o licencias problemáticas.

Cómo funciona
- Job `Dependency Review (PRs)` del workflow CI se ejecuta en cada `pull_request`.
- Usa `actions/dependency-review-action@v4` y analiza cambios en `pom.xml` (y `package.json` si aplica).

Comportamiento
- Por defecto, no bloquea el merge; deja comentarios/alertas en el PR.
- Se puede endurecer para fallar por severidades altas (no habilitado por ahora).

Buenas prácticas
- Evitá subir dependencias nuevas sin necesidad.
- Si aparece una advertencia, buscá una versión parcheada o alternativa.
