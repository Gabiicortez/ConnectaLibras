package com.connectalibras.ia.local;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import com.chaquo.python.Python;
import com.chaquo.python.PyObject;
import com.chaquo.python.android.AndroidPlatform;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import org.json.JSONObject;
import java.util.List;

/** Câmera -> pontos/RGB -> arquivo Python do usuário -> texto. Chamado em um executor. */
public final class LocalRecognizer implements AutoCloseable {
 private final HandLandmarker detector;private final PyObject session;
 public final String version,name;private long stamp;private boolean closed;
 public LocalRecognizer(Context context)throws Exception{
  synchronized(Python.class){if(!Python.isStarted())Python.start(new AndroidPlatform(context.getApplicationContext()));}
  PyObject loaded=Python.getInstance().getModule("connecta_bridge").callAttr("Sessao",context.getFilesDir().getAbsolutePath());
  HandLandmarker hands=null;
  try {
   JSONObject info=new JSONObject(loaded.callAttr("info_json").toString());version=info.getString("versao");name=info.getString("nome");
   hands=HandLandmarker.createFromOptions(context,HandLandmarker.HandLandmarkerOptions.builder()
    .setBaseOptions(BaseOptions.builder().setModelAssetPath("ia/hand_landmarker.task").build())
    .setRunningMode(RunningMode.VIDEO).setNumHands(2).setMinHandDetectionConfidence(.55f)
    .setMinHandPresenceConfidence(.55f).setMinTrackingConfidence(.55f).build());
   session=loaded;detector=hands;
  }catch(Exception|LinkageError e){if(hands!=null)hands.close();try{loaded.callAttr("encerrar");}catch(Exception cleanup){Log.w("ConnectaPython","Falha ao liberar a IA",cleanup);}throw e;}
 }
 public RecognitionResult process(Bitmap bitmap,long now,String mode)throws Exception{
  if(closed)throw new IllegalStateException("IA encerrada");
  if(bitmap==null||bitmap.isRecycled())throw new IllegalArgumentException("Frame da câmera indisponível");
  // Copie o RGB enquanto o bitmap ainda pertence ao chamador. MPImage.close()
  // pode reciclar esse mesmo bitmap; nenhuma leitura dele deve ocorrer depois.
  int width=bitmap.getWidth(),height=bitmap.getHeight();int[] pixels=new int[width*height];bitmap.getPixels(pixels,0,width,0,0,width,height);byte[] rgb=new byte[pixels.length*3];
  for(int i=0;i<pixels.length;i++){int j=i*3,color=pixels[i];rgb[j]=(byte)(color>>16);rgb[j+1]=(byte)(color>>8);rgb[j+2]=(byte)color;}
  stamp=Math.max(now,stamp+1);MPImage input=new BitmapImageBuilder(bitmap).build();HandLandmarkerResult result;
  try{result=detector.detectForVideo(input,stamp);}finally{input.close();}
  float[] raw=new float[126];boolean[] used=new boolean[2];
  for(int h=0;h<Math.min(2,result.landmarks().size());h++){
   int slot=result.handedness().get(h).get(0).categoryName().equalsIgnoreCase("left")?0:1;if(used[slot])slot=1-slot;used[slot]=true;
   List<NormalizedLandmark> hand=result.landmarks().get(h);for(int i=0;i<21;i++){NormalizedLandmark p=hand.get(i);int j=slot*63+i*3;raw[j]=p.x();raw[j+1]=p.y();raw[j+2]=p.z();}
  }
  JSONObject r=new JSONObject(session.callAttr("reconhecer_json",rgb,width,height,raw,now,mode,!result.landmarks().isEmpty()).toString());
  return new RecognitionResult(r.getString("texto"),r.getString("rotulo"),(float)r.getDouble("confianca"),r.getBoolean("maos"),r.getBoolean("novo"));
 }
 @Override public void close(){if(closed)return;closed=true;try{session.callAttr("encerrar");}catch(Exception e){Log.w("ConnectaPython","Erro em encerrar() da IA",e);}finally{detector.close();}}
}
