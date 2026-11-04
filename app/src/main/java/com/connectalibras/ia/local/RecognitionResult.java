package com.connectalibras.ia.local;
/** Resultado devolvido pelo arquivo Python do usuário. */
public final class RecognitionResult {
 public final String sentence,label;public final float confidence;public final boolean handPresent,added;
 public RecognitionResult(String s,String l,float c,boolean h,boolean a){sentence=s;label=l;confidence=c;handPresent=h;added=a;}
}
