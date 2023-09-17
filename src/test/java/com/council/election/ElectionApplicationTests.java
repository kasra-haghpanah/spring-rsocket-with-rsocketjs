package com.council.election;

import com.council.election.configuration.property.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@ComponentScan(basePackages = "com.council.election")
@Import({Properties.class})

@SpringBootTest(classes={Properties.class})
class ElectionApplicationTests {

    @Test
    void contextLoads() {
    }

}
