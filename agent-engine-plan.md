# Agent Engine — Plan técnico de implementación

> **Versión del plan:** 1.0  
> **Producto:** Agent Engine  
> **Fecha:** 11 de octubre de 2026  
> **Estado:** plan técnico para implementación; Fase 0 de compatibilidad obligatoria.  
> **Descripción del repositorio:** Intelligent coding workflow, sequential agent orchestration, and persistent memory for Codex.

## 1. Resumen ejecutivo

Agent Engine será un complemento privado para **Codex Desktop y Codex CLI** destinado a mejorar la consistencia de las tareas de programación. Antes de modificar código, recuperará las reglas del programador, investigará los componentes relevantes, conservará la referencia al plan original y presentará un resumen de los cambios previstos. Durante la implementación coordinará operaciones secuenciales, mantendrá el contexto de cada bloque y verificará los resultados. Al finalizar realizará una **revisión integral obligatoria** y propondrá al programador los conocimientos que convenga conservar en memoria permanente.

La solución tendrá tres ámbitos de responsabilidad:

| Ámbito | Función principal | Límite |
| --- | --- | --- |
| Plugin de Codex | Planificar, delegar, ejecutar, verificar y recuperar contexto | No genera commits ni ejecuta operaciones destructivas ajenas al alcance |
| Coordinador local | Bloqueos por repositorio, caché, respaldos, journal y continuidad operativa | Solo administra operaciones locales asociadas a Agent Engine |
| Servidor y panel | Autenticación, reglas, skills, modelos, memorias, planes, checkpoints e historial | No edita ni almacena el repositorio local |

**Principios obligatorios:** dos contenedores Docker; backend inicial en la propia máquina macOS; ejecución estrictamente secuencial; prioridad de instrucciones del programador dentro de la jerarquía de seguridad de Codex; sin commits automáticos; sin código ni diffs completos en PostgreSQL; protección de modificaciones preexistentes; uso de GPT-6 Luna como principal y GPT-6.1 Sol solo tras autorización individual; revisión integral obligatoria al finalizar y propuestas de memoria permanente sujetas a aprobación.

**Límite de garantía:** Agent Engine controla las operaciones que pasan por sus mecanismos cooperantes. No puede impedir universalmente las escrituras realizadas por editores externos, comandos ya iniciados o rutas de herramientas que no invoquen los hooks. Por tanto, distinguirá explícitamente entre **bloqueo preventivo**, **detección posterior** y **restauración condicional**; nunca declarará como impedido un cambio que solo ha sido observado después de producirse.

## 2. Objetivos y alcance

### 2.1. Objetivos

1. Recuperar automáticamente reglas globales y específicas de proyecto antes de ejecutar cambios.
2. Inspeccionar el código existente y priorizar su reutilización.
3. Aplicar planes de trabajo verificables y conservar el plan original durante todas las etapas.
4. Coordinar skills y subagentes de forma secuencial, sin delegación simultánea.
5. Reducir incumplimientos de convenciones y correcciones manuales mediante supervisión por bloque.
6. Completar cada tarea con una revisión integral que cubra todos los requisitos y efectos de integración.
7. Mantener memoria, checkpoints y continuidad después de compactaciones, interrupciones y desconexiones temporales.
8. Permitir administrar reglas, skills, modelos y memoria tanto desde Codex como desde un panel sencillo.
9. Evaluar mejoras reales en tareas correctas, cumplimiento, repeticiones, tokens y tiempo.

### 2.2. Excluido de la versión 1.0

- Commits, publicación de ramas, pull requests o despliegues automáticos.
- Ejecución remota de código desde el backend o la aplicación web.
- Paralelismo o delegación anidada entre subagentes.
- Copias completas de repositorios, archivos fuente, diffs o respaldos en PostgreSQL.
- Herramientas de CI/CD, gestor de proyectos avanzado o motor independiente de inferencia.
- Bases vectoriales, embeddings obligatorios y grafos de conocimiento; `pgvector` se reserva para una evolución posterior.
- Compartición de memoria entre usuarios sin autorización explícita.
- Garantías de interceptación universal por hooks sin evidencia en las versiones de Codex utilizadas.

## 3. Arquitectura y tecnologías

### 3.1. Infraestructura

| Componente | Tecnología y responsabilidad |
| --- | --- |
| `agent-engine-server` | Java 21, Spring Boot, Spring Security, Authorization Server, API REST, MCP Streamable HTTP y panel Vue 3 + Bootstrap compilado |
| `agent-engine-postgresql` | PostgreSQL, Full Text Search, persistencia y migraciones Flyway |
| Plugin privado Codex | Manifest portable, skills, configuración MCP, hooks y roles de subagentes |
| `agent-engine-local` | Python 3.12, coordinador local ligero, IPC mediante Unix domain socket, SQLite local para registro operativo y respaldos en disco |

El coordinador local **no es un contenedor**. Se instalará junto con el plugin y podrá iniciarse bajo demanda. Usará un directorio privado por usuario para archivos temporales, estado, respaldos y eventos pendientes de sincronización. Las credenciales se protegerán mediante macOS Keychain y no se copiarán tokens en logs. **No leerá, copiará ni supondrá acceso a las credenciales privadas administradas internamente por Codex.**

Los contenedores se administrarán con Docker Compose. PostgreSQL estará disponible únicamente en la red interna de Docker. El servidor expondrá la API y el panel en una interfaz local (`127.0.0.1` o equivalente), con HTTPS y un certificado de confianza local cuando sea necesario para OAuth. No se añadirá un proxy contenedorizado obligatorio.

### 3.2. Módulos del servidor

`auth`, `user`, `project`, `profile`, `rule`, `skill`, `model`, `memory`, `task`, `checkpoint`, `execution`, `mcp` y `audit`.

Se implementará un monolito modular. Los servicios REST y MCP reutilizarán la misma lógica de negocio y autorización. El backend no tendrá acceso al disco de los proyectos asistidos.

### 3.3. Estructura propuesta del repositorio

```text
agent-engine/
├── server/
│   ├── src/main/java/.../{auth,user,project,profile,rule,skill,model,memory,task,checkpoint,execution,mcp,audit}/
│   ├── src/main/resources/db/migration/
│   └── src/test/
├── dashboard/
│   └── src/{views,components,services}/
├── plugin/
│   ├── plugin.json
│   ├── mcp.json
│   ├── skills/{analyze-task,research-codebase,design-solution,create-plan,execute-plan,verify-changes,manage-memory,restore-context}/
│   ├── hooks/{hooks.json,scripts/}
│   └── agents/
├── local/
│   ├── agent_engine_local/{coordinator,context,lock,backup,cache,journal,sync,policy}/
│   └── tests/
├── infra/
│   ├── compose.yaml
│   └── Dockerfile
├── docs/
│   ├── compatibility-matrix.md
│   ├── architecture.md
│   ├── security.md
│   └── evaluation.md
└── README.md
```

El formato definitivo del manifest y el mecanismo de carga de hooks se fijarán a partir de la versión real de Codex durante la Fase 0. Los scripts locales se empaquetarán sin exigir Docker para su ejecución.

## 4. Integración con Codex Desktop y CLI

### 4.1. Empaquetado

Se utilizará un plugin privado instalable desde un origen local o marketplace privado compatible. Contendrá un manifest `plugin.json`, las skills, un archivo `mcp.json` y la configuración de hooks que soporte la versión de Codex. La instalación y habilitación de hooks exigirá la confianza explícita del usuario.

### 4.2. Herramientas y eventos

| Evento previsto | Responsabilidad |
| --- | --- |
| `SessionStart` | Asociar identidad, proyecto, contexto, versiones y checkpoint |
| `UserPromptSubmit` | Identificar tarea, recuperar reglas y clasificar alcance |
| `PreToolUse` | Aplicar precondiciones a operaciones efectivamente interceptables |
| `PostToolUse` | Registrar operaciones cubiertas y comprobar cambios relevantes |
| Continuación de procesos (`write_stdin`) | No constituye un nuevo `PreToolUse`; controlar por registro del proceso y verificación al terminar |
| `PreCompact` | Consolidar checkpoint incremental antes de compactar |
| `PostCompact` y/o `SessionStart` con origen de compactación | Restaurar contexto conforme al soporte real del cliente |
| `SubagentStart` / `SubagentStop` | Registrar delegaciones y verificar serialización, cuando estén disponibles |
| `Stop`, `Interrupt`, `SessionEnd` | Finalizar, suspender, registrar estado y evitar pérdida de continuidad |

Los hooks son mecanismos de integración, **no una garantía universal de bloqueo de escrituras**. Su cobertura, decisiones permitidas y diferencias entre Desktop y CLI deberán medirse en la Fase 0. La verificación de cambios al final de cada bloque seguirá siendo obligatoria.

### 4.2.1. Comandos prolongados, sesiones interactivas y `write_stdin`

La autorización `PreToolUse` de un comando se evalúa al **iniciarlo**. Si el comando continúa abierto, una entrada posterior mediante `write_stdin` no provoca una nueva evaluación `PreToolUse`; su `PostToolUse` puede recibirse solo al finalizar el proceso original. La ejecución de un comando inicialmente permitido no autoriza automáticamente todos sus efectos posteriores.

**Política de la versión 1.0:**

1. Para operaciones de edición, preferir comandos **no interactivos, acotados y con finalización observable**. Las herramientas de escritura deberán quedar asociadas a tarea, bloque, alcance y respaldo antes de su uso.
2. Registrar, cuando resulte observable, `processId`, grupo de proceso, comando, directorio de trabajo, `taskId`, `blockId`, hora de inicio, límites de duración y estado. El identificador de un proceso no sustituye a la comprobación de los archivos que efectivamente cambió.
3. Prohibir como mecanismo administrado de edición las sesiones de shell interactivo con entrada posterior libre o procesos que puedan continuar modificando el repositorio sin puntos de control. Cuando no puedan restringirse, pasar a `BLOCKED` para operaciones de escritura dependientes de esa protección.
4. Permitir pruebas o compilaciones prolongadas solamente si sus efectos y rutas se conocen y son recuperables; el supervisor realizará una comprobación del estado real del repositorio tras su terminación. Si no puede comprobar la finalización o el proceso sigue escribiendo, no liberar el bloqueo ni aprobar el bloque.
5. No asumir que un hook observará entradas tardías o herramientas especializadas fuera de su cobertura. Marcar esos casos como `PARTIAL` o `UNSUPPORTED` en la matriz de compatibilidad y no utilizarlos para operaciones críticas que exijan prevención fiable.

Los comandos iniciados directamente por otras herramientas o usuarios permanecen fuera del control preventivo del coordinador. Los cambios detectados por inspección posterior podrán motivar una suspensión, pero no se describirán como escrituras impedidas.

### 4.3. Instalación local y actualización

- Instalar el plugin y sus dependencias locales de forma reproducible.
- Verificar que el servidor MCP está disponible y autenticado.
- Registrar y confiar los hooks que Codex efectivamente reconozca.
- Sincronizar reglas, skills y políticas al iniciar sesión o tarea.
- Aplicar los cambios de skills en la siguiente sesión cuando la versión del cliente no permita recarga inmediata.
- Informar explícitamente de mecanismos no soportados; no simular que están activos.

## 5. Identidad, autenticación y privacidad

### 5.1. Usuarios

El registro estará restringido a invitaciones emitidas por el administrador. La instalación inicial habilitará un bootstrap de administrador con token de un solo uso y caducidad. La API verificará propietario y proyecto en cada operación.

### 5.2. Autenticación

El panel utilizará sesiones web seguras. El endpoint MCP expondrá OAuth para clientes públicos mediante Authorization Code + PKCE, descubrimiento y metadatos exigidos por la especificación MCP aplicable. Se evaluará soporte real de Client ID Metadata Documents (CIMD) y Dynamic Client Registration (DCR). Los tokens tendrán audiencia, ámbitos, vencimiento, revocación y protección apropiados.

El despliegue inicial se limitará a macOS local. El procedimiento de certificado HTTPS confiable y el flujo de redirección hacia el navegador serán comprobados en Desktop y CLI antes de integrar todo el sistema.

### 5.2.1. Identidad independiente del coordinador Python

Codex se autenticará ante el servidor MCP mediante su propio cliente OAuth. **El coordinador local será otro cliente OAuth público**, registrado para Agent Engine con Authorization Code + PKCE, redirección de loopback validada y vinculación inicial mediante el navegador. No utilizará los tokens internos de la sesión de Codex.

1. El desarrollador iniciará la vinculación del coordinador local desde el instalador o comando de configuración de Agent Engine. El navegador abrirá el flujo de autorización del mismo servidor local.
2. El backend emitirá credenciales de sesión **para el cliente local** con ámbito mínimo, vinculadas a usuario y dispositivo lógico; no se concederán permisos administrativos generales por defecto.
3. Los tokens se almacenarán en macOS Keychain; el proceso Python accederá únicamente a sus propias credenciales. Se rotarán los tokens de actualización y se aceptará revocación.
4. Las solicitudes de registro de eventos, checkpoints y pruebas del coordinador usarán rutas y scopes dedicados. Cada operación verificará propietario, proyecto, `taskId`, estado y versiones; un `userId` aportado como argumento no bastará para autenticar la solicitud.
5. El socket Unix tendrá permisos restringidos al usuario local, comprobación de propietario y protocolo de peticiones tipado. Esto **no representa aislamiento absoluto contra procesos maliciosos ejecutados bajo el mismo usuario de macOS**; las confirmaciones humanas y validaciones del servidor seguirán siendo necesarias para operaciones sensibles.
6. Si caduca la sesión del coordinador y no puede renovarse, se continuará únicamente en el modo offline previamente autorizado. La reconexión deberá renovar o volver a vincular el cliente local.

Durante la Fase 0 se probará por separado la autenticación MCP de Codex y la vinculación OAuth del coordinador. Una credencial válida para una vía no implicará acceso automático a la otra.

### 5.3. Datos que pueden persistirse remotamente

**Permitidos:** reglas, skills propias, planes, resúmenes, decisiones, modelos, referencias a rutas relativas, hashes, versiones, metadatos de checkpoints y registros resumidos de ejecución.

**No permitidos por captura automática:** código fuente completo, diffs completos, contenido de archivos, respaldos locales, tokens, contraseñas, secretos y transcripciones completas.

El sistema aplicará sanitización y listas de campos permitidos en eventos, errores y memorias propuestas. La información derivada del repositorio no se considerará una instrucción confiable por sí misma.

## 6. Reglas, perfiles y skills

### 6.1. Prioridad de instrucciones

| Orden | Procedencia |
| --- | --- |
| 1 | Solicitud explícita vigente del programador |
| 2 | Reglas y skills del programador para el proyecto |
| 3 | Preferencias globales del programador |
| 4 | Instrucciones aplicables del repositorio, incluido `AGENTS.md` |
| 5 | Skills generales de Agent Engine |
| 6 | Convenciones predeterminadas |

La jerarquía anterior opera **dentro de los límites de autoridad y seguridad impuestos por Codex**. En caso de conflicto se evaluarán origen, ámbito, especificidad, prioridad y vigencia. Dos reglas incompatibles y equivalentes deberán bloquear la decisión afectada hasta su resolución.

### 6.2. Estado de las reglas

Las reglas tendrán identificador, ámbito, obligatoriedad, fuente, versión, vigencia y estado. Las instrucciones obligatorias globales deberán formar parte de todos los contextos de implementación. Las reglas de proyecto o módulo solo se cargarán cuando apliquen.

### 6.3. Skills

Skills iniciales: `analyze-task`, `research-codebase`, `design-solution`, `create-plan`, `execute-plan`, `verify-changes`, `manage-memory`, `restore-context`.

Las skills serán breves, tendrán criterios explícitos de activación y podrán referenciar archivos auxiliares bajo demanda. Las skills definidas por el programador prevalecerán sobre procedimientos genéricos en caso de conflicto del mismo nivel de autoridad. Se conservará el contenido y versión activos de cada tarea para que una sincronización no modifique inesperadamente su comportamiento a mitad de un bloque.

## 7. Model Registry & Router

### 7.1. Catálogo inicial

| Prioridad | Modelo previsto | Niveles permitidos | Política |
| --- | --- | --- | --- |
| Principal | GPT-6 Luna (`gpt-6-luna`) | `low`, `medium`, `high`, `xhigh` | Predeterminado para agente y subagentes |
| Secundario | GPT-6.1 Sol (`gpt-6.1-sol`) | `medium`, `high` | Requiere confirmación explícita por cada invocación |

Los identificadores y esfuerzos se validarán contra la versión y permisos reales del cliente; la tabla anterior define **políticas iniciales administrables**, no constantes del código.

### 7.2. Reglas de selección

1. Leer el modelo principal y secundarios habilitados, ordenados por prioridad.
2. Evaluar función del agente, nivel de tarea y razonamientos permitidos.
3. Confirmar disponibilidad efectiva de modelo y esfuerzo en Codex.
4. Si se selecciona Sol, pedir confirmación específica para **esa invocación**, conforme al contrato de consentimiento de la sección 7.3. Si no puede obtenerse, mantener Luna o suspender la delegación.
5. Configurar el agente mediante interfaces realmente soportadas; no asumir que cambiar PostgreSQL modifica una conversación ya iniciada.
6. Registrar modelo y esfuerzo efectivamente utilizados como metadatos de la ejecución, sin almacenar conversaciones completas.

Valores de referencia para Luna: coordinación simple `low`; exploración `medium`; diseño y planificación `high`; implementación `medium/high`; revisión crítica `high/xhigh` según necesidad. El panel permitirá editar modelos, prioridades, niveles permitidos y preferencias por rol.

### 7.3. Consentimiento individual para GPT-6.1 Sol

Se definirá **invocación** como **cada solicitud individual del coordinador para iniciar una nueva ejecución de agente o subagente con GPT-6.1 Sol**. No equivale a una tarea completa, a toda una sesión de Codex ni a cada petición interna de inferencia que Codex pueda realizar dentro del agente ya iniciado; Agent Engine no tiene visibilidad fiable sobre todas esas peticiones internas.

**Contrato obligatorio de consentimiento:**

- La solicitud mostrará modelo, esfuerzo (`medium` o `high`), rol, tarea, bloque, objetivo y motivo de selección de Sol.
- La autorización la concederá el programador mediante una acción explícita de la interfaz local o administrativa confiable. No podrá concederse a partir de un resumen escrito por el propio agente ni mediante una llamada MCP no autenticada como acción humana.
- El servidor generará un identificador de autorización ligado a `userId`, `taskId`, `blockId`, `delegationId`, `modelId`, `effort`, versión del plan y caducidad; vigencia inicial de **dos minutos** sin consumir.
- La autorización será **de un solo uso** y deberá consumirse de forma atómica, con validación del servidor o una concesión local previamente emitida y aún vigente, **antes de iniciar** la delegación. Las solicitudes duplicadas no podrán consumirla otra vez.
- Una delegación nueva, cambio de modelo/esfuerzo o reintento que requiera crear un agente Sol distinto necesitará otra aprobación. Una invocación fallida se considerará consumida si llegó a comenzar; no se reutilizará el permiso.
- No se autorizarán nuevas invocaciones Sol estando sin conexión, ni se concederán autorizaciones globales por tarea. Si Codex no permite observar o controlar el momento de creación de ese agente, **no se ofrecerá selección automática de Sol** en esa superficie.
- Las respuestas de aprobación guardarán solo metadatos mínimos, actor, fecha, consentimiento utilizado y estado, sin registrar razonamientos ni conversaciones completas.

La duración real de la ejecución de un agente autorizado podrá exceder los dos minutos de vigencia inicial; esa vigencia limita **el inicio**, no interrumpe una delegación ya autorizada. La política deberá impedir que el modelo se sustituya silenciosamente durante esa ejecución.

## 8. Agent Orchestrator y delegación secuencial

### 8.1. Responsabilidades

| Rol | Operación | Permisos recomendados |
| --- | --- | --- |
| Main Coordinator | Mantener objetivo, contexto, plan, aprobación y transiciones | Control de la tarea |
| Codebase Explorer | Investigar código y dependencias | Solo lectura |
| Solution Planner | Comparar alternativas y preparar plan | Solo lectura y generación de plan |
| Implementation Worker | Aplicar cambios del bloque | Escritura dentro del alcance autorizado |
| Quality Reviewer | Evaluar resultados y riesgos | Solo lectura |
| Memory Manager | Buscar información y preparar propuestas | MCP y operaciones deterministas |

El coordinador podrá asumir roles mediante skills sin crear otro proceso de IA. La delegación será estrictamente secuencial: **máximo un subagente activo por tarea**, sin delegaciones anidadas o paralelas. Antes de lanzar un siguiente agente, el coordinador deberá recibir, validar y registrar el resultado del anterior.

### 8.2. Contrato de delegación

Cada delegación incluirá `taskId`, `blockId`, `planId`, `planVersion`, `contextVersion`, `role`, `goal`, `scope`, `requiredRules`, `references`, `allowedTools`, `acceptanceCriteria`, `modelPolicy` y `approvedScopeVersion`.

La respuesta incluirá estado, hallazgos relevantes, evidencias de referencia, riesgos, resultados y acciones propuestas. No transferirá automáticamente historiales extensos ni archivos completos.

### 8.3. Coordinación entre sesiones

La serialización por agente deberá complementarse con un **bloqueo local por raíz canónica de repositorio**, de forma que dos sesiones administradas por Agent Engine no escriban simultáneamente sobre la misma copia de trabajo. Una aplicación externa podrá seguir modificando archivos; tales modificaciones se detectarán mediante comprobación de estado y hashes. En clientes compatibles se establecerá `agents.max_concurrent_threads_per_session = 1` y el coordinador esperará la finalización del agente delegado antes de iniciar el siguiente. Esta configuración no sustituye al bloqueo por repositorio ni a la comprobación de la ejecución efectiva.

## 9. Agente coordinador local

### 9.1. Funcionamiento

`agent-engine-local` será un proceso ligero iniciado bajo demanda. Recibirá solicitudes mediante un socket Unix protegido por permisos del usuario. Mantendrá su estado durable en SQLite y administrará directorios de respaldo privados, sin necesidad de un servicio cloud ni de un contenedor adicional.

### 9.2. Funciones

- Identificar proyecto mediante raíz canónica, usuario y contexto de repositorio.
- Mantener bloqueo único de escritura por repositorio para sesiones cooperantes.
- Resolver propietario de la ejecución, latido y estado del proceso.
- Administrar caché validada con versiones, expiración e integridad.
- Crear respaldos locales y mantener un journal recuperable.
- Detectar modificaciones externas y conflictos.
- Exponer a hooks operaciones breves de comprobación y persistencia.
- Sincronizar eventos resumidos con API cuando haya conexión.

Los bloqueos se apoyarán en mecanismos del sistema operativo, con registro de tarea y sesión. No se liberarán por el solo hecho de superar un tiempo arbitrario sin comprobar el estado real del proceso; tras reinicio se verificará cualquier journal incompleto.

### 9.3. Propiedad y límites del coordinador

- La identidad de la copia de trabajo será la ruta canónica, dispositivo y otros atributos disponibles para evitar que dos accesos mediante alias o enlaces simples creen bloqueos diferentes.
- El bloqueo será exclusivo para una escritura administrada por Agent Engine por repositorio; las operaciones de lectura cooperantes podrán seguir ejecutándose cuando no comprometan el estado del bloque.
- La propiedad del bloqueo se asociará al proceso real y a una generación de sesión; se validará tras reinicios y antes de permitir recuperaciones. El TTL no será el único criterio de liberación.
- El coordinador registrará los procesos prolongados que haya podido identificar y mantendrá el bloque en ejecución mientras existan procesos con efectos de escritura aún no verificados.
- La coordinación se limita a **sesiones cooperantes que usan Agent Engine**; no bloquea al editor, terminal u otros programas del sistema. Las modificaciones externas solo podrán detectarse y gestionarse por conflicto.

## 10. Clasificación, resumen y aprobaciones

### 10.1. Niveles

| Nivel | Criterios de referencia | Aprobación del plan |
| --- | --- | --- |
| Quick | Cambio localizado, alcance claro, bajo riesgo y dependencias limitadas | No adicional; se respetan permisos nativos de Codex |
| Standard | Varios archivos o componentes relacionados, refactorización funcional o impacto moderado | Sí |
| Deep | Seguridad, persistencia, dinero, múltiples módulos, cambios arquitectónicos, migraciones o alto riesgo | Sí |

Se aplicará el nivel de mayor riesgo. Una modificación de pocas líneas podrá ser Deep si altera una condición crítica. La clasificación incluirá criterios explícitos de alcance, dependencia, reversibilidad, efecto funcional y criticidad; el número de líneas será solo un indicador auxiliar.

### 10.2. Resumen previo

El formato será: pasos numerados breves, componentes por modificar o crear, componentes por reutilizar, estrategia de verificación y **Hallazgos importantes** (`Ninguno` cuando corresponda). No se producirán documentos extensos para tareas triviales.

### 10.3. Vigencia de las aprobaciones

Las aprobaciones Standard/Deep se asociarán a `approvalId`, `planVersion`, `scopeHash`, `requiredRulesVersion`, usuario y fecha. Se solicitará nueva aprobación si cambia materialmente el objetivo, se incorporan archivos fuera del alcance, se sustituyen decisiones técnicas relevantes o una regla obligatoria exige replantear el trabajo. Las correcciones internas dentro del alcance aprobado no requerirán otra aprobación.

Las aprobaciones se registrarán por una **acción humana verificable** desde Codex cuando el host exponga un mecanismo adecuado o desde el panel/local coordinator. Una afirmación del agente de que el usuario aprobó el plan no constituirá evidencia suficiente para el servidor. Se guardará identificador de decisión, origen, usuario, versión y marca temporal; no será necesario guardar la conversación completa. Si no existe una vía confiable de autorización en una superficie, el plan permanecerá en `WAITING_APPROVAL`.

La aprobación de un plan es distinta del consentimiento por invocación para GPT-6.1 Sol.

## 11. Planes, bloques y contrato de contexto

### 11.1. Documentos de la tarea

- **Plan original:** solicitud/documento fuente inmutable, identificado por versión o hash. Las enmiendas se registran por separado.
- **Plan operativo:** bloques secuenciales, dependencias, criterios de aceptación y estado de avance.
- **Checkpoint:** estado durable, bloque actual, decisiones confirmadas, resultados, versiones, referencias y próxima acción segura.

### 11.2. Context Bundle

Campos mínimos: `userId`, `projectId`, `taskId`, `planId`, `planVersion`, `originalPlanRef`, `blockId`, `rulesVersion`, `skillsVersion`, `contextVersion`, `expiresAt`, `requiredRules`, `relevantRules`, `decisions`, `checkpointId`, `approvedScopeVersion`, `nextAction`.

El paquete deberá mantenerse reducido, sin omitir reglas obligatorias. Contendrá referencias a materiales más extensos que puedan consultarse bajo demanda. El contexto no se volverá a descargar o reinyectar íntegramente después de cada comando sin necesidad.

### 11.3. Ciclo de un bloque

1. Cargar el plan original y su referencia, las reglas obligatorias y el checkpoint vigente.
2. Identificar archivos y componentes relacionados; comprobar alcance y aprobación.
3. Adquirir bloqueo local y preparar respaldo verificable.
4. Aplicar las modificaciones permitidas.
5. Detectar archivos modificados y comprobar que coinciden con el alcance.
6. Ejecutar pruebas y reglas deterministas pertinentes.
7. Corregir problemas localizados con un máximo de dos intentos automáticos por problema.
8. Persistir estado, liberar recursos correspondientes y continuar con el siguiente bloque.

Ante una ambigüedad material, el coordinador deberá **reconsultar el plan original** antes de seguir y solicitar aprobación de cualquier enmienda sustancial.

## 12. Execution State Machine

| Estado | Operación | Condición principal |
| --- | --- | --- |
| `RECEIVED` | Registrar solicitud | Usuario/proyecto reconocidos |
| `CONTEXT_READY` | Recuperar reglas y memoria | Contexto obligatorio íntegro |
| `ANALYZING` | Investigar código | Dependencias esenciales identificadas |
| `PLANNED` | Definir bloques y resumen | Plan y criterios versionados |
| `WAITING_APPROVAL` | Solicitar autorización | Requerida para Standard/Deep |
| `EXECUTING` | Ejecutar bloque | Bloqueo, contexto y respaldo verificados |
| `VERIFYING` | Revisar bloque | Pruebas y reglas aplicables evaluadas |
| `CORRECTING` | Corregir hallazgos del bloque | Intentos limitados |
| `FINAL_REVIEW` | Evaluar trabajo completo | Todos los bloques terminados |
| `FINAL_CORRECTING` | Corregir hallazgos de integración | Corrección protegida y revalidación |
| `SUSPENDED` | Conservar tarea recuperable | Checkpoint seguro disponible |
| `BLOCKED` | Informar problema impeditivo | Sin afirmación de éxito |
| `COMPLETED` | Registrar resultado verificado | Final Review Gate superado |

Los estados y sus transiciones serán persistentes e idempotentes. No se declarará `COMPLETED` sin superar la revisión integral final. Una propuesta de memoria pendiente **no convierte una implementación verificada en incompleta**: la revisión de memoria tendrá su propio estado administrativo.

### 12.1. Autoridad de transición y evidencia mínima

**Ninguna herramienta MCP invocada por el modelo podrá declarar por sí sola `COMPLETED`, aprobar un plan o certificar que las pruebas pasaron.** Se separarán las operaciones de lectura/propuesta disponibles al modelo de las operaciones de avance operativo del coordinador autenticado.

Para aceptar una transición, el servidor validará dentro de una transacción:

1. Identidad y scopes del cliente local; pertenencia del usuario y proyecto.
2. Estado anterior y transición permitida; revisión esperada de la tarea mediante comparación y actualización optimista (CAS).
3. Plan original y enmiendas vigentes; versión del alcance y reglas obligatorias aplicables.
4. Aprobación humana válida para Standard/Deep cuando corresponda; autorización de Sol consumida para cada delegación Sol.
5. Presencia de evidencias tipadas exigidas por el destino: resultado del coordinador, estado de respaldo, archivos referenciados por hashes, pruebas ejecutadas con salida y código de retorno resumidos, hallazgos y verificación del bloque.
6. Identificadores de evento idempotentes y secuencia monotónica por tarea y dispositivo, rechazando duplicados, saltos incompatibles y regresiones de versión.

La evidencia se clasificará como `MODEL_REPORTED` (afirmación del agente), `LOCAL_CHECKED` (verificación instrumental del coordinador), `SERVER_CONSISTENT` (estructura, procedencia, autorización y transiciones coherentes) y `HUMAN_APPROVED` (decisión humana). El servidor **no puede demostrar por sí solo la corrección semántica del código local** si no recibe el código fuente; su validación garantiza consistencia de contratos, estados y evidencias, no una auditoría independiente del contenido del repositorio.

La transición a `COMPLETED` requerirá todos los bloques aprobados, evidencias obligatorias del Final Review Gate, ausencia de hallazgos bloqueantes abiertos, presupuesto de correcciones respetado y revisión vigente del plan. Si falta una condición, se rechazará la transición sin alterar el estado persistido.

## 13. Execution Supervisor y control híbrido

### 13.1. Prevención y verificación

| Categoría | Política |
| --- | --- |
| Lectura y análisis | Permitidos según herramientas autorizadas |
| Escritura dentro del alcance y recuperable | Respaldo verificado, controles disponibles y revisión posterior |
| Escritura no interceptable pero recuperable | Verificación posterior obligatoria, con limitación documentada |
| Escritura fuera del alcance | Bloquear o requerir nueva aprobación |
| Operación potencialmente irreversible | Exigir prevención fiable o detener |
| Restauración conflictiva | Bloquear; preservar cambios del programador |

El plugin no afirmará que impide una escritura cuando la única capacidad disponible es detectar sus efectos posteriormente. La protección se diseñará como controles preventivos cuando existan, más detección y restauración segura cuando sea aplicable.

### 13.2. Supervisión por bloque

Se verificarán requisitos del bloque, convenciones, dependencias, alcance de archivos, pruebas disponibles y riesgos de regresión. La revisión por IA se reservará para casos de valor comprobable, preferentemente mediante Luna, con delegación secuencial. Ante fallo corregible se aplicarán los presupuestos acumulativos establecidos en la sección 13.3; después se pasará a `BLOCKED`.

### 13.3. Presupuestos de corrección y prevención de ciclos

Los contadores se conservarán en el journal local y en el registro de tarea. El reinicio de sesión, la compactación, la reconexión o el cambio de redacción de un problema **no reiniciarán automáticamente** el presupuesto.

| Límite predeterminado | Valor inicial | Aplicación |
| --- | --- | --- |
| Correcciones por hallazgo equivalente | 2 | Identidad estable por requisito/regla, archivo y firma del fallo |
| Correcciones acumuladas por bloque | 3 | Incluye errores distintos detectados en ese bloque |
| Correcciones acumuladas por tarea | 6 | Incluye bloques y revisión final |
| Rondas de corrección durante Final Review Gate | 2 | Deben consumir también el presupuesto global |

Al alcanzar cualquiera de los límites, el coordinador detendrá los intentos automáticos afectados y pasará a `BLOCKED`. El usuario podrá autorizar una nueva revisión de alcance y presupuesto mediante una decisión explícita; la nueva autorización generará una revisión de tarea, conservando todo el historial anterior. No se concederán reintentos infinitos mediante la creación automática de hallazgos nominalmente distintos.

## 14. Final Review Gate — Revisión integral obligatoria

Una vez terminados todos los bloques, antes del estado `COMPLETED`, el coordinador ejecutará una **revisión integral del resultado**. Esta revisión no sustituirá las verificaciones por bloque.

### 14.1. Alcance

1. Contrastar cada requisito y criterio de aceptación con el **plan original**, incluyendo sus enmiendas autorizadas.
2. Revisar conjuntamente todos los archivos y componentes modificados; detectar inconsistencias entre bloques.
3. Confirmar el cumplimiento de reglas globales y específicas, la reutilización y la ausencia de cambios innecesarios.
4. Comprobar dependencias, contratos y comportamiento integrado; ejecutar compilaciones, pruebas y controles pertinentes disponibles.
5. Identificar tareas omitidas, funcionalidades incompletas, errores de configuración y problemas de regresión visibles.
6. Verificar la integridad final de respaldos, bloqueos, checkpoints e historial resumido, sin crear commits.

### 14.2. Política proporcional

| Nivel | Revisión final |
| --- | --- |
| Quick | Lista de comprobaciones breves y pruebas pertinentes, sin subagente obligatorio |
| Standard | Revisión integral del agente principal, con Luna `high` cuando sea soportado |
| Deep | Revisión integral y, cuando aporte independencia, subagente revisor secuencial con Luna `high` o `xhigh` autorizado |

Sol seguirá requiriendo confirmación por invocación si se solicita para una revisión. Se reutilizarán evidencias válidas de los bloques y no se repetirán búsquedas o pruebas sin una razón identificable.

### 14.3. Corrección y cierre

Los hallazgos se clasificarán en bloqueantes, corregibles y advertencias no bloqueantes. Los errores bloqueantes o criterios incumplidos deberán corregirse de forma localizada con respaldo y verificación posterior. Se aplicarán los presupuestos acumulativos de la sección 13.3, incluyendo un máximo de dos rondas de corrección final y el límite global de tarea. Se ejecutará nuevamente la comprobación afectada, además de las pruebas integradas necesarias. Ninguna corrección final podrá invalidar el plan aprobado o ampliar silenciosamente su alcance.

Si persisten defectos que impiden cumplir los criterios obligatorios, la tarea pasará a `BLOCKED` con un informe de pendientes; no se declarará completada. Las advertencias no bloqueantes se mostrarán explícitamente y no podrán ocultar requisitos incumplidos.

**La tarea llegará a `COMPLETED` únicamente después de superar Final Review Gate.** A continuación se presentarán las propuestas de memoria permanente que correspondan.

## 15. Respaldos locales y restauración segura

### 15.1. Manifiesto del bloque

Para cada archivo relevante se registrarán ruta canónica relativa al repositorio, tipo, existencia inicial, hashes anteriores y posteriores, permisos pertinentes, operación (`create`, `modify`, `delete`, `rename`), vínculos entre renombrados y estado del respaldo. Los enlaces simbólicos se conservarán como enlaces; no se seguirán automáticamente hacia ubicaciones externas.

### 15.2. Operación y atomicidad

- Preparar el manifiesto **antes** de cualquier escritura cubierta por Agent Engine.
- Crear respaldos en almacenamiento local privado y comprobar su integridad.
- Persistir un journal con estados `PREPARED`, `APPLYING`, `APPLIED`, `VERIFIED`, `RESTORING`, `RESTORED` o `CONFLICT`.
- Usar escritura temporal y renombrado atómico cuando el sistema de archivos lo permita; registrar operaciones que requieran tratamiento especial.
- Ante interrupción, reconstruir el estado desde el journal y los archivos reales, sin repetir escrituras de resultado incierto.

### 15.3. Restauración condicional

Restaurar solo cuando el estado actual coincida con el estado esperado después de la ejecución propia. Si se detecta un cambio externo, un enlace inesperado, un permiso no recuperable o un destino ambiguo, detener la restauración automática. Los archivos nuevos solo podrán retirarse si se demuestra su creación por Agent Engine y ausencia de modificaciones posteriores de terceros.

No se ejecutarán `git commit`, `git reset --hard`, `git clean` ni sustitutos destructivos automáticos. Git podrá consultarse en solo lectura cuando ayude a entender el repositorio.

### 15.4. Límite de los hashes y protección frente a carreras

La comparación de hashes detecta divergencias, pero no elimina la carrera entre **comprobar** un archivo y **reemplazarlo** (TOCTOU). Por ello, el diseño no ofrecerá una garantía absoluta de protección frente a editores externos que escriban simultáneamente.

Para restaurar un archivo regular se exigirá:

1. Bloqueo cooperante del repositorio, manifiesto íntegro, ausencia de procesos propios pendientes y ausencia de conflicto detectado.
2. Confirmación del tipo de archivo, ruta canónica, vínculo simbólico y metadatos relevantes mediante operaciones sobre directorios seguros; nunca seguir enlaces simbólicos que conduzcan fuera del ámbito autorizado.
3. Comparación inmediata de identidad del archivo y hash actual frente al último estado producido por Agent Engine; si difieren, **abortar sin sobrescribir**.
4. Preparación del contenido restaurado en archivo temporal privado sobre el mismo sistema de archivos, con permisos adecuados; sincronización del archivo antes del reemplazo cuando el sistema lo permita.
5. Sustitución atómica mediante operaciones del sistema de archivos, preservando la semántica documentada de permisos y enlaces; registro durable del resultado y comprobación posterior.

El reemplazo atómico reduce estados intermedios, pero **no equivale a una operación universal de compare-and-swap sobre el sistema de archivos**: una escritura externa todavía puede producirse entre validación y reemplazo. Si se detecta actividad externa, conflicto de identidad, permisos/ACL no preservables, hardlinks o un tipo de archivo no respaldado, se suspenderá la restauración automática. La restauración de directorios, renombrados y eliminaciones seguirá un manifiesto de operaciones reversible, con validaciones propias y sin borrar rutas que hayan cambiado de propietario o contenido.

### 15.5. Interrupciones y retención de respaldos

- El journal utilizará un protocolo de dos etapas (`BACKUP_PREPARING` → `BACKUP_READY`) y estados explícitos de restauración (`RESTORE_PREPARING` → `RESTORE_DONE` o `RESTORE_CONFLICT`), con confirmación durable antes de cambiar archivos. Tras un cierre abrupto, no se asumirá éxito sin reconstruir y comprobar el estado del disco.
- Los respaldos de bloques activos, suspendidos o bloqueados **no se eliminarán automáticamente** mientras sean necesarios para recuperación. Para bloques terminados y sincronizados se establecerá retención predeterminada de **siete días**.
- Presupuesto local inicial de **2 GiB** para respaldos de tareas finalizadas. Se podrán depurar respaldos de tareas terminadas y sincronizadas más antiguos; si el nuevo respaldo no cabe y no hay espacio reclamable seguro, el bloque quedará `BLOCKED` antes de editar.
- Archivos especialmente grandes, dispositivos, sockets, nodos especiales, hardlinks compartidos y atributos no recuperables requerirán exclusión explícita o autorización de una estrategia diferente; no se generará un respaldo parcial que aparente ser recuperable.
- Los respaldos residirán fuera del repositorio, con permisos privados y protección de disco disponible en macOS. No se enviarán al servidor ni aparecerán en los logs.

## 16. Memoria operativa y aprobación de memoria permanente

### 16.1. Registro automático

Los checkpoints, transiciones de estado, datos mínimos de continuidad, versiones y eventos resumidos se guardarán automáticamente. Este conocimiento operativo no constituirá una regla permanente.

### 16.2. Propuestas de memoria al finalizar

Durante la tarea, Agent Engine podrá generar **propuestas**, pero no las confirmará automáticamente como memorias permanentes. Después de Final Review Gate, mostrará únicamente las propuestas útiles y justificadas.

| Campo | Finalidad |
| --- | --- |
| Tipo | Regla, preferencia, decisión o conocimiento |
| Ámbito | Global o proyecto |
| Propuesta | Texto concreto que se conservaría |
| Justificación | Utilidad futura y contexto |
| Procedencia | Tarea, decisión y evidencia asociada |
| Conflictos | Duplicados, versiones previas o contradicciones |
| Acción | Aprobar, editar, rechazar o cambiar ámbito |

**Solo se guardarán en memoria permanente las propuestas aprobadas por el programador.** Si no hay propuestas útiles, no se solicitará revisión. Si la sesión finaliza sin decisión, las propuestas permanecerán pendientes sin impedir que una tarea ya verificada conserve su estado `COMPLETED`.

Las órdenes explícitas del usuario para crear, editar o eliminar una regla mediante MCP son acciones administrativas autorizadas y se distinguirán de la captura automática de sugerencias.

### 16.3. Duplicados y conflictos

Antes de presentar o guardar propuestas se buscarán memorias equivalentes o contradictorias. La sustitución de una regla aprobada dejará registro de versión y desactivación de la anterior. Se utilizarán tombstones y versiones para impedir la reaparición de elementos eliminados desde caché.

## 17. Caché local, desconexión y continuidad

- Mantener caché asociada a usuario, proyecto, tarea y versión, con integridad verificable, expiración y aislamiento local.
- Permitir continuar sin API **solo tareas previamente conocidas y autorizadas**, con reglas obligatorias y aprobación vigentes en la caché.
- Bloquear escrituras cuando falten reglas críticas, existan conflictos, haya expiración o se requiera autorización que no pueda validarse.
- No permitir administración remota ni aprobación implícita de Sol durante desconexión.
- Conservar eventos mínimos en cola local idempotente; sincronizar cuando vuelva la API, comprobando versiones y tombstones.
- Validar el estado real del repositorio al reanudar; no asumir que el último checkpoint refleja una escritura completada.

La API será fuente de verdad para reglas y memorias aprobadas; el coordinador local será fuente operativa para respaldos y journal. Los checkpoints tendrán identificadores y revisiones reconciliables, evitando sobrescrituras silenciosas.

### 17.1. Caducidad y autorización offline

- La vigencia inicial máxima de un contexto remoto validado para **continuar escrituras sin conexión** será de **cuatro horas desde la última sincronización correcta**. Este periodo será configurable a la baja y no se ampliará automáticamente por el uso de la caché.
- Solo podrán continuar tareas y bloques previamente autorizados. No se admitirán nuevas aprobaciones Standard/Deep, cambios administrativos ni nuevas invocaciones Sol sin conexión.
- La caché llevará `userId`, identidad del dispositivo, `projectId`, `taskId`, versiones de reglas/plan, instante de validación, caducidad y firma o comprobación de integridad local. Una regla crítica ausente, un bloqueo o una revisión incompatible implicará suspensión de escrituras.
- La caché que haya expirado podrá ayudar a **mostrar información o preparar una recuperación**, pero no a autorizar nuevas ediciones.

### 17.2. Reconciliación de checkpoints y eventos

1. Registrar primero localmente cada transición de operación mediante SQLite con identificador idempotente, `localSequence` monotónica, generación del coordinador y versión del checkpoint remoto conocida.
2. Enviar eventos mínimos en el orden establecido; el servidor confirmará aceptación y nueva revisión. El cliente retirará de la cola únicamente eventos confirmados.
3. Aplicar actualizaciones sobre la última revisión remota mediante CAS; ante discrepancia de versiones, detener la reconciliación de la tarea afectada y solicitar resolución en vez de sobrescribir datos remotos.
4. Al regresar la conexión, comprobar reglas revocadas, autorizaciones vencidas y memorias eliminadas; invalidar snapshots incompatibles. Nunca restaurar una regla borrada a partir de un paquete local anterior.
5. Cuando una tarea haya superado todas las verificaciones estando offline, presentar su resultado local como **finalizado, pendiente de sincronización**; el servidor solo persistirá `COMPLETED` después de recibir y validar las evidencias. La condición de sincronización será independiente del estado funcional local.
6. Los eventos pendientes se conservarán hasta su confirmación o intervención explícita; se fijará una cuota local inicial de **10.000 eventos** y **30 días de conservación** como umbral de aviso y mantenimiento. Si la cola queda llena o no puede persistir el siguiente evento necesario, se suspenderán nuevas escrituras hasta recuperar capacidad sin perder trazabilidad.

### 17.3. Retención de checkpoints

Los checkpoints operativos de tareas abiertas, suspendidas o bloqueadas se conservarán hasta su resolución o eliminación explícita por el usuario. Tras una tarea terminada y sincronizada, la retención local predeterminada de metadatos de recuperación será de **30 días**; el servidor conservará el historial y las referencias según su política administrativa. Los respaldos de archivos, regidos por la sección 15.5, tendrán una retención independiente.

## 18. Persistencia y modelo de datos

Los nombres siguientes son una propuesta de entidades para PostgreSQL. Se concretarán tipos, claves, restricciones e índices mediante migraciones Flyway.

| Tabla | Finalidad y campos relevantes |
| --- | --- |
| `users` | Identidad, hash de credenciales, rol y estado |
| `user_invitations` | Invitación, hash de token, vencimiento, invitador y consumo |
| `profiles` | Perfiles personales, propietario, estado y alcance |
| `projects` | Identificador estable, propietario, nombre, configuración y referencias |
| `rules` | Regla, fuente, ámbito, obligatoriedad, prioridad, versión y vigencia |
| `skills` | Nombre, contenido, versión, ámbito, estado y propietario |
| `model_catalog` | Identificador técnico, nombre visible, prioridad y disponibilidad |
| `model_reasoning_levels` | Modelo y esfuerzo permitido |
| `model_role_policies` | Rol, modelo preferido, esfuerzo y consentimiento requerido |
| `memories` | Texto aprobado, tipo, fuente, ámbito, vigencia y versión |
| `memory_proposals` | Propuesta, justificación, estado, decisión y relación con la tarea |
| `tasks` | Usuario, proyecto, objetivo, nivel, estado y referencia del plan |
| `task_plans` | Plan original, revisiones/enmiendas, hash, alcance y aprobación |
| `task_blocks` | Orden, criterio, estado, reintentos y resultado |
| `checkpoints` | Estado resumido, versiones y siguiente operación segura |
| `execution_events` | Identificador idempotente, tarea, bloque, tipo, resumen y fecha |
| `review_findings` | Hallazgo, severidad, estado, verificación y relación con plan |
| `local_clients` | Dispositivo lógico, propietario, vínculo OAuth y scopes del coordinador sin tokens en claro |
| `execution_approvals` | Actor, origen humano, plan/alcance/versiones, resultado, fecha y revocación |
| `model_consents` | Delegación Sol, modelo, esfuerzo, caducidad, consumo único y estado |
| `execution_evidence` | Tipo de prueba, origen, nivel de evidencia, referencia, hash, resultado y momento |
| `checkpoint_sync` | Revisión local/remota, secuencia confirmada y estado de reconciliación |
| `administrative_audit` | Actor, operación, recurso, versión y fecha |

Las entidades OAuth se ajustarán al esquema del proveedor seleccionado. Índices por usuario/proyecto/tarea/estado, control de concurrencia optimista para recursos editables, Full Text Search con GIN y unicidad de eventos por identificador. No se persistirán respaldos de archivos ni diffs completos en PostgreSQL.

## 19. API REST y herramientas MCP

| Recurso REST propuesto | Función | Herramientas MCP orientativas |
| --- | --- | --- |
| `/api/v1/profile` | Perfil activo | `get_profile` |
| `/api/v1/projects` | Administración de proyectos | `get_project_context`, `update_project` |
| `/api/v1/rules` | CRUD y versión de reglas | `search_rules`, `save_rule`, `delete_rule` |
| `/api/v1/skills` | CRUD y sincronización | `list_skills`, `get_skill`, `save_skill` |
| `/api/v1/models` | Catálogo y prioridades | `list_available_models`, `save_model` |
| `/api/v1/model-policies` | Modelos y esfuerzos por rol | `get_model_policy`, `update_model_policy` |
| `/api/v1/memories` | Consulta y administración de memorias aprobadas | `search_memory`, `save_memory`, `delete_memory` |
| `/api/v1/memory-proposals` | Revisión de propuestas | `list_memory_proposals`, `decide_memory_proposal` |
| `/api/v1/tasks` | Solicitud, estado y plan | `get_task`, `save_plan` |
| `/api/v1/tasks/{id}/blocks` | Bloques y progreso | `get_execution_block` (lectura para el modelo) |
| `/api/v1/checkpoints` | Persistencia y recuperación | `get_checkpoint` (lectura del modelo); guardado por coordinador |
| `/api/v1/executions` | Historial resumido | `record_execution_event`, `get_execution_history` |
| `/api/v1/reviews` | Resultados por bloque y finales | `list_review_findings` y propuesta de hallazgos; validación operativa por coordinador |
| `/api/v1/local-clients` | Vinculación y revocación del coordinador | No expuesta como acción libre al modelo |
| `/api/v1/execution-control` | Transiciones, evidencias, consumos de consentimientos y CAS | Solo coordinador local autenticado, con scopes restringidos |
| `/api/v1/admin/invitations` | Invitaciones administrativas | Solo administradores |
| `/mcp` | Endpoint MCP protegido por OAuth | Conjunto reducido de herramientas de alto valor |

Los nombres y rutas son contratos propuestos, no sustitutos de los endpoints estándar de OAuth. Se centralizarán autorización, validación y lógica en el servidor. Las respuestas utilizarán paginación y DTOs mínimos. Las operaciones MCP sensibles requerirán ámbito explícito y permisos asociados al usuario.

**Separación de confianza:** las herramientas MCP del modelo podrán **solicitar, consultar o proponer** cambios de estado; no tendrán acceso directo a los endpoints exclusivos de certificación del coordinador. Este último enviará evidencias instrumentadas de pruebas, archivos y respaldos. El servidor validará permisos, esquema y transiciones; no afirmará haber ejecutado localmente pruebas que solo recibió como resultados reportados. Las acciones humanas de aprobación deberán provenir de un canal de interacción verificable y no de parámetros arbitrarios aportados por el modelo.

## 20. Panel administrativo

| Sección | Funcionalidad |
| --- | --- |
| Inicio | Estado del servidor, conexión y última sincronización |
| Memorias | Buscar, revisar origen y vigencia, editar, desactivar o eliminar |
| Propuestas de memoria | Aprobar, modificar, rechazar y resolver conflictos |
| Reglas | Globales y de proyecto, prioridad, obligatoriedad y versiones |
| Skills | Listado, edición, validación y sincronización al inicio de sesión o tarea |
| Proyectos | Perfiles y referencias de contexto |
| Modelos | CRUD, prioridades, principal/secundarios, razonamiento por rol y autorización Sol |
| Ejecuciones | Título, plan, bloques, resultados, hallazgos, anotaciones y bitácora ligera |
| Configuración | Usuarios invitados, sesiones OAuth y preferencias de ejecución |

El panel no ejecutará código ni mostrará copias completas del repositorio. Las operaciones administrativas permitidas se expondrán también desde Codex mediante las herramientas MCP correspondientes.

## 21. Plan de implementación por fases

Cada fase deberá dejar pruebas reproducibles y un entregable verificable. Las fases son secuenciales; no se iniciará una fase dependiente sin completar las verificaciones esenciales de la anterior.

### Fase 0 — Viabilidad de Codex y contratos críticos

- [ ] Fijar versiones exactas de Codex Desktop, CLI y macOS de prueba.
- [ ] Construir plugin mínimo con manifest, skill, MCP y hooks confiados.
- [ ] Comprobar los eventos de ciclo de vida realmente disponibles en ambas superficies.
- [ ] Identificar rutas de escritura interceptables y no interceptables; probar decisiones de bloqueo y verificación posterior.
- [ ] Verificar `exec_command` prolongado, entrada tardía mediante `write_stdin`, proceso interactivo y llegada diferida de `PostToolUse`; documentar cuándo no existe un nuevo `PreToolUse`.
- [ ] Medir carreras entre hash previo y reemplazo de archivos; demostrar que los cambios concurrentes detectables bloquean restauraciones y describir el límite restante de protección.
- [ ] Probar coordinador local mínimo, comunicación por socket y bloqueo entre dos sesiones.
- [ ] Probar HTTPS local, certificado confiable y OAuth MCP con navegador.
- [ ] Probar **dos clientes OAuth independientes**: Codex-MCP y coordinador Python con PKCE/loopback; comprobar Keychain y scopes restringidos.
- [ ] Probar consentimiento Sol de un solo uso: consumo atómico, expiración, reintento y rechazo; no suponer visibilidad de cada petición interna al proveedor.
- [ ] Probar rechazo de una transición `COMPLETED` sin evidencia instrumental de Final Review Gate y una aprobación Standard/Deep falsa enviada desde MCP.
- [ ] Validar creación de subagentes, configuración de modelo/esfuerzo, permisos y secuencialidad.
- [ ] Comprobar sincronización de skills y restauración tras compactación.
- [ ] Entregar `docs/compatibility-matrix.md` con capacidades `SUPPORTED`, `PARTIAL`, `UNSUPPORTED`, evidencia y alternativa.

**Entregable:** recorrido mínimo que recupera una regla, prepara un bloque, respalda, edita, verifica, guarda un checkpoint y reanuda; matriz de compatibilidad sin supuestos no probados.

### Fase 1 — Infraestructura, API y autenticación

- [ ] Implementar Spring Boot y PostgreSQL con Docker Compose y Flyway.
- [ ] Configurar volúmenes, secretos, health checks y exposición local.
- [ ] Implementar administrador inicial, invitaciones y aislamiento de usuarios/proyectos.
- [ ] Implementar autorización web y OAuth MCP, con descubrimiento y revocación.
- [ ] Probar seguridad de scopes, audiencia, CSRF cuando aplique y cierre de sesión.
- [ ] Implementar registro de clientes locales, scopes separados por tipo de cliente y tokens de coordinador protegidos mediante Keychain.
- [ ] Implementar contratos de validación de transiciones, evidencia tipada, aprobación humana y CAS de estado.

**Entregable:** dos contenedores funcionales y usuarios autenticados desde panel y MCP.

### Fase 2 — Plugin y coordinador local

- [ ] Implementar estructura portable del plugin y scripts de hooks.
- [ ] Implementar coordinador local con arranque bajo demanda, socket seguro y SQLite.
- [ ] Implementar registro de proyecto, bloqueo de escritura y journal durable.
- [ ] Probar dos sesiones sobre el mismo repositorio y recuperación tras terminación inesperada.
- [ ] Registrar y verificar procesos prolongados; rechazar sesiones interactivas capaces de editar sin nueva comprobación.
- [ ] Implementar cliente OAuth propio para Python y sincronización sin acceso a credenciales internas de Codex.
- [ ] Integrar plugin con MCP autenticado y sincronización de eventos.

**Entregable:** plugin integrado y coordinador local que protege una ejecución secuencial.

### Fase 3 — Reglas, skills y catálogo de modelos

- [ ] Implementar perfiles, proyectos, reglas, versiones, jerarquía y detección de conflictos.
- [ ] Implementar CRUD de skills personales y sincronización al iniciar sesión/tarea.
- [ ] Crear catálogo dinámico y valores semilla de Luna y Sol.
- [ ] Incorporar razonamiento por rol, prioridades, disponibilidad y consentimiento Sol individual con expiración de dos minutos, consumo único y nueva aprobación por delegación.
- [ ] Publicar interfaz administrativa correspondiente en Vue 3.

**Entregable:** configuración modificable desde panel y Codex, con políticas efectivamente aplicables.

### Fase 4 — Memory Engine, caché y propuestas

- [ ] Crear memorias con ámbito, origen, confianza, versión y vigencia.
- [ ] Implementar Full Text Search con filtros e índices.
- [ ] Generar context bundles reducidos con reglas obligatorias.
- [ ] Implementar caché local versionada, comprobación de integridad y expiración de escritura offline inicial de cuatro horas.
- [ ] Implementar cola de eventos idempotentes, reconciliación con CAS, límites de retención y tombstones.
- [ ] Separar resultados locales pendientes de sincronización de estados `COMPLETED` confirmados por servidor.
- [ ] Crear flujo de propuestas de memoria pendientes y aprobación administrativa.
- [ ] Probar ausencia de escritura offline sin contexto válido.

**Entregable:** conocimiento recuperable, modo offline controlado y memoria permanente solo tras aprobación.

### Fase 5 — Workflow, clasificación y continuidad

- [ ] Implementar estados persistentes, transiciones y checkpoints con evidencias de autorización y revisión obligatoria del servidor.
- [ ] Preparar las ocho skills generales y criterios de activación.
- [ ] Clasificar Quick/Standard/Deep por riesgo y alcance.
- [ ] Implementar resumen previo, aprobación de Standard/Deep y reglas de invalidación.
- [ ] Implementar plan original inmutable, enmiendas y bloques operativos.
- [ ] Restablecer tarea después de compactación sin repetir bloques aprobados.

**Entregable:** tarea por bloques con aprobación, contexto vigente y continuidad comprobable.

### Fase 6 — Orquestación, supervisor y respaldos

- [ ] Implementar contratos de subagentes y una delegación activa por tarea.
- [ ] Integrar restricciones de lectura y escritura según función.
- [ ] Implementar permisos de escritura por repositorio y verificación de cambios fuera del alcance.
- [ ] Implementar respaldos por bloque para crear, modificar, borrar y renombrar archivos.
- [ ] Cubrir enlaces simbólicos, permisos, fallos de escritura y restauración conflictiva.
- [ ] Implementar verificación determinista y revisión especializada proporcional.
- [ ] Incorporar límites de autocorrección por hallazgo (2), bloque (3), tarea (6) y revisión final (2 rondas), persistentes entre sesiones.
- [ ] Probar operaciones atómicas, límites TOCTOU y conflictos con editores externos; documentar garantías y excepciones.
- [ ] Aplicar política de respaldos: retención siete días tras sincronización, cuota dos GiB y preservación de tareas activas/bloqueadas.

**Entregable:** implementación supervisada que no sobrescribe cambios preexistentes.

### Fase 7 — Final Review Gate y memoria al cierre

- [ ] Revisar cumplimiento completo del plan original y criterios de aceptación.
- [ ] Ejecutar comprobaciones integradas y detectar diferencias entre bloques.
- [ ] Implementar `FINAL_REVIEW` y `FINAL_CORRECTING` con reintentos acumulados y presupuestos globales limitados.
- [ ] Requerir evidencias instrumentadas y validación transaccional antes de persistir `COMPLETED`.
- [ ] Impedir `COMPLETED` si existen incumplimientos bloqueantes.
- [ ] Tras revisión final satisfactoria, presentar propuestas de memoria con aprobar/editar/rechazar.
- [ ] Verificar que ninguna propuesta se guarde permanentemente sin consentimiento.
- [ ] Registrar bitácora final sin código fuente ni diffs.

**Entregable:** tarea finalizada únicamente tras revisión integral, con propuestas de memoria administrables.

### Fase 8 — Evaluación, seguridad y publicación privada

- [ ] Ejecutar pruebas reproducibles en Desktop y CLI.
- [ ] Validar OAuth, autorizaciones, caídas de API, compactaciones, caché y reconexión.
- [ ] Probar dos sesiones, cambios manuales, journal incompleto y respaldos complejos.
- [ ] Probar fallo de API con caché de cuatro horas, sincronización posterior, conflictos de revisiones y límite de cola.
- [ ] Verificar que un evento MCP simulado no pueda certificar una prueba ni completar una tarea.
- [ ] Comparar tareas con Luna sin Agent Engine y con Agent Engine, desde el mismo estado inicial.
- [ ] Medir éxito funcional, cumplimiento, operaciones redundantes, reintentos, tokens y duración.
- [ ] Documentar límites de hooks y de aplicación dinámica de modelos.
- [ ] Entregar manual de instalación, matriz de compatibilidad y paquete privado.

**Entregable:** versión 1.0 verificable, documentada y desplegable localmente.

## 22. Pruebas y criterios de aceptación

| ID | Escenario | Resultado exigido |
| --- | --- | --- |
| T01 | Usuario invitado conecta Desktop y CLI | Acceso MCP autenticado en ambas superficies |
| T02 | Regla de proyecto creada desde panel | Recuperación en Codex y aplicación en bloque pertinente |
| T03 | Skill modificada entre sesiones | Nueva versión sincronizada sin reemplazar silenciosamente una activa |
| T04 | Cambiar modelo y razonamiento en panel | Política aplicada cuando el cliente lo permita; incompatibilidad señalada |
| T05 | Solicitud de Sol | Ninguna invocación sin aprobación explícita individual |
| T06 | Tarea Quick | Resumen breve y ejecución sin aprobación adicional del plan |
| T07 | Tarea Standard o Deep | Aprobación previa vinculada al alcance |
| T08 | Cambio significativo del plan aprobado | Requerimiento de nueva aprobación |
| T09 | Explorador, implementador y revisor | Delegaciones secuenciales sin simultaneidad |
| T10 | Dos sesiones escriben en el mismo repositorio | Una única ejecución de escritura administrada por Agent Engine |
| T11 | Modificación manual de archivo durante bloque | Conflicto detectado; no se sobrescribe la modificación ajena |
| T12 | Crear, borrar y renombrar archivos | Respaldo y restauración seguros, o bloqueo explícito ante conflicto |
| T13 | Enlace simbólico y permisos especiales | No se modifica una ruta externa imprevista; metadatos protegidos |
| T14 | Interrupción durante respaldo o escritura | Recuperación desde journal sin repetir operación incierta |
| T15 | Compactación en bloque intermedio | Plan y reglas restaurados; bloques anteriores no se repiten |
| T16 | API sin conexión con caché válida | Continúa solo una tarea previamente autorizada |
| T17 | API sin conexión y caché inválida | Escritura dependiente detenida |
| T18 | Regla personal contradice skill general | Se aplica precedencia legítima y se registra resolución |
| T19 | Duplicado o conflicto de memoria | No se crea memoria contradictoria sin decisión |
| T20 | Finalización con error entre bloques | Final Review Gate detecta, corrige y revalida, o bloquea |
| T21 | Finalización con requisitos pendientes | `COMPLETED` no se alcanza |
| T22 | Revisión final satisfactoria | Se presentan propuestas de memoria; nada se guarda sin autorización |
| T23 | Comprobación del historial | Solo plan, resultados, metadatos y resúmenes permitidos |
| T24 | Comprobación de Git | Ningún commit automático generado |
| T25 | Dos usuarios y proyectos distintos | Aislamiento de reglas, memorias, tareas y permisos |
| T26 | Memoria eliminada con cliente offline | Tombstone evita reaparecer tras reconexión |
| T27 | Comando prolongado seguido de `write_stdin` | No se asume una nueva autorización `PreToolUse`; cambios posteriores detectados al finalizar |
| T28 | Shell interactiva con capacidad de escritura persistente | No se utiliza como canal de edición administrada sin controles preventivos suficientes |
| T29 | Editor externo cambia archivo entre hash y restauración | Se detectan los conflictos observables; cualquier caso no prevenible se documenta sin afirmar garantía absoluta |
| T30 | Coordinador intenta reutilizar credenciales de Codex | Vinculación independiente PKCE; el coordinador no necesita tokens internos de Codex |
| T31 | Autorización Sol duplicada, caducada o consumida | Solo un inicio de delegación autorizado; el reintento requiere nueva confirmación |
| T32 | MCP intenta marcar bloque verificado mediante resumen libre | Servidor rechaza transición sin evidencia tipada y revisión correcta |
| T33 | Intento de `COMPLETED` sin Final Review Gate | Rechazado transaccionalmente, sin cambio de estado |
| T34 | Actualización concurrente del checkpoint | CAS detecta conflicto y no sobrescribe silenciosamente la versión vigente |
| T35 | Continuación offline más de cuatro horas | Nuevas escrituras dependientes quedan suspendidas hasta sincronizar |
| T36 | Cola de sincronización llena | Se conservan registros existentes y se bloquean nuevas escrituras dependientes |
| T37 | Serie de hallazgos distintos que agotan el presupuesto global | `BLOCKED` después de seis correcciones acumuladas, sin ciclos indefinidos |
| T38 | Reinicio durante restauración | Journal reconoce operación incompleta y no repite una escritura incierta |
| T39 | Caducidad y limpieza de respaldos | Solo se depuran respaldos terminados y sincronizados, sin afectar tareas activas |
| T40 | Ejecución offline completada localmente | Resultado local pendiente de sincronización; servidor valida antes de `COMPLETED` |

### 22.1. Requisitos de salida

- **Seguridad:** ningún escenario crítico de pérdida de cambios humanos o invocación no autorizada podrá considerarse aceptable.
- **Continuidad:** compactación, interrupción y recuperación deberán completar los escenarios T14-T17 sin duplicar bloques verificados.
- **Calidad:** todas las reglas deterministas obligatorias comprobables y requisitos funcionales deberán satisfacerse en las pruebas de aceptación.
- **Finalización:** ningún estado `COMPLETED` podrá persistirse si Final Review Gate identifica un incumplimiento bloqueante o si el servidor carece de las evidencias exigidas.
- **Identidad:** los clientes Codex-MCP y coordinador Python se autenticarán de forma separada, con scopes mínimos y sin compartir secretos internos de Codex.
- **Reintentos:** se respetarán los límites por hallazgo, bloque, tarea y revisión final, incluso después de compactación o reinicio.
- **Modo offline:** ninguna escritura continuará con un contexto de más de cuatro horas desde la última sincronización válida.
- **Modelos:** un esfuerzo o modelo incompatible no se aplicará silenciosamente.
- **Eficiencia:** el ensayo comparativo medirá éxito por tarea, tokens por resultado correcto, operaciones repetidas y tiempo; no se presumirá ahorro sin evidencia.

Los ensayos usarán un conjunto fijo de casos Quick, Standard y Deep, con repositorios iniciales equivalentes. Los umbrales numéricos de mejora de tokens y tiempo se fijarán contra la línea base medida, sin rebajar criterios críticos de seguridad y corrección.

## 23. Riesgos y decisiones de mitigación

| Riesgo | Respuesta del diseño |
| --- | --- |
| Diferencias entre hooks de Desktop y CLI | Prueba bloqueante de Fase 0, matriz y verificación posterior por bloque |
| Herramientas que evaden intercepción | Aislamiento del alcance, respaldo, inspección del sistema de archivos y bloqueo de operaciones irreversibles |
| `write_stdin` posterior al lanzamiento de comandos | Procesos no interactivos preferidos, proceso trazado, espera de finalización y verificación posterior; sin segunda evaluación `PreToolUse` |
| Carreras entre comprobación de hash y escritura | Revalidación inmediata, reemplazo atómico y bloqueo ante conflictos detectables; sin garantía universal frente a editores externos |
| Uso por Python de credenciales privadas de Codex | OAuth público separado con PKCE, scopes mínimos y macOS Keychain |
| Certificación ficticia de estados desde herramientas MCP | APIs operativas segregadas, evidencia tipada y transiciones CAS autorizadas por servidor |
| Autorización Sol reutilizada o caducada | Permiso de una delegación, ligado a versiones, vencimiento y consumo atómico |
| Reintentos infinitos por hallazgos distintos | Presupuestos globales durables por tarea, bloque y revisión final |
| Caché indefinida o sincronización conflictiva | Cuatro horas offline, cola idempotente, CAS de checkpoints y suspensión ante conflicto |
| OAuth local y certificado no aceptados | Prueba de descubrimiento, certificado, PKCE y redirección antes del desarrollo completo |
| Selección de modelo no modificable en caliente | Aplicación en sesión/etapa compatible, sin afirmación de cambio efectivo no comprobado |
| Subagentes concurrentes no controlados | Coordinación secuencial, delegación única y bloqueo local por repositorio |
| Sesión interrumpida durante modificación | Journal durable, estado incierto y reconciliación antes de reintentar |
| Restauración que elimina trabajo ajeno | Comprobación de hashes, metadatos y bloqueo ante divergencias |
| Reglas o memoria contradictorias | Versiones, prioridad, ámbito, procedencia y revisión humana |
| Memoria incorrecta almacenada automáticamente | Propuestas pendientes; aprobación explícita al finalizar |
| Verificación final insuficiente | Revisión integral contra plan, pruebas integradas y condición obligatoria para `COMPLETED` |
| Exceso de razonamiento y consumo | Skills selectivas, comprobaciones deterministas, subagentes secuenciales y evaluación empírica |
| Filtración de código o secretos al servidor | DTOs permitidos, saneamiento, datos mínimos y respaldos exclusivamente locales |

## 24. Documentación y referencias técnicas

Los siguientes documentos serán fuente de contraste durante la Fase 0 y deberán revisarse contra las versiones instaladas:

- Empaquetado de plugins Codex: https://developers.openai.com/plugins/build/plugins
- Hooks y límites, incluidos `exec_command` y `write_stdin`: https://learn.chatgpt.com/docs/hooks
- Configuración y subagentes Codex: https://learn.chatgpt.com/docs/agent-configuration/subagents
- Referencia de configuración Codex: https://learn.chatgpt.com/docs/config-file/config-reference
- SDK Java MCP: https://java.sdk.modelcontextprotocol.io/latest/
- Autenticación de plugins MCP: https://developers.openai.com/plugins/build/auth
- Especificación de autorización MCP: https://modelcontextprotocol.io/specification/2025-11-25/basic/authorization
- Cambios MCP 2026-07-28, incluidos metadatos y clientes nativos: https://blog.modelcontextprotocol.io/posts/2026-07-28/
- Spring Authorization Server: https://docs.spring.io/spring-authorization-server/reference/
- Modelos y parámetros de razonamiento: documentación vigente de modelos compatibles con la instalación de Codex.

## 25. Criterio de cierre

La versión **1.0** estará finalizada cuando exista una instalación privada operativa en Codex Desktop y CLI, con API y PostgreSQL en dos contenedores locales; autenticación MCP; administración de reglas, memorias, skills y modelos; recuperación de contexto; coordinación local y secuencial; respaldos por bloque; verificación progresiva; **Final Review Gate obligatorio**; y aprobación explícita de memoria permanente.

La Fase 0 deberá documentar las incompatibilidades antes de implementar componentes dependientes de ellas. Una capacidad no soportada no se describirá como completada. En caso de una limitación impeditiva, se registrará una alternativa compatible y se reevaluará el criterio de aceptación afectado sin introducir garantías falsas.

El cierre requerirá superar las pruebas T01-T40 relevantes y demostrar el recorrido integral: autorización humana, recuperación de reglas, preparación de contexto, bloqueo y respaldo, modificación, verificación por bloque, Final Review Gate con evidencia instrumental, transición autorizada y reconciliación de checkpoints. Se verificará por separado el manejo de procesos prolongados, consentimiento Sol de un solo uso, identidad OAuth propia del coordinador y los presupuestos acumulativos de recuperación.

**Definición de terminado:** una solicitud se convierte en una ejecución contextualizada y planificada; se aprueba cuando corresponda; se ejecuta de forma secuencial, se verifica por bloques, supera una revisión integral y conserva su continuidad sin alterar cambios ajenos ni producir commits. El programador decide posteriormente qué conocimientos nuevos merecen incorporarse a la memoria permanente.
