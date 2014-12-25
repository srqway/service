package idv.hsiehpinghan.mopsservice.manager.nosql;

import idv.hsiehpinghan.mopsservice.manager.IMopsManager;
import idv.hsiehpinghan.mopsservice.operator.FinancialReportOperator;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MopsManager implements IMopsManager {
	@Autowired
	FinancialReportOperator financialReportOperator;
	
	@Override
	public void downloadFinancialReport() {
		File dir = financialReportOperator.downloadFinancialReport();
	}


}
