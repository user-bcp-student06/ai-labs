# Plan de corrección

## auth

### Aprobación humana registrada
- Anthony
- 2026/10/26

### H-AUTH-01 — Parametrizar la consulta de login

- **Hallazgo de `HALLAZGOS.md`:** credenciales de login concatenadas en la consulta SQL; severidad Alta; estado inicial Pendiente, actualmente Resuelto.
- **Problema antes de la corrección:** `AuthResource.login` pasa username y password al repositorio en `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:35`. La versión previa de `LegacyDatabase.findByCredentials` los interpolaba directamente en SQL y ejecutaba esa cadena con `Statement` (`auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:32-35`, líneas antes del cambio).
- **Cambio propuesto:** sustituir la concatenación y `Statement` por una consulta con dos marcadores y `PreparedStatement`; enlazar username y password como valores separados. Mantener el acceso a datos dentro de `LegacyDatabase.findByCredentials`, cerrar los recursos con try-with-resources y usar una constante con nombre para la consulta. No añadir dependencias.
- **Contrato/comportamiento que se conserva:** no existe `docs/ENDPOINTS.md`, por lo que se conserva el comportamiento observado en `AuthResource`: credenciales válidas responden `200` con `token`, `expiresIn` y `user` (`auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:39-49`); credenciales no coincidentes responden `401` (`AuthResource.java:36-37`); solicitud o campos obligatorios ausentes/vacíos responden `400` (`AuthResource.java:26-33`). Los errores de consulta capturados por el repositorio terminan como `null` y el recurso los traduce a `401` (`auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:46-49` y `AuthResource.java:36-37`); no cambiar ese comportamiento dentro de este alcance.
- **Archivos a tocar durante la implementación:** `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java`; `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`.
- **Caracterización antes de tocar esos archivos:**
  1. Con la cuenta ficticia sembrada `student` / `Workshop123!`, comprobar `POST /auth/login` devuelve `200` y contiene `user.username`, `token` y `expiresIn`. El caso de éxito estaba parcialmente cubierto antes del cambio; ahora se comprueba en `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:18-23`.
  2. Con credenciales ficticias no vacías que no coincidan con una cuenta, comprobar `401`.
  3. Con username o password vacío, comprobar `400`.
  4. Con un valor ficticio que contenga una comilla, comprobar que no se autentica y observar el resultado actual. Según el flujo del código, se espera `401`, incluso si la consulta produce una excepción que el repositorio captura. Esta comprobación de estado por sí sola no demuestra que la consulta esté parametrizada.
- **Verificación específica de la corrección:** además de la caracterización HTTP, revisar que el código use `PreparedStatement` y enlaces separados para ambos argumentos. Si se requiere probarlo automáticamente, diseñar una prueba con JDBC instrumentado que compruebe SQL con marcadores y valores enlazados; primero evaluar si puede hacerse con las dependencias y el seam de prueba existentes, sin agregar dependencias sin justificar.
- **Clean Code:** mantener una sola responsabilidad en el método de acceso a datos, evitar lógica duplicada o anidamiento adicional, y nombrar la consulta y cualquier índice de parámetro si se extraen como constantes. No introducir números o cadenas mágicas innecesarios.
- **Estado de pruebas:** caracterización escrita y ejecutada antes del cambio; regresión ejecutada después del cambio. Los comandos, resultados y cobertura están registrados a continuación.

#### Ejecuciones y resultado — H-AUTH-01

- **Antes del cambio SQL — caracterización:** componente `auth`, hallazgo `H-AUTH-01`; `mvn verify` con cwd `auth/`; resultado: BUILD SUCCESS, 5 tests aprobados, 0 fallidos. El login válido, credenciales desconocidas, username vacío, password vacío y username con comilla coincidieron con el comportamiento observado. JaCoCo previo: `LegacyDatabase` 96.8% de instrucciones y 90.5% de líneas.
- **Después del cambio SQL — primera regresión:** componente `auth`, hallazgo `H-AUTH-01`; `mvn verify` con cwd `auth/`; resultado: BUILD SUCCESS, 5 tests aprobados, 0 fallidos. La cobertura de líneas de la clase fue inicialmente 82.6%; se ajustó el flujo de retorno para conservar el retorno existente fuera del bloque `catch`, sin cambiar su comportamiento.
- **Después del ajuste — regresión:** componente `auth`, hallazgo `H-AUTH-01`; `mvn verify` con cwd `auth/`; resultado: BUILD SUCCESS, 5 tests aprobados, 0 fallidos. JaCoCo: `findByCredentials` 94.7% de instrucciones y 91.7% de líneas; clase `LegacyDatabase` 93.9% de instrucciones y 87.0% de líneas.
- **Verificación final tras limpiar el tipo de retorno del helper de prueba:** componente `auth`, hallazgo `H-AUTH-01`; `mvn verify` con cwd `auth/`; resultado: BUILD SUCCESS, 5 tests aprobados, 0 fallidos. JaCoCo en `auth/target/site/jacoco/index.html` conserva 91.7% de líneas para `findByCredentials` y 87.0% para `LegacyDatabase`.
- **Verificación de la corrección:** inspección de `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:17-20` y `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:38-41` confirma SQL con marcadores, `PreparedStatement` y enlaces separados. La prueba de credencial con comilla devolvió 401 antes y después; por sí sola no demuestra parametrización. No se añadió una prueba instrumental de JDBC porque el código no ofrece un seam para inyectar el driver y el plan no justifica una dependencia nueva.
- **Controles remotos:** CodeQL y Dependency Review no ejecutados en este recorrido local.
- **Desviaciones:** no hubo desviación funcional respecto al alcance aprobado. No se agregó una prueba instrumental automática de enlace de parámetros porque el repositorio no ofrece un seam JDBC y no se justificó una dependencia nueva; la prueba de comilla es de comportamiento y no sustituye la inspección del código.
