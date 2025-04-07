package com.example.crystalProduct;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.crystalProduct.DTO.UserAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth FirebaseAuth;
    private FirebaseFirestore firestore;
    private EditText EtEmail, EtPwd, NickName, EtPwd2;
    private Button nBtnRegister;
    private TextView registerText2;

    private String strEmail, strPwd, strPwd2, strName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        FirebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance(); // Firestore 초기화

        // EditText 및 버튼 연결
        EtEmail = findViewById(R.id.et_email);
        EtPwd = findViewById(R.id.et_pwd);
        EtPwd2 = findViewById(R.id.et_pwd2);
        NickName = findViewById(R.id.et_nickname);
        registerText2 = findViewById(R.id.registerText2);
        nBtnRegister = findViewById(R.id.btn_register);

        // 🔥 bringToFront() 삭제 → 뷰를 가리는 문제 해결
        // EtEmail.bringToFront();
        // EtPwd.bringToFront();
        // EtPwd2.bringToFront();
        // NickName.bringToFront();
        // registerText2.bringToFront();

        // 🔹 회원가입 버튼 클릭 이벤트
        nBtnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                strEmail = EtEmail.getText().toString();
                strPwd = EtPwd.getText().toString();
                strPwd2 = EtPwd2.getText().toString();
                strName = NickName.getText().toString();

                if (TextUtils.isEmpty(strEmail) || TextUtils.isEmpty(strPwd) || TextUtils.isEmpty(strName)) {
                    Toast.makeText(RegisterActivity.this, "모두 입력하십시오.", Toast.LENGTH_SHORT).show();
                } else if (strPwd.length() < 6) {
                    Toast.makeText(RegisterActivity.this, "비밀번호는 6자리 이상입니다.", Toast.LENGTH_SHORT).show();
                } else if (!strPwd.equals(strPwd2)) {
                    Toast.makeText(RegisterActivity.this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    checkDuplicateNickname();
                }
            }
        });

        // 🔹 로그인 버튼 클릭 이벤트
        Button login_go = findViewById(R.id.login_go);
        login_go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }

    private void checkDuplicateNickname() {
        firestore.collection("UserAccount")
                .whereEqualTo("nickname", strName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            Toast.makeText(getApplicationContext(), "이미 등록된 닉네임입니다.", Toast.LENGTH_LONG).show();
                        } else {
                            registerNewUser();
                        }
                    } else {
                        System.out.println("❌ 닉네임 중복 체크 실패: " + task.getException());
                    }
                });
    }

    private void registerNewUser() {
        FirebaseAuth.createUserWithEmailAndPassword(strEmail, strPwd)
                .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = FirebaseAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                UserAccount account = new UserAccount();
                                account.setIdToken(firebaseUser.getUid());
                                account.setEmailId(firebaseUser.getEmail());
                                account.setPassword(strPwd);
                                account.setNickname(strName);

                                firestore.collection("UserAccount").document(firebaseUser.getUid())
                                        .set(account)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(RegisterActivity.this, "회원가입에 성공하셨습니다.", Toast.LENGTH_SHORT).show();

                                            // 🔥 EditText가 보이도록 강제로 VISIBLE 설정 후 초기화
                                            EtEmail.setVisibility(View.VISIBLE);
                                            EtPwd.setVisibility(View.VISIBLE);
                                            EtPwd2.setVisibility(View.VISIBLE);
                                            NickName.setVisibility(View.VISIBLE);

                                            EtEmail.setText("");
                                            EtPwd.setText("");
                                            EtPwd2.setText("");
                                            NickName.setText("");

                                            sendVerificationEmail(firebaseUser);
                                        })
                                        .addOnFailureListener(e -> {
                                            System.out.println("❌ Firestore 저장 실패: " + e.getMessage());
                                        });

                            } else {
                                System.out.println("❌ FirebaseUser가 null입니다.");
                            }
                        } else {
                            System.out.println("❌ 회원가입 실패!");
                            Toast.makeText(RegisterActivity.this, "이미 등록된 이메일입니다.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void sendVerificationEmail(FirebaseUser user) {
        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                System.out.println("✅ 인증 메일 전송 완료");
                                Toast.makeText(RegisterActivity.this, "인증메일이 전송되었습니다.", Toast.LENGTH_LONG).show();
                            } else {
                                System.out.println("❌ 인증 메일 전송 실패: " + task.getException());
                                Toast.makeText(RegisterActivity.this, "인증메일 전송에 실패했습니다.", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        } else {
            System.out.println("❌ FirebaseUser가 null입니다.");
        }
    }
}
