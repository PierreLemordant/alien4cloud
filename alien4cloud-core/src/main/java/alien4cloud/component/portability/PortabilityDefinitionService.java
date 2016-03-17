package alien4cloud.component.portability;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.collect.Sets;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import alien4cloud.common.ParsedPropertiesDefinitions;
import alien4cloud.common.parser.PropertiesDefinitionYamlParser;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.ExtendedPropertyDefinition;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.impl.ErrorCode;

/**
 * A {@link PropertyDefinition} store for portability indicators.
 * 
 */
@Component
@Slf4j
public class PortabilityDefinitionService {

    @Resource
    private ApplicationContext applicationContext;
    @Inject
    private PropertiesDefinitionYamlParser parser;

    @Getter
    private Map<PortabilityPropertyEnum, PropertyDefinition> portabilityDefinitions;

    private Map<String, List<String>> policies;

    private static final String PORTABILITY_PROPERTIES_FILE_PATH = "classpath:portability/portabilities_definitions.yml";

    @PostConstruct
    public void init() throws ParsingException {
        // load the portability properties definitions
        org.springframework.core.io.Resource resource = applicationContext.getResource(PORTABILITY_PROPERTIES_FILE_PATH);
        try {
            ParsedPropertiesDefinitions parsed = parser.parseFile(resource.getURI().toString(), resource.getFilename(), resource.getInputStream(), null)
                    .getResult();
            portabilityDefinitions = keysFromStringToEnum(parsed.getDefinitions());
            policies = parsed.getPolicies();
        } catch (IOException e) {
            log.error("Failed to open stream", e);
            throw new ParsingException(resource.getFilename(),
                    new ParsingError(ErrorCode.MISSING_FILE, "Unable to load file.", null, e.getMessage(), null, PORTABILITY_PROPERTIES_FILE_PATH));
        }
    }

    /**
     * For a given portability indicator, provide the right {@link PropertyDefinition}.
     */
    public PropertyDefinition getPropertyDefinition(String portabilityIndicatorKey) {
        try {
            return portabilityDefinitions.get(PortabilityPropertyEnum.valueOf(portabilityIndicatorKey));
        } catch (IllegalArgumentException e) {
            throw new NotFoundException(String.format("The PortabilityPropertyEnum entry <%s> does not exists !", portabilityIndicatorKey));
        }
    }

    private Map<PortabilityPropertyEnum, PropertyDefinition> keysFromStringToEnum(Map<String, PropertyDefinition> definitions) {
        Map<PortabilityPropertyEnum, PropertyDefinition> toReturn = Maps.newHashMap();
        for (Entry<String, PropertyDefinition> entry : definitions.entrySet()) {
            try {
                toReturn.put(PortabilityPropertyEnum.valueOf(entry.getKey()), entry.getValue());
            } catch (IllegalArgumentException e) {
                log.warn("Unrecognized portability property <" + entry.getKey() + ">. Will be ignored");
            }
        }
        return toReturn;
    }

    /**
     * TODO: this should be filtered by the context. For example:
     * - a location resource node template should only allow those comming from type (readonly)
     * - a location compute node template should allow RUNTIME_PACKAGES for edition.
     * 
     * @return
     */
    public Map<String, PropertyDefinition> getAvailablePortabilityDefinitions(boolean isOrchestratorOnDemandResource, boolean isCompute,
            boolean isMatchingResource) {
        Map<String, PropertyDefinition> result = Maps.newHashMap();
        List<String> policyNames = getPolicyNames(isOrchestratorOnDemandResource, isCompute, isMatchingResource);
        Set<String> availableKeys = getAvailableKeys(policyNames);
        Set<String> readonlyKeys = getReadonlyKeys(policyNames);
        for (Entry<PortabilityPropertyEnum, PropertyDefinition> e : portabilityDefinitions.entrySet()) {
            String portabilityIndicatorName = e.getKey().toString();
            if (availableKeys.contains(portabilityIndicatorName)) {
                PropertyDefinition propertyDefinition = e.getValue();
                ExtendedPropertyDefinition extendedPropertyDefinition = new ExtendedPropertyDefinition(propertyDefinition);
                if (readonlyKeys.contains(portabilityIndicatorName)) {
                    extendedPropertyDefinition.setEditable(false);
                } else {
                    extendedPropertyDefinition.setEditable(true);
                }
                result.put(portabilityIndicatorName, extendedPropertyDefinition);
            }
        }
        return result;
    }

    private List<String> getPolicyNames(boolean isOrchestratorOnDemandResource, boolean isCompute, boolean isMatchingResource) {
        List<String> result = Lists.newArrayList();
        StringBuilder key = new StringBuilder("base");
        result.add(key.toString());
        if (isOrchestratorOnDemandResource) {
            key.append(".orchestratorOnDemandResources");
            result.add(key.toString());
        }
        if (isCompute) {
            key.append(".compute");
            result.add(key.toString());
        }
        if (isMatchingResource) {
            key.append(".matchingStage");
            result.add(key.toString());
        }
        return result;
    }

    private Set<String> getAvailableKeys(List<String> policyNames) {
        return getPoliciesEntry(policyNames, "keys");
    }

    private Set<String> getReadonlyKeys(List<String> policyNames) {
        return getPoliciesEntry(policyNames, "readonly");
    }

    private Set<String> getPoliciesEntry(List<String> policyNames, String suffixe) {
        Set<String> result = Sets.newHashSet();
        for (String policy : policyNames) {
            String keys = policy + "." + suffixe;
            List<String> entries = policies.get(keys);
            if (entries != null) {
                for (String entry : entries) {
                    result.add(entry);
                }
            }
        }
        return result;
    }

}
