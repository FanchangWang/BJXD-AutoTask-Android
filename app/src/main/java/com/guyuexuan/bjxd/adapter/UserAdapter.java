package com.guyuexuan.bjxd.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.guyuexuan.bjxd.R;
import com.guyuexuan.bjxd.model.User;
import com.guyuexuan.bjxd.util.StorageUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private final List<User> mUserList = new ArrayList<>();
    StorageUtil mStorageUtil;
    private WeakReference<OnItemActionListener> mActionListenerRef;

    /**
     * 设置条目操作监听器
     *
     * @param listener 实现 OnItemActionListener 接口的对象，用于接收删除事件
     */
    public void setOnItemActionListener(OnItemActionListener listener) {
        mActionListenerRef = new WeakReference<>(listener);
    }

    public void setInitialData(StorageUtil storageUtil) {
        mStorageUtil = storageUtil;
        mUserList.clear();
        List<User> userList = mStorageUtil.getUserList();
        if (userList != null && !userList.isEmpty()) {
            mUserList.addAll(userList);
        }
        notifyItemRangeInserted(0, mUserList.size());
    }

    /**
     * 交换用户列表中的两个项
     *
     * @param fromPosition 第一个项的位置
     * @param toPosition   第二个项的位置
     * @return 如果交换成功则返回 true，否则返回 false
     */
    public boolean swapItems(int fromPosition, int toPosition) {
        if (fromPosition < 0 || toPosition < 0 || fromPosition >= mUserList.size() || toPosition >= mUserList.size()) {
            return false;
        }
        Collections.swap(mUserList, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        // 3. 核心：通知范围内的条目刷新序号
        // 范围是从 from 和 to 之间的所有项
        int start = Math.min(fromPosition, toPosition);
        int count = Math.abs(fromPosition - toPosition) + 1;
        notifyItemRangeChanged(start, count);
        mStorageUtil.saveUserList(mUserList);
        return true;
    }

    /**
     * 删除用户列表中的项
     *
     * @param position 待删除项的位置
     */
    public void removeItem(int position) {
        if (position < 0 || position >= mUserList.size()) {
            return;
        }
        mUserList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, mUserList.size() - position);
        mStorageUtil.saveUserList(mUserList);
    }

    public void saveItem(User user, int position) {
        if (position == -1) {
            mUserList.add(user);
            notifyItemInserted(mUserList.size() - 1);
        } else {
            mUserList.set(position, user);
            notifyItemChanged(position);
        }
    }

    public void deleteUser(int position) {
        if (position < 0 || position >= mUserList.size()) {
            return;
        }
        OnItemActionListener mActionListener = mActionListenerRef.get();
        if (mActionListener != null) {
            mActionListener.onDeleteUser(position);
        }
    }

    public void copyUserToken(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }
        OnItemActionListener mActionListener = mActionListenerRef.get();
        if (mActionListener != null) {
            mActionListener.onCopyUserToken(token);
        }
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = mUserList.get(position);
        holder.itemView.setTag(R.id.tag_adapter, this);
        holder.bind(user, position);
    }

    @Override
    public int getItemCount() {
        return mUserList.size();
    }

    /**
     * 条目操作监听器接口
     * 用于处理列表中用户的删除事件
     */
    public interface OnItemActionListener {
        void onDeleteUser(int position);

        void onCopyUserToken(String token);
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView orderText;
        private final TextView nicknameText;
        private final TextView phoneText;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            orderText = itemView.findViewById(R.id.tv_order_number);
            nicknameText = itemView.findViewById(R.id.tv_nickname);
            phoneText = itemView.findViewById(R.id.tv_phone);
            ImageButton deleteButton = itemView.findViewById(R.id.btn_delete);
            ImageButton copyButton = itemView.findViewById(R.id.btn_copy);

            // 设置删除按钮点击监听
            deleteButton.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    ((UserAdapter) itemView.getTag(R.id.tag_adapter)).deleteUser(position);
                }
            });

            // 设置复制按钮点击监听
            copyButton.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    String token = ((User) itemView.getTag(R.id.tag_user)).getToken();
                    ((UserAdapter) itemView.getTag(R.id.tag_adapter)).copyUserToken(token);
                }
            });
        }

        @SuppressLint("SetTextI18n")
        public void bind(User user, int position) {
            // 设置数据
            int orderIdx = position + 1;
            orderText.setText(String.valueOf(orderIdx));
            nicknameText.setText(user.getNickname());
            phoneText.setText(user.getMaskedPhone() + "\n" + user.getAddedTime());

            // 绑定Tag数据
            itemView.setTag(R.id.tag_adapter, itemView.getTag(R.id.tag_adapter)); // 保留Adapter引用
            itemView.setTag(R.id.tag_user, user);
            itemView.setTag(R.id.tag_position, position);
        }
    }
}
