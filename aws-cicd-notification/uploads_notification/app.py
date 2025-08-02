import json
import boto3
import os

sns = boto3.client('sns')
TOPIC_ARN = os.environ['TOPIC_ARN']

def lambda_handler(event, context):
    print("Incoming events:", json.dumps(event))

    records = event.get('Records', [])
    if not records:
        print("No record found.")
        return {'statusCode': 200, 'body': 'No records'}

    for record in records:
        try:
            body = json.loads(record['body'])
            image_url = body.get('downloadUrl', 'No URL found')

            message = f"A image was uploaded \n\nURL: {image_url}"

            sns.publish(
                TopicArn=TOPIC_ARN,
                Message=message,
                Subject="New Upload Notifications"
            )
            print("✅ Message published to SNS check email")

        except Exception as e:
            print("❌ Error processing record", str(e))

    return {
        'statusCode': 200,
        'body': 'Processed'
    }
