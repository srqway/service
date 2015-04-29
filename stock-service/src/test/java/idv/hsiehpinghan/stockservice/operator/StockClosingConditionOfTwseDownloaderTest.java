package idv.hsiehpinghan.stockservice.operator;

import idv.hsiehpinghan.stockservice.suit.TestngSuitSetting;

import java.io.IOException;

import org.springframework.context.ApplicationContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class StockClosingConditionOfTwseDownloaderTest {

	@BeforeClass
	public void beforeClass() throws IOException {
		ApplicationContext applicationContext = TestngSuitSetting
				.getApplicationContext();

	}
	
	@Test
	public void ttt() {

	}
}
