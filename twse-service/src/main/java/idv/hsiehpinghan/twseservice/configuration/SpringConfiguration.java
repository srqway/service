package idv.hsiehpinghan.twseservice.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@PropertySource("classpath:/twse-service.property")
@Configuration("twseServiceSpringConfiguration")
@ComponentScan(basePackages = { "idv.hsiehpinghan.twseservice" })
public class SpringConfiguration {
	// private Logger logger = Logger.getLogger(this.getClass().getName());

}
