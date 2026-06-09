package com.topnivo.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
@EnableTransactionManagement
@EnableScheduling
public class TopnivoBackendApplication {

	// keytool -genkey -alias https_cert -storetype PKCS12 -keyalg RSA -keysize 2048 -keystore keystore.p12 -validity 365

	public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Africa/Lagos"));

		SpringApplication.run(TopnivoBackendApplication.class, args);
	}
}

