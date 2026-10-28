---
name: revision-pr-autorizacion
description: 'Revisa localmente un pull request mediante MCP buscando defectos y riesgos de autorizacion de acceso a recursos. Usar para una revision de seguridad de PR con evidencia del diff y referencias fijadas por SHA, sin publicar una review.'
argument-hint: 'URL del PR y, opcionalmente, ruta para guardar la revision local'
---

# Revision local de autorizacion en un PR

## Alcance y criterio

Revisa exclusivamente la autorizacion de acceso a recursos: los cambios no deben permitir que un actor acceda o modifique un recurso sin la comprobacion de permisos, propiedad o aislamiento entre tenants que corresponda. Considera tambien eliminaciones o cambios en controles existentes. No revises otros aspectos de seguridad, pruebas en general ni compatibilidad. No supongas que una comprobacion falta sin verificar el contexto necesario.

## Entrada

- URL del PR. Si falta, solicitala antes de continuar.
- Ruta opcional para el informe local. Por defecto usa `revision-pr-<numero>.md` en la raiz del workspace. Si ya existe, no lo sobrescribas: solicita otra ruta o usa un nombre disponible.

## Procedimiento

1. Utiliza las herramientas MCP disponibles para consultar el PR y su diff. Obtiene la URL, el repositorio, el numero, los nombres y los SHA completos de las referencias base y cabeza. No uses el checkout local como sustituto del PR o del diff consultados mediante MCP.
2. Recupera el diff completo correspondiente a esos SHA, incluyendo todas las paginas. Comprueba que las respuestas identifican la misma base y cabeza; si el PR cambia durante la consulta, vuelve a obtener sus referencias y el diff hasta tener una version consistente. No mezcles datos de versiones distintas.
3. Examina los cambios relevantes para el criterio, incluidos controles eliminados y rutas de acceso a recursos. Para confirmar un hallazgo, consulta mediante MCP el contexto adicional necesario en el SHA de cabeza; cuando sea relevante para una eliminacion o comparacion, consulta tambien el SHA de base. No uses el contenido actual de una rama como si fuera el de un SHA fijado.
4. Ancla cada observacion a una linea o fragmento concreto del diff: archivo, lado base o cabeza, numero de linea y fragmento breve. Incluye un enlace directo a la linea del diff si MCP lo proporciona; de lo contrario, proporciona la URL del PR, los SHA y los datos de localizacion anteriores. Explica la relacion entre esa evidencia y el posible acceso no autorizado.
5. Clasifica cada observacion como **defecto demostrado** solo si el diff y el contexto fijado acreditan la condicion y sus consecuencias; en caso contrario, como **riesgo por confirmar**. Indica una prueba o comprobacion concreta que reproduzca o descarte cada observacion, con actor, recurso y resultado esperado cuando corresponda. Registra como ejecutada una prueba solo si realmente se ejecuto, con su comando y resultado; de lo contrario, marcala como pendiente.
6. Escribe el informe local con el formato siguiente. Si no hay observaciones, indica expresamente que no se encontraron y describe que controles y cambios se comprobaron. Indica tambien el contexto o las comprobaciones que no pudieron revisarse.
7. No publiques comentarios, revisiones ni aprobaciones en el PR. El informe no es una aprobacion ni una evaluacion general del PR.

Si MCP no esta disponible, el PR o el diff no son accesibles, falta alguna pagina, no pueden fijarse o relacionarse los SHA con el diff, o no puede obtenerse el contexto necesario, explica la limitacion en el informe y no presentes la revision como completa. No inventes evidencia ni resultados; para contexto faltante, registra solo riesgos sustentados en el diff y especifica que falta confirmar.

## Formato del informe

```markdown
# Revision local de autorizacion

- PR: <URL>
- Repositorio y numero: <repositorio>, #<numero>
- Base: <nombre de referencia> @ <SHA completo>
- Cabeza: <nombre de referencia> @ <SHA completo>
- Criterio: autorizacion de acceso y modificacion de recursos.
- Cobertura y limitaciones: <archivos o cambios revisados; diff completo o incompleto; contexto no disponible>

## Observaciones

### <titulo> — <defecto demostrado | riesgo por confirmar>
- Evidencia del diff: <archivo>, <base/cabeza>, linea <numero>, `<fragmento>`; <enlace directo si disponible>.
- Analisis: <condicion, actor, recurso, consecuencia y contexto fijado pertinente>.
- Confirmacion: <prueba o comprobacion concreta y resultado esperado>.
- Estado de la prueba: <pendiente | ejecutada: comando y resultado>.

## Comprobaciones realizadas

<Controles y cambios examinados, incluso si no hay observaciones; pruebas ejecutadas o pendientes.>

Revision local limitada al criterio indicado; no constituye una aprobacion del PR.
```



Opción 2
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
---
name: Revisor de PR
description: "Revisa localmente un pull request solo por compatibilidad de contratos HTTP. Úsalo cuando se pida comprobar si un PR rompe rutas o formatos de API existentes; requiere URL del PR o owner/repo#número y acceso de lectura al GitHub MCP."
user-invocable: true
---

# Revisión local de compatibilidad de un PR

## Alcance y criterio

Revisa exclusivamente la **compatibilidad hacia atrás de los contratos HTTP públicos que el PR modifica**. El criterio es: un cliente que usaba el contrato en la base debe poder seguir llamando la misma ruta y método, con los mismos requisitos de autenticación, formato de solicitud, códigos de estado y estructura/formato de respuesta, sin cambios en el cliente.

No evalúes seguridad, calidad ni suficiencia de pruebas, estilo, rendimiento o conveniencia del cambio. No conviertas una sospecha de esas áreas en un hallazgo de compatibilidad. Si la compatibilidad depende de evidencia que no está disponible, registra un riesgo que requiera confirmación, no un defecto demostrado.

## Procedimiento

1. Obtén la URL del PR o `owner/repo#número`. Si falta o no identifica inequívocamente un PR, solicítalo; no adivines repositorio ni número.
2. Usa el GitHub MCP en modo de lectura. Llama a `github-mcp-server-pull_request_read` con `method: "get"` y registra el enlace, repositorio, ramas y los SHA completos de `base` y `head`. Estos dos SHA definen la versión revisada; no uses nombres de rama como sustituto.
3. Obtén la lista de archivos y el parche con `github-mcp-server-pull_request_read` (`method: "get_files"` y `method: "get_diff"`). Limita la inspección a cambios que afecten el criterio HTTP. Conserva rutas, números de línea del parche y fragmentos añadidos/eliminados como evidencia.
4. Comprueba estabilidad de la instantánea: vuelve a solicitar los datos del PR y confirma que ambos SHA siguen iguales a los anotados antes de usar el diff. Si alguno cambió, descarta ese diff y vuelve a obtener los datos y el parche para los SHA nuevos. Si no puedes establecer una instantánea estable, detén la revisión y explica el bloqueo; no presentes observaciones como si correspondieran a una versión fija.
5. Compara el contrato de la base con el cambio en `head`: ruta, método, autenticación requerida, esquema y formato de solicitud/respuesta, y códigos de estado. Cuando haga falta contexto, recupéralo con `github-mcp-server-get_file_contents`, pasando explícitamente `sha` igual al SHA de `base` o `head` pertinente. No uses archivos locales, ramas móviles ni versiones distintas como evidencia de esa comparación.
6. Sigue el uso del contrato solo hasta donde sea necesario para demostrar compatibilidad. En este workspace, respeta `AGENTS.md`: identifica el componente cubierto por el PR y no leas otros componentes salvo que el alcance se amplíe. Si no se puede comprobar la relación con un consumidor sin ampliar el alcance, clasifica el punto como riesgo pendiente de confirmación.
7. Emite una observación solo si está sustentada por el diff revisado. Distingue:
   - **Defecto demostrado**: el diff rompe un contrato de base identificable y la incompatibilidad queda establecida con la evidencia disponible.
   - **Riesgo que requiere confirmación**: hay una posibilidad concreta de ruptura, pero falta identificar/verificar un consumidor, una regla contractual o una condición de ejecución.
   
   Para cada observación, especifica el cambio exacto, su impacto y una prueba o comprobación concreta que podría confirmarla. No afirmes que una comprobación o prueba pasó si no la ejecutaste. Si la ejecutaste, informa el comando exacto y el resultado real.
8. Presenta el informe en la conversación como revisión local. No uses herramientas MCP para crear una revisión, comentario, aprobación, etiqueta o cambio en el PR. No describas el informe como aprobación del PR.

## Formato de entrega

Incluye todos estos apartados, incluso si no hay hallazgos:

### Identificación
- PR: enlace y `owner/repo#número`
- Base: rama y SHA completo
- Head: rama y SHA completo
- Instantánea: confirma si los SHA se mantuvieron estables durante la lectura

### Criterio y comprobaciones
- Criterio único de compatibilidad aplicado.
- Contratos y archivos del diff inspeccionados.
- Aspectos comprobados: rutas/métodos, autenticación, solicitud, respuesta y códigos de estado, indicando cuáles aplicaban.
- Pruebas ejecutadas, con comando y resultado, o explícitamente: “No ejecuté pruebas”.

### Observaciones
Para cada observación indica:
- Clasificación: **Defecto demostrado** o **Riesgo que requiere confirmación**.
- Ubicación: ruta y línea(s) del diff; en eliminaciones, cita el fragmento eliminado y su ubicación en el parche.
- Evidencia: fragmento breve del diff y el contrato de base afectado, con las referencias SHA correspondientes.
- Impacto de compatibilidad.
- Prueba o comprobación concreta para confirmar o descartar el punto; indica si solo se propone o si realmente se ejecutó.

Si no hay observaciones, dilo expresamente y resume qué contratos comprobaste; no infieras que el PR es seguro o compatible en todos los aspectos. Si hay evidencia insuficiente, identifica qué falta y evita convertir la ausencia de evidencia en un resultado positivo.
