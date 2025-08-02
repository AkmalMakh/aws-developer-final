// package com.example.regioninfo.component;

// import jakarta.annotation.PostConstruct;
// import org.springframework.scheduling.annotation.Scheduled;
// import org.springframework.stereotype.Component;
// import software.amazon.awssdk.services.sns.SnsClient;
// import software.amazon.awssdk.services.sns.model.PublishRequest;
// import software.amazon.awssdk.services.sqs.SqsClient;
// import software.amazon.awssdk.services.sqs.model.*;

// import java.time.LocalDateTime;
// import java.util.List;
// import java.util.logging.Logger;

// @Component
// public class SqsToSnsProcessor {

//     private static final Logger logger = Logger.getLogger(SqsToSnsProcessor.class.getName());

//     private final SqsClient sqsClient;
//     private final SnsClient snsClient;

//     private final String sqsQueueUrl = "https://sqs.us-east-1.amazonaws.com/151182332702/akmal-UploadsNotificationQueue";
//     private final String topicArn = "arn:aws:sns:us-east-1:151182332702:akmal-UploadsNotificationTopic";

//     public SqsToSnsProcessor(SqsClient sqsClient, SnsClient snsClient) {
//         this.sqsClient = sqsClient;
//         this.snsClient = snsClient;
//     }

//     @PostConstruct
//     public void init() {
//         logger.info("SQS → SNS processor initialized");
//     }

//     @Scheduled(fixedDelay = 3000)
//     public void processMessages() {
//         try {
//             System.out.println("Scheduled job triggered at: " + LocalDateTime.now());
//             ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
//                     .queueUrl(sqsQueueUrl)
//                     .maxNumberOfMessages(5)
//                     .waitTimeSeconds(5)
//                     .build();

//             List<Message> messages = sqsClient.receiveMessage(receiveRequest).messages();

//             for (Message message : messages) {
//                 logger.info("Processing SQS message: " + message.body());

//                 // Publish to SNS
//                 PublishRequest publishRequest = PublishRequest.builder()
//                         .topicArn(topicArn)
//                         .subject("Image Uploaded")
//                         .message(message.body())
//                         .build();
//                 snsClient.publish(publishRequest);
//                 logger.info("Published to SNS");

//                 // Delete message
//                 DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
//                         .queueUrl(sqsQueueUrl)
//                         .receiptHandle(message.receiptHandle())
//                         .build();
//                 sqsClient.deleteMessage(deleteRequest);
//                 logger.info("Deleted from SQS");
//             }

//         } catch (Exception e) {
//             logger.severe("Error in SQS to SNS processor: " + e.getMessage());
//             e.printStackTrace();
//         }
//     }
// }
