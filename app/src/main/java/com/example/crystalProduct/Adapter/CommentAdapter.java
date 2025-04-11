package com.example.crystalProduct.Adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.crystalProduct.DTO.CommentsDTO;
import com.example.crystalProduct.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {

    private Context mContext;
    private List<CommentsDTO> mComments;
    private String postid;
    private String postuid;

    FirebaseUser firebaseUser;
    FirebaseFirestore db;

    public CommentAdapter(Context mContext, List<CommentsDTO> mComments, String postid, String postuid) {
        this.mContext = mContext;
        this.mComments = mComments;
        this.postid = postid;
        this.postuid = postuid;
        db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.comment_item, parent, false);
        return new CommentAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull CommentAdapter.ViewHolder holder, int position) {
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        CommentsDTO comment = mComments.get(position);

        holder.comment.setText(comment.getComment());
        getUserInfo(holder.username, comment.getPublisher(), postuid, holder.writer);

        holder.itemView.setOnLongClickListener(v -> {
            if (comment.getPublisher().equals(firebaseUser.getUid())) {
                AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();
                alertDialog.setTitle("댓글을 삭제하시겠습니까?");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "No", (dialog, which) -> dialog.dismiss());
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes", (dialog, which) -> {
                    db.collection("Comments").document(postid)
                            .collection("commentList").document(comment.getCommentid())
                            .delete().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(mContext, "댓글이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                                }
                            });
                    dialog.dismiss();
                });
                alertDialog.show();
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return mComments.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView username, comment, writer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.username);
            comment = itemView.findViewById(R.id.comment);
            writer = itemView.findViewById(R.id.writer);
        }
    }

    private void getUserInfo(TextView username, String publisherId, String postuid, TextView writer) {
        db.collection("UserAccount").document(publisherId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("nickname");
                        username.setText(name);
                        if (postuid.equals(publisherId)) {
                            writer.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }
}
