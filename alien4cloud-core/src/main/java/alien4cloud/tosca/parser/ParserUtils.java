package alien4cloud.tosca.parser;

import java.util.List;

import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;

/**
 * Utility class to help with parsing.
 */
public final class ParserUtils {

    /**
     * Utility to get a scalar.
     * 
     * @param node The node from which to get a scalar value.
     * @param parsingErrors The parsing errors in which to add errors in case the node is not a scalar node.
     * @return The Scalar value or null if the node is not a scalar node.
     */
    public static String getScalar(Node node, List<ParsingError> parsingErrors) {
        if (node instanceof ScalarNode) {
            return ((ScalarNode) node).getValue();
        }
        addTypeError(node, parsingErrors, "scalar");
        return null;
    }

    /**
     * Add an invalid type {@link ParsingError} to the given parsing errors list.
     * 
     * @param node The node that is causing the type error.
     * @param parsingErrors The parsing errors in which to add the error.
     * @param expectedType The type that was actually expected.
     */
    public static void addTypeError(Node node, List<ParsingError> parsingErrors, String expectedType) {
        parsingErrors.add(new ParsingError("Invalid type", node.getStartMark(), "Expected the type to match tosca type", node.getEndMark(), expectedType));
    }
}