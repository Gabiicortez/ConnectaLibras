package com.connectalibras.ia;
import android.graphics.*;
import androidx.camera.core.ImageProxy;
import java.nio.ByteBuffer;

/** CameraX RGBA -> bitmap orientado e espelhado; nenhuma compressão JPEG ou rede. */
public final class CameraAnalyzer {
 private CameraAnalyzer(){}
 public static Bitmap bitmap(ImageProxy image,boolean front){
  int w=image.getWidth(),h=image.getHeight();ImageProxy.PlaneProxy plane=image.getPlanes()[0];ByteBuffer pixels=plane.getBuffer().duplicate();
  Bitmap source=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
  if(plane.getPixelStride()!=4){source.recycle();throw new IllegalArgumentException("A câmera precisa fornecer RGBA8888");}
  if(plane.getRowStride()==w*4)source.copyPixelsFromBuffer(pixels);
  else {ByteBuffer packed=ByteBuffer.allocate(w*h*4);int first=pixels.position();byte[] row=new byte[w*4];for(int y=0;y<h;y++){pixels.position(first+y*plane.getRowStride());pixels.get(row);packed.put(row);}packed.rewind();source.copyPixelsFromBuffer(packed);}
  Matrix rotation=new Matrix();rotation.postRotate(image.getImageInfo().getRotationDegrees());Bitmap upright=Bitmap.createBitmap(source,0,0,w,h,rotation,true);if(upright!=source)source.recycle();
  if(front){Matrix mirror=new Matrix();mirror.setScale(-1,1);Bitmap flipped=Bitmap.createBitmap(upright,0,0,upright.getWidth(),upright.getHeight(),mirror,true);if(flipped!=upright)upright.recycle();upright=flipped;}
  Bitmap small=Bitmap.createScaledBitmap(upright,384,288,true);if(small!=upright)upright.recycle();return small;
 }
}
