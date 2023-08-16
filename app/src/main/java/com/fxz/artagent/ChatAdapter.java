package com.fxz.artagent;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
    private List<MessageBean> messages;
    private static OnButtonClickListener onButtonClickListener;

    public ChatAdapter(List<MessageBean> messages) {
        this.messages = messages;
    }

    public void setOnclickTheme(OnButtonClickListener onButtonClickListener) {
        this.onButtonClickListener = onButtonClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes;
        switch (viewType) {
            case 0:
                layoutRes = R.layout.item_chat_message_image_left;
                break;
            case 1:
                layoutRes = R.layout.item_chat_message_right;
                break;
            default:
                layoutRes = R.layout.item_chat_message_left;
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new ViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MessageBean message = messages.get(position);
        if (message.isImage()) {
            Glide.with(holder.imageView)
                    .load(message.getMessage())
                    .into(holder.imageView);
            if(message.isUser()) {
                holder.textMessage.setVisibility(View.GONE);
            }
            holder.imageView.setVisibility(View.VISIBLE);
        } else {
            holder.textMessage.setText(message.getMessage());
            holder.textMessage.setVisibility(View.VISIBLE);
            if(message.isUser()) {
                holder.imageView.setVisibility(View.GONE);
            } else {
                if (holder.themesContainer != null) {
                    holder.themesContainer.setVisibility(message.hasThemes() ? View.VISIBLE : View.GONE);
                }
            }
        }
        if (message.hasThemes() && holder.themesContainer != null) {
            List<String> themes = message.getThemes();
            if (themes.size() > 0) holder.theme1.setText(themes.get(0));
            if (themes.size() > 1) holder.theme2.setText(themes.get(1));
            if (themes.size() > 2) holder.theme3.setText(themes.get(2));
            holder.themesContainer.setVisibility(View.VISIBLE);
        } else if (holder.themesContainer != null) {
            holder.themesContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemViewType(int position) {
        MessageBean messageBean = messages.get(position);
        if (messageBean.isUser()) {
            return 1;  // 右边消息
        } else {
            if (messageBean.isImage()) {
                return 0;  // 左边图片
            } else {
                return 2;  // 左边文字
            }
        }
    }

    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage;
        ImageView imageView;
        LinearLayout themesContainer;
        TextView theme1, theme2, theme3;

        ViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textMessage);
            themesContainer = itemView.findViewById(R.id.themesContainer);

            switch (viewType) {
                case 0:
                    imageView = itemView.findViewById(R.id.iv_left_image);
                    break;
                case 1:
                    imageView = itemView.findViewById(R.id.iv_right_image);
                    break;
                case 2:
                    theme1 = itemView.findViewById(R.id.theme1);
                    theme2 = itemView.findViewById(R.id.theme2);
                    theme3 = itemView.findViewById(R.id.theme3);
                    theme1.setOnClickListener(view -> onButtonClickListener.onButtonClick(1));
                    theme2.setOnClickListener(view -> onButtonClickListener.onButtonClick(2));
                    theme3.setOnClickListener(view -> onButtonClickListener.onButtonClick(3));
                    break;
            }
        }
    }

    public interface OnButtonClickListener {
        void onButtonClick(int position);
    }
}