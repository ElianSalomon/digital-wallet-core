# digital-wallet-core

## Ejercicio 1: Modelado de Base de Datos Financiera

Este proyecto define el modelo de base de datos para una plataforma que combina wallet digital y gestion de creditos. La idea principal es cubrir los flujos basicos de una fintech: registro de usuarios, wallets, movimientos de dinero, solicitudes de credito, aprobacion, generacion de cuotas y pagos.

El script SQL principal se encuentra en `database/schema.sql`.

## Diagrama Entidad-Relacion

### Flujo general del modelo

El modelo parte de la tabla `usuarios`, donde se guarda la informacion principal del cliente, sus datos de contacto, documento, credenciales y estado dentro del sistema.

Cada usuario tiene un registro asociado en `perfiles_crediticios`. Esta separacion ayuda a no mezclar datos personales con informacion de riesgo financiero, como score, ingreso mensual, limite de credito y deuda actual.

Un usuario puede tener varias `wallets`. Cada wallet pertenece a un solo usuario y mantiene su alias, moneda, saldo y estado activo/inactivo.

La tabla `transacciones` representa las operaciones financieras del sistema: depositos, retiros, transferencias, desembolsos de credito y pagos de credito. Cada transaccion tiene un tipo y un estado tomados desde tablas de catalogo.

Las wallets se conectan con las transacciones mediante `wallet_origen_id` y `wallet_destino_id`. En un deposito se usa normalmente una wallet destino; en un retiro, una wallet origen; y en una transferencia participan ambas. Con esto se pueden representar varios tipos de movimiento sin crear una tabla distinta para cada caso.

La tabla `movimientos_wallet` guarda el impacto real de cada operacion sobre las wallets. Una transaccion puede generar uno o varios movimientos. Por ejemplo, una transferencia genera un debito en la wallet origen y un credito en la wallet destino.

Un usuario tambien puede solicitar varios `creditos`. Cada credito guarda el monto solicitado, monto aprobado, tasa de interes, plazo y fechas importantes como solicitud, aprobacion y desembolso.

El estado del credito se maneja con `cat_estados_credito`, donde se definen valores como solicitado, en revision, aprobado, rechazado, desembolsado, pagado, moroso o cancelado.

Un credito puede tener muchas `cuotas_credito`. Cada cuota representa una obligacion de pago con numero de cuota, fecha de vencimiento, capital, interes, monto total, monto pagado y estado.

El estado de cada cuota se toma desde `cat_estados_cuota`, con valores como pendiente, pagada, vencida o pago parcial.

Finalmente, `pagos_credito` relaciona cada pago con su credito, su cuota y la transaccion financiera que lo respalda. Esto permite saber de donde salio el dinero y a que cuota se aplico.

### Relaciones principales

- `usuarios` 1:1 `perfiles_crediticios`
- `cat_estados_crediticios` 1:M `perfiles_crediticios`
- `usuarios` 1:M `wallets`
- `cat_tipos_transaccion` 1:M `transacciones`
- `cat_estados_transaccion` 1:M `transacciones`
- `wallets` 1:M `transacciones` como wallet origen
- `wallets` 1:M `transacciones` como wallet destino
- `transacciones` 1:M `movimientos_wallet`
- `wallets` 1:M `movimientos_wallet`
- `usuarios` 1:M `creditos`
- `wallets` 1:M `creditos` como wallet de desembolso
- `cat_estados_credito` 1:M `creditos`
- `creditos` 1:M `cuotas_credito`
- `cat_estados_cuota` 1:M `cuotas_credito`
- `creditos` 1:M `pagos_credito`
- `cuotas_credito` 1:M `pagos_credito`
- `transacciones` 1:1 `pagos_credito`

## Justificacion Tecnica y Consideraciones de Seguridad

El modelo se trabajo en Tercera Forma Normal para evitar datos repetidos y mantener una estructura mas facil de validar. Por eso los estados y tipos se separaron en catalogos: estados crediticios, estados de credito, estados de cuotas, tipos de transaccion y estados de transaccion. Asi las tablas principales solo guardan referencias y no textos que puedan quedar inconsistentes.

Las tablas principales usan UUID generados con `gen_random_uuid()`. En una API financiera esto es mas seguro que usar IDs consecutivos, porque hace mas dificil enumerar recursos o intentar acceder a informacion de otros usuarios probando identificadores.

Para dinero se usa `NUMERIC(15,4)` en saldos, montos, intereses, limites, cuotas y pagos. No se usan tipos flotantes porque pueden generar diferencias de redondeo. En una wallet o sistema de credito, incluso diferencias pequenas pueden causar problemas en saldos, reportes o conciliaciones.

Tambien se agregaron restricciones `CHECK` para proteger reglas basicas desde la base de datos. Por ejemplo, una wallet no puede quedar con saldo negativo, las transacciones deben tener montos mayores a cero, un pago no puede superar el total de la cuota y la deuda actual no puede exceder el limite de credito.

La columna `idempotency_key` en `transacciones` tiene una restriccion `UNIQUE`. Esto ayuda a evitar operaciones duplicadas si el cliente reintenta una peticion por timeout, error de red o doble envio. Es importante porque una misma transferencia, deposito, retiro o pago no debe procesarse dos veces.

La separacion entre `transacciones` y `movimientos_wallet` deja una mejor trazabilidad. La transaccion funciona como encabezado de la operacion y los movimientos muestran como afecto a cada wallet. Esto sirve para auditoria, historial y conciliacion.

Los indices se colocaron en columnas que se van a consultar con frecuencia: wallets por usuario, transacciones por wallet y fecha, transacciones por tipo y estado, creditos por usuario y estado, cuotas por vencimiento y pagos por credito. Esto ayuda especialmente en historial de movimientos, reportes mensuales y dashboards.

Las llaves foraneas mantienen la integridad entre usuarios, wallets, transacciones, creditos, cuotas y pagos. Con esto se evitan registros huerfanos y se conserva la consistencia entre los modulos del sistema.

Tambien se incluyen fechas de creacion y actualizacion en las tablas principales para tener una auditoria basica de los cambios y del ciclo de vida de cada registro.

## Ejercicio 4: API REST Fintech

La API REST esta implementada en `backend/digital-wallet-core` con Spring Boot. La estructura del codigo sigue capas separadas: `entity`, `dto`, `mapper`, `repository`, `services`, `services.impl`, `controller`, `security`, `config` y `exception`.

### Endpoints principales

- `POST /api/auth/register`: registro de usuario.
- `POST /api/auth/login`: autenticacion y generacion de JWT.
- `POST /api/wallets`: creacion de wallet autenticada.
- `GET /api/wallets`: listado de wallets activas del usuario.
- `GET /api/wallets/{walletId}/balance`: consulta de saldo.
- `GET /api/wallets/{walletId}/history`: historial de movimientos.
- `POST /api/transactions/deposit`: deposito.
- `POST /api/transactions/withdraw`: retiro.
- `POST /api/transactions/transfer`: transferencia.
- `POST /api/credits`: solicitud de credito.
- `GET /api/credits`: creditos del usuario autenticado.
- `PATCH /api/credits/{creditoId}/approve`: aprobacion de credito, requiere rol `ADMIN`.
- `GET /api/credits/{creditoId}/payment-plan`: plan de pagos del credito.

### Seguridad y validaciones

La API usa JWT en el header `Authorization: Bearer <token>`. Los endpoints protegidos requieren autenticacion y la aprobacion de creditos requiere rol `ADMIN`.

Las entradas se validan con Bean Validation usando restricciones como `@NotBlank`, `@Email`, `@DecimalMin`, `@Min`, `@Max`, `@Past` y patrones para moneda ISO de tres letras.

El manejo de errores esta centralizado en `GlobalExceptionHandler`, devolviendo codigos HTTP semanticos como `400`, `401`, `403`, `404`, `422` y `500`.

El rate limiting se implementa con un filtro en memoria por IP, configurable con `RATE_LIMIT_REQUESTS_PER_MINUTE`.

La documentacion interactiva queda disponible en `/swagger-ui.html` y el JSON OpenAPI en `/api-docs` cuando la aplicacion esta corriendo.

La coleccion Postman esta en `postman/digital-wallet-core.postman_collection.json`.
