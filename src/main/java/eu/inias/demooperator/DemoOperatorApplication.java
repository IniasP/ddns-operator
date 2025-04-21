package eu.inias.demooperator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoOperatorApplication {

	public static void main(String[] args) {
		System.setProperty("java.net.preferIPv4Stack" , "true");
		SpringApplication.run(DemoOperatorApplication.class, args);
	}

}
