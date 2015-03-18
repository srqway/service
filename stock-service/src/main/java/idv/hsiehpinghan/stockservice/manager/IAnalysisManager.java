package idv.hsiehpinghan.stockservice.manager;

import idv.hsiehpinghan.stockdao.entity.RatioDifference;
import idv.hsiehpinghan.stockdao.enumeration.ReportType;

import java.io.IOException;

public interface IAnalysisManager {
	boolean updateAnalyzedData() throws IOException;

	RatioDifference getRatioDifference(String stockCode, ReportType reportType,
			int year, int season);

	boolean sendAnalysisMail();
}
