package com.example.crystalProduct;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.crystalProduct.Adapter.CommentAdapter;
import com.example.crystalProduct.DTO.CommentsDTO;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ProductDetailPage extends AppCompatActivity {

    ImageView imageView_image, delete_post;
    TextView textView_title, textView_price, textView_deadline, textView_form, textView_description;

    RecyclerView recyclerView;
    private CommentAdapter commentAdapter;
    private List<CommentsDTO> commentList;

    EditText addcomment;
    ImageView heart;
    ImageView post;

    String postid, publisherid;
    String postuid, postToken;

    FirebaseUser firebaseUser;

    CheckBox demands_btn;
    TextView demands_t;

    FirebaseFirestore firestore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.product_detail);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        firestore = FirebaseFirestore.getInstance();

        Intent intent = getIntent();

        imageView_image = findViewById(R.id.detailImage);
        textView_title = findViewById(R.id.detailTitle);
        textView_price = findViewById(R.id.detailPrice);
        textView_deadline = findViewById(R.id.detailDeadline);
        textView_form = findViewById(R.id.detailForm);
        textView_description = findViewById(R.id.detailDescription);

        String image_url = intent.getStringExtra("image");
        String title = intent.getStringExtra("title");
        String price = intent.getStringExtra("price");
        String deadline = intent.getStringExtra("deadline");
        String form = intent.getStringExtra("form");
        String description = intent.getStringExtra("description");

        postid = intent.getStringExtra("postid");
        publisherid = intent.getStringExtra("publisherid");
        postuid = intent.getStringExtra("postuid");
        postToken = intent.getStringExtra("postToken");

        if (image_url != null && !image_url.isEmpty()) {
            Glide.with(ProductDetailPage.this).load(image_url).into(imageView_image);
        }

        textView_title.setText(title);
        textView_price.setText(price);
        textView_deadline.setText(deadline);
        textView_form.setText(form);
        textView_description.setText(description);

        recyclerView = findViewById(R.id.comment_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentList = new ArrayList<>();
        commentAdapter = new CommentAdapter(this, commentList, postid, postuid);
        recyclerView.setAdapter(commentAdapter);

        addcomment = findViewById(R.id.add_comment);
        post = findViewById(R.id.comment_post);

        post.setOnClickListener(v -> {
            if (addcomment.getText().toString().isEmpty()) {
                Toast.makeText(ProductDetailPage.this, "댓글을 입력해주세요.", Toast.LENGTH_SHORT).show();
            } else {
                addcomment();
            }
        });

        readComments();

        heart = findViewById(R.id.detail_heart);
        isLiked(postid, heart);

        heart.setOnClickListener(view -> {
            if (heart.getTag().equals("like")) {
                firestore.collection("Likes").document(firebaseUser.getUid())
                        .collection("PostLikes").document(postid)
                        .set(new HashMap<>());
                Toast.makeText(ProductDetailPage.this, "관심상품에 등록되었습니다.", Toast.LENGTH_SHORT).show();
            } else {
                firestore.collection("Likes").document(firebaseUser.getUid())
                        .collection("PostLikes").document(postid)
                        .delete();
                Toast.makeText(ProductDetailPage.this, "관심상품에서 취소되었습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        demands_btn = findViewById(R.id.btn_buy);
        demands_t = findViewById(R.id.buy_count);

        isDemands(postid, demands_btn);
        nrDemands(demands_t, postid);

        demands_btn.setOnClickListener(view -> {
            if (demands_btn.getTag().equals("not_demand")) {
                firestore.collection("Demands").document(postid)
                        .collection("UserDemands").document(firebaseUser.getUid())
                        .set(new HashMap<>());
                Toast.makeText(ProductDetailPage.this, "수요조사 찬성", Toast.LENGTH_SHORT).show();
            } else {
                firestore.collection("Demands").document(postid)
                        .collection("UserDemands").document(firebaseUser.getUid())
                        .delete();
                Toast.makeText(ProductDetailPage.this, "수요조사 찬성 취소", Toast.LENGTH_SHORT).show();
            }
        });

        delete_post = findViewById(R.id.delete_post);

        if (postuid.equals(firebaseUser.getUid())) {
            delete_post.setVisibility(View.VISIBLE);
            delete_post.setOnClickListener(v -> {
                AlertDialog alertDialog = new AlertDialog.Builder(ProductDetailPage.this).create();
                alertDialog.setTitle("작성하신 글을 삭제하시겠습니까?");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "No", (dialog, which) -> dialog.dismiss());
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes", (dialog, which) -> {
                    deleteContent(postToken, postid);
                    dialog.dismiss();
                });
                alertDialog.show();
            });
        }
    }

    private void addcomment() {
        String commentId = firestore.collection("Comments").document(postid)
                .collection("CommentList").document().getId();

        HashMap<String, Object> map = new HashMap<>();
        map.put("comment", addcomment.getText().toString());
        map.put("publisher", firebaseUser.getUid());
        map.put("commentid", commentId);

        firestore.collection("Comments").document(postid)
                .collection("CommentList").document(commentId)
                .set(map);

        addcomment.setText("");
    }

    private void readComments() {
        CollectionReference ref = firestore.collection("Comments").document(postid)
                .collection("CommentList");

        ref.addSnapshotListener((value, error) -> {
            if (value == null) return;

            commentList.clear();
            for (DocumentSnapshot doc : value.getDocuments()) {
                CommentsDTO comment = doc.toObject(CommentsDTO.class);
                commentList.add(comment);
            }
            commentAdapter.notifyDataSetChanged();
        });
    }

    private void isLiked(final String postid, final ImageView imageView) {
        firestore.collection("Likes").document(firebaseUser.getUid())
                .collection("PostLikes").document(postid)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null && snapshot.exists()) {
                        imageView.setImageResource(R.drawable.heart_on);
                        imageView.setTag("liked");
                    } else {
                        imageView.setImageResource(R.drawable.heart_off);
                        imageView.setTag("like");
                    }
                });
    }

    private void isDemands(final String postid, final CheckBox checkBox) {
        firestore.collection("Demands").document(postid)
                .collection("UserDemands").document(firebaseUser.getUid())
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null && snapshot.exists()) {
                        checkBox.setChecked(true);
                        checkBox.setTag("demand");
                    } else {
                        checkBox.setChecked(false);
                        checkBox.setTag("not_demand");
                    }
                });
    }

    private void nrDemands(final TextView demands_p, String postId) {
        firestore.collection("Demands").document(postId)
                .collection("UserDemands")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        demands_p.setText(value.size() + " people");
                    }
                });
    }

    private void deleteContent(String postToken, String postid) {
        firestore.collection("Post").document(postToken).delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ProductDetailPage.this, "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    }
                });

        firestore.collection("Comments").document(postid).delete();
        firestore.collection("Demands").document(postid).delete();
        firestore.collection("Likes").document(firebaseUser.getUid())
                .collection("PostLikes").document(postid).delete();
    }
}
