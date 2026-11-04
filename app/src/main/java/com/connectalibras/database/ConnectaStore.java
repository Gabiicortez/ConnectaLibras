package com.connectalibras.database;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.io.*;
import java.util.*;

public final class ConnectaStore {
    private static ConnectaStore instance;
    private final SQLiteDatabase db;
    public static synchronized ConnectaStore get(Context c) {
        if(instance==null) instance=new ConnectaStore(c.getApplicationContext()); return instance;
    }
    private ConnectaStore(Context c) {
        File file=c.getDatabasePath("connecta_local.db");
        if(!file.exists()) {
            File parent=file.getParentFile(); if(parent!=null && !parent.exists() && !parent.mkdirs()) throw new IllegalStateException("Não foi possível criar a pasta do banco");
            File temp=new File(file.getPath()+".tmp");
            try(InputStream in=c.getAssets().open("connecta_inicial.db"); FileOutputStream out=new FileOutputStream(temp)) {
                byte[] bytes=new byte[8192]; int n; while((n=in.read(bytes))!=-1) out.write(bytes,0,n); out.getFD().sync();
            } catch(IOException e) { temp.delete(); throw new IllegalStateException("Não foi possível preparar o banco",e); }
            if(!temp.renameTo(file)) throw new IllegalStateException("Não foi possível instalar o banco");
        }
        db=SQLiteDatabase.openDatabase(file.getPath(),null,SQLiteDatabase.OPEN_READWRITE);
        db.setForeignKeyConstraintsEnabled(true);
        if(db.getVersion()!=1) throw new IllegalStateException("Versão de banco não suportada");
    }
    public static final class User {
        public long id; public String name,email,role,fullName,type; public boolean notifications;
        User(Cursor c) { id=c.getLong(0);name=c.getString(1);email=c.getString(2);role=c.getString(3);fullName=c.getString(4);type=c.getString(5);notifications=c.getInt(6)==1; }
    }
    public User user(long id) {
        try(Cursor c=db.rawQuery("SELECT id,nome_usuario,email,papel,nome_completo,tipo_usuario,notificacoes FROM usuarios WHERE id=? AND ativo=1",new String[]{""+id})) { return c.moveToFirst()?new User(c):null; }
    }
    public User login(String login,String password) {
        long id=-1; String hash=null;
        try(Cursor c=db.rawQuery("SELECT id,senha_hash FROM usuarios WHERE (nome_usuario=? OR email=?) AND ativo=1",new String[]{login.trim(),login.trim()})) {
            if(c.moveToFirst()) {id=c.getLong(0);hash=c.getString(1);}
        }
        return hash!=null && PasswordHash.verify(password,hash)?user(id):null;
    }
    public void register(String name,String fullName,String email,String password,String type) {
        if(!name.trim().matches("[A-Za-z0-9_.-]{3,32}") || fullName.trim().isEmpty() || password.length()<6 || !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() || !(type.equals("libras")||type.equals("ouvinte"))) throw new IllegalArgumentException("Dados de cadastro inválidos");
        ContentValues v=new ContentValues();v.put("nome_usuario",name.trim());v.put("email",email.trim().toLowerCase(Locale.ROOT));v.put("senha_hash",PasswordHash.hash(password));v.put("papel","usuario");v.put("nome_completo",fullName.trim());v.put("tipo_usuario",type);db.insertOrThrow("usuarios",null,v);
    }
    public void updateUser(long id,String fullName,String email,String newPassword) {
        ContentValues v=new ContentValues();v.put("nome_completo",fullName.trim());v.put("email",email.trim().toLowerCase(Locale.ROOT));
        if(!newPassword.isEmpty()) v.put("senha_hash",PasswordHash.hash(newPassword));
        if(db.update("usuarios",v,"id=? AND ativo=1",new String[]{""+id})!=1) throw new IllegalStateException("Conta indisponível");
    }
    public long addTranslation(long user,String text,String origin,String version) {
        ContentValues v=new ContentValues();v.put("usuario_id",user);v.put("texto",text.trim());v.put("origem",origin);v.put("modelo_versao",version);return db.insertOrThrow("traducoes",null,v);
    }
    public List<String[]> history(long user) {
        List<String[]> list=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT id,texto,criado_em FROM traducoes WHERE usuario_id=? ORDER BY id DESC",new String[]{""+user})) {while(c.moveToNext()) list.add(new String[]{c.getString(0),c.getString(1),c.getString(2)});}return list;
    }
    public void clearHistory(long user) {db.delete("traducoes","usuario_id=?",new String[]{""+user});}
    public List<String[]> signs(long user,boolean favorites) {
        String sql=favorites?"SELECT s.id,s.palavra,s.descricao,1 FROM sinais s JOIN favoritos f ON f.sinal_id=s.id WHERE f.usuario_id=? ORDER BY s.palavra":"SELECT s.id,s.palavra,s.descricao,EXISTS(SELECT 1 FROM favoritos f WHERE f.sinal_id=s.id AND f.usuario_id=?) FROM sinais s WHERE s.validado=1 ORDER BY s.palavra";
        List<String[]> list=new ArrayList<>();try(Cursor c=db.rawQuery(sql,new String[]{""+user})) {while(c.moveToNext()) list.add(new String[]{c.getString(0),c.getString(1),c.getString(2),c.getString(3)});}return list;
    }
    public void favorite(long user,long sign,boolean enable) {
        if(enable) {ContentValues v=new ContentValues();v.put("usuario_id",user);v.put("sinal_id",sign);db.insertWithOnConflict("favoritos",null,v,SQLiteDatabase.CONFLICT_IGNORE);}
        else db.delete("favoritos","usuario_id=? AND sinal_id=?",new String[]{""+user,""+sign});
    }
    public void addSign(long moderator,String key,String word,String description) {
        User u=user(moderator);if(u==null || !u.role.equals("moderador")) throw new SecurityException("Acesso restrito ao moderador");
        ContentValues v=new ContentValues();v.put("chave",key.trim());v.put("palavra",word.trim());v.put("descricao",description.trim());v.put("validado",1);db.insertOrThrow("sinais",null,v);
    }
    public void feedback(long user,long translation,boolean correct,String correction) {
        try(Cursor c=db.rawQuery("SELECT id FROM traducoes WHERE id=? AND usuario_id=?",new String[]{""+translation,""+user})) {if(!c.moveToFirst()) throw new SecurityException("Tradução indisponível");}
        ContentValues v=new ContentValues();v.put("usuario_id",user);v.put("traducao_id",translation);v.put("correto",correct?1:0);v.put("texto_corrigido",correction);db.insertWithOnConflict("feedbacks",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void changePassword(long id,String old,String next) {
        User u=user(id);if(u==null || login(u.email,old)==null) throw new IllegalArgumentException("Senha atual incorreta");
        ContentValues v=new ContentValues();v.put("senha_hash",PasswordHash.hash(next));db.update("usuarios",v,"id=?",new String[]{""+id});
    }
    public void notifications(long id,boolean enabled) {ContentValues v=new ContentValues();v.put("notificacoes",enabled?1:0);db.update("usuarios",v,"id=?",new String[]{""+id});}
    public java.util.List<com.connectalibras.models.Conversa> translations(long user,boolean favoritesOnly) {
        java.util.List<com.connectalibras.models.Conversa> rows=new java.util.ArrayList<>();
        try(Cursor c=db.rawQuery("SELECT id,texto,criado_em,favorito,origem FROM traducoes WHERE usuario_id=?"+(favoritesOnly?" AND favorito=1":"")+" ORDER BY id DESC",new String[]{""+user})) {
            while(c.moveToNext()) rows.add(new com.connectalibras.models.Conversa(c.getLong(0),c.getString(1),c.getString(2),c.getInt(3)==1,c.getString(4)));
        } return rows;
    }
    public void favoriteTranslation(long user,long id,boolean enabled) {ContentValues v=new ContentValues();v.put("favorito",enabled?1:0);if(db.update("traducoes",v,"id=? AND usuario_id=?",new String[]{""+id,""+user})!=1)throw new IllegalArgumentException("Registro indisponível");}
    public java.util.List<String> members(long moderator) {
        User u=user(moderator);if(u==null || !u.role.equals("moderador"))throw new SecurityException("Acesso restrito");
        java.util.List<String> rows=new java.util.ArrayList<>();try(Cursor c=db.rawQuery("SELECT id,nome_usuario,email,papel FROM usuarios ORDER BY id",null)) {while(c.moveToNext())rows.add("#"+c.getLong(0)+" • "+c.getString(1)+" • "+c.getString(3)+"\n"+c.getString(2));}return rows;
    }
}
