package com.github.tvbox.osc.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.databinding.FragmentFilesBinding;

public class FilesFragment extends Fragment {
    private FragmentFilesBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentFilesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textFiles;
        textView.setText(R.string.act_search);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
