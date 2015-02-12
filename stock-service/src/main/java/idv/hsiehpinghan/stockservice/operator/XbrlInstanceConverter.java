package idv.hsiehpinghan.stockservice.operator;

import idv.hsiehpinghan.datatypeutility.utility.StringUtility;
import idv.hsiehpinghan.stockdao.entity.Xbrl;
import idv.hsiehpinghan.stockdao.entity.Xbrl.InfoFamily;
import idv.hsiehpinghan.stockdao.entity.Xbrl.InstanceFamily;
import idv.hsiehpinghan.stockdao.enumeration.PeriodType;
import idv.hsiehpinghan.stockdao.enumeration.ReportType;
import idv.hsiehpinghan.stockdao.enumeration.UnitType;
import idv.hsiehpinghan.stockdao.repository.XbrlRepository;
import idv.hsiehpinghan.xbrlassistant.assistant.InstanceAssistant;
import idv.hsiehpinghan.xbrlassistant.enumeration.XbrlTaxonomyVersion;
import idv.hsiehpinghan.xbrlassistant.xbrl.Instance;
import idv.hsiehpinghan.xbrlassistant.xbrl.Presentation;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class XbrlInstanceConverter {
	private static final String COMMA_STRING = StringUtility.COMMA_STRING;
	private static final String DATE_PATTERN = "yyyyMMdd";
	// private Logger logger = Logger.getLogger(this.getClass().getName());

	@Autowired
	private XbrlRepository xbrlRepo;

	public Xbrl convert(String stockCode, ReportType reportType, int year,
			int season, ObjectNode objNode) throws ParseException {
		Xbrl entity = xbrlRepo.generateEntity(stockCode, reportType, year,
				season);
		generateRowKey(entity, stockCode, reportType, year, season);
		generateColumnFamilies(entity, objNode);
		return entity;
	}

	private void generateColumnFamilies(Xbrl entity, ObjectNode objNode)
			throws ParseException {
		Date ver = Calendar.getInstance().getTime();
		generateInfoFamily(entity, objNode, ver);
		generateInstanceFamily(entity, objNode, ver);
	}

	private Date getDate(JsonNode dateNode) throws ParseException {
		if (dateNode == null) {
			return null;
		}
		return DateUtils.parseDate(dateNode.textValue(), DATE_PATTERN);
	}

	private void generateInstanceFamily(Xbrl entity, ObjectNode objNode,
			Date ver) throws ParseException {
		InstanceFamily instanceFamily = entity.getInstanceFamily();
		JsonNode instanceNode = objNode.get(InstanceAssistant.INSTANCE);
		Iterator<Entry<String, JsonNode>> fields = instanceNode.fields();
		while (fields.hasNext()) {
			Entry<String, JsonNode> eleIdEnt = fields.next();
			String eleId = eleIdEnt.getKey();
			ArrayNode arrNode = (ArrayNode) eleIdEnt.getValue();
			for (JsonNode dataNode : arrNode) {
				PeriodType periodType = PeriodType.getPeriodType(dataNode.get(
						"periodType").textValue());
				Date instant = getDate(dataNode.get("instant"));
				Date startDate = getDate(dataNode.get("startDate"));
				Date endDate = getDate(dataNode.get("endDate"));
				UnitType unitType = UnitType.getUnitType(dataNode.get("unit")
						.textValue());
				BigDecimal value = new BigDecimal(dataNode.get("value")
						.textValue());
				instanceFamily.setInstanceValue(eleId, periodType, instant,
						startDate, endDate, ver, unitType, value);
			}
		}
	}

	private void generateInfoFamilyVersion(Date ver, JsonNode infoNode,
			InfoFamily infoFamily) {
		String version = infoNode.get(InstanceAssistant.VERSION).textValue();
		infoFamily.setVersion(ver, XbrlTaxonomyVersion.valueOf(version));
	}

	private void generateInfoFamilyBalanceSheet(Date ver, JsonNode contextNode,
			InfoFamily infoFamily) {
		StringBuilder sb = new StringBuilder();
		JsonNode presentIdNode = contextNode.get(Presentation.Id.BalanceSheet);
		ArrayNode instantArrNode = (ArrayNode) presentIdNode
				.get(Instance.Attribute.INSTANT);
		sb.setLength(0);
		for (JsonNode context : instantArrNode) {
			sb.append(context.textValue() + COMMA_STRING);
		}
		infoFamily.setBalanceSheetContext(ver, sb.toString());
	}

	private void generateInfoFamilyStatementOfComprehensiveIncome(Date ver,
			JsonNode contextNode, InfoFamily infoFamily) {
		StringBuilder sb = new StringBuilder();
		JsonNode presentIdNode = contextNode
				.get(Presentation.Id.StatementOfComprehensiveIncome);
		ArrayNode instantArrNode = (ArrayNode) presentIdNode
				.get(Instance.Attribute.DURATION);
		sb.setLength(0);
		for (JsonNode context : instantArrNode) {
			sb.append(context.textValue() + COMMA_STRING);
		}
		infoFamily.setStatementOfComprehensiveIncomeContext(ver, sb.toString());
	}

	private void generateInfoFamilyStatementOfCashFlows(Date ver,
			JsonNode contextNode, InfoFamily infoFamily) {
		StringBuilder sb = new StringBuilder();
		JsonNode presentIdNode = contextNode
				.get(Presentation.Id.StatementOfCashFlows);
		ArrayNode instantArrNode = (ArrayNode) presentIdNode
				.get(Instance.Attribute.DURATION);
		sb.setLength(0);
		for (JsonNode context : instantArrNode) {
			sb.append(context.textValue() + COMMA_STRING);
		}
		infoFamily.setStatementOfCashFlowsContext(ver, sb.toString());
	}

	private void generateInfoFamilyStatementOfChangesInEquity(Date ver,
			JsonNode contextNode, InfoFamily infoFamily) {
		StringBuilder sb = new StringBuilder();
		JsonNode presentIdNode = contextNode
				.get(Presentation.Id.StatementOfChangesInEquity);
		ArrayNode instantArrNode = (ArrayNode) presentIdNode
				.get(Instance.Attribute.DURATION);
		sb.setLength(0);
		for (JsonNode context : instantArrNode) {
			sb.append(context.textValue() + COMMA_STRING);
		}
		infoFamily.setStatementOfChangesInEquityContext(ver, sb.toString());
	}

	private void generateInfoFamily(Xbrl entity, ObjectNode objNode, Date ver) {
		InfoFamily infoFamily = entity.getInfoFamily();
		JsonNode infoNode = objNode.get(InstanceAssistant.INFO);
		generateInfoFamilyVersion(ver, infoNode, infoFamily);
		JsonNode contextNode = infoNode.get(InstanceAssistant.CONTEXT);
		generateInfoFamilyBalanceSheet(ver, contextNode, infoFamily);
		generateInfoFamilyStatementOfComprehensiveIncome(ver, contextNode,
				infoFamily);
		generateInfoFamilyStatementOfCashFlows(ver, contextNode, infoFamily);
		generateInfoFamilyStatementOfChangesInEquity(ver, contextNode,
				infoFamily);
	}

	private void generateRowKey(Xbrl entity, String stockCode,
			ReportType reportType, int year, int season) {
		entity.new RowKey(stockCode, reportType, year, season, entity);
	}
}
