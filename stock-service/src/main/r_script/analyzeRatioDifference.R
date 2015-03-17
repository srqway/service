xbrlFile <- '/tmp/getXbrlFromHbase/xbrl'
resultFile <- '/tmp/getXbrlFromHbase/result'
(function(parameter) {
	df <- read.csv(xbrlFile, colClasses=c('stockCode'='character', 'reportType'='factor', 'year'='integer', 'season'='integer', 'elementId'='character', 'periodType'='factor', 'instant'='Date', 'startDate'='Date', 'endDate'='Date', 'ratioDifference'='numeric'))
	target_year_season <- max(df$year*100 + df$season)
	target_year <- target_year_season %/% 100
	target_season <- target_year_season %% 100
	target_elements <- subset(df, subset=(year==target_year & season==target_season))
	history_elements <- subset(df, subset=(year!=target_year & season!=target_season), select=c(elementId, ratioDifference))
	history_groups <- split(history_elements, history_elements$elementId)
	
	
	resultDf <- data.frame(pValue=as.Date(character()),
			File=character(), 
			User=character()) 
	
	
	for(i in 1:nrow(target_elements)) {
		targetRow <- target_elements[i,]
		ratioDifference = targetRow$ratioDifference
		group <- history_groups[targetRow$elementId]
		htest <- t.test(group[[1]]$ratioDifference, mu=ratioDifference)
		pValue <- htest$p.value
		if(0.05 < pValue) {
			next
		}
		statistic = htest$statistic;
		degreeOfFreedom = htest$parameter;
		confidenceInterval = htest$conf.int;
		sampleMean = htest$estimate;
		hypothesizedMean = htest$null.value;
		
#		stop(pValue)
	}
})()