Read `SKILL.md`
Completed with input: {
  "path": "C:\\Users\\Usuario\\Documents\\S62039-anthony-alvarado-gomez\\ai-labs\\.github\\skills\\feature-intake-spec-checklist\\SKILL.md"
}

"Read" failed
Completed with input: {
  "path": "C:\\Users\\Usuario\\Documents\\S62039-anthony-alvarado-gomez\\ai-labs\\examples\\input.sample.md"
}
Tool execution failed: Path does not exist

Find files matching `**/input.sample.md`

Read 
Completed with input: {
  "path": "C:\\Users\\Usuario\\Documents\\S62039-anthony-alvarado-gomez\\ai-labs\\.github\\skills\\feature-intake-spec-checklist\\examples\\input.sample.md"
}

# Feature Intake: Confirmación previa de transferencias

## 1. Summary
- Permitir que el usuario revise y confirme una transferencia antes de enviarla, para reducir las quejas por transferencias duplicadas.
- Impacto esperado: reducir las quejas por transferencias duplicadas en un 20 % durante un mes.

## 2. Scope
- **Dentro del alcance**
  - Confirmación antes del envío.
  - Mensaje de resumen de la transferencia.
  - Validación de datos.
- **Fuera del alcance**
  - Cambios de infraestructura.
  - Rediseño completo de la interfaz.

## 3. Supuestos
- La funcionalidad se integrará con la API REST y los servicios existentes de cuentas y transferencias.
- No se asume un formato concreto para el resumen ni reglas específicas de validación; deben definirse antes de implementar esos detalles.

## 4. Decisiones técnicas por tomar (pendientes o resueltas)
- **Resueltas:** utilizar la API REST y el frontend existentes; mantener el registro estandarizado.
- **Pendientes:** definir los campos que aparecerán en el resumen y las reglas de validación de datos.
- **Pendiente:** definir cómo se medirá la cantidad de quejas por transferencias duplicadas y cuál será la referencia para comparar el cambio mensual.

## 5. Riesgos y mitigaciones
- **Exposición de datos sensibles en logs.** Mitigación: revisar los registros generados por el flujo y comprobar que no incluyan datos sensibles.
- **Errores en la validación del importe.** Mitigación: acordar las reglas de validación y probar entradas válidas e inválidas antes de liberar la funcionalidad.
- **Dependencia de los servicios de cuentas y transferencias.** Mitigación: verificar la integración con ambos servicios en las pruebas funcionales.

## 6. Criterios de aceptación (medibles)
- Una transferencia no se envía antes de que el usuario confirme explícitamente la operación.
- Antes de confirmar, se muestra un resumen de la transferencia con los datos acordados para revisión.
- Los datos que no cumplan las reglas de validación acordadas impiden el envío de la transferencia.
- Durante el mes de medición acordado, las quejas por transferencias duplicadas disminuyen al menos un 20 % frente a la referencia definida.

## 7. Plan de validación
- **Pruebas unitarias**
  - Verificar que la transferencia no se envía sin confirmación.
  - Verificar la presentación del resumen con los datos acordados.
  - Verificar el rechazo de datos inválidos según las reglas acordadas.
- **Pruebas funcionales**
  - Recorrer el flujo de revisión, confirmación y envío usando los servicios de cuentas y transferencias.
  - Comprobar el comportamiento ante datos inválidos y errores de integración.
- **Seguridad (comprobaciones mínimas)**
  - Inspeccionar los logs del flujo y comprobar que no exponen datos sensibles.
  - Verificar que los datos enviados corresponden a los revisados en el resumen.
- **Evidencia requerida (qué adjuntar)**
  - Salida de las pruebas unitarias y funcionales.
  - Checklist de revisión de logs.
  - Referencia al método de medición y a la comparación mensual de quejas.

## 8. Tareas sugeridas por rol
- **Chapter/PO**
  - Confirmar el objetivo y la métrica de quejas por transferencias duplicadas.
  - Definir la referencia y el período exactos para la comparación mensual.
  - Acordar los datos que debe incluir el resumen y las reglas de validación.
- **Tech Lead**
  - Definir la integración del flujo con la API REST existente.
  - Acordar cómo se preservará la correspondencia entre los datos revisados y los enviados.
  - Revisar el enfoque de logging y los riesgos de integración.
- **Backend**
  - Integrar el flujo de confirmación con los servicios de cuentas y transferencias.
  - Aplicar las reglas de validación acordadas antes del envío.
  - Añadir pruebas unitarias y verificar que los logs no expongan datos sensibles.
- **Frontend**
  - Implementar la revisión del resumen y la confirmación previa al envío.
  - Mostrar errores de validación sin enviar la transferencia.
- **QA**
  - Preparar casos para envío confirmado, ausencia de confirmación y datos inválidos.
  - Verificar funcionalmente las integraciones con cuentas y transferencias.
  - Adjuntar resultados de pruebas y checklist de revisión de logs.
- **Cyber**
  - Revisar que el flujo y sus logs no expongan datos sensibles.
  - Comprobar que los datos enviados correspondan a los revisados y confirmados.

## 9. Preguntas abiertas
- ¿Qué campos debe mostrar el resumen antes de confirmar?
- ¿Cuáles son las reglas exactas de validación del importe y de los demás datos?
- ¿Cómo se define y obtiene la referencia para medir las quejas por transferencias duplicadas?
- ¿Qué comportamiento se espera si falla el servicio de cuentas o el de transferencias?