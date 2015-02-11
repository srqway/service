package idv.hsiehpinghan.stockservice.manager.hbase;


public class StockClosingConditionHbaseManagerTest {
	// private StockClosingConditionHbaseManager manager;
	// private StockServiceProperty stockServiceProperty;
	// private StockClosingConditionRepository condRepo;
	//
	// @BeforeClass
	// public void beforeClass() throws Exception {
	// ApplicationContext applicationContext = TestngSuitSetting
	// .getApplicationContext();
	// stockServiceProperty = applicationContext
	// .getBean(StockServiceProperty.class);
	// manager = applicationContext
	// .getBean(StockClosingConditionHbaseManager.class);
	// condRepo = applicationContext
	// .getBean(StockClosingConditionRepository.class);
	//
	// // dropAndCreateTable();
	// }
	//
	// @Test
	// public void saveStockClosingConditionOfTwseToHBase() throws Exception {
	// File dir = stockServiceProperty
	// .getStockClosingConditionDownloadDirOfTwse();
	// truncateProcessedLogFle(dir);
	// int processedAmt = manager.saveStockClosingConditionOfTwseToHBase(dir);
	// Assert.assertTrue(processedAmt > 0);
	// }
	//
	// @Test
	// public void saveStockClosingConditionOfGretaiToHBase() throws Exception {
	// File dir = stockServiceProperty
	// .getStockClosingConditionDownloadDirOfGretai();
	// truncateProcessedLogFle(dir);
	// int processedAmt = manager
	// .saveStockClosingConditionOfGretaiToHBase(dir);
	// Assert.assertTrue(processedAmt > 0);
	// }
	//
	// private void truncateProcessedLogFle(File dir) throws IOException {
	// File processedLog = new File(dir, "processed.log");
	// FileUtils.write(processedLog, "", false);
	// }
	//
	// private void dropAndCreateTable() throws Exception {
	// HbaseEntityTestUtility.dropAndCreateTargetTable(condRepo);
	// }
}
