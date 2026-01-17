package com.guyuexuan.bjxd;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.guyuexuan.bjxd.util.AppUtils;
import com.guyuexuan.bjxd.util.StorageUtil;

import java.util.Objects;

public class ConfigActivity extends AppCompatActivity {
    private StorageUtil storageUtil;
    private String aiApiKey;
    private String aiRequestUrl;
    private String aiModel;
    private String aiRequestParams;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        // 设置标题
        setTitle(AppUtils.getAppNameWithVersion(this));

        storageUtil = new StorageUtil(this);

        SwitchMaterial manualAnswerSwitch = findViewById(R.id.manualAnswerSwitch);

        TextInputEditText aiApiKeyInput = findViewById(R.id.aiApiKeyInput);
        TextInputEditText aiRequestUrlInput = findViewById(R.id.aiRequestUrlInput);
        TextInputEditText aiModelInput = findViewById(R.id.aiModelInput);
        TextInputEditText aiRequestParamsInput = findViewById(R.id.aiRequestParamsInput);

        manualAnswerSwitch.setChecked(storageUtil.isManualAnswer());

        aiApiKey = storageUtil.getAiApiKey();
        aiRequestUrl = storageUtil.getAiRequestUrl();
        aiModel = storageUtil.getAiModel();
        aiRequestParams = storageUtil.getAiRequestParams();
        aiApiKeyInput.setText(aiApiKey);
        aiRequestUrlInput.setText(aiRequestUrl);
        aiModelInput.setText(aiModel);
        aiRequestParamsInput.setText(aiRequestParams);

        findViewById(R.id.saveButton).setOnClickListener(v -> {
            // 答题：手动答题
            storageUtil.setManualAnswer(manualAnswerSwitch.isChecked());
            // 答题：AI 模型配置
            aiApiKey = Objects.requireNonNull(aiApiKeyInput.getText()).toString().trim();
            aiRequestUrl = Objects.requireNonNull(aiRequestUrlInput.getText()).toString().trim();
            aiModel = Objects.requireNonNull(aiModelInput.getText()).toString().trim();
            aiRequestParams = Objects.requireNonNull(aiRequestParamsInput.getText()).toString().trim();
            storageUtil.saveAiApiKey(aiApiKey);
            storageUtil.saveAiRequestUrl(aiRequestUrl);
            storageUtil.saveAiModel(aiModel);
            storageUtil.saveAiRequestParams(aiRequestParams);
            // toast 提示保存成功
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
            finish();
        });

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
    }
}
