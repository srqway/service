package idv.hsiehpinghan.mopsservice.operator.implement;

import idv.hsiehpinghan.mopsservice.suit.TestngSuitSetting;
import idv.hsiehpinghan.seleniumutility.webelement.Select;

import java.io.IOException;

import junit.framework.Assert;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
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
		HtmlUnitDriver webDriver = operator.moveToTargetPage();
		WebElement cap = webDriver.findElement(By.cssSelector("#caption"));
		Assert.assertEquals("單一產業案例文件下載", cap.getText().trim());
	}
	
	@Test(dependsOnMethods = {"moveToTargetPage"})
	public void getMarketTypeSelector() {
		Select select = operator.getMarketTypeSelector();
		System.out.println(select);
	}
	
//	@Test
//	public void downloadFinancialReport() {
//		operator.downloadFinancialReport();
//	}
	
	private void print(Select select) {

	}
}
