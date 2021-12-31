package org.citra.citra_emu.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import org.citra.citra_emu.R;

public final class CreditsDialog extends DialogFragment {
    private ViewGroup mViewGroup;

    public static CreditsDialog newInstance() {
        return new CreditsDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        mViewGroup = (ViewGroup) getActivity().getLayoutInflater()
                .inflate(R.layout.dialog_credits, null);

        builder.setView(mViewGroup);
        return builder.create();
    }
}