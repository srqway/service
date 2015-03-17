package idv.hsiehpinghan.stockservice.operator;

import idv.hsiehpinghan.stockservice.suit.TestngSuitSetting;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.springframework.context.ApplicationContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class FinancialReportAnalyzerTest {
	private FinancialReportAnalyzer analyzer;
	
	@BeforeClass
	public void beforeClass() throws IOException {
		ApplicationContext applicationContext = TestngSuitSetting
				.getApplicationContext();
		analyzer = applicationContext.getBean(FinancialReportAnalyzer.class);
	}

	@Test
	public void analyzeRatioDifference() throws Exception {
		File targetDirectory = new File("/tmp/getXbrlFromHbase");
		File logFile = new File(FileUtils.getTempDirectory(), "analyzeRatioDifference.log");
		analyzer.analyzeRatioDifference(targetDirectory, logFile);
	}
}
