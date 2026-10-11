# Arquitectura y convenciones

## Módulos

| Carpeta | Artefacto | Responsabilidad |
| --- | --- | --- |
| `core` | `agent-engine-core-1.0.0-SNAPSHOT.jar` | Dominio, DTO, servicios, handlers de negocio, repositorios, DAO y migraciones |
| `api` | `api.war` | Controladores, handlers HTTP, filtros, configuración de seguridad HTTP y MCP; contexto `/api` |
| `webapp` | `ROOT.war` | Controladores, handlers HTTP, filtros, configuración de seguridad HTTP y panel Vue; contexto `/` |
| `plugin` | Plugin privado de Codex | Skills, hooks y bridge del coordinador |
| `coordinator` | Paquete Python | Journal local, respaldos, ejecución administrada y sincronización |
| `infra` | Configuración Docker/WildFly | HTTPS, despliegue de ambos WAR y secretos |

Los módulos Java contienen `src/main/java` y `src/main/resources`; las pruebas Java del core viven en `src/test/java`. Cada WAR incorpora el JAR de `core`; los servicios se ejecutan dentro de su aplicación Spring. Ambos comparten PostgreSQL y sesiones JDBC. El coordinador se ejecuta fuera de Docker para acceder al repositorio y al Keychain del usuario.

## Organización por funcionalidad

```text
core/src/main/java/io/agentengine/core/
├── audit/
│   ├── domain/AuditEntry.java
│   ├── record/AuditResponseRecord.java
│   ├── dataconverter/AuditDataConverter.java
│   ├── service/
│   │   ├── AuditService.java
│   │   └── AuditServiceImpl.java
│   └── repository/AuditRepository.java
├── auth/
├── common/
│   ├── domain/VersionedResource.java
│   ├── enums/ResourceKind.java
│   ├── factory/ResourceEntityFactory.java
│   ├── dataconverter/ResourceDataConverter.java
│   ├── record/
│   ├── handler/
│   │   ├── ResourceKindHandler.java
│   │   ├── ResourcePersistenceHandler.java
│   │   ├── ResourcePersistenceHandlerResolver.java
│   │   └── ResourceValidationHandler.java
│   ├── repository/dao/
│   │   ├── ResourceDAO.java
│   │   └── ResourceDAOImpl.java
│   ├── security/IdentitySecurity.java
│   ├── service/
│   └── util/
├── execution/
├── memory/
├── model/
├── profile/
├── project/
├── rule/
├── skill/
├── task/
└── user/
```

Las interfaces de servicio y DAO comparten **carpeta y paquete** con sus implementaciones. Spring Data genera las implementaciones de los repositorios JPA. No se utiliza una subcarpeta `impl`. Los records de solicitudes, respuestas y evidencia de ejecución viven en `record` y terminan en `Record`. El sufijo `DTO` queda reservado a clases DTO; los convertidores usan `toRecord` y `toRecords` para los records. Los descriptores de formulario del frontend viven en `*/config/*FormFields.js`; no son DTO. `common/dataconverter/ResourceFormDataConverter.js` transforma los valores del formulario.

Los controladores dependen de interfaces de servicios. Los servicios resuelven autorización, validación, reglas de negocio y transacciones. Usan repositorios Spring Data para CRUD y consultas habituales; reservan los DAO para búsqueda PostgreSQL, bloqueos con lectura actualizada, consumo único, revocación y secuencias atómicas. Los DAO reciben identificadores explícitos y no consultan la identidad HTTP. `AuditService` transmite su propietario a `AuditRepository` y convierte las entidades mediante `AuditDataConverter`.

## Persistencia

Las entidades usan `jakarta.persistence`, Lombok `@Getter`, `@Setter` y `@NoArgsConstructor`. Los identificadores UUID se asignan antes de persistir para conservar las referencias e identificadores estables del protocolo.

`UserAccount` y `AuditEntry` tienen columnas explícitas. Las entidades de recursos de cada funcionalidad heredan los campos comunes de `VersionedResource`: propietario, proyecto, revisión, tombstone, datos JSONB y fecha de actualización. La forma de los datos se valida con DTO y servicios. Esta estructura conserva el esquema flexible de recursos del motor; no convierte cada campo JSON en una columna independiente.

Cada funcionalidad dispone de repositorios JPA tipados, como `ProjectRepository`, `ModelPolicyRepository`, `UserRepository` y `ExecutionEventRepository`. Cada repositorio extiende directamente `JpaRepository<Entidad, UUID>` y declara consultas sencillas de su entidad. Los handlers de persistencia por funcionalidad conectan esos repositorios al servicio común; el resolver selecciona handlers sin conversiones genéricas no verificadas. `ResourceDAO` ejecuta únicamente la búsqueda PostgreSQL y las lecturas con bloqueo. Una lista sin texto de búsqueda utiliza el repositorio. `TaskDAO` conserva únicamente el consumo atómico de consentimiento y la secuencia/sincronización del protocolo. La creación de aprobaciones, consentimientos, invitaciones, eventos y evidencia utiliza repositorios.

Las clases de `dataconverter` convierten entidades a DTO y solicitudes a entidades. Las relaciones persistidas usan valores escalares, por ejemplo `UUID ownerId`, `UUID projectId` y `UUID taskId`, sin `@ManyToOne`, `@OneToMany` ni referencias a otras entidades. Los UUID conservan el contrato de identificación distribuida; los identificadores externos de proveedor se mantienen como texto.

`ResourceKind` enumera únicamente tipos de recurso. `ResourceKindHandler` resuelve sus nombres públicos y `ResourceEntityFactory` crea sus entidades. El DAO obtiene el nombre de tabla de la anotación JPA de la entidad, sin duplicar ese dato en el enum. Los handlers de persistencia conectan un recurso con su repositorio tipado; no son repositorios genéricos. El resolver se identifica como `persistenceHandlers` en los servicios.

## Nombres de base de datos

Las tablas del motor utilizan nombres juntos y en singular: `useraccount`, `project`, `profile`, `rule`, `skill`, `model`, `modeldefault`, `modelrolepolicie`, `memory`, `memoryproposal`, `task`, `taskplan`, `taskblock`, `checkpoint`, `reviewfinding`, `localclient`, `executionapproval`, `modelconsent`, `executionevent`, `executionevidence`, `checkpointsync` y `administrativeaudit`. Sus columnas siguen la misma convención: `ownerid`, `projectid`, `taskid`, `planid`, `modelid`, `createdat`, etc.

La migración `V4__domain_naming.sql` renombra las tablas y columnas existentes conservando datos, índices y claves foráneas. Las migraciones V1-V3 mantienen sus checksums. Las tablas y columnas internas de OAuth y Spring Session conservan el contrato estándar de Spring. La prueba `scripts/verify_domain_migration.py` crea el esquema antiguo en una transacción, aplica V4 y comprueba conservación de datos, búsqueda y relaciones; termina con `ROLLBACK`.

## Control de concurrencia y esquema

Las actualizaciones de recursos conservan el control de revisión y el bloqueo de fila. Flyway administra el esquema; Hibernate lo valida con `ddl-auto: validate`. `open-in-view` está desactivado y los controladores reciben DTO, evitando exponer entidades o hashes de contraseñas.

## Legibilidad

Java utiliza cuatro espacios, firmas, llamadas y expresiones largas en una línea, campos consecutivos y líneas vacías entre bloques lógicos. Las consultas SQL/JPQL usan cadenas normales en una línea y parámetros nombrados. El perfil Eclipse de `config/java-formatter.xml`, aplicado con Spotless, evita cortes automáticos con un margen de 9999 caracteres; `.editorconfig` desactiva el límite de longitud. Los POM conservan cuatro espacios de indentación.

Los métodos públicos de los DAO ejecutan las operaciones. Los métodos privados `build...Query` construyen las consultas y `build...Parameters` reúnen los parámetros; `ResourceDAOImpl` separa también el `RowMapper`. `ResourceDataConverter.toRecords` concentra la conversión por stream. Las variables locales usan `var` cuando el inicializador permite inferir el tipo; los tipos explícitos se conservan en firmas, campos, lambdas que requieren un tipo objetivo y variables inicializadas con `null`. Se utiliza `var` cuando la inicialización permite conocer el tipo y `Objects.isNull`/`Objects.nonNull` en las comprobaciones de los servicios, convertidores y seguridad. `IdentitySecurity` concentra el acceso a la identidad y las comprobaciones de autorización del core.

Los estados y niveles viven en `task/enums/TaskState.java` y `TaskLevel.java`. `task/workflow/WorkflowTransitions.java` contiene las reglas de transición, mientras que `CorrectionBudget.java` contiene los límites de corrección. Los valores del protocolo mantienen su representación previa.

Desde la raíz del reactor se puede aplicar y comprobar el formato con `mvn spotless:apply` y `mvn spotless:check`. Para ejecutar el formatter desde un módulo aislado, proporcionar `-Dagentengine.format-config=/ruta/absoluta/AgentEngine/config/java-formatter.xml`.

## Límites de las capas

```text
api/webapp: controller → handler HTTP (cuando hay adaptación) → interfaz de service
core:       ServiceImpl → handler de negocio/soporte y DataConverter → repository o DAO
```

Los repositorios contienen CRUD y filtros habituales; no seleccionan funcionalidades ni validan permisos, estados o modelos. Los DAO contienen SQL y operaciones complejas. Los WAR deshabilitan la autodetección JTA (`spring.jta.enabled: false`): el DataSource Hikari existente es local, no XA. JPA y JDBC comparten la transacción de Spring sobre ese DataSource. Los servicios establecen las transacciones de negocio; los DAO Java con operaciones especiales exigen una transacción existente mediante `Propagation.MANDATORY`. Las consultas simples pueden utilizar JPQL parametrizado dentro del repositorio cuando el filtro no se expresa cómodamente como nombre de método.

`KnowledgeService/KnowledgeServiceImpl` protege la administración de conocimiento. `TaskService/TaskServiceImpl` protege las operaciones de tareas, planes, bloques y propuestas. La autorización de escritura se valida en esos servicios, incluso si los invoca MCP u otro componente. Los handlers HTTP convierten solicitudes y construyen respuestas; no establecen transacciones.

El core utiliza `DomainException` y `DomainError` sin códigos HTTP. Cada WAR traduce esos errores en su handler `ApiProblem`. Los filtros y la configuración HTTP de seguridad están en los WAR. La autenticación de cuentas se resuelve en `AccountAuthenticationServiceImpl`. La autenticación del cliente local y la consulta de vigencia de tokens se resuelven en `LocalClientAuthenticationServiceImpl` y `AccessTokenServiceImpl`; la configuración HTTP no consulta la persistencia OAuth. El escaneo de entidades y repositorios JPA se configura en `core/common/repository/config/PersistenceConfiguration`, fuera de los WAR. La configuración de los adaptadores JDBC estándar de OAuth permanece exclusivamente en `core/auth/repository/config`; no contiene altas de clientes ni lógica HTTP.

La emisión y renovación de tokens pertenecen exclusivamente a API; webapp conserva login, sesión compartida y validación JWT. La renovación en API sigue `RefreshSerializationFilter → TokenRefreshService → RefreshTokenDAO`. El servicio conserva la transacción durante el intercambio del token; el DAO adquiere el bloqueo PostgreSQL usando un hash y un parámetro SQL. `SigningKeyHandler` encapsula la clave compartida y su bloqueo de archivo para el arranque concurrente de ambos WAR.

## Coordinador y ejecución de procesos

```text
coordinator/agent_engine_local/
├── transport/      # CLI, IPC y MCP local
├── service/        # Coordinator, Synchronizer y delegación
├── handler/        # Sistema de archivos, directorios privados y ejecución de pruebas
├── integration/    # Cliente remoto OAuth/Keychain y cliente nativo de Codex
├── repository/     # JournalRepository y SynchronizationRepository
│   └── dao/        # JournalDAO y SynchronizationDAO
└── common/         # Excepciones locales
```

El transporte IPC valida el sobre y llama al servicio. El coordinador y el sincronizador no acceden a la conexión SQLite. El repositorio resuelve lecturas sencillas; `JournalDAO` guarda checkpoint y evento con CAS en una única transacción, y `SynchronizationDAO` serializa el control de capacidad y la inserción de la cola. `ProcessExecutionHandler` ejecuta las pruebas y captura evidencia; `CodexClient` encapsula el proceso externo del modelo. El bridge y los hooks del plugin utilizan exclusivamente el transporte local.

No existe un módulo Java Batch en este repositorio. La ejecución de comandos y el procesamiento de la cola de eventos corresponden al coordinador local. Se preserva el contrato público del bridge, el journal existente y la estructura del paquete instalable.

## Verificación de arquitectura

`scripts/verify_layers.py` inspecciona todos los archivos Java de producción y todos los módulos Python del coordinador. Rechaza dependencias HTTP dentro del core, persistencia directa fuera de DAO/configuración, repositorios con lógica de selección, acceso a entidades/repositorios desde los WAR, relaciones JPA entre entidades, carpetas `impl`, records con sufijo DTO, records sin sufijo Record, responsabilidades dentro de ResourceKind y SQL fuera de persistencia en Python. `scripts/validate.py` ejecuta esa comprobación antes de las pruebas locales.

## Catálogo dinámico de modelos

Los nombres Java usan camelCase (`ownerId`, `projectId`, `createdAt`, `passwordHash`), incluidos getters, setters, métodos derivados y propiedades JPQL. Las anotaciones `@Column` conservan los nombres físicos existentes.

`ModelConfigurationService/ModelConfigurationServiceImpl` consulta los repositorios tipados del propietario. La configuración del proyecto prevalece sobre la global, incluso cuando deshabilita un modelo. Las políticas validan el modelo habilitado y sus esfuerzos configurados. Sin política, se elige por prioridad y se utiliza el primer esfuerzo configurado. Los identificadores iniciales de Sol y Luna solo son datos SQL de `modeldefault`, creados por V6; no limitan la validación ni la delegación.

`requiresVerification` exige autorización humana individual antes de iniciar una delegación, con consumo atómico de un solo uso y vencimiento. La migración V5 conserva el valor anterior de `requiresConsent`; los formularios y el contexto de tareas utilizan el catálogo dinámico. Los esfuerzos y modelos ajenos al proyecto, deshabilitados o sin configuración se rechazan.

La migración `V6__model_defaults.sql` crea `modeldefault` y sus datos iniciales. Una consulta completa los catálogos de cuentas existentes sin modificar personalizaciones ni tombstones. Un trigger copia las plantillas vigentes al crear una cuenta; `AuthServiceImpl` guarda únicamente la cuenta y no contiene modelos, esfuerzos ni un método `seed`. Los registros de catálogo de cada propietario siguen siendo configurables desde la webapp.

## Selección dinámica en plugin y MCP

Los manifiestos MCP, hooks, skills y bridge no fijan modelos ni esfuerzos. El MCP local expone contexto de solo lectura; el MCP HTTP permite consultar y administrar el catálogo dentro de los permisos del propietario. `run_role` solicita al servidor una delegación por rol y proyecto, y `CodexClient` pasa exactamente su `modelId` y `effort` al proceso. La selección se solicita de nuevo en cada delegación.

Si el permiso no incluye modelo y esfuerzo como cadenas no vacías, el cliente rechaza el inicio antes de crear el proceso. Un fallo del modelo configurado no activa sustitución local. Las pruebas de `test_codex_client.py` comprueban modelos/esfuerzos personalizados, cambios entre ejecuciones, permisos inválidos y ausencia de sustitución. El proceso Codex se simula en estas pruebas; la disponibilidad real del modelo requiere validación en el host.
