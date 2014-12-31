package idv.hsiehpinghan.botservice.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@PropertySource("classpath:bot-service.property")
@Configuration("botServiceSpringConfiguration")
@ComponentScan(basePackages = { "idv.hsiehpinghan.botservice" })
public class SpringConfiguration {
	// private Logger logger = Logger.getLogger(this.getClass().getName());

}
