package idv.hsiehpinghan.twseservice.operator;

import idv.hsiehpinghan.datetimeutility.utility.DateUtility;
import idv.hsiehpinghan.seleniumassistant.webelement.Select;
import idv.hsiehpinghan.seleniumassistant.webelement.TextInput;
import idv.hsiehpinghan.twseservice.suit.TestngSuitSetting;

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
	private String typeText = "委託及成交統計資訊";

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
	public void selectType() {
		downloader.selectType(typeText);
		Select typeSel = downloader.getBrowser().getSelect(
				By.cssSelector("#main-content > form > select"));
		// Assert.assertEquals(typeSel.getValue(), "MS2");
	}

	@Test(dependsOnMethods = { "selectType" })
	public void downloadCsv() {
		downloader.downloadCsv();
	}

	@Test
	public void getFileName() {
		String str = "attachment; filename=A11220130102MS2.csv";
		Assert.assertEquals(downloader.getFileName(str), "A11220130102MS2.csv");
	}

	// @Test(dependsOnMethods = { "selectType" })
	// public void query() {
	// downloader.query();
	// Td td = downloader
	// .getBrowser()
	// .getTd(By
	// .cssSelector("#main-content > table:nth-child(7) > thead:nth-child(1) > tr:nth-child(1) > td:nth-child(1)"));
	// String actual = td.getText();
	// String expected = DateUtility.getRocDateString(date, "yyyy年MM月dd日")
	// + typeText + " (自101年4月2日起提供)";
	// Assert.assertEquals(actual, expected);
	// }
}
