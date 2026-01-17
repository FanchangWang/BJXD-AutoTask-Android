package com.guyuexuan.bjxd;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.guyuexuan.bjxd.adapter.UserAdapter;
import com.guyuexuan.bjxd.model.User;
import com.guyuexuan.bjxd.util.AppUtils;
import com.guyuexuan.bjxd.util.StorageUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements UserAdapter.OnUserActionListener {
    private final List<User> users = new ArrayList<>();
    private UserAdapter adapter;
    private StorageUtil storageUtil;
    private TextView accountCountText;

    // 定义 ActivityResultLauncher
    private final ActivityResultLauncher<Intent> addUserLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == MainActivity.RESULT_OK) {
                    // 刷新用户列表
                    refreshUserList();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 设置标题
        setTitle(AppUtils.getAppNameWithVersion(this));

        storageUtil = new StorageUtil(this);
        initViews();
        loadUsers();
    }

    private void initViews() {
        accountCountText = findViewById(R.id.accountCountText);

        RecyclerView userList = findViewById(R.id.userList);
        userList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdapter(users, this);
        userList.setAdapter(adapter);

        findViewById(R.id.addUserButton).setOnClickListener(v -> openAddUserActivity());

        findViewById(R.id.configButton).setOnClickListener(v -> startActivity(new Intent(this, ConfigActivity.class)));

        findViewById(R.id.startTaskButton).setOnClickListener(v -> {
            if (users.isEmpty()) {
                Toast.makeText(this, "请先添加账号", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(this, TaskActivity.class));
        });

        // 添加拖拽排序功能
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            private boolean isDragging = false;

            @Override
            public boolean isLongPressDragEnabled() {
                // 禁用长按拖动
                return false;
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getBindingAdapterPosition();
                int toPosition = target.getBindingAdapterPosition();
                Collections.swap(users, fromPosition, toPosition);
                adapter.notifyItemMoved(fromPosition, toPosition);

                // 更新所有可见项的序号
                for (int i = 0; i < recyclerView.getChildCount(); i++) {
                    RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
                    if (holder instanceof UserAdapter.UserViewHolder) {
                        ((UserAdapter.UserViewHolder) holder).updateOrder(holder.getBindingAdapterPosition() + 1);
                    }
                }

                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                if (isDragging) {
                    isDragging = false;
                    onMoveUser(viewHolder.getBindingAdapterPosition(), viewHolder.getBindingAdapterPosition());
                }
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                isDragging = actionState == ItemTouchHelper.ACTION_STATE_DRAG;
            }
        };

        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(userList);

        // 将拖动手柄与 ItemTouchHelper 关联
        adapter.setOnStartDragListener(touchHelper::startDrag);
    }

    private void updateAccountCount() {
        accountCountText.setText(String.format(Locale.getDefault(), "Token 有效期 28 天，过期后重新添加即可。\n当前共有 %d 个账号", users.size()));
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadUsers() {
        users.clear();
        users.addAll(storageUtil.getUsers());
        adapter.notifyDataSetChanged();

        // 更新账号数量显示
        updateAccountCount();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUsers();
    }

    @Override
    public void onDeleteUser(User user) {
        new AlertDialog.Builder(this)
                .setTitle("确认删除")
                .setMessage("确定要删除该账号吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    int position = users.indexOf(user);
                    if (position != -1) {
                        users.remove(position);
                        storageUtil.saveUsers(users);
                        adapter.notifyItemRemoved(position);
                    }
                    // 更新账号数量显示
                    updateAccountCount();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public void onMoveUser(int fromPosition, int toPosition) {
        // 更新排序后保存
        for (int i = 0; i < users.size(); i++) {
            users.get(i).setOrder(i);
        }
        storageUtil.saveUsers(users);
    }

    private void openAddUserActivity() {
        // 使用新的方式启动 Activity
        addUserLauncher.launch(new Intent(this, AddUserActivity.class));
    }

    private void refreshUserList() {
        loadUsers();
    }
}
