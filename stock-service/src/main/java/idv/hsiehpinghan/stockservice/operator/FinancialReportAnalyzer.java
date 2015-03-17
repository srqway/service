package idv.hsiehpinghan.stockservice.operator;

import idv.hsiehpinghan.stockservice.property.StockServiceProperty;

import java.io.File;
import java.io.IOException;

import org.rosuda.JRI.Rengine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FinancialReportAnalyzer {
	// private Logger logger = Logger.getLogger(this.getClass().getName());

	@Autowired
	private StockServiceProperty stockServiceProperty;
	@Autowired
	private Rengine rengine;

	public void analyzeRatioDifference(File targetDirectory, File logFile)
			throws IOException {
		File script = new File(stockServiceProperty.getRScriptDir(),
				"analyzeRatioDifference.R");
		try {
			if (rengine.waitForR() == false) {
				throw new RuntimeException("Wait for R fail !!!");
			}
			if (logFile != null) {
				rengine.eval("sink('" + logFile.getAbsolutePath() + "')");
			}
			setParameter(targetDirectory);
			rengine.eval("source('" + script.getAbsolutePath() + "')");
		} finally {
			if (rengine != null) {
				if (logFile != null) {
					rengine.eval("sink()");
				}
				rengine.end();
			}
		}

		// groups <- split(dataFrame, dataFrame$elementId);
		// test <- subset(dataFrame, subset=(year==2013 & season==4));
		// http://stackoverflow.com/questions/12127149/r-finding-the-max-date-for-each-id
		// http://www.statmethods.net/management/userfunctions.html
		// https://blog.udemy.com/r-tutorial/
	}

	private void setParameter(File targetDirectory) {
		File xbrlFile = new File(targetDirectory, "xbrl");
		rengine.eval("xbrlFile <- '" + xbrlFile.getAbsolutePath() + "'");
		File resultFile = new File(targetDirectory, "result.csv");
		rengine.eval("resultFile <- '" + resultFile.getAbsolutePath() + "'");
	}
}
