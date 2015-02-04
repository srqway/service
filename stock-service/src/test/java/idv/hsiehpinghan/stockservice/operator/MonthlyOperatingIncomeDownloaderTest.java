package idv.hsiehpinghan.stockservice.operator;

import idv.hsiehpinghan.seleniumassistant.webelement.Div;
import idv.hsiehpinghan.stockservice.property.StockServiceProperty;
import idv.hsiehpinghan.stockservice.suit.TestngSuitSetting;

import java.io.IOException;

import org.openqa.selenium.By;
import org.springframework.context.ApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class MonthlyOperatingIncomeDownloaderTest {
	private StockServiceProperty stockServiceProperty;
	private MonthlyOperatingIncomeDownloader downloader;

	@BeforeClass
	public void beforeClass() throws IOException {
		ApplicationContext applicationContext = TestngSuitSetting
				.getApplicationContext();
		stockServiceProperty = applicationContext
				.getBean(StockServiceProperty.class);
		downloader = applicationContext
				.getBean(MonthlyOperatingIncomeDownloader.class);
	}

	@Test
	public void moveToTargetPage() {
		downloader.moveToTargetPage();
		String text = downloader.getBrowser()
				.getDiv(By.cssSelector("#caption")).getText();
		Assert.assertEquals(text, "   採用IFRSs後之月營業收入資訊");
	}
	
	@Test(dependsOnMethods={"moveToTargetPage"})
	public void selectSearchType() {
		downloader.selectSearchType("歷史資料");
		Div yearDiv = downloader.getBrowser().getDiv(By.cssSelector("#year"));
		Assert.assertEquals(yearDiv.isDisplayed(), true);
	}
}
