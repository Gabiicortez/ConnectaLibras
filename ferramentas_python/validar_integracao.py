"""Valida o banco inicial em copias temporarias, sem alterar seus dados."""
from pathlib import Path
import sqlite3
import tempfile
import shutil
import banco

ROOT = Path(__file__).resolve().parent

def deve_rejeitar(con, sql, args=()):
    try:
        con.execute(sql, args)
    except sqlite3.IntegrityError:
        return
    raise AssertionError('Regra SQL nao foi aplicada: ' + sql)

def main():
    with tempfile.TemporaryDirectory() as folder:
        path = Path(folder) / 'connecta.db'
        shutil.copy(ROOT / 'connecta.db', path)
        original = banco.CAMINHO_BANCO
        banco.CAMINHO_BANCO = path
        try:
            assert banco.autenticar('admin', '1234')['id'] == 1
            assert banco.autenticar('robertowfs1@gmail.com', '1234')['papel'] == 'moderador'
            assert banco.autenticar('admin', 'senha-errada') is None
            assert banco.autenticar("' OR 1=1 --", '1234') is None
            with banco.conectar(path) as con:
                assert con.execute('PRAGMA integrity_check').fetchone()[0] == 'ok'
                assert not con.execute('PRAGMA foreign_key_check').fetchall()
                assert con.execute('PRAGMA user_version').fetchone()[0] == 1
                hash_novo = banco.gerar_hash('novaSenha123')
                con.execute('UPDATE usuarios SET senha_hash=?,email=? WHERE id=1', (hash_novo, 'admin.novo@example.com'))
                con.execute('INSERT INTO usuarios(nome_usuario,nome_completo,email,senha_hash,tipo_usuario) VALUES(?,?,?,?,?)', ('outro','Outro Usuario','outro@example.com',banco.gerar_hash('senha123'),'libras'))
                deve_rejeitar(con,'INSERT INTO usuarios(nome_usuario,email,senha_hash) VALUES(?,?,?)',('ADMIN','diferente@example.com','hash'))
                deve_rejeitar(con,'INSERT INTO usuarios(nome_usuario,email,senha_hash) VALUES(?,?,?)',('diferente','OUTRO@example.com','hash'))
                deve_rejeitar(con,"INSERT INTO usuarios(nome_usuario,email,senha_hash,papel) VALUES('invalido','invalido@example.com','hash','root')")
                deve_rejeitar(con,"INSERT INTO traducoes(usuario_id,texto,origem) VALUES(999,'texto','manual')")
                id_traducao=con.execute('INSERT INTO traducoes(usuario_id,texto,origem) VALUES(?,?,?)',(1,'Olá, Libras','manual')).lastrowid
                con.execute('UPDATE traducoes SET favorito=1 WHERE id=? AND usuario_id=?',(id_traducao,1))
                assert con.execute('SELECT COUNT(*) FROM traducoes WHERE usuario_id=2 AND favorito=1').fetchone()[0] == 0
                assert con.execute('UPDATE traducoes SET favorito=0 WHERE id=? AND usuario_id=?',(id_traducao,2)).rowcount == 0
                con.execute('INSERT INTO feedbacks(usuario_id,traducao_id,correto,texto_corrigido) VALUES(?,?,?,?)',(1,id_traducao,0,'Texto corrigido'))
                con.execute('UPDATE usuarios SET notificacoes=1 WHERE id=1')
            banco.inicializar()  # Reabrir nao redefine a senha/email ou duplica o admin.
            assert banco.autenticar('admin','1234') is None
            assert banco.autenticar('admin.novo@example.com','novaSenha123')['id'] == 1
            with banco.conectar(path) as con:
                assert con.execute('SELECT COUNT(*) FROM usuarios').fetchone()[0] == 2
                assert con.execute('SELECT notificacoes FROM usuarios WHERE id=1').fetchone()[0] == 1
                assert con.execute('SELECT texto FROM traducoes WHERE usuario_id=1 AND favorito=1').fetchone()[0] == 'Olá, Libras'
                con.execute('DELETE FROM traducoes WHERE usuario_id=1')
                assert con.execute('SELECT COUNT(*) FROM feedbacks').fetchone()[0] == 0
        finally:
            banco.CAMINHO_BANCO = original
    print('OK: login, persistencia, troca de senha, unicidade, vinculos, favoritos por usuario, preferencias e cascata.')

if __name__ == '__main__':
    main()
