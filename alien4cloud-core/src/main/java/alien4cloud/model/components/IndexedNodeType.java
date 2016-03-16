package alien4cloud.model.components;

import static alien4cloud.dao.model.FetchContext.QUICK_SEARCH;
import static alien4cloud.dao.model.FetchContext.TAG_SUGGESTION;

import alien4cloud.component.portability.PortabilityPropertyEnum;
import alien4cloud.json.deserializer.AllPropertyValueDeserializer;
import alien4cloud.utils.jackson.ConditionalAttributes;
import alien4cloud.utils.jackson.ConditionalOnAttribute;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.NumberField;
import org.elasticsearch.annotation.query.FetchContext;
import org.elasticsearch.annotation.query.TermsFacet;
import org.elasticsearch.mapping.IndexType;

@Getter
@Setter
@EqualsAndHashCode(of = {}, callSuper = true)
@ESObject
@SuppressWarnings("rawtypes")
public class IndexedNodeType extends IndexedArtifactToscaElement {

    @FetchContext(contexts = { QUICK_SEARCH, TAG_SUGGESTION }, include = { false, false })
    @TermsFacet(paths = "type")
    private List<CapabilityDefinition> capabilities;

    @FetchContext(contexts = { QUICK_SEARCH, TAG_SUGGESTION }, include = { false, false })
    @TermsFacet(paths = "type")
    private List<RequirementDefinition> requirements;

    @FetchContext(contexts = { QUICK_SEARCH, TAG_SUGGESTION }, include = { false, false })
    @TermsFacet
    private List<String> defaultCapabilities;

    @NumberField(index = IndexType.not_analyzed, includeInAll = false)
    private long alienScore;

    /**
     * When the type is created from a topology template (substitution), contains the topology id.
     */
    private String substitutionTopologyId;

    /**
     * portability informations
     */

    @ConditionalOnAttribute({ ConditionalAttributes.ES, ConditionalAttributes.REST })
    @JsonDeserialize(contentUsing = AllPropertyValueDeserializer.class)
    @TermsFacet(keysEnum = PortabilityPropertyEnum.class, paths = "value")
    private Map<String, AbstractPropertyValue> portability;
}