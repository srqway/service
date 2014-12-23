package idv.hsiehpinghan.mopsservice.configuration;

import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration("mopsServiceSpringConfiguration")
@ComponentScan(basePackages = { "idv.hsiehpinghan.mopsservice" })
public class SpringConfiguration {
//	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	@Bean
	public HtmlUnitDriver HtmlUnitDriver() {
		return new HtmlUnitDriver(true);
	}
}
