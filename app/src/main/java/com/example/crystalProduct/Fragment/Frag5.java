package com.example.crystalProduct.Fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.crystalProduct.DTO.ImageDTO;
import com.example.crystalProduct.LoginActivity;
import com.example.crystalProduct.Adapter.MyRecyclerViewAdapter;
import com.example.crystalProduct.NicknameResetActivity;
import com.example.crystalProduct.PasswordResetActivity;
import com.example.crystalProduct.ProductDetailPage;
import com.example.crystalProduct.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Frag5 extends Fragment {

    private View view;
    private Button btn_logout, btn_reNickname, btn_rePassword, btn_secession;
    private TextView email_info, nickname_info, emptyLiked;
    private RecyclerView recyclerView;
    private MyRecyclerViewAdapter uploadedImageAdapter;
    private List<ImageDTO> imageDTOList = new ArrayList<>();
    private List<String> postList = new ArrayList<>();
    private List<String> uidList = new ArrayList<>();
    private FirebaseAuth mAuth;
    private FirebaseUser firebaseUser;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.frag5_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        firebaseUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        btn_logout = view.findViewById(R.id.btn_logout);
        btn_reNickname = view.findViewById(R.id.btn_reNickname);
        btn_rePassword = view.findViewById(R.id.btn_rePassword);
        btn_secession = view.findViewById(R.id.btn_secession);
        email_info = view.findViewById(R.id.email_address);
        nickname_info = view.findViewById(R.id.nickname_info);
        emptyLiked = view.findViewById(R.id.emptyLiked);
        recyclerView = view.findViewById(R.id.heart_recyclerview);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        uploadedImageAdapter = new MyRecyclerViewAdapter(imageDTOList, uidList, (details, pos) -> {
            Intent intent = new Intent(getActivity(), ProductDetailPage.class);
            intent.putExtra("image", details.getImageUrl());
            intent.putExtra("title", details.getTitle());
            intent.putExtra("price", details.getPrice());
            intent.putExtra("deadline", details.getDeadline());
            intent.putExtra("form", details.getPurchaseLink());
            intent.putExtra("description", details.getDescription());
            intent.putExtra("postid", details.getPostid());
            intent.putExtra("publisherid", details.getUserEmail());
            intent.putExtra("postuid", details.getUid());
            intent.putExtra("postToken", pos);
            startActivity(intent);
        });
        recyclerView.setAdapter(uploadedImageAdapter);

        loadUserInfo();
        getHeart();

        btn_logout.setOnClickListener(v -> showLogoutDialog());
        btn_reNickname.setOnClickListener(v -> startActivity(new Intent(getActivity(), NicknameResetActivity.class)));
        btn_rePassword.setOnClickListener(v -> startActivity(new Intent(getActivity(), PasswordResetActivity.class)));
        btn_secession.setOnClickListener(v -> showSecessionDialog());

        ImageView heartIcon = view.findViewById(R.id.SecondIcon);
        int color = ContextCompat.getColor(getActivity(), R.color.mainPurple);
        heartIcon.setColorFilter(color);

        return view;
    }

    private void loadUserInfo() {
        if (firebaseUser != null) {
            String uid = firebaseUser.getUid();
            db.collection("UserAccount").document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String email = documentSnapshot.getString("emailId");
                            String nickname = documentSnapshot.getString("nickname");
                            email_info.setText(email);
                            nickname_info.setText(nickname);
                        }
                    });
        }
    }

    private void getHeart() {
        if (firebaseUser != null) {
            String uid = firebaseUser.getUid();
            db.collection("Likes").document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            postList.clear();
                            Map<String, Object> data = documentSnapshot.getData();
                            if (data != null) {
                                postList.addAll(data.keySet());
                            }
                            showHeartProduct();
                        }
                    });
        }
    }

    private void showHeartProduct() {
        db.collection("Post")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    imageDTOList.clear();
                    uidList.clear();
                    for (DocumentSnapshot snapshot : querySnapshot) {
                        ImageDTO imageDTO = snapshot.toObject(ImageDTO.class);
                        if (imageDTO != null && postList.contains(imageDTO.getPostid())) {
                            imageDTOList.add(imageDTO);
                            uidList.add(snapshot.getId());
                        }
                    }
                    uploadedImageAdapter.notifyDataSetChanged();
                    emptyLiked.setVisibility(imageDTOList.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("정말로 로그아웃 하시겠습니까?")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", (dialog, which) -> {
                    mAuth.signOut();
                    startActivity(new Intent(getActivity(), LoginActivity.class));
                })
                .show();
    }

    private void showSecessionDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("정말로 탈퇴하겠습니까?")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (firebaseUser != null) {
                        firebaseUser.delete().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                startActivity(new Intent(getActivity(), LoginActivity.class));
                            }
                        });
                    }
                })
                .show();
    }
}
