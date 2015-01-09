package idv.hsiehpinghan.mopsservice.manager.hbase;

import idv.hsiehpinghan.mopsdao.repository.FinancialReportPresentationRepository;
import idv.hsiehpinghan.mopsservice.property.MopsServiceProperty;
import idv.hsiehpinghan.mopsservice.suit.TestngSuitSetting;
import idv.hsiehpinghan.xbrlassistant.enumeration.XbrlTaxonomyVersion;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.springframework.context.ApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class MopsHbaseManagerTest {
	private MopsHbaseManager mopsManager;
	private FinancialReportPresentationRepository repository;
	private MopsServiceProperty mopsServiceProperty;

	@BeforeClass
	public void beforeClass() throws IOException {
		ApplicationContext applicationContext = TestngSuitSetting
				.getApplicationContext();
		mopsManager = applicationContext.getBean(MopsHbaseManager.class);
		repository = applicationContext
				.getBean(FinancialReportPresentationRepository.class);
		mopsServiceProperty = applicationContext
				.getBean(MopsServiceProperty.class);
	}

	// @Test
	public void updateFinancialReportPresentation()
			throws NoSuchFieldException, SecurityException,
			IllegalArgumentException, IllegalAccessException,
			NoSuchMethodException, InvocationTargetException,
			InstantiationException, IOException {
		String tableName = repository.getTargetTableName();
		if (repository.isTableExists(tableName)) {
			repository.dropTable(tableName);
			repository.createTable(repository.getTargetTableClass());
		}
		XbrlTaxonomyVersion[] versions = XbrlTaxonomyVersion.values();
		for (XbrlTaxonomyVersion ver : versions) {
			Assert.assertFalse(repository.exists(ver));
		}
		mopsManager.updateFinancialReportPresentation();
		for (XbrlTaxonomyVersion ver : versions) {
			Assert.assertTrue(repository.exists(ver));
		}
	}

	@Test
	public void saveFinancialReportToHBase() throws Exception {
		File xbrlDirectory = new File(mopsServiceProperty.getExtractDir());
		int processFilesAmt = mopsManager.saveFinancialReportToHBase(xbrlDirectory);
		
//		Assert.assertEquals(7136, processFilesAmt);
//		System.err.println(processFilesAmt);
	}
}
