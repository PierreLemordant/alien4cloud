package alien4cloud.component.portability;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.common.collect.Maps;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import alien4cloud.common.ParsedPropertiesDefinitions;
import alien4cloud.common.parser.PropertiesDefinitionYamlParser;
import alien4cloud.exception.NotFoundException;
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

    private static final String PORTABILITY_PROPERTIES_FILE_PATH = "classpath:portability/portabilities_definitions.yml";

    @PostConstruct
    public void init() throws ParsingException {
        // load the portability properties definitions
        org.springframework.core.io.Resource resource = applicationContext.getResource(PORTABILITY_PROPERTIES_FILE_PATH);
        try {
            ParsedPropertiesDefinitions parsed = parser.parseFile(resource.getURI().toString(), resource.getFilename(), resource.getInputStream(), null)
                    .getResult();
            portabilityDefinitions = keysFromStringToEnum(parsed.getDefinitions());
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
    public Map<String, PropertyDefinition> getAvailablePortabilityDefinitions(/* Here something to express the context ..? */) {
        Map<String, PropertyDefinition> result = Maps.newHashMap();
        for (Entry<PortabilityPropertyEnum, PropertyDefinition> e : portabilityDefinitions.entrySet()) {
            result.put(e.getKey().toString(), e.getValue());
        }
        return result;
    }

}
