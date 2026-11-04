package com.connectalibras.models;
public class Conversa {
 private final long id; private final String textoTraduzido,dataHora,origem; private final boolean favorito;
 public Conversa(long id,String texto,String data,boolean favorito,String origem){this.id=id;textoTraduzido=texto;dataHora=data;this.favorito=favorito;this.origem=origem;}
 public long getId(){return id;} public String getTextoTraduzido(){return textoTraduzido;}
 public String getDataHora(){return dataHora;} public boolean isFavorito(){return favorito;} public String getOrigem(){return origem;}
}
