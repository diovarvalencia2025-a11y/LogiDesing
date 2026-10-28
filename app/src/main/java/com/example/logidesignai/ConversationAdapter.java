package com.example.logidesignai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    public interface OnConversationClickListener {
        void onConversationClick(ConversationItem item);
        void onConversationLongClick(ConversationItem item);
    }

    private final List<ConversationItem> conversationList = new ArrayList<>();
    private final List<ConversationItem> conversationListFull = new ArrayList<>();
    private final OnConversationClickListener listener;

    public ConversationAdapter(List<ConversationItem> list, OnConversationClickListener listener) {
        this.listener = listener;
        actualizarDatos(list);
    }

    public void actualizarDatos(List<ConversationItem> nuevaLista) {
        conversationList.clear();
        conversationListFull.clear();
        if (nuevaLista != null) {
            conversationList.addAll(nuevaLista);
            conversationListFull.addAll(nuevaLista);
        }
        notifyDataSetChanged();
    }

    public void filtrar(String texto) {
        conversationList.clear();
        if (texto == null || texto.trim().isEmpty()) {
            conversationList.addAll(conversationListFull);
        } else {
            String query = texto.toLowerCase().trim();
            for (ConversationItem item : conversationListFull) {
                if (item.getTitle().toLowerCase().contains(query) ||
                        (item.getNiche() != null && item.getNiche().toLowerCase().contains(query))) {
                    conversationList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConversationItem item = conversationList.get(position);
        holder.tvConversationTitle.setText(item.getTitle());
        holder.tvConversationTime.setText(item.getFormattedRelativeTime());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConversationClick(item);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onConversationLongClick(item);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return conversationList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvConversationTitle;
        TextView tvConversationTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvConversationTitle = itemView.findViewById(R.id.tvConversationTitle);
            tvConversationTime = itemView.findViewById(R.id.tvConversationTime);
        }
    }
}
