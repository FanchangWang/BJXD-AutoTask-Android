package com.guyuexuan.bjxd.adapter;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.guyuexuan.bjxd.R;
import com.guyuexuan.bjxd.model.User;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private final OnUserActionListener listener;
    private List<User> userList;
    private OnStartDragListener dragListener;

    public UserAdapter(List<User> userList, OnUserActionListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    public void setOnStartDragListener(OnStartDragListener listener) {
        this.dragListener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.bind(user, position + 1, listener, dragListener);

        holder.copyButton.setOnClickListener(v -> {
            // 复制 token 到剪切板
            ClipboardManager clipboard = (ClipboardManager) holder.itemView.getContext()
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Token", user.getToken());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(holder.itemView.getContext(), "Token 已复制", Toast.LENGTH_SHORT).show();
        });

        // 更新显示昵称和添加时间
        holder.phoneText.setText(user.getMaskedPhone() + "\n" + user.getAddedTime()); // 显示两行
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public interface OnUserActionListener {
        void onDeleteUser(User user);

        void onMoveUser(int fromPosition, int toPosition);
    }

    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder holder);
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView orderText;
        private final TextView nicknameText;
        private final TextView phoneText;
        private final ImageButton deleteButton;
        private final ImageView dragHandle;
        private final ImageButton copyButton;

        private OnUserActionListener listener;
        private OnStartDragListener dragListener;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            orderText = itemView.findViewById(R.id.orderText);
            nicknameText = itemView.findViewById(R.id.nicknameText);
            phoneText = itemView.findViewById(R.id.phoneText);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            dragHandle = itemView.findViewById(R.id.dragHandle);
            copyButton = itemView.findViewById(R.id.copyButton);
        }

        public void bind(User user, int order, OnUserActionListener listener, OnStartDragListener dragListener) {
            this.listener = listener;
            this.dragListener = dragListener;

            orderText.setText(String.valueOf(order));
            nicknameText.setText(user.getNickname());
            phoneText.setText(user.getMaskedPhone() + "\n" + user.getAddedTime());

            deleteButton.setOnClickListener(v -> {
                if (this.listener != null) {
                    this.listener.onDeleteUser(user);
                }
            });

            // 设置拖动手柄的触摸监听
            dragHandle.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    if (this.dragListener != null) {
                        this.dragListener.onStartDrag(this);
                    }
                }
                return false;
            });
        }

        public void updateOrder(int order) {
            orderText.setText(String.valueOf(order));
        }
    }
}
