CREATE OR REPLACE FUNCTION procesar_transaccion(
    p_wallet_origen UUID,
    p_wallet_destino UUID,
    p_monto NUMERIC(15,4),
    p_tipo VARCHAR
)
RETURNS BOOLEAN AS $$
DECLARE
    v_tipo_id SMALLINT;
    v_estado_completada_id SMALLINT;
    v_tipo_codigo VARCHAR(30);
    v_transaccion_id UUID;
    v_origen wallets%ROWTYPE;
    v_destino wallets%ROWTYPE;
    v_saldo_anterior NUMERIC(15,4);
    v_saldo_posterior NUMERIC(15,4);
BEGIN
    IF p_monto IS NULL OR p_monto <= 0.0000 THEN
        RAISE EXCEPTION 'El monto debe ser mayor a cero';
    END IF;

    v_tipo_codigo := UPPER(TRIM(p_tipo));

    SELECT id INTO v_tipo_id
    FROM cat_tipos_transaccion
    WHERE codigo = v_tipo_codigo;

    IF v_tipo_id IS NULL THEN
        RAISE EXCEPTION 'Tipo de transaccion no soportado: %', p_tipo;
    END IF;

    SELECT id INTO v_estado_completada_id
    FROM cat_estados_transaccion
    WHERE codigo = 'COMPLETADA';

    IF v_estado_completada_id IS NULL THEN
        RAISE EXCEPTION 'No existe el estado de transaccion COMPLETADA';
    END IF;

    IF v_tipo_codigo IN ('DEPOSITO', 'DESEMBOLSO_CREDITO') THEN
        IF p_wallet_destino IS NULL OR p_wallet_origen IS NOT NULL THEN
            RAISE EXCEPTION 'La transaccion % requiere solo wallet destino', v_tipo_codigo;
        END IF;
    ELSIF v_tipo_codigo IN ('RETIRO', 'PAGO_CREDITO') THEN
        IF p_wallet_origen IS NULL OR p_wallet_destino IS NOT NULL THEN
            RAISE EXCEPTION 'La transaccion % requiere solo wallet origen', v_tipo_codigo;
        END IF;
    ELSIF v_tipo_codigo = 'TRANSFERENCIA' THEN
        IF p_wallet_origen IS NULL OR p_wallet_destino IS NULL THEN
            RAISE EXCEPTION 'La transferencia requiere wallet origen y wallet destino';
        END IF;

        IF p_wallet_origen = p_wallet_destino THEN
            RAISE EXCEPTION 'La wallet origen y destino no pueden ser iguales';
        END IF;
    ELSE
        RAISE EXCEPTION 'Tipo de transaccion no implementado: %', v_tipo_codigo;
    END IF;

    PERFORM 1
    FROM wallets
    WHERE id IN (p_wallet_origen, p_wallet_destino)
    ORDER BY id
    FOR UPDATE;

    IF p_wallet_origen IS NOT NULL THEN
        SELECT * INTO v_origen
        FROM wallets
        WHERE id = p_wallet_origen;

        IF v_origen.id IS NULL THEN
            RAISE EXCEPTION 'Wallet origen no encontrada';
        END IF;

        IF v_origen.activa = FALSE THEN
            RAISE EXCEPTION 'Wallet origen inactiva';
        END IF;

        IF v_origen.saldo < p_monto THEN
            RAISE EXCEPTION 'Fondos insuficientes en wallet origen';
        END IF;
    END IF;

    IF p_wallet_destino IS NOT NULL THEN
        SELECT * INTO v_destino
        FROM wallets
        WHERE id = p_wallet_destino;

        IF v_destino.id IS NULL THEN
            RAISE EXCEPTION 'Wallet destino no encontrada';
        END IF;

        IF v_destino.activa = FALSE THEN
            RAISE EXCEPTION 'Wallet destino inactiva';
        END IF;
    END IF;

    IF p_wallet_origen IS NOT NULL AND p_wallet_destino IS NOT NULL AND v_origen.moneda <> v_destino.moneda THEN
        RAISE EXCEPTION 'Las wallets deben manejar la misma moneda';
    END IF;

    INSERT INTO transacciones (
        idempotency_key,
        tipo_transaccion_id,
        estado_transaccion_id,
        wallet_origen_id,
        wallet_destino_id,
        monto,
        moneda,
        descripcion,
        creada_por
    )
    VALUES (
        gen_random_uuid()::TEXT,
        v_tipo_id,
        v_estado_completada_id,
        p_wallet_origen,
        p_wallet_destino,
        p_monto,
        COALESCE(v_origen.moneda, v_destino.moneda),
        v_tipo_codigo,
        COALESCE(v_origen.usuario_id, v_destino.usuario_id)
    )
    RETURNING id INTO v_transaccion_id;

    IF p_wallet_origen IS NOT NULL THEN
        v_saldo_anterior := v_origen.saldo;
        v_saldo_posterior := v_origen.saldo - p_monto;

        UPDATE wallets
        SET saldo = v_saldo_posterior,
            actualizada_en = NOW()
        WHERE id = p_wallet_origen;

        INSERT INTO movimientos_wallet (
            transaccion_id,
            wallet_id,
            naturaleza,
            monto,
            saldo_anterior,
            saldo_posterior
        )
        VALUES (
            v_transaccion_id,
            p_wallet_origen,
            'D',
            p_monto,
            v_saldo_anterior,
            v_saldo_posterior
        );
    END IF;

    IF p_wallet_destino IS NOT NULL THEN
        v_saldo_anterior := v_destino.saldo;
        v_saldo_posterior := v_destino.saldo + p_monto;

        UPDATE wallets
        SET saldo = v_saldo_posterior,
            actualizada_en = NOW()
        WHERE id = p_wallet_destino;

        INSERT INTO movimientos_wallet (
            transaccion_id,
            wallet_id,
            naturaleza,
            monto,
            saldo_anterior,
            saldo_posterior
        )
        VALUES (
            v_transaccion_id,
            p_wallet_destino,
            'C',
            p_monto,
            v_saldo_anterior,
            v_saldo_posterior
        );
    END IF;

    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION verificar_fondos_suficientes()
RETURNS TRIGGER AS $$
DECLARE
    v_tipo_codigo VARCHAR(30);
    v_saldo NUMERIC(15,4);
    v_activa BOOLEAN;
BEGIN
    SELECT codigo INTO v_tipo_codigo
    FROM cat_tipos_transaccion
    WHERE id = NEW.tipo_transaccion_id;

    IF v_tipo_codigo IN ('RETIRO', 'TRANSFERENCIA', 'PAGO_CREDITO') THEN
        IF NEW.wallet_origen_id IS NULL THEN
            RAISE EXCEPTION 'La transaccion % requiere wallet origen', v_tipo_codigo;
        END IF;

        SELECT saldo, activa INTO v_saldo, v_activa
        FROM wallets
        WHERE id = NEW.wallet_origen_id
        FOR UPDATE;

        IF v_saldo IS NULL THEN
            RAISE EXCEPTION 'Wallet origen no encontrada';
        END IF;

        IF v_activa = FALSE THEN
            RAISE EXCEPTION 'Wallet origen inactiva';
        END IF;

        IF v_saldo < NEW.monto THEN
            RAISE EXCEPTION 'Fondos insuficientes para procesar la transaccion';
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


DROP TRIGGER IF EXISTS trg_verificar_fondos_suficientes ON transacciones;

CREATE TRIGGER trg_verificar_fondos_suficientes
BEFORE INSERT OR UPDATE OF wallet_origen_id, tipo_transaccion_id, monto
ON transacciones
FOR EACH ROW
EXECUTE FUNCTION verificar_fondos_suficientes();


CREATE OR REPLACE FUNCTION generar_plan_pagos(p_credito_id UUID)
RETURNS VOID AS $$
DECLARE
    v_credito creditos%ROWTYPE;
    v_estado_pendiente_id SMALLINT;
    v_estado_credito_codigo VARCHAR(30);
    v_tasa_mensual NUMERIC(15,8);
    v_cuota_total NUMERIC(15,4);
    v_capital_mensual NUMERIC(15,4);
    v_interes_mensual NUMERIC(15,4);
    v_saldo_insoluto NUMERIC(15,4);
    v_numero_cuota INTEGER;
BEGIN
    SELECT * INTO v_credito
    FROM creditos
    WHERE id = p_credito_id
    FOR UPDATE;

    IF v_credito.id IS NULL THEN
        RAISE EXCEPTION 'Credito no encontrado';
    END IF;

    SELECT codigo INTO v_estado_credito_codigo
    FROM cat_estados_credito
    WHERE id = v_credito.estado_credito_id;

    IF v_estado_credito_codigo <> 'APROBADO' THEN
        RAISE EXCEPTION 'Solo se puede generar plan de pagos para creditos aprobados';
    END IF;

    IF v_credito.monto_aprobado IS NULL OR v_credito.monto_aprobado <= 0.0000 THEN
        RAISE EXCEPTION 'El credito aprobado debe tener monto aprobado mayor a cero';
    END IF;

    SELECT id INTO v_estado_pendiente_id
    FROM cat_estados_cuota
    WHERE codigo = 'PENDIENTE';

    IF v_estado_pendiente_id IS NULL THEN
        RAISE EXCEPTION 'No existe el estado de cuota PENDIENTE';
    END IF;

    IF EXISTS (SELECT 1 FROM cuotas_credito WHERE credito_id = p_credito_id) THEN
        RETURN;
    END IF;

    v_tasa_mensual := v_credito.tasa_interes_anual / 12 / 100;

    IF v_tasa_mensual = 0 THEN
        v_cuota_total := ROUND(v_credito.monto_aprobado / v_credito.plazo_meses, 4);
    ELSE
        v_cuota_total := ROUND(
            v_credito.monto_aprobado *
            (
                v_tasa_mensual * POWER(1 + v_tasa_mensual, v_credito.plazo_meses)
            ) /
            (
                POWER(1 + v_tasa_mensual, v_credito.plazo_meses) - 1
            ),
            4
        );
    END IF;

    v_saldo_insoluto := v_credito.monto_aprobado;

    FOR v_numero_cuota IN 1..v_credito.plazo_meses LOOP
        v_interes_mensual := ROUND(v_saldo_insoluto * v_tasa_mensual, 4);
        v_capital_mensual := ROUND(v_cuota_total - v_interes_mensual, 4);

        IF v_numero_cuota = v_credito.plazo_meses THEN
            v_capital_mensual := v_saldo_insoluto;
            v_cuota_total := ROUND(v_capital_mensual + v_interes_mensual, 4);
        END IF;

        INSERT INTO cuotas_credito (
            credito_id,
            estado_cuota_id,
            numero_cuota,
            fecha_vencimiento,
            monto_capital,
            monto_interes,
            monto_total
        )
        VALUES (
            p_credito_id,
            v_estado_pendiente_id,
            v_numero_cuota,
            (COALESCE(v_credito.fecha_aprobacion, NOW())::DATE + (v_numero_cuota || ' month')::INTERVAL)::DATE,
            v_capital_mensual,
            v_interes_mensual,
            v_cuota_total
        );

        v_saldo_insoluto := ROUND(v_saldo_insoluto - v_capital_mensual, 4);
    END LOOP;
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION generar_plan_pagos_al_aprobar()
RETURNS TRIGGER AS $$
DECLARE
    v_estado_nuevo VARCHAR(30);
    v_estado_anterior VARCHAR(30);
BEGIN
    SELECT codigo INTO v_estado_nuevo
    FROM cat_estados_credito
    WHERE id = NEW.estado_credito_id;

    IF OLD.estado_credito_id IS NOT NULL THEN
        SELECT codigo INTO v_estado_anterior
        FROM cat_estados_credito
        WHERE id = OLD.estado_credito_id;
    END IF;

    IF v_estado_nuevo = 'APROBADO' AND COALESCE(v_estado_anterior, '') <> 'APROBADO' THEN
        PERFORM generar_plan_pagos(NEW.id);
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


DROP TRIGGER IF EXISTS trg_generar_plan_pagos_al_aprobar ON creditos;

CREATE TRIGGER trg_generar_plan_pagos_al_aprobar
AFTER UPDATE OF estado_credito_id
ON creditos
FOR EACH ROW
EXECUTE FUNCTION generar_plan_pagos_al_aprobar();
