package idv.hsiehpinghan.stockservice.manager.hbase;

import idv.hsiehpinghan.stockdao.repository.hbase.TaxonomyRepository;
import idv.hsiehpinghan.stockservice.suit.TestngSuitSetting;
import idv.hsiehpinghan.xbrlassistant.enumeration.XbrlTaxonomyVersion;

import org.springframework.context.ApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class FinancialReportHbaseManagerTest {
	// private final String DATE_PATTERN = "yyyyMMdd";
	// private String allStockCode =
	// StockDownloadInfo.StockCodeFamily.StockCodeQualifier.ALL;
	// private String allReportType =
	// StockDownloadInfo.ReportTypeFamily.ReportTypeQualifier.ALL;
	// private String allYear = StockDownloadInfo.YearFamily.YearQualifier.ALL;
	// private String allSeason =
	// StockDownloadInfo.SeasonFamily.SeasonQualifier.ALL;
	// private FinancialReportHbaseManager reportManager;
	private FinancialReportHbaseManager manager;
	private TaxonomyRepository taxonomyRepo;

	// private FinancialReportInstanceRepository instanceRepo;
	// private StockDownloadInfoRepository infoRepo;
	// private FinancialReportDataRepository dataRepo;
	// private StockServiceProperty stockServiceProperty;
	// private HbaseAssistant hbaseAssistant;
	//
	@BeforeClass
	public void beforeClass() throws Exception {
		ApplicationContext applicationContext = TestngSuitSetting
				.getApplicationContext();
		manager = applicationContext.getBean(FinancialReportHbaseManager.class);
		taxonomyRepo = applicationContext.getBean(TaxonomyRepository.class);

		// dropAndCreateTable();
	}

	@Test
	public void updateTaxonomyPresentation() throws Exception {
		String tableName = taxonomyRepo.getTargetTableName();
		if (taxonomyRepo.isTableExists(tableName)) {
			taxonomyRepo.dropTable(tableName);
			taxonomyRepo.createTable(taxonomyRepo.getTargetTableClass());
		}
		XbrlTaxonomyVersion[] versions = XbrlTaxonomyVersion.values();
		for (XbrlTaxonomyVersion taxVer : versions) {
			Assert.assertFalse(taxonomyRepo.exists(taxVer));
		}
		manager.updateTaxonomyPresentation();
		for (XbrlTaxonomyVersion ver : versions) {
			Assert.assertTrue(taxonomyRepo.exists(ver));
		}
	}

	// @Test
	// public void processXbrlFiles() throws Exception {
	// File instanceFile = SystemResourceUtility
	// .getFileResource("xbrl-instance/2013-01-sii-01-C/tifrs-fr0-m1-ci-cr-1101-2013Q1.xml");
	// reportManager.processXbrlFiles(instanceFile, new ArrayList<String>(0));
	// String[] strArr = instanceFile.getName().split("-");
	// String stockCode = strArr[5];
	// ReportType reportType = ReportType.getMopsReportType(strArr[4]);
	// int year = Integer.valueOf(strArr[6].substring(0, 4));
	// int season = Integer.valueOf(strArr[6].substring(5, 6));
	// FinancialReportInstance entity = instanceRepo.get(stockCode,
	// reportType, year, season);
	// // Test version.
	// String version = entity.getInfoFamily()
	// .getLatestValue(InstanceAssistant.VERSION).getInfoContent();
	// Assert.assertEquals(version, "TIFRS_CI_CR_2013_03_31");
	// // Test instance.
	// String elementId =
	// "tifrs-SCF_DecreaseIncreaseInFinancialAssetsHeldForTrading";
	// String periodType = Instance.Attribute.DURATION;
	// Date startDate = DateUtils.parseDate("20130101", DATE_PATTERN);
	// Date endDate = DateUtils.parseDate("20130331", DATE_PATTERN);
	// InstanceValue val = entity.getInstanceFamily().getLatestValue(
	// elementId, periodType, startDate, endDate);
	// Assert.assertEquals(val.getUnit(), "TWD");
	// Assert.assertEquals(val.getValue().toString(), "-120107000");
	// // Test downloadInfo.
	// StockDownloadInfo downloadInfo = getOrCreateStockDownloadInfo();
	// Assert.assertTrue(downloadInfo.getStockCodeFamily()
	// .getLatestValue(allStockCode).getStockCodes().contains("1101"));
	// Assert.assertTrue(downloadInfo.getReportTypeFamily()
	// .getLatestValue(allReportType).getReportTypes()
	// .contains(ReportType.CONSOLIDATED_STATEMENT));
	// Assert.assertTrue(downloadInfo.getYearFamily().getLatestValue(allYear)
	// .getYears().contains(2013));
	// Assert.assertTrue(downloadInfo.getSeasonFamily()
	// .getLatestValue(allSeason).getSeasons().contains(1));
	// }
	//
	// @Test(dependsOnMethods = { "processXbrlFiles" })
	// public void saveFinancialReportToHBase() throws Exception {
	// File xbrlDir = stockServiceProperty.getFinancialReportExtractDir();
	// int actual = reportManager.saveFinancialReportToHBase(xbrlDir);
	// String[] ext = { "xml" };
	// int expected = FileUtils.listFiles(xbrlDir, ext, true).size();
	// Assert.assertEquals(actual, expected);
	// }
	//
	// @Test(dependsOnMethods = { "saveFinancialReportToHBase" })
	// public void updateFinancialReportInstance() throws Exception {
	// reportManager.updateFinancialReportInstance();
	// StockDownloadInfo infoEntity = infoRepo.get(instanceRepo
	// .getTargetTableName());
	// Assert.assertTrue(infoEntity.getStockCodeFamily()
	// .getQualifierVersionValueSet().size() > 0);
	// Assert.assertTrue(infoEntity.getReportTypeFamily()
	// .getQualifierVersionValueSet().size() > 0);
	// Assert.assertTrue(infoEntity.getYearFamily()
	// .getQualifierVersionValueSet().size() > 0);
	// Assert.assertTrue(infoEntity.getSeasonFamily()
	// .getQualifierVersionValueSet().size() > 0);
	// }
	//
	// @Test(dependsOnMethods = { "updateFinancialReportInstance" })
	// public void calculateFinancialReport() throws Exception {
	// reportManager.calculateFinancialReport();
	// int actual = hbaseAssistant
	// .getRowAmount(dataRepo.getTargetTableClass());
	// int expected = hbaseAssistant.getRowAmount(instanceRepo
	// .getTargetTableClass());
	// Assert.assertEquals(actual, expected);
	// }
	//
	// private void dropAndCreateTable() throws Exception {
	// HbaseEntityTestUtility.dropAndCreateTargetTable(presentRepo);
	// HbaseEntityTestUtility.dropAndCreateTargetTable(instanceRepo);
	// HbaseEntityTestUtility.dropAndCreateTargetTable(infoRepo);
	// HbaseEntityTestUtility.dropAndCreateTargetTable(dataRepo);
	// }
	//
	// private StockDownloadInfo getOrCreateStockDownloadInfo()
	// throws IllegalAccessException, NoSuchMethodException,
	// SecurityException, InstantiationException,
	// IllegalArgumentException, InvocationTargetException, IOException {
	// String tableName = instanceRepo.getTargetTableName();
	// return infoRepo.getOrCreateEntity(tableName);
	// }
}
