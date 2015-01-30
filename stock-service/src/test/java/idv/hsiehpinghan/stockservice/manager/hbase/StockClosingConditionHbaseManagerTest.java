package idv.hsiehpinghan.stockservice.manager.hbase;

import java.io.File;

import idv.hsiehpinghan.stockservice.property.StockServiceProperty;
import idv.hsiehpinghan.stockservice.suit.TestngSuitSetting;

import org.springframework.context.ApplicationContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class StockClosingConditionHbaseManagerTest {
	private StockClosingConditionHbaseManager manager;
	private StockServiceProperty stockServiceProperty;
	
	@BeforeClass
	public void beforeClass() throws Exception {
		ApplicationContext applicationContext = TestngSuitSetting
				.getApplicationContext();
		manager = applicationContext
				.getBean(StockClosingConditionHbaseManager.class);
		stockServiceProperty = applicationContext
				.getBean(StockServiceProperty.class);
	}

	@Test
	public void saveStockClosingConditionToHBase() throws Exception {
		File dir = new File(stockServiceProperty.getDownloadDir(), "closing-condition");
		manager.saveStockClosingConditionToHBase(dir);
	}
}
