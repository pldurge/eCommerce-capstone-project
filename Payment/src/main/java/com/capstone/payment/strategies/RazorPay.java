package com.capstone.payment.strategies;

import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class RazorPay implements IPaymentStrategy{

    private final RazorpayClient razorPayClient;

    public RazorPay(RazorpayClient razorPayClient) {
        this.razorPayClient = razorPayClient;
    }


    @Override
    public String generatePaymentLink(String orderId, String name, String email, String phoneNumber, Long amount, String gateway) throws Exception {
        JSONObject paymentLinkRequest = new JSONObject();
        paymentLinkRequest.put("amount",amount);
        paymentLinkRequest.put("currency","INR");
        paymentLinkRequest.put("accept_partial",false);
        paymentLinkRequest.put("expire_by",System.currentTimeMillis() + 10*60*100); //expiry is 10 min
        paymentLinkRequest.put("reference_id", orderId);
        paymentLinkRequest.put("description","Payment request for Payment service class on Scaler");

        JSONObject customer = new JSONObject();
        customer.put("name", name);
        customer.put("contact",phoneNumber);
        customer.put("email",email);

        paymentLinkRequest.put("customer",customer);

        JSONObject notify = new JSONObject();
        notify.put("sms",true);
        notify.put("email",true);

        paymentLinkRequest.put("notify", notify);
        paymentLinkRequest.put("reminder_enable",true);

        JSONObject notes = new JSONObject();
        notes.put("policy_name","Jeevan Bima");

        paymentLinkRequest.put("notes",notes);

        paymentLinkRequest.put("callback_url","https://www.scaler.com/");
        paymentLinkRequest.put("callback_method","get");

        PaymentLink payment = razorPayClient.paymentLink.create(paymentLinkRequest);
        return payment.get("short_url");
    }
}
