package idv.hsiehpinghan.stockservice.operator;

import idv.hsiehpinghan.stockdao.enumeration.DollarType;
import idv.hsiehpinghan.stockservice.suit.TestngSuitSetting;

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
		downloader = applicationContext.getBean(ExchangeRateDownloader.class);
	}

	@Test
	public void downloadExchangeRate() {
		List<DollarType> dollars = new ArrayList<DollarType>(1);
		dollars.add(DollarType.USD);
		File f = downloader.downloadExchangeRate(dollars);
		Assert.assertTrue(f.list().length > 0);
	}
}
