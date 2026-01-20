package com.guyuexuan.bjxd;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
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

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_USER = BuildConfig.APPLICATION_ID + ".USER";
    public static final String EXTRA_POSITION = BuildConfig.APPLICATION_ID + ".POSITION";
    private UserAdapter adapter;
    // 定义 ActivityResultLauncher
    private final ActivityResultLauncher<Intent> addUserLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == MainActivity.RESULT_OK) {
            // 检查返回的数据 Intent 是否为空
            Intent data = result.getData();
            if (data != null) {
                // 从 Intent 中获取返回的 User 对象
                User user;
                // 2. 使用新的 API，并处理 Android 13 以下的兼容性
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    user = data.getSerializableExtra(EXTRA_USER, User.class);
                } else {
                    // 对于旧版本，继续使用过时的方法
                    user = (User) data.getSerializableExtra(EXTRA_USER);
                }
                int position = data.getIntExtra(EXTRA_POSITION, -1);
                // 刷新用户列表
                if (user != null) { // 3. 最好增加一个非空判断
                    adapter.saveItem(user, position);
                }
            }
        }
    });
    @SuppressWarnings("FieldCanBeLocal")
    private UserAdapter.OnItemActionListener onItemActionListener; // 防止弱引用失效

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 设置标题
        setTitle(AppUtils.getAppNameWithVersion(this));

        // 初始化 StorageUtil
        StorageUtil storageUtil = new StorageUtil(this);

        // 获取控件
        RecyclerView recyclerView = findViewById(R.id.rv_user_list);
        Button addUserButton = findViewById(R.id.btn_add_user);
        Button configButton = findViewById(R.id.btn_config);
        Button startTaskButton = findViewById(R.id.btn_start_task);


        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdapter();
        recyclerView.setAdapter(adapter);

        addUserButton.setOnClickListener(v -> addUserLauncher.launch(new Intent(this, AddUserActivity.class)));
        configButton.setOnClickListener(v -> startActivity(new Intent(this, ConfigActivity.class)));
        startTaskButton.setOnClickListener(v -> {
            if (adapter.getItemCount() == 0) {
                Toast.makeText(this, "请先添加账号！", Toast.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(this, TaskActivity.class));
            }
        });

        // 添加拖拽排序功能
        ItemTouchHelper touchHelper = getItemTouchHelper();
        touchHelper.attachToRecyclerView(recyclerView);

        onItemActionListener = new UserAdapter.OnItemActionListener() {
            @Override
            public void onDeleteUser(int position) {
                if (position != RecyclerView.NO_POSITION) {
                    new AlertDialog.Builder(MainActivity.this).setTitle("确认删除").setMessage("确定要删除该账号吗？").setPositiveButton("确定", (dialog, which) -> adapter.removeItem(position)).setNegativeButton("取消", null).show();
                }
            }

            @Override
            public void onCopyUserToken(String token) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Token", token);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, "Token 已复制", Toast.LENGTH_SHORT).show();
            }
        };

        adapter.setOnItemActionListener(onItemActionListener);

        // 设置初始用户数据
        adapter.setInitialData(storageUtil);
    }

    @NonNull
    private ItemTouchHelper getItemTouchHelper() {
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getBindingAdapterPosition();
                int toPosition = target.getBindingAdapterPosition();
                return adapter.swapItems(fromPosition, toPosition);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            // 当状态改变时触发（选定、开始拖动、结束拖动）
            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);

                // 检查是否处于拖拽状态
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                    // 开始拖动：设置反馈效果（如变色、缩放）
                    viewHolder.itemView.setBackgroundColor(Color.LTGRAY); // 临时变灰
                    viewHolder.itemView.setScaleX(1.05f); // 略微放大
                    viewHolder.itemView.setScaleY(1.05f);
                    viewHolder.itemView.setElevation(10f); // 增加阴影层级
                }
            }

            // 当手指松开，拖动动画结束，视图恢复原位时触发
            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);

                // 结束拖动：恢复原始状态
                viewHolder.itemView.setBackgroundColor(Color.TRANSPARENT); // 恢复透明
                viewHolder.itemView.setScaleX(1.0f); // 恢复原大
                viewHolder.itemView.setScaleY(1.0f);
                viewHolder.itemView.setElevation(0f); // 恢复阴影
            }
        };

        return new ItemTouchHelper(callback);
    }
}
