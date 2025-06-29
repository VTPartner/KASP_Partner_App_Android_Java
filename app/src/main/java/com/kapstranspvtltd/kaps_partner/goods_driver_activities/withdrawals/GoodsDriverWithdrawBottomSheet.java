package com.kapstranspvtltd.kaps_partner.goods_driver_activities.withdrawals;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityGoodsDriverWithdrawBottomSheetBinding;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

public class GoodsDriverWithdrawBottomSheet extends BottomSheetDialogFragment {
    private ActivityGoodsDriverWithdrawBottomSheetBinding binding;
    private Double currentBalance = 0.0;
    private WithdrawListener withdrawListener;
    private PreferenceManager preferenceManager;

    public interface WithdrawListener {
        void onWithdrawRequested(JSONObject withdrawalData, WithdrawCallback callback);
    }

    public interface WithdrawCallback {
        void onSuccess();
        void onFailure(String message);
    }

    public GoodsDriverWithdrawBottomSheet(Double currentBalance) {
        this.currentBalance = currentBalance;
    }

    public void setWithdrawListener(WithdrawListener listener) {
        this.withdrawListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppBottomSheetDialogTheme);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = ActivityGoodsDriverWithdrawBottomSheetBinding.inflate(inflater, container, false);
        preferenceManager = new PreferenceManager(requireContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
        setupListeners();
    }

    private void setupViews() {
        binding.tvAvailableBalance.setText(String.format("₹%.2f", currentBalance));
    }

    private void setupListeners() {
        binding.btnClose.setOnClickListener(v -> dismiss());

        binding.paymentMethodGroup.setOnCheckedChangeListener((group, checkedId) -> {
            binding.bankDetailsLayout.setVisibility(
                    checkedId == R.id.rbBankAccount ? View.VISIBLE : View.GONE);
            binding.upiLayout.setVisibility(
                    checkedId == R.id.rbUpi ? View.VISIBLE : View.GONE);
        });

        binding.btnWithdrawSubmit.setOnClickListener(v -> validateAndSubmit());

        // Amount validation
        binding.etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateAmount(s.toString());
            }
        });
    }

    private void validateAmount(String amountStr) {
        if (amountStr.isEmpty()) {
            binding.amountInputLayout.setError("Please enter amount");
            return;
        }

        double amount = Double.parseDouble(amountStr);
        if (amount < 10) {
            binding.amountInputLayout.setError("Minimum withdrawal amount is ₹10");
        } else if (amount > currentBalance) {
            binding.amountInputLayout.setError("Amount exceeds available balance");
        } else {
            binding.amountInputLayout.setError(null);
        }
    }

    private void validateAndSubmit() {
        String amount = binding.etAmount.getText().toString();
        if (amount.isEmpty()) {
            binding.amountInputLayout.setError("Please enter amount");
            return;
        }

        double withdrawAmount = Double.parseDouble(amount);
        if (withdrawAmount < 10) {
            binding.amountInputLayout.setError("Minimum withdrawal amount is ₹10");
            return;
        }

        if (withdrawAmount > currentBalance) {
            binding.amountInputLayout.setError("Amount exceeds available balance");
            return;
        }

        boolean isBankSelected = binding.paymentMethodGroup.getCheckedRadioButtonId() == R.id.rbBankAccount;

        if (isBankSelected) {
            String accountNumber = binding.etAccountNumber.getText().toString();
            String ifscCode = binding.etIfscCode.getText().toString();
            String accountName = binding.etAccountName.getText().toString();

            if (accountNumber.isEmpty() || ifscCode.isEmpty() || accountName.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all bank details", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!ifscCode.matches("^[A-Z]{4}0[A-Z0-9]{6}$")) {
                Toast.makeText(getContext(), "Invalid IFSC code", Toast.LENGTH_SHORT).show();
                return;
            }

            submitBankWithdrawal(withdrawAmount, accountNumber, ifscCode, accountName);
        } else {
            String upiId = binding.etUpiId.getText().toString();
            if (upiId.isEmpty()) {
                binding.upiLayout.setError("Please enter UPI ID");
                return;
            }

            if (!upiId.matches("^[\\w.-]+@[\\w.-]+$")) {
                binding.upiLayout.setError("Invalid UPI ID");
                return;
            }

            submitUPIWithdrawal(withdrawAmount, upiId);
        }
    }

    private void submitBankWithdrawal(double amount, String accountNumber, String ifscCode, String accountName) {
        try {
            JSONObject withdrawalData = new JSONObject();
            withdrawalData.put("amount", amount);
            withdrawalData.put("payment_method", "BANK");
            withdrawalData.put("account_number", accountNumber);
            withdrawalData.put("ifsc_code", ifscCode);
            withdrawalData.put("account_name", accountName);

            if (withdrawListener != null) {
                withdrawListener.onWithdrawRequested(withdrawalData, new WithdrawCallback() {
                    @Override
                    public void onSuccess() {
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                dismiss();
                                Toast.makeText(getContext(), "Withdrawal request submitted successfully", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }

                    @Override
                    public void onFailure(String message) {
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error processing request", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void submitUPIWithdrawal(double amount, String upiId) {
        try {
            JSONObject withdrawalData = new JSONObject();
            withdrawalData.put("amount", amount);
            withdrawalData.put("payment_method", "UPI");
            withdrawalData.put("upi_id", upiId);

            if (withdrawListener != null) {
                withdrawListener.onWithdrawRequested(withdrawalData, new WithdrawCallback() {
                    @Override
                    public void onSuccess() {
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                dismiss();
                                Toast.makeText(getContext(), "Withdrawal request submitted successfully", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }

                    @Override
                    public void onFailure(String message) {
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error processing request", Toast.LENGTH_SHORT).show();
            }
        }
    }
}