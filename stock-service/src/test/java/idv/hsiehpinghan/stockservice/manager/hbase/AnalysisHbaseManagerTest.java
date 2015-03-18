package idv.hsiehpinghan.stockservice.manager.hbase;

import java.math.BigDecimal;
import java.util.TreeSet;

import idv.hsiehpinghan.hbaseassistant.utility.HbaseEntityTestUtility;
import idv.hsiehpinghan.stockdao.entity.RatioDifference;
import idv.hsiehpinghan.stockdao.repository.RatioDifferenceRepository;
import idv.hsiehpinghan.stockservice.suit.TestngSuitSetting;

import org.springframework.context.ApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class AnalysisHbaseManagerTest {
	private AnalysisHbaseManager manager;
	private RatioDifferenceRepository diffRepo;
	private BigDecimal pValueThreshold = new BigDecimal("0.01");
	
	@BeforeClass
	public void beforeClass() throws Exception {
		ApplicationContext applicationContext = TestngSuitSetting
				.getApplicationContext();
		manager = applicationContext.getBean(AnalysisHbaseManager.class);
		diffRepo = applicationContext.getBean(RatioDifferenceRepository.class);
		// dropAndCreateTable();
	}

//	@Test
	public void updateAnalyzedData() throws Exception {
		boolean result = manager.updateAnalyzedData();
		Assert.assertTrue(result);
	}

	@Test
	public void getRatioDifferences() throws Exception {
//		TreeSet<RatioDifference> entities = manager.getBeyondThresholdRatioDifferences(pValueThreshold);
//		
//		System.err.println(entities.size());
//		
//		
//		Assert.assertTrue(entities.size() > 0);
	}
	
	private void dropAndCreateTable() throws Exception {
		HbaseEntityTestUtility.dropAndCreateTargetTable(diffRepo);
	}
}
