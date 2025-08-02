def lambda_handler(event, context):
    print("✅ Pre-traffic hook executed")
    return {
        'status': 'Succeeded'
    }