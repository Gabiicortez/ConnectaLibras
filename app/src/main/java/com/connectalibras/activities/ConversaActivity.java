package com.connectalibras.activities;

import com.connectalibras.R;
import com.connectalibras.ia.CameraAnalyzer;
import com.connectalibras.ia.local.LocalRecognizer;
import com.connectalibras.ia.local.RecognitionResult;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Size;
import android.view.Surface;
import android.widget.*;
import androidx.core.content.ContextCompat;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import java.util.concurrent.*;
import com.google.common.util.concurrent.ListenableFuture;

public class ConversaActivity extends BaseActivity {
 private TextView text,status;private Button start,changeCamera;private Spinner mode;
 private PreviewView preview;private ProcessCameraProvider provider;
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private volatile boolean running;private volatile int generation;private boolean loading,front=true;
 // Engine and lastFrame belong to worker. Sentence and UI state belong to main thread.
 private LocalRecognizer engine;private long lastFrame;private String sentence="",version="";
 private final ActivityResultLauncher<String> permission=registerForActivityResult(new ActivityResultContracts.RequestPermission(),granted->{if(granted)begin();else message("Permita a câmera para reconhecer Libras.");});
 @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_conversa);
  text=findViewById(R.id.txtTraducao);status=findViewById(R.id.txtStatusIA);start=findViewById(R.id.btnIniciarTraducao);preview=findViewById(R.id.cameraPreview);mode=findViewById(R.id.spinnerModoIA);changeCamera=findViewById(R.id.btnTrocarCamera);
  mode.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Automático","Letras e números","Gestos e palavras"}));
  findViewById(R.id.btnVoltar).setOnClickListener(v->finish());
  changeCamera.setOnClickListener(v->{if(running||loading)return;front=!front;status.setText(front?"Câmera frontal selecionada":"Câmera traseira selecionada");});
  start.setEnabled(false);requireUser(()->{start.setEnabled(true);status.setText("Sua IA Libras V6.1 • pronta para iniciar");});
  start.setOnClickListener(v->{if(running||loading){stop();return;}if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)permission.launch(Manifest.permission.CAMERA);else begin();});
 }
 private void begin(){
  if(running||loading||current==null||isFinishing()||worker.isShutdown())return;
  final int gen=++generation;final boolean useFront=front;final String selected=new String[]{"AUTO","ESTATICO","DINAMICO"}[mode.getSelectedItemPosition()];
  loading=true;start.setText("Cancelar");mode.setEnabled(false);changeCamera.setEnabled(false);status.setText("Carregando seu arquivo Python…");sentence="";text.setText("Aguardando sinais…");
  worker.execute(()->{
   try{
    LocalRecognizer loaded=new LocalRecognizer(getApplicationContext());
    if(gen!=generation){loaded.close();return;}
    engine=loaded;lastFrame=0;
    runOnUiThread(()->{if(gen!=generation||isFinishing())return;loading=false;running=true;version=loaded.version;start.setText("Parar e salvar");bindCamera(gen,useFront,selected);});
   }catch(Exception|LinkageError e){runOnUiThread(()->{if(gen!=generation)return;stop();dialog("Não foi possível iniciar a IA",e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());});}
  });
 }
 private void bindCamera(int gen,boolean useFront,String selected){
  ListenableFuture<ProcessCameraProvider> future=ProcessCameraProvider.getInstance(this);
  future.addListener(()->{
   if(!running||gen!=generation)return;
   try{
    provider=future.get();provider.unbindAll();CameraSelector selector=useFront?CameraSelector.DEFAULT_FRONT_CAMERA:CameraSelector.DEFAULT_BACK_CAMERA;
    if(!provider.hasCamera(selector))throw new IllegalStateException("Esta câmera não está disponível. Troque para a outra câmera.");
    int rotation=preview.getDisplay()!=null?preview.getDisplay().getRotation():Surface.ROTATION_0;
    Preview usePreview=new Preview.Builder().setTargetRotation(rotation).build();usePreview.setSurfaceProvider(preview.getSurfaceProvider());
    ImageAnalysis analysis=new ImageAnalysis.Builder().setTargetResolution(new Size(640,480)).setTargetRotation(rotation)
     .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
    analysis.setAnalyzer(worker,image->{
     if(!running||gen!=generation||engine==null||SystemClock.uptimeMillis()-lastFrame<75){image.close();return;}
     lastFrame=SystemClock.uptimeMillis();Bitmap bitmap=null;
     try{
      bitmap=CameraAnalyzer.bitmap(image,useFront);long before=SystemClock.uptimeMillis();RecognitionResult result=engine.process(bitmap,lastFrame,selected);long elapsed=SystemClock.uptimeMillis()-before;
      runOnUiThread(()->{if(!running||gen!=generation||isFinishing())return;sentence=result.sentence;text.setText(sentence.isEmpty()?"Aguardando sinal confirmado…":sentence);
       String label=result.label.isEmpty()?"mantenha o sinal até confirmar":result.label+" • "+Math.round(result.confidence*100)+"%";
       status.setText(result.handPresent?"Sua IA Python • "+label+" • "+elapsed+" ms":"Sua IA Python • posicione as mãos na câmera");});
     }catch(Exception|LinkageError e){runOnUiThread(()->{if(gen!=generation)return;stop();dialog("Reconhecimento interrompido",e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());});}
     finally {image.close();if(bitmap!=null&&!bitmap.isRecycled())bitmap.recycle();}
    });
    provider.bindToLifecycle(this,selector,usePreview,analysis);status.setText("Sua IA Python • aguardando sinais");
   }catch(Exception e){stop();message("Não foi possível abrir a câmera: "+e.getMessage());}
  },ContextCompat.getMainExecutor(this));
 }
 private void stop(){
  generation++;running=false;loading=false;if(provider!=null)provider.unbindAll();
  if(start!=null){start.setEnabled(current!=null);start.setText("Iniciar Tradução");}if(mode!=null)mode.setEnabled(true);if(changeCamera!=null)changeCamera.setEnabled(true);
  if(!worker.isShutdown())worker.execute(()->{if(engine!=null){engine.close();engine=null;}});
  if(!sentence.trim().isEmpty()&&current!=null){String saved=sentence,model=version;sentence="";long owner=current.id;runTask(null,()->store.addTranslation(owner,saved,"camera",model),id->message("Tradução salva no histórico."));}
  if(status!=null)status.setText("Reconhecimento parado.");
 }
 @Override protected void onPause(){stop();super.onPause();}
 @Override protected void onDestroy(){stop();worker.shutdown();super.onDestroy();}
}
