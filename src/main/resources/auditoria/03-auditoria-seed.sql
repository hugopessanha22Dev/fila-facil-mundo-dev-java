INSERT INTO aud_clientes (id, nome, documento) VALUES
 (1, 'Cliente Alpha', '11111111111'),
 (2, 'Cliente Beta', '22222222222'),
 (3, 'Cliente Gama', '33333333333')
ON CONFLICT (id) DO NOTHING;

INSERT INTO aud_motoristas (id, nome, cnh) VALUES
 (1, 'Motorista Ana', 'CNH-A1'),
 (2, 'Motorista Bruno', 'CNH-B2'),
 (3, 'Motorista Carla', 'CNH-C3')
ON CONFLICT (id) DO NOTHING;

-- Entregas: algumas com ocorrencia/reembolso, uma sem ocorrencia
INSERT INTO aud_entregas (id, cliente_id, motorista_id, status, data_entrega, valor_centavos) VALUES
 (1, 1, 1, 'entregue', '2026-11-01', 10000),
 (2, 1, 2, 'atrasada', '2026-11-02', 15000),
 (3, 2, 1, 'danificada', '2026-11-03', 20000),
 (4, 2, 2, 'entregue', '2026-11-04', 8000),
 (5, 3, 3, 'incompleta', '2026-11-05', 12000),
 (6, 3, 1, 'entregue', '2026-11-06', 9000),
 (7, 1, 3, 'atrasada', '2026-11-07', 11000),
 (8, 2, 3, 'entregue', '2026-11-08', 7000)
ON CONFLICT (id) DO NOTHING;

INSERT INTO aud_ocorrencias (id, entrega_id, tipo, descricao) VALUES
 (1, 2, 'atraso', 'Cliente reportou atraso na janela'),
 (2, 3, 'avaria', 'Embalagem danificada'),
 (3, 5, 'faltante', 'Faltou 1 item do pedido'),
 (4, 7, 'atraso', 'Congestionamento na rota')
ON CONFLICT (id) DO NOTHING;
-- entrega 1,4,6,8 sem ocorrencia (LEFT JOIN)

INSERT INTO aud_reembolsos (id, entrega_id, status, motivo, valor_centavos) VALUES
 (1, 2, 'pendente', 'atraso', 5000),
 (2, 3, 'pendente', 'danificado', 20000),
 (3, 5, 'aprovado', 'incompleta', 3000),
 (4, 7, 'negado', 'atraso', 2000)
ON CONFLICT (id) DO NOTHING;

SELECT setval('aud_clientes_id_seq', (SELECT MAX(id) FROM aud_clientes));
SELECT setval('aud_motoristas_id_seq', (SELECT MAX(id) FROM aud_motoristas));
SELECT setval('aud_entregas_id_seq', (SELECT MAX(id) FROM aud_entregas));
SELECT setval('aud_ocorrencias_id_seq', (SELECT MAX(id) FROM aud_ocorrencias));
SELECT setval('aud_reembolsos_id_seq', (SELECT MAX(id) FROM aud_reembolsos));
