package idv.hsiehpinghan.twseservice.suit;

import idv.hsiehpinghan.objectutility.utility.ClassUtility;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.testng.annotations.BeforeSuite;

public class TestngSuitSetting {
	public static final String URL_BASE = "http://127.0.0.1:8080/";

	private static AnnotationConfigApplicationContext applicationContext;

	@BeforeSuite
	public void beforeSuite() throws Exception {
		Class<?>[] clsArr = ClassUtility.getAnnotatedClasses(
				"idv.hsiehpinghan", Configuration.class);
		applicationContext = new AnnotationConfigApplicationContext(clsArr);
		// firefoxBrowser = applicationContext.getBean(FirefoxBrowser.class);
		// htmlUnitFirefoxVersionBrowser = applicationContext
		// .getBean(HtmlUnitFirefoxVersionBrowser.class);
	}

	public static ApplicationContext getApplicationContext() {
		return applicationContext;
	}
}
