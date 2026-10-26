# Hallazgos

## auth

| ID | Componente | Severidad | Estado | Hallazgo | Evidencia y límites |
|---|---|---|---|---|---|
| H-AUTH-01 | auth | Alta | Resuelto | Las credenciales de `POST /auth/login` se concatenaban en la consulta SQLite en lugar de enlazarse como parámetros. | Descubrimiento antes de corregir: `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:35` pasa username y password; la versión previa de `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:32` concatenaba ambos y la línea 35 lo ejecutaba con `Statement`. Resuelto usando `PreparedStatement` con parámetros enlazados en `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:17-20` y `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:38-41`. Caracterización y regresión final: `mvn verify` desde `auth/`, 5 tests aprobados antes y después. JaCoCo final: `findByCredentials` 91.7% de líneas; `LegacyDatabase` 87.0% de líneas. La prueba con comilla valida el rechazo observable, no demuestra por sí sola el enlace de parámetros; este se verificó en el código. CodeQL y Dependency Review remotos no ejecutados. |
