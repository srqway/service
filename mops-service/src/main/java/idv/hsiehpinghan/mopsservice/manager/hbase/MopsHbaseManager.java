package idv.hsiehpinghan.mopsservice.manager.hbase;

import idv.hsiehpinghan.hdfsassistant.utility.HdfsAssistant;
import idv.hsiehpinghan.mopsdao.enumeration.ReportType;
import idv.hsiehpinghan.mopsdao.repository.FinancialReportInstanceRepository;
import idv.hsiehpinghan.mopsdao.repository.FinancialReportPresentationRepository;
import idv.hsiehpinghan.mopsservice.manager.IMopsManager;
import idv.hsiehpinghan.mopsservice.operator.FinancialReportDownloader;
import idv.hsiehpinghan.mopsservice.property.MopsServiceProperty;
import idv.hsiehpinghan.xbrlassistant.assistant.InstanceAssistant;
import idv.hsiehpinghan.xbrlassistant.assistant.TaxonomyAssistant;
import idv.hsiehpinghan.xbrlassistant.enumeration.XbrlTaxonomyVersion;
import idv.hsiehpinghan.xbrlassistant.xbrl.Presentation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
	private InstanceAssistant instanceAssistant;
	@Autowired
	private FinancialReportPresentationRepository presentRepo;
	@Autowired
	private FinancialReportInstanceRepository instantRepo;

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
			for (int i = 0, size = versions.length; i < size; ++i) {
				version = versions[i];
				ObjectNode presentNode = taxonomyAssistant.getPresentationJson(
						version, presentIds);
				if (presentRepo.exists(version)) {
					logger.info(version + " exists.");
					continue;
				}
				presentRepo.put(version, presentIds, presentNode);
				logger.info(version + " updated.");
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
	public boolean updateFinancialReportInstance() {
		File xbrlDir = downloadFinancialReportInstance();
		if (xbrlDir == null) {
			return false;
		}
		try {
			int processFilesAmt = saveFinancialReportToHBase(xbrlDir);
			logger.info("Saved " + processFilesAmt + " xbrl files to hbase.");
		} catch (Exception e) {
			logger.error("Save financial report to hbase fail !!!");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	File downloadFinancialReportInstance() {
		try {
			File xbrlDir = downloader.downloadFinancialReport();
			logger.info(xbrlDir.getAbsolutePath() + " download finish.");
			return xbrlDir;
		} catch (Exception e) {
			logger.error("Download financial report fail !!!");
			return null;
		}
	}

	int saveFinancialReportToHBase(File xbrlDir) throws Exception {
		return processXbrlFiles(xbrlDir);
	}

	int processXbrlFiles(File file) throws Exception {
		int count = 0;
		if (file.isDirectory()) {
			File[] fs = file.listFiles();
			for (File f : fs) {
				count += processXbrlFiles(f);
			}
		} else {
			// ex. tifrs-fr0-m1-ci-cr-1101-2013Q1.xml
			String[] strArr = file.getName().split("-");
			String stockCode = strArr[5];
			ReportType reportType = ReportType.getReportType(strArr[4]);
			int year = Integer.valueOf(strArr[6].substring(0, 4));
			int season = Integer.valueOf(strArr[6].substring(5, 6));
			ObjectNode objNode = instanceAssistant.getInstanceJson(file,
					presentIds);
			if (instantRepo.exists(stockCode, reportType, year, season) == false) {
				instantRepo.put(stockCode, reportType, year, season, objNode,
						presentIds);
				logger.info(file.getName() + " saved to hbase.");
				++count;
			} else {
				logger.info(file.getName() + " already saved to hbase.");
			}

		}
		return count;
	}
}
