package com.example.catalog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** The aws.* properties, same names and values as REACTIVE. */
@ConfigurationProperties(prefix = "aws")
public record AwsProperties(String region, S3 s3, Sns sns) {

    public record S3(String bucket) {}

    public record Sns(String stockDepletedTopicArn) {}
}
