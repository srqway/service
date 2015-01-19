package idv.hsiehpinghan.mopsservice.operator;

import idv.hsiehpinghan.mopsservice.suit.TestngSuitSetting;

import java.io.IOException;

import org.springframework.context.ApplicationContext;
import org.testng.annotations.BeforeClass;

public class FinancialReportCalculatorTest {
	private FinancialReportCalculator calculator;

	@BeforeClass
	public void beforeClass() throws IOException {
		ApplicationContext applicationContext = TestngSuitSetting
				.getApplicationContext();
		calculator = applicationContext
				.getBean(FinancialReportCalculator.class);
	}

}
