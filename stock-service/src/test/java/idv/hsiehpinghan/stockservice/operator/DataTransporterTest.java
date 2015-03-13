package idv.hsiehpinghan.stockservice.operator;

import idv.hsiehpinghan.stockservice.suit.TestngSuitSetting;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.springframework.context.ApplicationContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class DataTransporterTest {
	private DataTransporter transporter;

	@BeforeClass
	public void beforeClass() throws IOException {
		ApplicationContext applicationContext = TestngSuitSetting
				.getApplicationContext();
		transporter = applicationContext.getBean(DataTransporter.class);
	}

	@Test
	public void getXbrlFromHbase() throws Exception {
		File targetDirectory = new File(FileUtils.getTempDirectory(),
				"getXbrlFromHbase");
//		transporter.getXbrlFromHbase(targetDirectory);
	}
}
