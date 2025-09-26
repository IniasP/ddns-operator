package eu.inias.ddnsoperator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DdnsOperatorApplication {

	void main(String[] args) {
		System.setProperty("java.net.preferIPv4Stack" , "true");
		SpringApplication.run(DdnsOperatorApplication.class, args);
	}

}
