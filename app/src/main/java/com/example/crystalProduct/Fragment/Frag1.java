package com.example.crystalProduct.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.crystalProduct.DTO.ImageDTO;
import com.example.crystalProduct.Adapter.MyRecyclerViewAdapter;
import com.example.crystalProduct.ProductDetailPage;
import com.example.crystalProduct.R;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class Frag1 extends Fragment {

    private View view;
    private List<ImageDTO> imageDTOList = new ArrayList<>();
    private List<String> uidList = new ArrayList<>();
    private FirebaseFirestore firebaseFirestore;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        firebaseFirestore = FirebaseFirestore.getInstance();
        view = inflater.inflate(R.layout.frag1_home, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.main_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        final MyRecyclerViewAdapter uploadedImageAdapter = new MyRecyclerViewAdapter(imageDTOList, uidList, new MyRecyclerViewAdapter.ItemClickListener() {
            @Override
            public void onItemClick(ImageDTO details, String pos) {
                Intent intent = new Intent(getActivity(), ProductDetailPage.class);
                intent.putExtra("image", details.getImageUrl());
                intent.putExtra("title", details.getTitle());
                intent.putExtra("price", details.getPrice());
                intent.putExtra("deadline", details.getDeadline());
                intent.putExtra("form", details.getPurchaseLink());
                intent.putExtra("description", details.getDescription());
                intent.putExtra("postid", details.getPostid());
                intent.putExtra("publisherid", details.getUid());
                intent.putExtra("postuid", details.getUid());
                intent.putExtra("postToken", pos);
                startActivity(intent);
            }
        });

        recyclerView.setAdapter(uploadedImageAdapter);

        CollectionReference postRef = firebaseFirestore.collection("Post");
        postRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    return;
                }

                imageDTOList.clear();
                uidList.clear();

                for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    ImageDTO imageDTO = doc.toObject(ImageDTO.class);
                    String uidKey = doc.getId(); // 문서 ID

                    imageDTOList.add(0, imageDTO);
                    uidList.add(0, uidKey);
                }

                uploadedImageAdapter.notifyDataSetChanged();
            }
        });

        return view;
    }
}
