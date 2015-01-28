package idv.hsiehpinghan.twseservice.operator;

import idv.hsiehpinghan.datetimeutility.utility.DateUtility;
import idv.hsiehpinghan.seleniumassistant.webelement.TextInput;
import idv.hsiehpinghan.twseservice.suit.TestngSuitSetting;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.openqa.selenium.By;
import org.springframework.context.ApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class StockClosingConditionDownloaderTest {
	private StockClosingConditionDownloader downloader;
	private Date date = DateUtility.getDate(2013, 1, 2);

	@BeforeClass
	public void beforeClass() throws IOException {
		ApplicationContext applicationContext = TestngSuitSetting
				.getApplicationContext();
		downloader = applicationContext
				.getBean(StockClosingConditionDownloader.class);
	}

	@Test
	public void moveToTargetPage() {
		downloader.moveToTargetPage();
		String breadcrumbs = downloader.getBrowser()
				.getDiv(By.cssSelector("#breadcrumbs")).getText();
		Assert.assertTrue(breadcrumbs.trim().startsWith(
				"首頁 > 交易資訊 > 盤後資訊 > 每日收盤行情"));
	}

	@Test(dependsOnMethods = { "moveToTargetPage" })
	public void inputDataDate() {
		downloader.inputDataDate(date);
		TextInput dataDateInput = downloader.getBrowser().getTextInput(
				By.id("date-field"));
		String actual = dataDateInput.getValue();
		String expected = DateUtility.getRocDateString(date, "yyyy/MM/dd");
		Assert.assertEquals(actual, expected);
	}

	@Test(dependsOnMethods = { "inputDataDate" })
	public void repeatTryDownload() {
		downloader.repeatTryDownload(date);
	}

	@Test(dependsOnMethods = { "repeatTryDownload" })
	public void downloadStockClosingCondition() throws Exception {
		File dir = downloader.downloadStockClosingCondition();
		Assert.assertTrue(dir.list().length > 0);
	}

	@Test
	public void getFileName() {
		String str = "attachment; filename=A11220130102MS2.csv";
		Assert.assertEquals(downloader.getFileName(str), "A11220130102MS2.csv");
	}

}
