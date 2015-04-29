package idv.hsiehpinghan.stockservice.operator;

import idv.hsiehpinghan.datetimeutility.utility.DateUtility;
import idv.hsiehpinghan.seleniumassistant.browser.HtmlUnitFirefoxVersionBrowser;
import idv.hsiehpinghan.stockservice.operator.StockClosingConditionOfTwseDownloader.RunnableDownloader;
import idv.hsiehpinghan.stockservice.property.StockServiceProperty;
import idv.hsiehpinghan.stockservice.suit.TestngSuitSetting;
import idv.hsiehpinghan.threadutility.utility.ThreadPoolUtility;
import idv.hsiehpinghan.threadutility.utility.ThreadUtility;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.time.DateUtils;
import org.openqa.selenium.By;
import org.springframework.context.ApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class StockClosingConditionOfTwseDownloader_RunnableDownloaderTest {
	private ApplicationContext applicationContext;
	private Date beginDate = DateUtility.getDate(2015, 1, 1);
	private Date endDate = DateUtility.getDate(2015, 2, 1);
	private HtmlUnitFirefoxVersionBrowser browser;
	private RunnableDownloader runnableDownloader;
	
	@BeforeClass
	public void beforeClass() throws IOException {
		applicationContext = TestngSuitSetting
				.getApplicationContext();
		runnableDownloader = applicationContext.getBean(RunnableDownloader.class, beginDate, endDate);
		browser = applicationContext.getBean(HtmlUnitFirefoxVersionBrowser.class);
	}
	
	@Test
	@SuppressWarnings("static-access")
	public void moveToTargetPage() {
		runnableDownloader.moveToTargetPage(browser);
		String breadcrumbs = browser.getDiv(By.cssSelector("#breadcrumbs")).getText();
		Assert.assertTrue(breadcrumbs.trim().startsWith(
				"首頁 > 交易資訊 > 盤後資訊 > 每日收盤行情"));
	}

//	@Test
//	public void run() {
//		Collection<Runnable> runnables = generateRunnables(1);
//		ThreadPoolUtility.submitWithCachedThreadPool(runnables);
//	}
	
	private Set<Runnable> generateRunnables(int runnableAmout) {
		Set<Runnable> runnables = new HashSet<Runnable>(runnableAmout);
		for (int i = 0; i < runnableAmout; ++i) {
			Date beginDate = DateUtils.addDays(this.beginDate, i);
			Date endDate = DateUtils.addDays(this.endDate, i);
			Runnable runnable = applicationContext.getBean(RunnableDownloader.class, beginDate, endDate);
			runnables.add(runnable);
		}
		return runnables;
	}
}
