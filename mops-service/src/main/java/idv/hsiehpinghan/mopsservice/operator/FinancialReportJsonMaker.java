package idv.hsiehpinghan.mopsservice.operator;

import idv.hsiehpinghan.mopsdao.entity.FinancialReportPresentation;
import idv.hsiehpinghan.mopsdao.repository.FinancialReportInstanceRepository;
import idv.hsiehpinghan.mopsdao.repository.FinancialReportPresentationRepository;
import idv.hsiehpinghan.xbrlassistant.enumeration.XbrlTaxonomyVersion;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class FinancialReportJsonMaker {
	// private Logger logger = Logger.getLogger(this.getClass().getName());

	@Autowired
	private FinancialReportPresentationRepository presentRepo;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private FinancialReportInstanceRepository instanceRepo;

	public Map<String, ObjectNode> getPresentationJsonMap(
			List<String> presentIds, XbrlTaxonomyVersion taxonomyVersion)
			throws IllegalAccessException, NoSuchMethodException,
			SecurityException, InstantiationException,
			IllegalArgumentException, InvocationTargetException, IOException {
		FinancialReportPresentation present = presentRepo.get(taxonomyVersion);
		Map<String, ObjectNode> map = new HashMap<String, ObjectNode>(
				presentIds.size());
		for (String presentId : presentIds) {
			String jsonStr = present.getJsonFamily().getValue(presentId)
					.getJson();
			ObjectNode objNode = generateJsonObject(jsonStr);
			map.put(presentId, objNode);
		}
		return map;
	}

	private ObjectNode generateJsonObject(String jsonString)
			throws JsonProcessingException, IOException {
		ObjectNode srcNode = (ObjectNode) objectMapper.readTree(jsonString);
		ObjectNode targetNode = objectMapper.createObjectNode();
		generateSubNodeContent(srcNode, targetNode);
		return targetNode;
	}
	
	private void generateSubNodeContent(ObjectNode srcNode, ObjectNode targetNode) {
		for(int i = 0, size = srcNode.size(); i < size; ++i) {
			JsonNode node = srcNode.get(i);
			System.err.println(node.asText());
		}
		
	}
}
