package idv.hsiehpinghan.stockservice.manager.hbase;

import idv.hsiehpinghan.hbaseassistant.utility.HbaseEntityTestUtility;
import idv.hsiehpinghan.stockdao.repository.hbase.CompanyBasicInfoRepository;
import idv.hsiehpinghan.stockservice.property.StockServiceProperty;
import idv.hsiehpinghan.stockservice.suit.TestngSuitSetting;

import java.io.File;

import junit.framework.Assert;

import org.springframework.context.ApplicationContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class CompanyBasicInfoHbaseManagerTest {
	private CompanyBasicInfoHbaseManager manager;
	private StockServiceProperty stockServiceProperty;
	private CompanyBasicInfoRepository comInfoRepo;

	@BeforeClass
	public void beforeClass() throws Exception {
		ApplicationContext applicationContext = TestngSuitSetting
				.getApplicationContext();
		stockServiceProperty = applicationContext
				.getBean(StockServiceProperty.class);
		manager = applicationContext
				.getBean(CompanyBasicInfoHbaseManager.class);
		comInfoRepo = applicationContext
				.getBean(CompanyBasicInfoRepository.class);

		dropAndCreateTable();
	}

	@Test
	public void saveCompanyBasicInfoToHBase() throws Exception {
		manager.truncateProcessedLog();
		File dir = stockServiceProperty.getCompanyBasicInfoDownloadDir();
		int processedAmt = manager.saveCompanyBasicInfoToHBase(dir);
		Assert.assertTrue(processedAmt > 0);
	}

	private void dropAndCreateTable() throws Exception {
		HbaseEntityTestUtility.dropAndCreateTargetTable(comInfoRepo);
	}
}
