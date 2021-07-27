#!/usr/bin/env bash

read -p "Enter Bucket Name to sync with or just press enter to build only: " BUCKET_NAME

if [ "$BUCKET_NAME" != "" ]; then
  echo "All code will be sync to bucket $BUCKET_NAME."
else
  BUCKET_NAME="awslogaggregator-logaccount"
  echo "All code will be sync to default bucket $BUCKET_NAME."
fi

APP_VERSION="v0.0.1"

echo "App version v0.0.1"

# Copy code to S3 if bucket configured
if [ "$BUCKET_NAME" != "" ]; then

  read -p "Do you want to build app and lambda? (y/n) :" BUILD
  if [ "$BUILD" = "y" ]; then
    #clean up zip file
    rm -r target

    echo "Building EMR app"
    # build application
    mvn install

    # Zip and copy Zipped module
    cd automation/lambda/handlers/
    zip -r ../../../target/lambda.zip .
    cd ../../../
  else
    echo "skipping app"
  fi

  read -p "Do you want to copy app? (y/n) :" APP
  if [ "$APP" = "y" ]; then
    aws s3 cp target/aws-logaggregator-v1.jar s3://$BUCKET_NAME/$APP_VERSION/app/
  else
    echo "skipping app"
  fi

  # Sync all config s3 config location
  read -p "Do you want to copy config? (y/n) :" CONFIG
  if [ "$CONFIG" = "y" ]; then
    aws s3 cp config/ s3://$BUCKET_NAME/$APP_VERSION/config/ --recursive
  else
    echo "skipping config"
  fi

  # Sync all Lambda s3 lambda location
  read -p "Do you want to copy dependencies? (y/n) :" DEPENDENCIES
  if [ "$DEPENDENCIES" = "y" ]; then
    aws s3 cp automation/dependencies s3://$BUCKET_NAME/$APP_VERSION/dependencies/ --recursive
  else
    echo "skipping dependencies"
  fi

  # Sync all Lambda s3 lambda location
  read -p "Do you want to copy lambda? (y/n) :" LAMBDA
  if [ "$LAMBDA" = "y" ]; then
    aws s3 cp target/lambda.zip s3://$BUCKET_NAME/$APP_VERSION/lambda/
  else
    echo "skipping lambda"
  fi

  # Sync all template s3 template location
  read -p "Do you want to copy template? (y/n) :" TEMPLATE
  if [ "$TEMPLATE" = "y" ]; then
    aws s3 cp automation/template/ s3://$BUCKET_NAME/$APP_VERSION/template/ --recursive
  else
    echo "skipping template"
  fi

  # Sync all bootstrap s3 bootstrap location
  read -p "Do you want to copy bootstrap? (y/n) :" BOOTSTRAP
  if [ "$BOOTSTRAP" = "y" ]; then
    aws s3 cp automation/bootstrap/ s3://$BUCKET_NAME/$APP_VERSION/bootstrap/ --recursive
  else
    echo "skipping bootstrap"
  fi
  # Create logs bucket
#  aws s3api put-object --bucket $BUCKET_NAME --key emr_logs/
fi
