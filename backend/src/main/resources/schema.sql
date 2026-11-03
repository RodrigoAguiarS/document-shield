CREATE TABLE IF NOT EXISTS documento (
 id UUID PRIMARY KEY, nome_original VARCHAR(255) NOT NULL, nome_armazenado VARCHAR(255) NOT NULL UNIQUE,
 tipo_mime VARCHAR(100) NOT NULL, extensao VARCHAR(16) NOT NULL, tamanho_bytes BIGINT NOT NULL,
 hash_sha256 VARCHAR(64) NOT NULL, caminho_armazenamento VARCHAR(1024) NOT NULL UNIQUE,
 quantidade_paginas INT, status VARCHAR(32) NOT NULL, data_upload TIMESTAMP WITH TIME ZONE NOT NULL,
 data_atualizacao TIMESTAMP WITH TIME ZONE NOT NULL, mensagem_erro VARCHAR(2000));
CREATE INDEX IF NOT EXISTS idx_documento_status ON documento(status);
CREATE INDEX IF NOT EXISTS idx_documento_hash ON documento(hash_sha256);
CREATE INDEX IF NOT EXISTS idx_documento_data ON documento(data_upload);

CREATE TABLE IF NOT EXISTS regra_deteccao (
 id UUID PRIMARY KEY, codigo VARCHAR(32) NOT NULL UNIQUE, nome VARCHAR(255) NOT NULL, descricao VARCHAR(2000) NOT NULL,
 tipo_ocorrencia VARCHAR(64) NOT NULL, nivel_risco_padrao VARCHAR(32) NOT NULL, pontuacao_base INT NOT NULL,
 parametros_json TEXT, ativa BOOLEAN NOT NULL, versao VARCHAR(32) NOT NULL,
 data_criacao TIMESTAMP WITH TIME ZONE NOT NULL, data_atualizacao TIMESTAMP WITH TIME ZONE NOT NULL);
CREATE INDEX IF NOT EXISTS idx_regra_ativa ON regra_deteccao(ativa);

CREATE TABLE IF NOT EXISTS analise_documento (
 id UUID PRIMARY KEY, documento_id UUID NOT NULL, status VARCHAR(32) NOT NULL, nivel_risco VARCHAR(32),
 pontuacao_risco INT, total_ocorrencias INT NOT NULL, total_ocorrencias_criticas INT NOT NULL,
 total_ocorrencias_altas INT NOT NULL, total_ocorrencias_medias INT NOT NULL, total_ocorrencias_baixas INT NOT NULL,
 total_regras_executadas INT NOT NULL, versao_motor VARCHAR(32) NOT NULL, data_inicio TIMESTAMP WITH TIME ZONE,
 data_fim TIMESTAMP WITH TIME ZONE, duracao_milissegundos BIGINT, resumo VARCHAR(2000), mensagem_erro VARCHAR(2000),
 CONSTRAINT fk_analise_documento FOREIGN KEY(documento_id) REFERENCES documento(id));
CREATE INDEX IF NOT EXISTS idx_analise_documento ON analise_documento(documento_id);
CREATE INDEX IF NOT EXISTS idx_analise_status ON analise_documento(status);
CREATE INDEX IF NOT EXISTS idx_analise_risco ON analise_documento(nivel_risco);
CREATE INDEX IF NOT EXISTS idx_analise_inicio ON analise_documento(data_inicio);

CREATE TABLE IF NOT EXISTS ocorrencia (
 id UUID PRIMARY KEY, analise_id UUID NOT NULL, regra_id UUID, tipo VARCHAR(64) NOT NULL, nivel_risco VARCHAR(32) NOT NULL,
 titulo VARCHAR(255) NOT NULL, descricao VARCHAR(2000) NOT NULL, conteudo_suspeito TEXT, conteudo_normalizado TEXT,
 numero_pagina INT, posicao_x REAL, posicao_y REAL, largura REAL, altura REAL, fonte VARCHAR(255), tamanho_fonte REAL,
 cor_texto VARCHAR(64), opacidade REAL, pontuacao INT NOT NULL, data_identificacao TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT fk_ocorrencia_analise FOREIGN KEY(analise_id) REFERENCES analise_documento(id),
 CONSTRAINT fk_ocorrencia_regra FOREIGN KEY(regra_id) REFERENCES regra_deteccao(id));
CREATE INDEX IF NOT EXISTS idx_ocorrencia_analise ON ocorrencia(analise_id);
CREATE INDEX IF NOT EXISTS idx_ocorrencia_regra ON ocorrencia(regra_id);
CREATE INDEX IF NOT EXISTS idx_ocorrencia_tipo ON ocorrencia(tipo);

CREATE TABLE IF NOT EXISTS evidencia (
 id UUID PRIMARY KEY, ocorrencia_id UUID NOT NULL, tipo VARCHAR(64) NOT NULL, descricao VARCHAR(2000) NOT NULL,
 valor_encontrado TEXT, valor_esperado TEXT, detalhes_tecnicos TEXT, numero_pagina INT,
 data_registro TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT fk_evidencia_ocorrencia FOREIGN KEY(ocorrencia_id) REFERENCES ocorrencia(id));
CREATE INDEX IF NOT EXISTS idx_evidencia_ocorrencia ON evidencia(ocorrencia_id);

CREATE TABLE IF NOT EXISTS execucao_regra (
 id UUID PRIMARY KEY, analise_id UUID NOT NULL, regra_id UUID NOT NULL, estado VARCHAR(32) NOT NULL,
 executada BOOLEAN NOT NULL, quantidade_ocorrencias INT NOT NULL, sucesso BOOLEAN NOT NULL,
 duracao_milissegundos BIGINT NOT NULL, mensagem_erro VARCHAR(2000), data_execucao TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT fk_execucao_analise FOREIGN KEY(analise_id) REFERENCES analise_documento(id),
 CONSTRAINT fk_execucao_regra FOREIGN KEY(regra_id) REFERENCES regra_deteccao(id));
CREATE INDEX IF NOT EXISTS idx_execucao_analise ON execucao_regra(analise_id);
CREATE INDEX IF NOT EXISTS idx_execucao_regra ON execucao_regra(regra_id);

CREATE TABLE IF NOT EXISTS integracao_api (
 id UUID PRIMARY KEY, key_id VARCHAR(32) NOT NULL UNIQUE, token_hash VARCHAR(64) NOT NULL,
 nome VARCHAR(255) NOT NULL, descricao VARCHAR(500), escopo VARCHAR(64) NOT NULL, ativa BOOLEAN NOT NULL,
 expira_em TIMESTAMP WITH TIME ZONE, revogada_em TIMESTAMP WITH TIME ZONE,
 ultimo_uso_em TIMESTAMP WITH TIME ZONE,
 requisicoes_por_minuto INT, criada_em TIMESTAMP WITH TIME ZONE NOT NULL);
ALTER TABLE integracao_api ADD COLUMN IF NOT EXISTS ultimo_uso_em TIMESTAMP WITH TIME ZONE;
ALTER TABLE integracao_api ADD COLUMN IF NOT EXISTS descricao VARCHAR(500);
CREATE INDEX IF NOT EXISTS idx_integracao_api_key_id ON integracao_api(key_id);

CREATE TABLE IF NOT EXISTS api_usage_aggregate (
 id UUID PRIMARY KEY, integration_id UUID, aggregation_key VARCHAR(36) NOT NULL,
 granularity VARCHAR(8) NOT NULL, period_start TIMESTAMP WITH TIME ZONE NOT NULL,
 total_requests BIGINT NOT NULL, total_success BIGINT NOT NULL,
 total_client_errors BIGINT NOT NULL, total_server_errors BIGINT NOT NULL,
 total_rate_limit BIGINT NOT NULL, total_auth_failures BIGINT NOT NULL,
 total_timeouts BIGINT NOT NULL, total_validation_errors BIGINT NOT NULL,
 total_processing_failures BIGINT NOT NULL, sum_duration_ms BIGINT NOT NULL,
 duration_count BIGINT NOT NULL, sum_processing_ms BIGINT NOT NULL,
 processing_count BIGINT NOT NULL, bucket_100 BIGINT NOT NULL, bucket_250 BIGINT NOT NULL,
 bucket_500 BIGINT NOT NULL, bucket_1000 BIGINT NOT NULL, bucket_2500 BIGINT NOT NULL,
 bucket_5000 BIGINT NOT NULL, bucket_10000 BIGINT NOT NULL, bucket_30000 BIGINT NOT NULL,
 bucket_over_30000 BIGINT NOT NULL, max_duration_ms BIGINT NOT NULL,
 updated_at TIMESTAMP WITH TIME ZONE NOT NULL, version BIGINT NOT NULL,
 CONSTRAINT uk_api_usage_bucket UNIQUE(aggregation_key, granularity, period_start));
CREATE INDEX IF NOT EXISTS idx_api_usage_period ON api_usage_aggregate(granularity, period_start);
CREATE INDEX IF NOT EXISTS idx_api_usage_integration ON api_usage_aggregate(integration_id, granularity, period_start);

CREATE TABLE IF NOT EXISTS security_policy (
 id UUID PRIMARY KEY, policy_key UUID NOT NULL, nome VARCHAR(255) NOT NULL, descricao VARCHAR(1000),
 tipo VARCHAR(32) NOT NULL, versao INT NOT NULL, status VARCHAR(32) NOT NULL,
 data_criacao TIMESTAMP WITH TIME ZONE NOT NULL, data_atualizacao TIMESTAMP WITH TIME ZONE NOT NULL,
 data_ativacao TIMESTAMP WITH TIME ZONE, identificador_criador VARCHAR(255) NOT NULL,
 entity_version BIGINT NOT NULL, CONSTRAINT uk_security_policy_version UNIQUE(policy_key, versao));
CREATE INDEX IF NOT EXISTS idx_security_policy_key ON security_policy(policy_key, versao);
CREATE INDEX IF NOT EXISTS idx_security_policy_status ON security_policy(tipo, status);

CREATE TABLE IF NOT EXISTS security_policy_rule (
 id UUID PRIMARY KEY, politica_id UUID NOT NULL, pontuacao_minima INT NOT NULL,
 pontuacao_maxima INT NOT NULL, decisao VARCHAR(32) NOT NULL, descricao VARCHAR(1000), prioridade INT NOT NULL,
 CONSTRAINT fk_policy_rule_policy FOREIGN KEY(politica_id) REFERENCES security_policy(id),
 CONSTRAINT ck_policy_rule_min CHECK(pontuacao_minima >= 0 AND pontuacao_minima <= 100),
 CONSTRAINT ck_policy_rule_max CHECK(pontuacao_maxima >= 0 AND pontuacao_maxima <= 100),
 CONSTRAINT ck_policy_rule_range CHECK(pontuacao_minima <= pontuacao_maxima),
 CONSTRAINT uk_policy_rule_priority UNIQUE(politica_id, prioridade));
CREATE INDEX IF NOT EXISTS idx_policy_rule_policy ON security_policy_rule(politica_id, pontuacao_minima);

CREATE TABLE IF NOT EXISTS integration_policy (
 id UUID PRIMARY KEY, integracao_id UUID NOT NULL UNIQUE, politica_id UUID NOT NULL,
 data_vinculacao TIMESTAMP WITH TIME ZONE NOT NULL, data_atualizacao TIMESTAMP WITH TIME ZONE NOT NULL,
 entity_version BIGINT NOT NULL,
 CONSTRAINT fk_integration_policy_integration FOREIGN KEY(integracao_id) REFERENCES integracao_api(id),
 CONSTRAINT fk_integration_policy_policy FOREIGN KEY(politica_id) REFERENCES security_policy(id));
CREATE INDEX IF NOT EXISTS idx_integration_policy_policy ON integration_policy(politica_id);

CREATE TABLE IF NOT EXISTS policy_audit (
 id UUID PRIMARY KEY, politica_id UUID, policy_key UUID, versao INT,
 operacao VARCHAR(64) NOT NULL, administrador VARCHAR(255) NOT NULL,
 data_evento TIMESTAMP WITH TIME ZONE NOT NULL, detalhes VARCHAR(2000));
CREATE INDEX IF NOT EXISTS idx_policy_audit_policy ON policy_audit(policy_key, data_evento);
CREATE INDEX IF NOT EXISTS idx_policy_audit_date ON policy_audit(data_evento);
