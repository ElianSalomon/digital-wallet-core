SELECT
    u.id AS usuario_id,
    CONCAT(u.nombre, ' ', u.apellido) AS nombre_completo,
    w.id AS wallet_id,
    w.alias,
    w.saldo,
    w.moneda
FROM usuarios u
INNER JOIN wallets w ON w.usuario_id = u.id
WHERE w.activa = TRUE
ORDER BY u.apellido, u.nombre, w.alias;


WITH transacciones_mensuales AS (
    SELECT
        DATE_TRUNC('month', t.creada_en) AS mes,
        ctt.codigo AS tipo_transaccion,
        SUM(t.monto) AS volumen_total
    FROM transacciones t
    INNER JOIN cat_tipos_transaccion ctt ON ctt.id = t.tipo_transaccion_id
    INNER JOIN cat_estados_transaccion cet ON cet.id = t.estado_transaccion_id
    WHERE cet.codigo = 'COMPLETADA'
    GROUP BY DATE_TRUNC('month', t.creada_en), ctt.codigo
),
transacciones_con_lag AS (
    SELECT
        mes,
        tipo_transaccion,
        volumen_total,
        LAG(volumen_total) OVER (
            PARTITION BY tipo_transaccion
            ORDER BY mes
        ) AS volumen_mes_anterior
    FROM transacciones_mensuales
)
SELECT
    mes,
    tipo_transaccion,
    volumen_total,
    COALESCE(volumen_mes_anterior, 0.0000) AS volumen_mes_anterior,
    CASE
        WHEN volumen_mes_anterior IS NULL OR volumen_mes_anterior = 0.0000 THEN NULL
        ELSE ROUND(((volumen_total - volumen_mes_anterior) / volumen_mes_anterior) * 100, 2)
    END AS porcentaje_crecimiento_mensual
FROM transacciones_con_lag
ORDER BY mes DESC, tipo_transaccion;


WITH creditos_por_estado AS (
    SELECT
        cec.codigo AS estado_credito,
        COUNT(c.id) AS cantidad_creditos,
        SUM(COALESCE(c.monto_aprobado, c.monto_solicitado)) AS monto_total
    FROM creditos c
    INNER JOIN cat_estados_credito cec ON cec.id = c.estado_credito_id
    GROUP BY cec.codigo
)
SELECT
    estado_credito,
    cantidad_creditos,
    monto_total,
    ROUND(
        cantidad_creditos * 100.0 / NULLIF(SUM(cantidad_creditos) OVER (), 0),
        2
    ) AS porcentaje_sobre_total_creditos,
    ROUND(
        monto_total * 100.0 / NULLIF(SUM(monto_total) OVER (), 0),
        2
    ) AS porcentaje_sobre_monto_global
FROM creditos_por_estado
ORDER BY cantidad_creditos DESC, estado_credito;


SELECT
    u.id AS usuario_id,
    CONCAT(u.nombre, ' ', u.apellido) AS nombre_completo,
    pc.score_crediticio,
    pc.ingreso_mensual,
    pc.limite_credito,
    pc.deuda_actual,
    COUNT(DISTINCT c.id) FILTER (WHERE cec.codigo IN ('APROBADO', 'DESEMBOLSADO', 'PAGADO')) AS total_creditos_aprobados,
    COUNT(cu.id) FILTER (WHERE ccu.codigo = 'PAGADA') AS cuotas_pagadas,
    COUNT(cu.id) FILTER (WHERE ccu.codigo IN ('PENDIENTE', 'PAGO_PARCIAL')) AS cuotas_pendientes,
    COALESCE(SUM(pc2.monto), 0.0000) AS total_pagado_creditos
FROM usuarios u
INNER JOIN perfiles_crediticios pc ON pc.usuario_id = u.id
INNER JOIN cat_estados_crediticios cec2 ON cec2.id = pc.estado_crediticio_id
LEFT JOIN creditos c ON c.usuario_id = u.id
LEFT JOIN cat_estados_credito cec ON cec.id = c.estado_credito_id
LEFT JOIN cuotas_credito cu ON cu.credito_id = c.id
LEFT JOIN cat_estados_cuota ccu ON ccu.id = cu.estado_cuota_id
LEFT JOIN pagos_credito pc2 ON pc2.cuota_credito_id = cu.id
WHERE pc.score_crediticio >= 700
  AND cec2.codigo IN ('EXCELENTE', 'BUENO')
  AND NOT EXISTS (
      SELECT 1
      FROM creditos c_vencido
      INNER JOIN cuotas_credito cu_vencida ON cu_vencida.credito_id = c_vencido.id
      INNER JOIN cat_estados_cuota ccu_vencida ON ccu_vencida.id = cu_vencida.estado_cuota_id
      WHERE c_vencido.usuario_id = u.id
        AND ccu_vencida.codigo = 'VENCIDA'
  )
GROUP BY
    u.id,
    u.nombre,
    u.apellido,
    pc.score_crediticio,
    pc.ingreso_mensual,
    pc.limite_credito,
    pc.deuda_actual
ORDER BY pc.score_crediticio DESC, total_creditos_aprobados DESC, total_pagado_creditos DESC;
