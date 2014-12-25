package idv.hsiehpinghan.mopsservice.operator;

import idv.hsiehpinghan.mopsservice.suit.TestngSuitSetting;
import idv.hsiehpinghan.seleniumassistant.browser.BrowserBase;
import idv.hsiehpinghan.seleniumassistant.webelement.Select;

import java.io.IOException;

import junit.framework.Assert;

import org.openqa.selenium.By;
import org.springframework.context.ApplicationContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class FinancialReportOperatorTest {
	private FinancialReportOperator operator;

	@BeforeClass
	public void beforeClass() throws IOException {
		ApplicationContext applicationContext = TestngSuitSetting
				.getApplicationContext();
		operator = applicationContext.getBean(FinancialReportOperator.class);
	}

	@Test
	public void moveToTargetPage() {
		operator.moveToTargetPage();
		BrowserBase browser = operator.getBrowser();
		String capText = browser.getDiv(By.cssSelector("#caption")).getText();
		Assert.assertEquals("單一產業案例文件下載", capText.trim());
	}

	@Test(dependsOnMethods = { "moveToTargetPage" })
	public void getMarketTypeSelect() {
		Select select = operator.getMarketTypeSelect();
		Assert.assertEquals(4, select.getOptions().size());
	}

	@Test(dependsOnMethods = { "getMarketTypeSelect" })
	public void downloadFinancialReport() {
		operator.downloadFinancialReport();
	}

}
