/*
 * Copyright (c) 2013 Mark Prichard, CloudBees
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloudbees.gasp.services;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Config implements ServletContextListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class.getName());

    // APN Cert/Key PEM filenames: read via ClassLoader
    private static final String apnsCertFilename = "apnsappcert.pem";
    private static final String apnsKeyFilename = "apnsappkey.pem";

    // AWS Credentials properties file: read via ClassLoader
    private static final String awsCredentialsFilename = "AwsCredentials.properties";
    private final InputStream awsCredentials = this
                                               .getClass()
                                               .getClassLoader()
                                               .getResourceAsStream(awsCredentialsFilename);

    //TODO: Remove when registration API is hooked up
    private static String apnsToken;
    private static String gcmRegistrationId;

    // Apple Development iOS Push Services Certificate and Private Key
    private static String apnsCertificate;
    private static String apnsKey;

    // Google Cloud Messaging API Key
    private static String gcmApiKey;

    // AWS SNS Client object
    private static AmazonSNS amazonSNS;


    public static String getApnsCertificate() {
        return apnsCertificate;
    }

    public static String getApnsKey() {
        return apnsKey;
    }

    public static String getGcmApiKey() {
        return gcmApiKey;
    }

    public static AmazonSNS getAmazonSNS() {
        return amazonSNS;
    }

    public static String getApnsCertFilename() {
        return apnsCertFilename;
    }

    public static String getApnsKeyFilename() {
        return apnsKeyFilename;
    }

    public static String getAwsCredentialsFilename() {
        return awsCredentialsFilename;
    }

    // Utility class to read PEM file into a String
    private String getPemFromStream(String Filename) {
        String pemData = new String();

        // Load cert/key file via ClassLoader
        InputStream is = this.getClass()
                             .getClassLoader()
                             .getResourceAsStream(Filename);

        // Read PEM cert/key file from InputStream
        InputStreamReader isr = new InputStreamReader ( is ) ;
        BufferedReader reader = new BufferedReader ( isr ) ;

        try{
            String readString = reader.readLine ( ) ;
            while ( readString != null ) {
                // Add newline to each row of PEM cert/key
                pemData = pemData + readString + '\n';
                readString = reader.readLine ( ) ;
            }
            isr.close ( ) ;
        } catch ( IOException ioe ) {
            ioe.printStackTrace ( ) ;
        }
        // Remove trailing newline
        return StringUtils.chomp(pemData);
    }

    public void contextInitialized(ServletContextEvent event) {
        try {
            //TODO: (temporary) get APNS device token for testing from system property
            //TODO: Devices will register from didRegisterForRemoteNotificationsWithDeviceToken
            if ((apnsToken = System.getProperty("APNS_TOKEN")) != null) {
                APNDataStore.register(apnsToken);
            }
            else {
                LOGGER.error("APNS_TOKEN not set");
            }

            // TODO: (temporary) get GCM registration ID for testing from system property
            // TODO: Devices will register from onCreate() in the main Activity
            if ((gcmRegistrationId = System.getProperty("GCM_REGID")) != null) {
                GCMDataStore.register(gcmRegistrationId);
            }
            else {
                LOGGER.error("GCM_REGID not set");
            }

            if ((gcmApiKey = System.getProperty("GCM_APIKEY")) == null) {
                LOGGER.error("GCM_APIKEY not set");
            }

            // Read APN iOS Push Service Certificate from ClassLoader
            apnsCertificate = getPemFromStream(apnsCertFilename);
            LOGGER.debug("Read APNS certificate from: " + getApnsCertFilename());
            LOGGER.debug('\n' + apnsCertificate);

            // Read APN iOS Push Service Private Key from ClassLoader
            apnsKey = getPemFromStream(apnsKeyFilename);
            LOGGER.debug("Read APNS private key from: " + getApnsKeyFilename());
            LOGGER.debug('\n' + apnsKey);

            // Read AWS credentials and create new SNS client
            amazonSNS = new AmazonSNSClient(new PropertiesCredentials(awsCredentials));
            LOGGER.debug("Read AWS Credentials from: " + getAwsCredentialsFilename());
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public void contextDestroyed(ServletContextEvent event) {
    }
}