CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE cat_estados_crediticios (
    id SMALLSERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE cat_estados_credito (
    id SMALLSERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE cat_estados_cuota (
    id SMALLSERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE cat_tipos_transaccion (
    id SMALLSERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE cat_estados_transaccion (
    id SMALLSERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE usuarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    telefono VARCHAR(30) UNIQUE,
    documento_identidad VARCHAR(50) NOT NULL UNIQUE,
    fecha_nacimiento DATE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(30) NOT NULL DEFAULT 'USER',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE perfiles_crediticios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL UNIQUE REFERENCES usuarios(id),
    estado_crediticio_id SMALLINT NOT NULL REFERENCES cat_estados_crediticios(id),
    score_crediticio INTEGER NOT NULL,
    ingreso_mensual NUMERIC(15,4) NOT NULL,
    limite_credito NUMERIC(15,4) NOT NULL DEFAULT 0.0000,
    deuda_actual NUMERIC(15,4) NOT NULL DEFAULT 0.0000,
    evaluado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (score_crediticio BETWEEN 0 AND 1000),
    CHECK (ingreso_mensual >= 0.0000),
    CHECK (limite_credito >= 0.0000),
    CHECK (deuda_actual >= 0.0000),
    CHECK (deuda_actual <= limite_credito)
);

CREATE TABLE wallets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    alias VARCHAR(80) NOT NULL,
    moneda VARCHAR(3) NOT NULL DEFAULT 'MXN',
    saldo NUMERIC(15,4) NOT NULL DEFAULT 0.0000,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    creada_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizada_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (usuario_id, alias),
    CHECK (saldo >= 0.0000),
    CHECK (moneda = UPPER(moneda))
);

CREATE TABLE transacciones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(120) NOT NULL UNIQUE,
    tipo_transaccion_id SMALLINT NOT NULL REFERENCES cat_tipos_transaccion(id),
    estado_transaccion_id SMALLINT NOT NULL REFERENCES cat_estados_transaccion(id),
    wallet_origen_id UUID REFERENCES wallets(id),
    wallet_destino_id UUID REFERENCES wallets(id),
    monto NUMERIC(15,4) NOT NULL,
    moneda VARCHAR(3) NOT NULL DEFAULT 'MXN',
    descripcion VARCHAR(255),
    referencia_externa VARCHAR(120),
    creada_por UUID REFERENCES usuarios(id),
    creada_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizada_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (monto > 0.0000),
    CHECK (moneda = UPPER(moneda)),
    CHECK (wallet_origen_id IS NOT NULL OR wallet_destino_id IS NOT NULL),
    CHECK (wallet_origen_id IS NULL OR wallet_destino_id IS NULL OR wallet_origen_id <> wallet_destino_id)
);

CREATE TABLE movimientos_wallet (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaccion_id UUID NOT NULL REFERENCES transacciones(id),
    wallet_id UUID NOT NULL REFERENCES wallets(id),
    naturaleza VARCHAR(1) NOT NULL,
    monto NUMERIC(15,4) NOT NULL,
    saldo_anterior NUMERIC(15,4) NOT NULL,
    saldo_posterior NUMERIC(15,4) NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (transaccion_id, wallet_id, naturaleza),
    CHECK (naturaleza IN ('D', 'C')),
    CHECK (monto > 0.0000),
    CHECK (saldo_anterior >= 0.0000),
    CHECK (saldo_posterior >= 0.0000)
);

CREATE TABLE creditos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    wallet_desembolso_id UUID REFERENCES wallets(id),
    estado_credito_id SMALLINT NOT NULL REFERENCES cat_estados_credito(id),
    monto_solicitado NUMERIC(15,4) NOT NULL,
    monto_aprobado NUMERIC(15,4),
    tasa_interes_anual NUMERIC(15,4) NOT NULL,
    plazo_meses INTEGER NOT NULL,
    fecha_solicitud TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_aprobacion TIMESTAMPTZ,
    fecha_desembolso TIMESTAMPTZ,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (monto_solicitado > 0.0000),
    CHECK (monto_aprobado IS NULL OR monto_aprobado > 0.0000),
    CHECK (monto_aprobado IS NULL OR monto_aprobado <= monto_solicitado),
    CHECK (tasa_interes_anual >= 0.0000),
    CHECK (plazo_meses > 0)
);

CREATE TABLE cuotas_credito (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    credito_id UUID NOT NULL REFERENCES creditos(id),
    estado_cuota_id SMALLINT NOT NULL REFERENCES cat_estados_cuota(id),
    numero_cuota INTEGER NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    monto_capital NUMERIC(15,4) NOT NULL,
    monto_interes NUMERIC(15,4) NOT NULL,
    monto_total NUMERIC(15,4) NOT NULL,
    monto_pagado NUMERIC(15,4) NOT NULL DEFAULT 0.0000,
    pagada_en TIMESTAMPTZ,
    creada_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizada_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (credito_id, numero_cuota),
    CHECK (numero_cuota > 0),
    CHECK (monto_capital >= 0.0000),
    CHECK (monto_interes >= 0.0000),
    CHECK (monto_total > 0.0000),
    CHECK (monto_pagado >= 0.0000),
    CHECK (monto_pagado <= monto_total)
);

CREATE TABLE pagos_credito (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    credito_id UUID NOT NULL REFERENCES creditos(id),
    cuota_credito_id UUID NOT NULL REFERENCES cuotas_credito(id),
    transaccion_id UUID NOT NULL UNIQUE REFERENCES transacciones(id),
    monto NUMERIC(15,4) NOT NULL,
    pagado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (monto > 0.0000)
);

INSERT INTO cat_estados_crediticios (codigo, nombre) VALUES
('SIN_HISTORIAL', 'Sin historial'),
('BUENO', 'Bueno'),
('RIESGO_MEDIO', 'Riesgo medio'),
('RIESGO_ALTO', 'Riesgo alto'),
('BLOQUEADO', 'Bloqueado');

INSERT INTO cat_estados_credito (codigo, nombre) VALUES
('SOLICITADO', 'Solicitado'),
('EN_REVISION', 'En revision'),
('APROBADO', 'Aprobado'),
('RECHAZADO', 'Rechazado'),
('DESEMBOLSADO', 'Desembolsado'),
('PAGADO', 'Pagado'),
('MOROSO', 'Moroso'),
('CANCELADO', 'Cancelado');

INSERT INTO cat_estados_cuota (codigo, nombre) VALUES
('PENDIENTE', 'Pendiente'),
('PAGADA', 'Pagada'),
('VENCIDA', 'Vencida'),
('PAGO_PARCIAL', 'Pago parcial');

INSERT INTO cat_tipos_transaccion (codigo, nombre) VALUES
('DEPOSITO', 'Deposito'),
('RETIRO', 'Retiro'),
('TRANSFERENCIA', 'Transferencia'),
('DESEMBOLSO_CREDITO', 'Desembolso de credito'),
('PAGO_CREDITO', 'Pago de credito');

INSERT INTO cat_estados_transaccion (codigo, nombre) VALUES
('PENDIENTE', 'Pendiente'),
('COMPLETADA', 'Completada'),
('RECHAZADA', 'Rechazada'),
('REVERSADA', 'Reversada');

CREATE INDEX idx_perfiles_crediticios_estado ON perfiles_crediticios(estado_crediticio_id);
CREATE INDEX idx_wallets_usuario ON wallets(usuario_id);
CREATE INDEX idx_wallets_usuario_activa ON wallets(usuario_id, activa);
CREATE INDEX idx_transacciones_origen_fecha ON transacciones(wallet_origen_id, creada_en DESC);
CREATE INDEX idx_transacciones_destino_fecha ON transacciones(wallet_destino_id, creada_en DESC);
CREATE INDEX idx_transacciones_tipo_fecha ON transacciones(tipo_transaccion_id, creada_en DESC);
CREATE INDEX idx_transacciones_estado_fecha ON transacciones(estado_transaccion_id, creada_en DESC);
CREATE INDEX idx_movimientos_wallet_fecha ON movimientos_wallet(wallet_id, creado_en DESC);
CREATE INDEX idx_creditos_usuario_estado ON creditos(usuario_id, estado_credito_id);
CREATE INDEX idx_creditos_estado_fecha ON creditos(estado_credito_id, fecha_solicitud DESC);
CREATE INDEX idx_cuotas_credito_estado_vencimiento ON cuotas_credito(estado_cuota_id, fecha_vencimiento);
CREATE INDEX idx_cuotas_credito_credito_vencimiento ON cuotas_credito(credito_id, fecha_vencimiento);
CREATE INDEX idx_pagos_credito_credito_fecha ON pagos_credito(credito_id, pagado_en DESC);
