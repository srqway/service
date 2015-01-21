package idv.hsiehpinghan.botservice.suit;

import idv.hsiehpinghan.objectutility.utility.ClassUtility;

import java.io.IOException;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.testng.annotations.BeforeSuite;

public class TestngSuitSetting {
	private static AnnotationConfigApplicationContext applicationContext;

	@BeforeSuite()
	public void beforeSuite() throws IOException, ClassNotFoundException {
		Class<?>[] clsArr = ClassUtility.getAnnotatedClasses(
				"idv.hsiehpinghan", Configuration.class);
		applicationContext = new AnnotationConfigApplicationContext(clsArr);
	}

	public static ApplicationContext getApplicationContext() {
		return applicationContext;
	}

}
