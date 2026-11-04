PRAGMA foreign_keys = ON;
CREATE TABLE IF NOT EXISTS usuarios (
 id INTEGER PRIMARY KEY AUTOINCREMENT,
 nome_completo TEXT NOT NULL DEFAULT '',
 tipo_usuario TEXT NOT NULL DEFAULT 'ouvinte' CHECK(tipo_usuario IN ('libras','ouvinte')),
 notificacoes INTEGER NOT NULL DEFAULT 0 CHECK(notificacoes IN (0,1)),
 nome_usuario TEXT NOT NULL COLLATE NOCASE UNIQUE,
 email TEXT NOT NULL COLLATE NOCASE UNIQUE,
 senha_hash TEXT NOT NULL,
 papel TEXT NOT NULL DEFAULT 'usuario' CHECK(papel IN ('usuario','moderador')),
 ativo INTEGER NOT NULL DEFAULT 1 CHECK(ativo IN (0,1)),
 criado_em TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS sinais (
 id INTEGER PRIMARY KEY AUTOINCREMENT,
 chave TEXT NOT NULL UNIQUE,
 palavra TEXT NOT NULL,
 categoria TEXT NOT NULL DEFAULT 'geral',
 descricao TEXT,
 video_caminho TEXT,
 validado INTEGER NOT NULL DEFAULT 0 CHECK(validado IN (0,1))
);
CREATE TABLE IF NOT EXISTS traducoes (
 id INTEGER PRIMARY KEY AUTOINCREMENT,
 usuario_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
 favorito INTEGER NOT NULL DEFAULT 0 CHECK(favorito IN (0,1)),
 texto TEXT NOT NULL,
 origem TEXT NOT NULL CHECK(origem IN ('camera','video','manual')),
 confianca REAL CHECK(confianca BETWEEN 0 AND 1),
 modelo_versao TEXT,
 criado_em TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_traducoes_usuario ON traducoes(usuario_id, criado_em DESC);
CREATE TABLE IF NOT EXISTS favoritos (
 usuario_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
 sinal_id INTEGER NOT NULL REFERENCES sinais(id) ON DELETE CASCADE,
 criado_em TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(usuario_id,sinal_id)
);
CREATE TABLE IF NOT EXISTS feedbacks (
 id INTEGER PRIMARY KEY AUTOINCREMENT,
 usuario_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
 traducao_id INTEGER NOT NULL REFERENCES traducoes(id) ON DELETE CASCADE,
 correto INTEGER NOT NULL CHECK(correto IN (0,1)),
 texto_corrigido TEXT,
 criado_em TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(usuario_id,traducao_id)
);
CREATE TABLE IF NOT EXISTS modelos_ia (
 id INTEGER PRIMARY KEY AUTOINCREMENT,
 versao TEXT NOT NULL UNIQUE,
 arquivo_caminho TEXT,
 status TEXT NOT NULL CHECK(status IN ('treinando','validando','publicado','arquivado')),
 criado_em TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
PRAGMA user_version = 1;
