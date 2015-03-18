if(exists('xbrlFile') == FALSE) {
	msg <- 'xbrlFile not exists !!!'
	print(msg)
	stop(msg)
}
if(exists('resultFile') == FALSE) {
	msg <- 'resultFile not exists !!!'
	print(msg)
	stop(msg)
}
(function(parameter) {
	df <- read.csv(xbrlFile, colClasses=c(
			'stockCode'='character', 
			'reportType'='character', 
			'year'='integer', 
			'season'='integer', 
			'elementId'='character', 
			'periodType'='character', 
			'instant'='Date', 
			'startDate'='Date', 
			'endDate'='Date', 
			'ratioDifference'='numeric'
		)
	)
	rm(xbrlFile)
	target_year_season <- max(df$year*100 + df$season)
	target_year <- target_year_season %/% 100
	target_season <- target_year_season %% 100
	target_elements <- subset(df, subset=(year==target_year & season==target_season))
	history_elements <- subset(df, subset=(!(year==target_year & season==target_season)), select=c(elementId, ratioDifference))
	history_groups <- split(history_elements, history_elements$elementId)
	length = nrow(target_elements)
	tempDf <- data.frame(
		stockCode = character(length),
		reportType = character(length),
		year = integer(length),
		season = integer(length),
		elementId = character(length),
		periodType = character(length),
		instant = seq(as.Date('1-01-01'), by=0, len=length),
		startDate = seq(as.Date('1-01-01'), by=0, len=length),
		endDate = seq(as.Date('1-01-01'), by=0, len=length),			
		statistic = numeric(length),
		degreeOfFreedom = numeric(length),
		confidenceInterval = numeric(length),
		sampleMean = numeric(length),
		hypothesizedMean = numeric(length),
		pValue = numeric(length),
		stringsAsFactors=FALSE
	)
		
	for(i in 1:nrow(target_elements)) {
		targetRow <- target_elements[i,]
		ratioDifference = targetRow$ratioDifference
		group <- history_groups[targetRow$elementId]
		diffs <- group[[1]]$ratioDifference
		if(length(diffs) < 2) {
			next
		}
		htest <- t.test(diffs, mu=ratioDifference)
		pValue <- htest$p.value
		if(is.na(pValue) | (0.05 < pValue)) {
			next
		}
		tempDf$stockCode[i] <- targetRow$stockCode
		tempDf$reportType[i] <- targetRow$reportType
		tempDf$year[i] <- targetRow$year
		tempDf$season[i] <- targetRow$season
		tempDf$elementId[i] <- targetRow$elementId
		tempDf$periodType[i] <- targetRow$periodType
		tempDf$instant[i] <- targetRow$instant
		tempDf$startDate[i] <- targetRow$startDate
		tempDf$endDate[i] <- targetRow$endDate
		tempDf$statistic[i] <- htest$statistic;
		tempDf$degreeOfFreedom[i] <- htest$parameter;
		tempDf$confidenceInterval[i] <- htest$conf.int;
		tempDf$sampleMean[i] <- htest$estimate;
		tempDf$hypothesizedMean[i] <- htest$null.value;
		tempDf$pValue[i] <- pValue
	}
	resultDf <- subset(tempDf, subset=(stockCode != ''));
	write.csv(resultDf, file=resultFile, row.names = FALSE)
	rm(resultDf)
})()