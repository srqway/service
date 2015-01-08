package idv.hsiehpinghan.mopsservice.manager.hbase;

import idv.hsiehpinghan.hdfsassistant.utility.HdfsAssistant;
import idv.hsiehpinghan.mopsservice.suit.TestngSuitSetting;

import java.io.File;
import java.io.IOException;

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class MopsHbaseManagerTest {
	private Environment env;
	private MopsHbaseManager mopsManager;
	private HdfsAssistant hdfsAssistant;
	
	@BeforeClass
	public void beforeClass() throws IOException {
		ApplicationContext applicationContext = TestngSuitSetting
				.getApplicationContext();
		env = applicationContext.getBean(Environment.class);
		mopsManager = applicationContext
				.getBean(MopsHbaseManager.class);
		hdfsAssistant = applicationContext.getBean(HdfsAssistant.class);
	}
	
	private File getXbrlDir() {
		String eProp = env.getProperty("mops-service.extract_dir");
		return new File(eProp, "xbrl");
	}
//	@Test
//	public void saveFinancialReportToDatabase() {
//		String eProp = env.getProperty("mops-service.extract_dir");
//		File xbrlDirectory = new File(eProp);
//		mopsManager.saveFinancialReportToDatabase(xbrlDirectory);
//	}
}
