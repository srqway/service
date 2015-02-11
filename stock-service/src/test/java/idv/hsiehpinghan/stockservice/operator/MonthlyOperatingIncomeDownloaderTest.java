package idv.hsiehpinghan.stockservice.operator;


public class MonthlyOperatingIncomeDownloaderTest {
	// private StockServiceProperty stockServiceProperty;
	// private MonthlyOperatingIncomeDownloader downloader;
	// private String stockCode = "2330";
	// private Date date = DateUtility.getDate(2014, 6, 9);
	//
	// @BeforeClass
	// public void beforeClass() throws IOException {
	// ApplicationContext applicationContext = TestngSuitSetting
	// .getApplicationContext();
	// stockServiceProperty = applicationContext
	// .getBean(StockServiceProperty.class);
	// downloader = applicationContext
	// .getBean(MonthlyOperatingIncomeDownloader.class);
	// }
	//
	// @Test
	// public void moveToTargetPage() {
	// downloader.moveToTargetPage();
	// String text = downloader.getBrowser()
	// .getDiv(By.cssSelector("#caption")).getText();
	// Assert.assertEquals(text, "   採用IFRSs後之月營業收入資訊");
	// }
	//
	// @Test(dependsOnMethods = { "moveToTargetPage" })
	// public void selectSearchType() {
	// downloader.selectSearchType("歷史資料");
	// Div yearDiv = downloader.getBrowser().getDiv(By.cssSelector("#year"));
	// Assert.assertEquals(yearDiv.isDisplayed(), true);
	// }
	//
	// @Test(dependsOnMethods = { "selectSearchType" })
	// public void inputStockCode() {
	// downloader.inputStockCode(stockCode);
	// TextInput input = downloader.getBrowser().getTextInput(
	// By.cssSelector("#co_id"));
	// Assert.assertEquals(input.getValue(), stockCode);
	// }
	//
	// @Test(dependsOnMethods = { "inputStockCode" })
	// public void inputYear() {
	// downloader.inputYear(date);
	// TextInput input = downloader.getBrowser().getTextInput(
	// By.cssSelector("#year"));
	// int year = DateUtility.getRocYear(date);
	// Assert.assertEquals(input.getValue(), String.valueOf(year));
	// }
	//
	// @Test(dependsOnMethods = { "inputYear" })
	// public void selectMonth() {
	// downloader.selectMonth(date);
	// Select sel = downloader.getBrowser()
	// .getSelect(By.cssSelector("#month"));
	// int month = DateUtility.getMonth(date);
	// Assert.assertEquals(sel.getSelectedText(), String.valueOf(month));
	// }
	//
	// @Test(dependsOnMethods = { "selectMonth" })
	// public void repeatTryDownload() {
	// try {
	// downloader.repeatTryDownload("2330", date);
	// } catch (Exception e) {
	// System.err.println(downloader.getBrowser().getWebDriver()
	// .getPageSource());
	// throw new RuntimeException(e);
	// }
	// String fileName = downloader.getFileName(stockCode, date);
	// File dir = stockServiceProperty.getMonthlyOperatingIncomeDownloadDir();
	// Assert.assertTrue(ArrayUtils.contains(dir.list(), fileName));
	// }
	//
	// @Test(dependsOnMethods = { "repeatTryDownload" })
	// public void downloadMonthlyOperatingIncome() throws Exception {
	// File dir = downloader.downloadMonthlyOperatingIncome();
	// Date date = DateUtility.getDate(2013, 1, 22);
	// String fileName = downloader.getFileName(stockCode, date);
	// Assert.assertTrue(ArrayUtils.contains(dir.list(), fileName));
	// }
	//
	// @Test
	// public void getStockCodes() throws Exception {
	// String[] stockCodes = downloader.getStockCodes();
	// Assert.assertTrue(0 < stockCodes.length);
	// }
}
