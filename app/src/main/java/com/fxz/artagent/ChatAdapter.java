package com.fxz.artagent;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
    private List<MessageBean> messages;

    public ChatAdapter(List<MessageBean> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = (viewType == 1) ? R.layout.item_chat_message_right : R.layout.item_chat_message_left;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new ViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MessageBean message = messages.get(position);
//        holder.textMessage.setText(message.getMessage());
        if (message.isImage()) {
            Glide.with(holder.imageView)
                    .load(message.getMessage())
                    .into(holder.imageView);
            holder.textMessage.setVisibility(View.GONE);
            holder.imageView.setVisibility(View.VISIBLE);
        } else {
            holder.textMessage.setText(message.getMessage());
            holder.imageView.setVisibility(View.GONE);
            holder.textMessage.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemViewType(int position) {
        MessageBean messageBean = messages.get(position);
        if (messageBean.isUser()) {
            return 1;
        }
        return 0;
    }


    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage;
        ImageView imageView;

        ViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textMessage);
            imageView = itemView.findViewById(viewType == 1 ? R.id.iv_right_image : R.id.iv_left_image);
        }
    }
}