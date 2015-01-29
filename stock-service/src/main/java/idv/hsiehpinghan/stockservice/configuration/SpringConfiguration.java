package idv.hsiehpinghan.stockservice.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@PropertySource("classpath:/stock-service.property")
@Configuration("stockServiceSpringConfiguration")
@ComponentScan(basePackages = { "idv.hsiehpinghan.stockservice" })
public class SpringConfiguration {
	// private Logger logger = Logger.getLogger(this.getClass().getName());

}
