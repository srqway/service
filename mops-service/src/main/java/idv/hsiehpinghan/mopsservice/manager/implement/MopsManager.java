package idv.hsiehpinghan.mopsservice.manager.implement;

import idv.hsiehpinghan.mopsservice.manager.IMopsManager;
import idv.hsiehpinghan.mopsservice.operator.IFinancialReportOperator;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MopsManager implements IMopsManager {
	@Autowired
	IFinancialReportOperator financialReportOperator;
	
	@Override
	public void downloadFinancialReport() {
		File dir = financialReportOperator.downloadFinancialReport();
	}


}
