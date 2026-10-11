# Agent Engine

Motor de coordinación secuencial y memoria para Codex, basado en [agent-engine-plan.md](agent-engine-plan.md).

El servidor está dividido en `core` (JAR), `api` (WAR) y `webapp` (WAR). Ambos WAR se despliegan en WildFly mediante Docker; PostgreSQL guarda recursos, auditoría, sesiones y autorizaciones. El plugin y el coordinador Python están separados del servidor.

La [arquitectura](docs/architecture.md) describe las capas. Las interfaces y sus implementaciones comparten carpeta; los records de intercambio terminan en `Record`. Spring Data resuelve el CRUD, los DAO las consultas especiales y los convertidores las transformaciones. El esquema usa tablas en singular y sin guiones bajos. La [matriz de compatibilidad](docs/compatibility-matrix.md) distingue las comprobaciones locales de la validación pendiente en Codex.

## Desplegar el servidor

Requiere Docker Compose y configuración externa del entorno: `ENGINE_ISSUER` (URL HTTPS de la API), `ENGINE_SECRETS_DIR` (directorio absoluto de secretos), `HTTPS_BIND_ADDRESS` y `HTTPS_PORT`. El directorio debe contener `spring.datasource.password`, `engine.bootstrap-token`, `tls.password` y `server.p12`, con el certificado correspondiente al dominio del entorno.

```sh
docker compose up --build -d
```

`compose.yml` define los servicios PostgreSQL y WildFly, sus redes, volúmenes y secretos. Para construir la imagen de WildFly utiliza `infra/docker/Dockerfile`, que compila el frontend y los módulos Java y despliega los dos WAR. `infra/docker/entrypoint.sh` prepara los secretos al arrancar; `infra/wildfly/configure.cli` configura HTTPS. Hay un único archivo Compose.

El panel se publica en la raíz del dominio configurado, la API en `/api/v1` y MCP en `/api/mcp`. El registro del primer administrador usa el secreto externo `engine.bootstrap-token`, con un plazo de 15 minutos desde la inicialización de la base de datos. Los usuarios posteriores requieren una invitación administrativa. La conexión MCP HTTP del plugin queda pendiente de configurar con la URL del entorno.

La compilación Java requiere JDK 21, Maven y Node.js para el panel; el Dockerfile incluye estas herramientas. El reactor genera `core/target/agent-engine-core-1.0.0-SNAPSHOT.jar`, `api/target/api.war` y `webapp/target/ROOT.war`.

## Verificar

Requiere macOS y Python 3.12 o superior. Verificado con Python 3.14.7 en macOS y Python 3.12.15 en un contenedor Linux descartable; Python 3.12 sobre macOS sigue pendiente de prueba.

```sh
python3 scripts/validate.py
```

Las pruebas utilizan repositorios temporales. La demo incluye un reinicio abrupto del daemon y termina en `LOCAL_DONE_PENDING_SYNC`, sin declarar `COMPLETED` remoto ni crear commits.

## Ejecutar el coordinador

```sh
python3 plugin/scripts/bridge.py serve
```

El estado reside en `~/.agent-engine`, fuera de los repositorios, con permisos privados. `AGENT_ENGINE_HOME` permite seleccionar otro directorio privado. El daemon se mantiene en primer plano; en esta versión no se instala un servicio ni se inicia automáticamente.

En otra terminal, registrar una tarea con un archivo JSON. El recorrido local sin servidor permite únicamente Quick. Las tareas Standard y Deep se importan desde el servidor después de su aprobación humana.

```json
{
  "operation": "create",
  "arguments": {
    "repository": "/ruta/absoluta/al/repositorio-de-prueba",
    "scope": ["message.txt"],
    "plan": "Cambiar el mensaje y verificar su contenido",
    "rules": ["Preservar modificaciones preexistentes; no crear commits"],
    "tests": [["python3", "-c", "from pathlib import Path; assert Path('message.txt').read_text() == 'After\\n'"]]
  }
}
```

```sh
python3 plugin/scripts/bridge.py request --file /ruta/a/task.json
```

Los siguientes sobres utilizan el `id` retornado como `task_id`. El [contrato local](docs/local-protocol.md) documenta preparación, edición, verificación, revisión y restauración. Seleccionar únicamente comandos de verificación conocidos, sin efectos externos ni procesos desacoplados. No incluir credenciales en planes, reglas o argumentos.

## Empaquetar e instalar para pruebas

```sh
python3 scripts/package_plugin.py --out dist/agent-engine
codex plugin marketplace add ./dist/agent-engine
codex plugin add agent-engine@agent-engine-private
```

El empaquetador exige un destino nuevo y copia el runtime Python dentro del plugin. No modifica la configuración personal de Codex. Los dos comandos `codex plugin` sí registran e instalan el plugin en el perfil del usuario; se ejecutan al decidir probarlo. Mantener el coordinador activo antes de usarlo.

Revisar y confiar los hooks mediante `/hooks` o la interfaz de Codex, y abrir una conversación de prueba. La instalación no concede confianza automáticamente. **Mientras el guard está activo, una tarea administrada o un coordinador inaccesible bloquea herramientas directas**; las operaciones administradas usan el bridge documentado. Es una política conservadora de laboratorio, destinada a repositorios de prueba.

La [matriz de compatibilidad](docs/compatibility-matrix.md) distingue instalación comprobada, pruebas locales y comportamiento del host todavía pendiente.
