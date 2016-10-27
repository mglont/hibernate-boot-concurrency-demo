package random

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.SpringApplication
import org.springframework.transaction.annotation.EnableTransactionManagement

@SpringBootApplication
@EnableTransactionManagement
class ExampleApplication {

    static void main(String[] args) {
        SpringApplication.run ExampleApplication, args
    }
}

