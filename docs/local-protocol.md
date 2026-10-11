# Protocolo local

## Autoridad y transporte

El daemon usa un socket Unix `coordinator.sock`, propiedad del usuario, con permisos `0600`; el directorio de estado tiene permisos `0700`. No expone TCP. Cada petición es una línea JSON UTF-8 de hasta 2 MiB:

```json
{"operation":"prepare","arguments":{"task_id":"UUID"}}
```

La respuesta contiene `ok` y `result`, o `ok: false` y un error sin contenido de archivos. Las operaciones no registradas se rechazan. El socket no aísla procesos ejecutados bajo el mismo usuario ni acredita intervención humana. La comunicación remota utiliza credenciales OAuth independientes; las decisiones humanas se registran en el servidor.

## Operaciones

| Operación | Argumentos | Precondición / resultado |
| --- | --- | --- |
| `create` | `repository`, `scope`, `plan`, `rules`, `tests`, `level` opcional | Solo Quick; adquiere bloqueo y registra `RECEIVED` |
| `prepare` | `task_id` | `RECEIVED`; registra baseline, prepara respaldos y termina en `PREPARED` |
| `apply` | `task_id`, `path`, `operation`, `content`, `destination`, `operation_id` | `PREPARED`/`APPLIED`; escribe dentro del alcance y registra `APPLIED` |
| `verify` | `task_id`, `timeout` opcional | Ejecuta argv originales, sin shell; `VERIFIED` o `VERIFY_FAILED` |
| `retry` | `task_id`, `finding` | Solo `VERIFY_FAILED`; consume presupuesto antes de permitir correcciones |
| `review` | `task_id`, `findings` | Solo `VERIFIED`; hallazgos bloquean; lista vacía registra `LOCAL_VERIFIED` |
| `restore` | `task_id` | Estado recuperable y snapshot coincidente; valida todos los respaldos antes de restaurar |
| `release` | `task_id` | `LOCAL_VERIFIED` → `LOCAL_DONE_PENDING_SYNC`; `RESTORED` → `CANCELLED` |
| `resume` | `task_id` | Recupera checkpoint y propiedad durable; nunca repite automáticamente escrituras inciertas |
| `context` | `repository` | Consulta plan original, reglas, alcance, estado y revisión de la tarea activa |
| `import_remote` | `repository`, `remote_task_id`, `tests`, `project_id` opcional | Importa el contexto remoto autorizado y los bloques registrados |
| `synchronize` | `task_id` | Reenvía eventos pendientes y reconcilia el estado remoto |
| `next_block` | `task_id`, `tests` | Selecciona el siguiente bloque después de verificar el actual |
| `run_role` | `task_id`, `role`, `consent_id`, `delegation_id` | Delegación secuencial; Los modelos con `requiresVerification` exigen consentimiento humano individual |
| `collect_backups` | Sin argumentos | Retira respaldos de tareas completadas y sincronizadas con al menos siete días |

`operation` de edición admite `create`, `modify` (predeterminado), `delete` y `rename`. El contenido es UTF-8 y tiene un límite de un MiB; los respaldos de archivos tienen un límite inicial de 16 MiB por archivo. Los directorios padre deben existir. Un renombrado se registra como creación del destino y eliminación del origen: **no es atómico como conjunto**, y una interrupción exige reconciliación.

Usar un `operation_id` estable para cada edición. Una repetición idéntica devuelve su recibo sin escribir; reutilizar el identificador con argumentos distintos se rechaza. Omitirlo impide identificar reenvíos de edición. Una operación en `APPLYING`/`UNCERTAIN` nunca se considera exitosa por un reenvío.

Ejemplo de edición:

```json
{"operation":"apply","arguments":{"task_id":"UUID","operation_id":"block-1-edit-1","path":"message.txt","content":"After\n","operation":"modify"}}
```

Ejemplo de verificación y revisión, en peticiones separadas:

```json
{"operation":"verify","arguments":{"task_id":"UUID"}}
```

```json
{"operation":"review","arguments":{"task_id":"UUID","findings":[]}}
```

La revisión semántica tiene origen `MODEL_REPORTED`; un resultado de proceso instrumentado tiene origen `LOCAL_CHECKED`. No existe certificación independiente de la corrección semántica. Un comando que termina con código cero solo acredita su resultado de ejecución.

## Durabilidad y límites

- SQLite WAL con `synchronous=FULL`, revisión optimista y secuencia durable de eventos. Archivos de respaldo y directorios se sincronizan antes de habilitar escrituras.
- Bloqueo `flock` más propietario durable. Tras caída, solo la misma tarea puede recuperar ese bloqueo; no se libera por TTL.
- El plan original permanece inmutable. Cada bloque conserva sus comandos de verificación. Para tareas remotas, la ejecución offline exige contexto autorizado firmado y un máximo de cuatro horas desde la última sincronización remota; una operación local no renueva ese plazo.
- Dos correcciones por identificador de hallazgo, tres por bloque y seis por tarea. El servidor limita también las rondas finales a dos. En tareas remotas, el identificador de hallazgo debe corresponder al UUID registrado en el servidor.
- Se detectan cambios fuera del alcance por snapshot del árbol, excluyendo `.git`. Los tests deben evitar crear o modificar archivos del repositorio. Los archivos especiales, hardlinks, ACL, flags y atributos no soportados bloquean la preparación. Se conserva `com.apple.provenance` en macOS.
- Los outputs de pruebas se procesan en un archivo temporal privado; solo se conserva hash, resultado y duración en la evidencia. El registro local de tarea sí conserva plan, reglas y argv: no introducir secretos en ellos.
- Al vencer el contexto o encontrar un conflicto, no se realizan nuevas escrituras administradas. Una operación puramente local no renueva la caducidad.
- Las operaciones ambiguas conservan `UNCERTAIN` y el propietario durable. La reconciliación de operaciones ambiguas y la cancelación administrativa requieren intervención explícita; la retención de respaldos se aplica mediante `collect_backups`. No borrar el directorio de estado para forzar una recuperación.

El coordinador observa y administra sus propias operaciones. Un editor externo puede escribir entre comprobación y reemplazo; los hashes y el renombrado atómico no proporcionan CAS universal. Las pruebas pueden crear procesos fuera de su grupo o alterar recursos externos; el coordinador no proporciona aislamiento universal para esas rutas.

## Plugin y hooks

El bridge empaquetado se invoca con `python3 <ruta-absoluta>/scripts/bridge.py request --base64 <sobre-codificado>`. El guard solo admite esa forma exacta sin composición de shell durante una tarea activa. El MCP stdio expone únicamente `get_context`, ligado al directorio de trabajo del proceso MCP; no acepta una ruta arbitraria ni operaciones de aprobación, modelos o certificación.

El guard de `PreToolUse` emite `permissionDecision: deny` para herramientas directas durante una tarea administrada, o ante coordinador inaccesible. Las pruebas de este repositorio verifican la salida del script; **no acreditan que el host ejecute o respete ese hook**. Un error o timeout del hook puede comportarse de manera diferente según el host. Hasta comprobarlo, usar exclusivamente el bridge en repositorios descartables.

`SessionStart` y `PostCompact` consultan el checkpoint durable. `PreCompact` no genera una transcripción ni un resumen nuevo: devuelve una indicación de recuperación del estado registrado. No se implementan todavía hooks de procesos prolongados, delegaciones o fin de sesión.
