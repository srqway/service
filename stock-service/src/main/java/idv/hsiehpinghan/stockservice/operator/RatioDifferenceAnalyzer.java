package idv.hsiehpinghan.stockservice.operator;

import idv.hsiehpinghan.stockservice.property.StockServiceProperty;

import java.io.File;
import java.io.IOException;

import org.rosuda.JRI.Rengine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RatioDifferenceAnalyzer {
	// private Logger logger = Logger.getLogger(this.getClass().getName());

	@Autowired
	private StockServiceProperty stockServiceProperty;
	@Autowired
	private Rengine rengine;

	public File analyzeRatioDifference(File targetDirectory) throws IOException {
		File script = new File(stockServiceProperty.getRScriptDir(),
				"analyzeRatioDifference.R");
		try {
			if (rengine.waitForR() == false) {
				throw new RuntimeException("Wait for R fail !!!");
			}
			File logFile = new File(targetDirectory,
					"analyzeRatioDifference.log");
			rengine.eval("sink('" + logFile.getAbsolutePath() + "')");
			File xbrlFile = new File(targetDirectory, "xbrl");
			File resultFile = new File(targetDirectory, "result.csv");
			setParameter(xbrlFile, resultFile);
			rengine.eval("source('" + script.getAbsolutePath() + "')");
			return resultFile;
		} finally {
			if (rengine != null) {
				rengine.eval("sink()");
			}
		}
	}

	private void setParameter(File xbrlFile, File resultFile) {
		rengine.eval("xbrlFile <- '" + xbrlFile.getAbsolutePath() + "'");
		rengine.eval("resultFile <- '" + resultFile.getAbsolutePath() + "'");
	}
}
