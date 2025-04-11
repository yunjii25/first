package com.example.crystalProduct.Fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.loader.content.CursorLoader;

import com.example.crystalProduct.DTO.ImageDTO;
import com.example.crystalProduct.IOnBackPressed;
import com.example.crystalProduct.MainActivity;
import com.example.crystalProduct.R;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.Calendar;

public class Frag4 extends Fragment implements IOnBackPressed {

    private static final int RESULT_OK = -1;
    private static final int GALLERY_CODE = 10;

    private View view;
    private Context context;

    private Button btnOk;
    private TextView btnUpload;
    private ImageView ivProfile, uploadIcon;
    private EditText etTitle, etDesc, etPrice, etPurchaseLink;
    private TextView dateTv;
    private String imageUrl = "", date = "";

    private FirebaseAuth mAuth;
    private FirebaseStorage storage;
    private FirebaseFirestore firestore;
    private DatePickerDialog.OnDateSetListener setListener;

    @Override
    public void onBackPressed() {
        Toast.makeText(context, "이미지 선택 안함", Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.frag4_create, container, false);

        context = container.getContext();
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        firestore = FirebaseFirestore.getInstance();

        btnOk = view.findViewById(R.id.btn_profile_Ok);
        btnUpload = view.findViewById(R.id.btn_upload);
        uploadIcon = view.findViewById(R.id.uploadIcon);
        ivProfile = view.findViewById(R.id.iv_profile);
        etTitle = view.findViewById(R.id.title);
        etDesc = view.findViewById(R.id.description);
        etPrice = view.findViewById(R.id.price);
        etPurchaseLink = view.findViewById(R.id.purchaseLink);
        dateTv = view.findViewById(R.id.deadline);

        btnOk.setOnClickListener(v -> {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                uploadPostWithImage(imageUrl);
            } else {
                uploadPostWithoutImage();
            }
        });

        ivProfile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
            startActivityForResult(intent, GALLERY_CODE);
            btnUpload.setVisibility(View.GONE);
            uploadIcon.setVisibility(View.GONE);
        });

        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        dateTv.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    getActivity(), android.R.style.Theme_Holo_Light_Dialog_MinWidth,
                    setListener, year, month, day);
            datePickerDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            datePickerDialog.show();
        });

        setListener = (view1, year1, month1, dayOfMonth) -> {
            month1++;
            date = year1 + "/" + month1 + "/" + dayOfMonth;
            dateTv.setText(date);
        };

        etDesc.setOnTouchListener((v, event) -> {
            if (v.getId() == R.id.description) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                }
            }
            return false;
        });

        // ✅ 런타임 권한 체크 및 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 1001);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1002);
            }
        }

        return view;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if ((requestCode == 1001 || requestCode == 1002) && (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
            Toast.makeText(context, "이미지 접근 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GALLERY_CODE && resultCode == RESULT_OK) {
            imageUrl = getRealPathFromUri(data.getData());
            File f = new File(imageUrl);
            ivProfile.setImageURI(Uri.fromFile(f));
        } else {
            Toast.makeText(context, "이미지 선택 안함", Toast.LENGTH_SHORT).show();
        }
    }

    private String getRealPathFromUri(Uri uri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        CursorLoader cursorLoader = new CursorLoader(getActivity().getApplicationContext(), uri, proj, null, null, null);
        Cursor cursor = cursorLoader.loadInBackground();
        int index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(index);
    }

    private void uploadPostWithImage(String uri) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri file = Uri.fromFile(new File(uri));
        StorageReference storageRef = storage.getReference().child("images/" + file.getLastPathSegment());
        UploadTask uploadTask = storageRef.putFile(file);

        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            return storageRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUrl = task.getResult();
                uploadPostToFirestore(user, downloadUrl.toString());
            } else {
                Toast.makeText(context, "이미지 업로드 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadPostWithoutImage() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        uploadPostToFirestore(user, null);  // 이미지 URL 없이 업로드
    }

    private void uploadPostToFirestore(FirebaseUser user, String imageUrl) {
        ImageDTO imageDTO = new ImageDTO();
        imageDTO.setImageUrl(imageUrl);  // null 허용
        imageDTO.setTitle(etTitle.getText().toString());
        imageDTO.setDescription(etDesc.getText().toString());
        imageDTO.setPrice(etPrice.getText().toString());
        imageDTO.setDeadline(date);
        imageDTO.setPurchaseLink(etPurchaseLink.getText().toString());
        imageDTO.setUid(user.getUid());

        firestore.collection("Post")
                .add(imageDTO)
                .addOnSuccessListener(documentReference -> {
                    // ✅ 여기서 자동 생성된 document ID를 가져와서 postid로 저장
                    String postId = documentReference.getId();
                    documentReference.update("postid", postId)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(context, "업로드 완료!", Toast.LENGTH_SHORT).show();
                                resetForm();
                                startActivity(new Intent(getActivity(), MainActivity.class));
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "postid 저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "업로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void resetForm() {
        etTitle.setText("");
        etDesc.setText("");
        etPrice.setText("");
        etPurchaseLink.setText("");
        dateTv.setText("");
        ivProfile.setImageResource(R.drawable.base_image_frag4);
        imageUrl = "";
        date = "";
    }
}
