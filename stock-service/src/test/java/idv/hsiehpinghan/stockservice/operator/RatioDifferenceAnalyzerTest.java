package idv.hsiehpinghan.stockservice.operator;

import idv.hsiehpinghan.stockservice.suit.TestngSuitSetting;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.springframework.context.ApplicationContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class RatioDifferenceAnalyzerTest {
	private RatioDifferenceAnalyzer analyzer;

	@BeforeClass
	public void beforeClass() throws IOException {
		ApplicationContext applicationContext = TestngSuitSetting
				.getApplicationContext();
		analyzer = applicationContext.getBean(RatioDifferenceAnalyzer.class);
	}

	@Test
	public void analyzeRatioDifference() throws Exception {
		File targetDirectory = new File("/tmp/getXbrlFromHbase");
		deleteResultFile(targetDirectory);
		File resultFile = analyzer.analyzeRatioDifference(targetDirectory);
		Assert.assertTrue(resultFile.exists());
	}

	private void deleteResultFile(File targetDirectory) {
		File resultFile = new File(targetDirectory, "result.csv");
		resultFile.delete();
	}
}
