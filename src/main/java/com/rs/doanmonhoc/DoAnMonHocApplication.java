package com.rs.doanmonhoc;

import com.rs.doanmonhoc.config.AttendanceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AttendanceProperties.class)
public class DoAnMonHocApplication {

    public static void main(String[] args) {
        SpringApplication.run(DoAnMonHocApplication.class, args);
    }

}
