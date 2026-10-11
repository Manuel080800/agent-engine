# Matriz de compatibilidad

Observación: 11 de octubre de 2026. Las pruebas locales y del servidor se distinguen de la validación pendiente en los hosts Codex.

## Entorno observado

| Componente | Versión / evidencia |
| --- | --- |
| Codex CLI | `0.162.1`; instalación aislada de un paquete anterior y consulta de opciones de ejecución |
| Codex Desktop | `26.1007.30912`, build `20156`; ejecución de hooks pendiente |
| macOS | `27.0.1`, build `26A434` |
| Python | `3.14.7` en macOS; runtime anterior probado con `3.12.15` en Linux descartable |
| Java | Temurin `21.0.11`; reactor core JAR y dos WAR compilados |
| WildFly | `41.0.1.Final-jdk21`; despliegue real de ambos WAR mediante Docker |
| PostgreSQL | `17.6`; migraciones y pruebas HTTP en base descartable |
| Node | `22.22.2`; compilación Vite y seis pruebas del frontend |

## Capacidades y alcance de la evidencia

| Capacidad | Resultado comprobado | Validación pendiente |
| --- | --- | --- |
| Paquete autocontenido y MCP stdio | Handshake desde un directorio temporal ajeno al checkout | Instalación y confianza de la versión actual en Desktop y CLI |
| Socket privado, exclusión y checkpoints | Pruebas locales y recuperación tras reinicio | Invocación completa desde ambos hosts |
| Respaldos y edición administrada | Operaciones, metadatos admitidos y conflictos probados | Recorrido integral del host; no existe CAS universal contra editores externos |
| Hooks | JSON de bloqueo y recuperación comprobado por tests | Eventos reales, compactación, confianza y comportamiento ante timeout |
| Delegaciones secuenciales | Contrato implementado; opciones de Codex CLI inspeccionadas | Ejecución con modelos reales y serialización en ambos hosts |
| Verificación humana de modelos | Emisión y consumo atómico implementados en el servidor | Recorrido completo con intervención humana y delegación real |
| HTTPS, OAuth y PKCE | Flujo HTTP, rechazo de verifier inválido y rotación/reutilización de refresh tokens comprobados | Dos clientes Codex reales y persistencia Keychain |
| Aprobación Standard/Deep | Aprobación humana e invalidación por cambio de reglas comprobadas vía HTTP | Recorrido completo del plugin |
| Evidencia y cierre remoto | Gate instrumental y transición `COMPLETED` comprobados vía HTTP | Revisión semántica y cierre con agentes reales |
| Sincronización offline | Caché firmada, caducidad, cola y reenvío idempotente probados con cliente remoto simulado | Fallos de red reales entre coordinador y servidor |
| Memoria | Propuestas y decisión humana implementadas | Validación integral del flujo de aprobación y uso posterior |
| Persistencia JPA y panel | CRUD, auditoría, aislamiento, sesiones y recursos comprobados vía HTTP; formularios, aprobación y diseño de escritorio/móvil revisados en navegador | Escenarios completos de aceptación del panel |

## Evidencia reproducible

```sh
python3 scripts/validate.py
```

Esta validación ejecuta 48 pruebas del coordinador, la demo local de recuperación y el handshake del paquete. Las veintitrés pruebas Java se ejecutan con el reactor Maven; el frontend dispone de `npm test` y `npm run format:check`.

`scripts/integration_http.py` requiere una URL explícita en `AGENT_ENGINE_TEST_URL` y un archivo de token en `AGENT_ENGINE_TEST_BOOTSTRAP_TOKEN_FILE`; `AGENT_ENGINE_TEST_CA_FILE` permite proporcionar una CA privada. La suite crea cuentas y recursos, por lo que requiere una base de datos de pruebas nueva y descartable. No guarda credenciales en el repositorio. `scripts/verify_domain_migration.py` utiliza `psql` con `PGHOST` y `PGDATABASE` explícitos, y las demás opciones de conexión estándar de PostgreSQL; termina con `ROLLBACK`. Sus resultados no certifican la ejecución de hooks ni agentes dentro de Codex. No se ha modificado la confianza de hooks ni el perfil personal del usuario.
