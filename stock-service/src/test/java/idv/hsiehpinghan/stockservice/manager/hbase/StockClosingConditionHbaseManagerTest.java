package idv.hsiehpinghan.stockservice.manager.hbase;

import idv.hsiehpinghan.hbaseassistant.utility.HbaseEntityTestUtility;
import idv.hsiehpinghan.stockdao.repository.hbase.StockClosingConditionRepository;
import idv.hsiehpinghan.stockservice.property.StockServiceProperty;
import idv.hsiehpinghan.stockservice.suit.TestngSuitSetting;

import java.io.File;

import junit.framework.Assert;

import org.springframework.context.ApplicationContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class StockClosingConditionHbaseManagerTest {
	private StockClosingConditionHbaseManager manager;
	private StockServiceProperty stockServiceProperty;
	private StockClosingConditionRepository condRepo;

	@BeforeClass
	public void beforeClass() throws Exception {
		ApplicationContext applicationContext = TestngSuitSetting
				.getApplicationContext();
		stockServiceProperty = applicationContext
				.getBean(StockServiceProperty.class);
		manager = applicationContext
				.getBean(StockClosingConditionHbaseManager.class);
		condRepo = applicationContext
				.getBean(StockClosingConditionRepository.class);

		dropAndCreateTable();
	}

	@Test
	public void saveStockClosingConditionOfTwseToHBase() throws Exception {
		File dir = stockServiceProperty.getStockClosingConditionDownloadDirOfTwse();
		int processedAmt = manager.saveStockClosingConditionOfTwseToHBase(dir);
		Assert.assertTrue(processedAmt > 0);
	}

	private void dropAndCreateTable() throws Exception {
		HbaseEntityTestUtility.dropAndCreateTargetTable(condRepo);
	}
}
