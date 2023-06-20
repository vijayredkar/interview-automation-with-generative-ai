package com.interview.automation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class InterviewAutomationApplication {
	
	Log LOGGER = LogFactory.getLog(InterviewAutomationApplication.class);

    public static void main(String[] args) 
    {
        SpringApplication.run(InterviewAutomationApplication.class, args);
    }
}
