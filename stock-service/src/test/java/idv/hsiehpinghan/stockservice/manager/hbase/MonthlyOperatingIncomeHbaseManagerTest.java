package idv.hsiehpinghan.stockservice.manager.hbase;


public class MonthlyOperatingIncomeHbaseManagerTest {
	// private MonthlyOperatingIncomeHbaseManager manager;
	// private StockServiceProperty stockServiceProperty;
	// private MonthlyOperatingIncomeRepository incomeRepo;
	//
	// @BeforeClass
	// public void beforeClass() throws Exception {
	// ApplicationContext applicationContext = TestngSuitSetting
	// .getApplicationContext();
	// stockServiceProperty = applicationContext
	// .getBean(StockServiceProperty.class);
	// manager = applicationContext
	// .getBean(MonthlyOperatingIncomeHbaseManager.class);
	// incomeRepo = applicationContext
	// .getBean(MonthlyOperatingIncomeRepository.class);
	//
	// dropAndCreateTable();
	// }
	//
	// @Test
	// public void saveMonthlyOperatingIncomeToHBase() throws Exception {
	// File dir = stockServiceProperty.getMonthlyOperatingIncomeDownloadDir();
	// truncateProcessedLogFle(dir);
	// int processedAmt = manager.saveMonthlyOperatingIncomeToHBase(dir);
	// Assert.assertTrue(processedAmt > 0);
	// }
	//
	// private void truncateProcessedLogFle(File dir) throws IOException {
	// File processedLog = new File(dir, "processed.log");
	// FileUtils.write(processedLog, "", false);
	// }
	//
	// private void dropAndCreateTable() throws Exception {
	// HbaseEntityTestUtility.dropAndCreateTargetTable(incomeRepo);
	// }
}
