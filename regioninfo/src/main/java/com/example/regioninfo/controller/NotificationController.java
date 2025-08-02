package com.example.regioninfo.controller;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicRequest;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicResponse;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.Subscription;
import software.amazon.awssdk.services.sns.model.UnsubscribeRequest;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    
    private final SnsClient snsClient;


    public NotificationController(SnsClient snsClient) {
        this.snsClient = snsClient;
    }

    @Value("${aws.sns.topicArn}")
    private String topicArn;
    
    @PostMapping("/subscribe")
    public ResponseEntity<String> subscribe(@RequestParam String email) {
        SubscribeRequest request = SubscribeRequest.builder()
            .topicArn(topicArn)    
            .protocol("email")
            .endpoint(email)
            .build();

        snsClient.subscribe(request);

        return ResponseEntity.ok("Confirmation email sent to " + email);
    }

@PostMapping("/unsubscribe")
public ResponseEntity<String> unsubscribe(@RequestParam String email) {
    ListSubscriptionsByTopicResponse response = snsClient.listSubscriptionsByTopic(
            ListSubscriptionsByTopicRequest.builder()
                    .topicArn(topicArn)
                    .build());

    for (Subscription sub : response.subscriptions()) {
        if (email.equalsIgnoreCase(sub.endpoint())) {
            String subscriptionArn = sub.subscriptionArn();

            // Skip unsubscribing "PendingConfirmation"
            if ("PendingConfirmation".equals(subscriptionArn)) {
                return ResponseEntity.badRequest().body("Email not yet confirmed: " + email);
            }

            snsClient.unsubscribe(UnsubscribeRequest.builder()
                    .subscriptionArn(subscriptionArn)
                    .build());
            return ResponseEntity.ok("Unsubscribed " + email);
        }
    }

    return ResponseEntity.status(404).body("Subscription not found for " + email);
}

}
