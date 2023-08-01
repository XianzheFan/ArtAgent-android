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

public class ChatDrawAdapter extends RecyclerView.Adapter<ChatDrawAdapter.ViewHolder> {
    private List<MessageBean> messages;

    public ChatDrawAdapter(List<MessageBean> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = (viewType == 1) ? R.layout.item_chat_message_right : R.layout.item_chat_message_left_draw;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MessageBean message = messages.get(position);
        boolean rig = message.isRightLayout();
        if (rig) {
            holder.textMessage.setText(message.getMessage());
        } else {
            Glide.with(holder.imageView)
                    .load(message.getMessage())
                    .into(holder.imageView);
        }

    }

    @Override
    public int getItemViewType(int position) {
        MessageBean messageBean = messages.get(position);
        if (messageBean.isRightLayout()) {
            return 1;
        }
        return 0;
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage;
        ImageView imageView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textMessage);
            imageView = itemView.findViewById(R.id.iv_image);
        }
    }
}
