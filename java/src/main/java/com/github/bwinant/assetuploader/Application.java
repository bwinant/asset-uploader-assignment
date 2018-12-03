package com.github.bwinant.assetuploader;

import javax.sql.DataSource;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class Application
{
    @Profile("default")
    @Bean
    public AWSCredentialsProvider envCredentialsProvider()
    {
        return new EnvironmentVariableCredentialsProvider();
    }

    @Profile("dev")
    @Bean
    public AWSCredentialsProvider profileCredentialsProvider(@Value("${aws.profile}") String profileName)
    {
        return new ProfileCredentialsProvider(profileName);
    }

    @Bean
    public AmazonS3 s3Client(AWSCredentialsProvider credentialsProvider, @Value("${aws.region}") String region)
    {
        return AmazonS3ClientBuilder.standard()
            .withCredentials(credentialsProvider)
            .withRegion(region)
            .build();
    }
    
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource)
    {
        return new JdbcTemplate(dataSource);
    }

    public static void main(String[] args)
    {
        SpringApplication springApp = new SpringApplication(Application.class);
        springApp.setHeadless(true);
        springApp.run(args);
    }
}
