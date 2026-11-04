package com.connectalibras.adapters;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.connectalibras.R;
import com.connectalibras.models.Conversa;
import java.util.List;
public class HistoricoAdapter extends RecyclerView.Adapter<HistoricoAdapter.Holder> {
 public interface Actions {void favorite(Conversa c);void feedback(Conversa c);}
 private final List<Conversa> rows;private final Actions actions;
 public HistoricoAdapter(List<Conversa> rows,Actions actions){this.rows=rows;this.actions=actions;}
 @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int type){return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_historico,parent,false));}
 @Override public void onBindViewHolder(@NonNull Holder h,int position){Conversa c=rows.get(position);h.date.setText(c.getDataHora()+" UTC • "+(c.getOrigem().equals("manual")?"Teste manual":"Câmera"));h.text.setText(c.getTextoTraduzido());h.favorite.setText(c.isFavorito()?"Remover favorito":"Favoritar");h.favorite.setOnClickListener(v->actions.favorite(c));h.feedback.setOnClickListener(v->actions.feedback(c));}
 @Override public int getItemCount(){return rows.size();}
 static class Holder extends RecyclerView.ViewHolder {final TextView date,text;final Button favorite,feedback;Holder(View v){super(v);date=v.findViewById(R.id.txtData);text=v.findViewById(R.id.txtMensagem);favorite=v.findViewById(R.id.btnFavoritarItem);feedback=v.findViewById(R.id.btnFeedbackItem);}}
}
