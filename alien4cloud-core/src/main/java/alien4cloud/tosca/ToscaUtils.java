package alien4cloud.tosca;

import java.util.List;

import alien4cloud.common.AlienConstants;
import alien4cloud.model.components.IndexedInheritableToscaElement;
import alien4cloud.paas.exception.PaaSTechnicalException;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants;
import alien4cloud.tosca.normative.NormativeComputeConstants;
import alien4cloud.utils.AlienUtils;

import com.google.common.collect.Lists;

public class ToscaUtils {

    private ToscaUtils() {
    }

    /**
     * Verify that the given {@link IndexedInheritableToscaElement} is from the given type.
     *
     * @param indexedInheritableToscaElement The {@link IndexedInheritableToscaElement} to verify.
     * @param type The type to match
     * @return <code>true</code> if the {@link IndexedInheritableToscaElement} is from the given type.
     */
    public static boolean isFromType(String type, IndexedInheritableToscaElement indexedInheritableToscaElement) {
        return isFromType(type, indexedInheritableToscaElement.getElementId(), indexedInheritableToscaElement.getDerivedFrom());
    }

    /**
     * Verify that the given <code>type</code> is or inherits the given <code>expectedType</code>.
     */
    public static boolean isFromType(String expectedType, String type, List<String> typeHierarchy) {
        return expectedType.equals(type) || (typeHierarchy != null && typeHierarchy.contains(expectedType));
    }

    /**
     * Return
     *
     * @param paaSNodeTemplate
     * @return
     */
    public static PaaSNodeTemplate getMandatoryHostTemplate(final PaaSNodeTemplate paaSNodeTemplate) {
        PaaSNodeTemplate nodeTemplate = getHostTemplate(paaSNodeTemplate);
        if (nodeTemplate == null) {
            throw new PaaSTechnicalException("Cannot get the service name: The node template <" + paaSNodeTemplate.getId()
                    + "> is not declared as hosted on a compute.");
        } else {
            return nodeTemplate;
        }
    }

    public static PaaSNodeTemplate getHostTemplate(PaaSNodeTemplate paaSNodeTemplate) {
        while (paaSNodeTemplate != null) {
            if (isFromType(NormativeComputeConstants.COMPUTE_TYPE, paaSNodeTemplate.getIndexedToscaElement())) {
                // Found the compute
                return paaSNodeTemplate;
            } else {
                // Not found then go to the parent
                paaSNodeTemplate = paaSNodeTemplate.getParent();
            }
        }
        return null;
    }

    /**
     * Returns the ordered nodeTemplate hierarchy for a given nodeTemplate
     *
     * @param paaSNodeTemplate
     * @return ordered nodeTemplate map
     */
    public static List<PaaSNodeTemplate> getParents(final PaaSNodeTemplate paaSNodeTemplate) {
        PaaSNodeTemplate parent = paaSNodeTemplate;
        List<PaaSNodeTemplate> templateList = Lists.newArrayList();
        while (parent != null) {
            parent = parent.getParent();
            if (parent != null) {
                templateList.add(parent);
            }
        }
        if (templateList.isEmpty()) {
            // youy nodeTemplate must be a compute => there is no host
            throw new PaaSTechnicalException("The node template <" + paaSNodeTemplate.getId() + "> is not declared as hosted on a compute.");
        }
        return templateList;
    }

    public static String getProperInterfaceName(String interfaceName) {
        if (ToscaNodeLifecycleConstants.STANDARD_SHORT.equalsIgnoreCase(interfaceName)) {
            return ToscaNodeLifecycleConstants.STANDARD;
        } else if (ToscaRelationshipLifecycleConstants.CONFIGURE_SHORT.equalsIgnoreCase(interfaceName)) {
            return ToscaRelationshipLifecycleConstants.CONFIGURE;
        }
        return interfaceName;
    }

    public static String formatedOperationOutputName(String nodeName, String interfaceName, String operationName, String output) {
        return AlienUtils.prefixWith(AlienConstants.OPERATION_NAME_SEPARATOR, output, new String[] { nodeName, interfaceName, operationName });
    }

}
