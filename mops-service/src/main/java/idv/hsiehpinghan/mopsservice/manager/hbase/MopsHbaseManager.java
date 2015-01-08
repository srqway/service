package idv.hsiehpinghan.mopsservice.manager.hbase;

import idv.hsiehpinghan.hdfsassistant.utility.HdfsAssistant;
import idv.hsiehpinghan.mopsdao.repository.FinancialReportPresentationRepository;
import idv.hsiehpinghan.mopsservice.manager.IMopsManager;
import idv.hsiehpinghan.mopsservice.operator.FinancialReportDownloader;
import idv.hsiehpinghan.mopsservice.property.MopsServiceProperty;
import idv.hsiehpinghan.xbrlassistant.assistant.TaxonomyAssistant;
import idv.hsiehpinghan.xbrlassistant.enumeration.XbrlTaxonomyVersion;
import idv.hsiehpinghan.xbrlassistant.xbrl.Presentation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class MopsHbaseManager implements IMopsManager {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private List<String> presentIds;

	@Autowired
	private FinancialReportDownloader downloader;
	@Autowired
	private HdfsAssistant hdfsAssistant;
	@Autowired
	private MopsServiceProperty mopsServiceProperty;
	@Autowired
	private TaxonomyAssistant taxonomyAssistant;
	@Autowired
	private FinancialReportPresentationRepository repository;

	public MopsHbaseManager() {
		presentIds = new ArrayList<String>(4);
		presentIds.add(Presentation.Id.BalanceSheet);
		presentIds.add(Presentation.Id.StatementOfComprehensiveIncome);
		presentIds.add(Presentation.Id.StatementOfCashFlows);
		presentIds.add(Presentation.Id.StatementOfChangesInEquity);
	}

	@Override
	public boolean updateFinancialReportPresentation() {
		XbrlTaxonomyVersion[] versions = XbrlTaxonomyVersion.values();
		XbrlTaxonomyVersion version = null;
		try {
			for(int i = 0, size = versions.length; i < size; ++i) {
				version = versions[i];
				ObjectNode presentNode = taxonomyAssistant.getPresentationJson(version, presentIds);
				repository.save(version, presentIds, presentNode);
				logger.info(version + " updated.");
				
				return true;
			}
			logger.info("Update financial report presentation finished.");
		} catch (Exception e) {
			logger.error(version + " update fail !!!");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public boolean updateFinancialReport() {
		File xbrlDir = downloadFinancialReport();
		if (xbrlDir == null) {
			return false;
		}
		if (saveFinancialReportToDatabase(xbrlDir) == false) {
			return false;
		}
		return true;
	}

	File downloadFinancialReport() {
		try {
			File xbrlDir = downloader.downloadFinancialReport();
			logger.info(xbrlDir.getAbsolutePath() + " download finish.");
			return xbrlDir;
		} catch (Exception e) {
			logger.error("Download fail !!!");
			return null;
		}
	}

	boolean saveFinancialReportToDatabase(File xbrlDir) {
		// processSubFiles(xbrlDir);
		return false;
	}

	// private void processSubFiles(File file) {
	// if (file.isDirectory()) {
	// File[] fs = file.listFiles();
	// for (File f : fs) {
	// processSubFiles(f);
	// }
	// } else {
	// // xbrlAssistant.get
	// System.err.println(file.getAbsolutePath());
	// }
	//
	// }

}
