package com.drawer.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI drawerOpenAPI() {
		return new OpenAPI()
			.info(new Info()
				.title("그림판 서버")
				.description("JSON 기반 그림판 API 문서")
				.version("v1.0.0")
				.license(new License().name("Apache 2.0").url("http://springdoc.org")));
	}
}

