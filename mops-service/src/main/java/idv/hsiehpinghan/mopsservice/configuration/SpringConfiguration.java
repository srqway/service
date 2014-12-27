package idv.hsiehpinghan.mopsservice.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@PropertySource("classpath:mops-service.property")
@Configuration("mopsServiceSpringConfiguration")
@ComponentScan(basePackages = { "idv.hsiehpinghan.mopsservice" })
public class SpringConfiguration {
	// private Logger logger = Logger.getLogger(this.getClass().getName());

}
