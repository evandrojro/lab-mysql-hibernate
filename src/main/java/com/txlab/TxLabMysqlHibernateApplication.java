package com.txlab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TxLabMysqlHibernateApplication {

	public static void main(String[] args) {
		SpringApplication.run(TxLabMysqlHibernateApplication.class, args);
	}

}
