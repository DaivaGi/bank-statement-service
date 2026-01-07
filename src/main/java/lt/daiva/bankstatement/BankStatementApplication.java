package lt.daiva.bankstatement;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(
        info = @Info(
                title = "Bank Statement API",
                version = "1.0",
                description = "Service for importing/exporting bank statements and calculating balances"
        )
)
@SpringBootApplication
public class BankStatementApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankStatementApplication.class, args);
    }

}
