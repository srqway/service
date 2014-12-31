package idv.hsiehpinghan.botservice.operator;

import idv.hsiehpinghan.botservice.enumeration.Dollar;
import idv.hsiehpinghan.botservice.suit.TestngSuitSetting;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.springframework.context.ApplicationContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ExchangeRateDownloaderTest {
	private ExchangeRateDownloader downloader;
	
	@BeforeClass
	public void beforeClass() throws IOException {
		ApplicationContext applicationContext = TestngSuitSetting
				.getApplicationContext();
		downloader = applicationContext
				.getBean(ExchangeRateDownloader.class);
	}

	@Test
	public void downloadExchangeRate() {
		List<Dollar> dollars = new ArrayList<Dollar>(1);
		dollars.add(Dollar.USD);
		File f = downloader.downloadExchangeRate(dollars);
		Assert.assertTrue(f.list().length > 0);
	}
}
