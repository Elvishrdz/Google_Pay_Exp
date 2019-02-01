package com.eahm.gpaytesting;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Optional;

public class MainActivity extends AppCompatActivity {

    /***
     * Cuando este implementado hay que validar los siguientes parametros del formulario y despues enviar a una lista de aprobacion
     * https://developers.google.com/pay/api/android/guides/test-and-deploy/integration-checklist
     */

    private static final int LOAD_PAYMENT_DATA_REQUEST_CODE = 117;
    private PaymentsClient mPaymentsClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Create a PaymentsClient instance
        mPaymentsClient =
                Wallet.getPaymentsClient(
                        this,
                        new Wallet.WalletOptions.Builder()
                                .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                                .build());

        IsReadyToPayRequest request = null;

        try {
            request = IsReadyToPayRequest.fromJson(getIsReadyToPayRequest().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(request != null) {
            Task<Boolean> task = mPaymentsClient.isReadyToPay(request);
            task.addOnCompleteListener(
                    new OnCompleteListener<Boolean>() {
                        @Override
                        public void onComplete(@NonNull Task<Boolean> task) {
                            try {
                                boolean result = task.getResult(ApiException.class);
                                if (result) {
                                    // show Google Pay as a payment option
                                }
                            }
                            catch (ApiException ignore) { }
                            catch (Exception ignore){ }


                        }
                    });
        }


        //Register event handler for user gesture
        findViewById(R.id.buttonBuy).setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PaymentDataRequest request = null;
                    try {
                        request = PaymentDataRequest.fromJson(getPaymentDataRequest().toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (request != null) {
                        AutoResolveHelper.resolveTask(
                                mPaymentsClient.loadPaymentData(request),
                                MainActivity.this,
                                // LOAD_PAYMENT_DATA_REQUEST_CODE is a constant integer value you define
                                LOAD_PAYMENT_DATA_REQUEST_CODE);
                    }
                }
            });




    }

    //region PART 1
    //Define your Google Pay API version
    private static JSONObject getBaseRequest() throws JSONException {
        return new JSONObject()
                .put("apiVersion", 2)
                .put("apiVersionMinor", 0);
    }

    //Choose a payment tokenization method
    private static JSONObject getTokenizationSpecification() throws JSONException {
        JSONObject tokenizationSpecification = new JSONObject();
        tokenizationSpecification.put("type", "PAYMENT_GATEWAY");
        tokenizationSpecification.put(
                "parameters",
                new JSONObject()
                        .put("gateway", "example")
                        .put("gatewayMerchantId", "exampleMerchantId"));

        return tokenizationSpecification;
    }

    //Define supported payment card networks
    private static JSONArray getAllowedCardNetworks() {
        return new JSONArray()
                .put("MASTERCARD")
                .put("VISA");
    }

    private static JSONArray getAllowedCardAuthMethods() {
        return new JSONArray()
                .put("PAN_ONLY")
                .put("CRYPTOGRAM_3DS");
    }

    //Describe your allowed payment methods
    private static JSONObject getBaseCardPaymentMethod() throws JSONException {
        JSONObject cardPaymentMethod = new JSONObject();
        cardPaymentMethod.put("type", "CARD");
        cardPaymentMethod.put(
                "parameters",
                new JSONObject()
                        .put("allowedAuthMethods", getAllowedCardAuthMethods())
                        .put("allowedCardNetworks", getAllowedCardNetworks()));

        return cardPaymentMethod;
    }

    private static JSONObject getCardPaymentMethod() throws JSONException {
        JSONObject cardPaymentMethod = getBaseCardPaymentMethod();
        cardPaymentMethod.put("tokenizationSpecification", getTokenizationSpecification());

        return cardPaymentMethod;
    }

    //Determine readiness to pay with the Google Pay API
    public static JSONObject getIsReadyToPayRequest() throws JSONException {
        JSONObject isReadyToPayRequest = getBaseRequest();
        isReadyToPayRequest.put(
                "allowedPaymentMethods",
                new JSONArray()
                        .put(getBaseCardPaymentMethod()));

        return isReadyToPayRequest;
    }
    //endregion PART 1

    //region PART 2
    //Create a PaymentDataRequest object
    private static JSONObject getTransactionInfo() throws JSONException {
        JSONObject transactionInfo = new JSONObject();
        transactionInfo.put("totalPrice", "12.34");
        transactionInfo.put("totalPriceStatus", "FINAL");
        transactionInfo.put("currencyCode", "USD");

        return transactionInfo;
    }

    private static JSONObject getMerchantInfo() throws JSONException {
        return new JSONObject()
                .put("merchantName", "Example Merchant");
    }

    public static JSONObject getPaymentDataRequest() throws JSONException {
        JSONObject paymentDataRequest = getBaseRequest();
        paymentDataRequest.put(
                "allowedPaymentMethods",
                new JSONArray()
                        .put(getCardPaymentMethod()));
        paymentDataRequest.put("transactionInfo", getTransactionInfo());
        paymentDataRequest.put("merchantInfo", getMerchantInfo());

        return paymentDataRequest;
    }
    //endregion PART 2

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // value passed in AutoResolveHelper
            case LOAD_PAYMENT_DATA_REQUEST_CODE:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        PaymentData paymentData = PaymentData.getFromIntent(data);
                        String json = paymentData.toJson();

                        // if using gateway tokenization, pass this token without modification
                        JSONObject paymentMethodData = null;
                        try {
                            paymentMethodData = new JSONObject(json).getJSONObject("paymentMethodData");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        String paymentToken = "";
                        try {
                            paymentToken = paymentMethodData.getJSONObject("tokenizationData").getString("token");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        try {
                            Toast.makeText(this, "" + paymentMethodData.getString("description") + " - " + paymentToken, Toast.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            Toast.makeText(this, "Completado con respuesta de  datos nulos", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }

                        break;
                    case Activity.RESULT_CANCELED:
                        break;
                    case AutoResolveHelper.RESULT_ERROR:
                        Status status = AutoResolveHelper.getStatusFromIntent(data);
                        // Log the status for debugging.
                        // Generally, there is no need to show an error to the user.
                        // The Google Pay payment sheet will present any account errors.
                        break;
                    default:
                        // Do nothing.
                }
                break;
            default:
                // Do nothing.
        }
    }

}
