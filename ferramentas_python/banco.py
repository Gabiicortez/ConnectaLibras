"""Banco local Connecta. Python 3, somente biblioteca padrao."""
import argparse
import getpass
import hashlib
import hmac
from pathlib import Path
import secrets
import sqlite3

PASTA = Path(__file__).resolve().parent
CAMINHO_BANCO = PASTA / 'connecta.db'
ITERACOES = 600_000

def gerar_hash(senha):
    salt = secrets.token_hex(16)
    digest = hashlib.pbkdf2_hmac('sha256', senha.encode('utf-8'), bytes.fromhex(salt), ITERACOES)
    return f'pbkdf2_sha256${ITERACOES}${salt}${digest.hex()}'

def verificar_senha(senha, armazenado):
    try:
        algoritmo, rodadas, salt, esperado = armazenado.split('$')
        if algoritmo != 'pbkdf2_sha256':
            return False
        atual = hashlib.pbkdf2_hmac('sha256', senha.encode('utf-8'), bytes.fromhex(salt), int(rodadas))
        return hmac.compare_digest(atual.hex(), esperado)
    except (ValueError, TypeError):
        return False

def conectar(caminho=None):
    conexao = sqlite3.connect(CAMINHO_BANCO if caminho is None else caminho, timeout=10)
    conexao.row_factory = sqlite3.Row
    conexao.execute('PRAGMA foreign_keys = ON')
    return conexao

def inicializar():
    with conectar() as con:
        con.executescript((PASTA / 'schema.sql').read_text(encoding='utf-8'))
        # Somente um banco vazio recebe a conta inicial. Nao redefine dados existentes.
        if con.execute('SELECT COUNT(*) FROM usuarios').fetchone()[0] == 0:
            con.execute('INSERT INTO usuarios(nome_usuario,email,senha_hash,papel,nome_completo) VALUES(?,?,?,?,?)',
                        ('admin', 'robertowfs1@gmail.com', gerar_hash('1234'), 'moderador', 'admin'))
    return CAMINHO_BANCO

def autenticar(login, senha):
    with conectar() as con:
        usuario = con.execute('SELECT * FROM usuarios WHERE (nome_usuario = ? OR email = ?) AND ativo = 1',
                              (login.strip(), login.strip())).fetchone()
    if usuario is None or not verificar_senha(senha, usuario['senha_hash']):
        return None
    return {k: usuario[k] for k in ('id', 'nome_usuario', 'email', 'papel')}

def main():
    parser = argparse.ArgumentParser(description='Connecta - banco SQLite local')
    parser.add_argument('comando', choices=['iniciar', 'login', 'trocar-senha'], nargs='?', default='iniciar')
    args = parser.parse_args()
    inicializar()
    if args.comando == 'iniciar':
        print(f'Banco pronto: {CAMINHO_BANCO}')
        return
    login = input('Usuario ou email: ')
    usuario = autenticar(login, getpass.getpass('Senha: '))
    if not usuario:
        print('Usuario ou senha incorretos.')
        raise SystemExit(1)
    if args.comando == 'login':
        print(f"Login confirmado. ID: {usuario['id']} | Usuario: {usuario['nome_usuario']} | Papel: {usuario['papel']}")
    else:
        senha = getpass.getpass('Nova senha: ')
        if not senha or senha != getpass.getpass('Confirme a nova senha: '):
            raise SystemExit('Senha vazia ou confirmacao diferente. Nada foi alterado.')
        with conectar() as con:
            con.execute('UPDATE usuarios SET senha_hash = ? WHERE id = ?', (gerar_hash(senha), usuario['id']))
        print('Senha atualizada.')

if __name__ == '__main__':
    main()
