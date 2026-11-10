INSERT INTO pipe_clientes_unidades (cliente_codigo, cliente_nome, unidade, regiao) VALUES
    ('CLI-001', 'Mercado Norte', 'UN-SP-01', 'SUDESTE'),
    ('CLI-002', 'Loja Sul', 'UN-RS-02', 'SUL'),
    ('CLI-003', 'Atacado Centro', 'UN-GO-03', 'CENTRO-OESTE'),
    ('CLI-004', 'Farma Leste', 'UN-BA-04', 'NORDESTE')
ON CONFLICT (cliente_codigo) DO NOTHING;
