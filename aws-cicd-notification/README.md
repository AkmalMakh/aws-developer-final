# 📦 Uploads Notification System (AWS Lambda + SAM + CI/CD)

A serverless image notification system that sends an email alert whenever a new image is uploaded to an S3 bucket. Built with AWS Lambda (Python), orchestrated using the AWS Serverless Application Model (SAM), and fully automated through a CI/CD pipeline using CodeBuild and CodePipeline.

---

## 🚀 Features

- ✅ Event-driven and fully serverless architecture
- ✅ Real-time email notifications via Amazon SNS
- ✅ SQS-based decoupled Lambda triggering
- ✅ SAM-based infrastructure as code
- ✅ GitHub-integrated CI/CD with AWS CodeBuild + CodePipeline
- ✅ Auto-deploys on every GitHub commit

---


## 🧩 Architecture Overview
```plaintext
GitHub Repo
    │
    ▼
CodePipeline ──▶ CodeBuild (runs `sam build` + `sam package`)
    │
    ▼
CloudFormation (deploys Lambda + infra)
    │
    ▼
S3 (Image uploaded)
    │
    ▼
SQS Queue ──▶ Lambda Function ──▶ SNS Email Notification
