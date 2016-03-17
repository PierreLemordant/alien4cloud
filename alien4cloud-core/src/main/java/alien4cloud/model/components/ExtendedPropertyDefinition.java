package alien4cloud.model.components;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * Add a <code>editable</code> which mean: the property related to this definition is editable.
 */
@Getter
@Setter
public class ExtendedPropertyDefinition extends PropertyDefinition {

    private boolean editable;

    private PropertyDefinition wrapped;

    public ExtendedPropertyDefinition(PropertyDefinition wrapped) {
        super();
        this.wrapped = wrapped;
    }

    @Override
    public List<PropertyConstraint> getConstraints() {
        return wrapped.getConstraints();
    }

    @Override
    public String getDescription() {
        return wrapped.getDescription();
    }

    @Override
    public PropertyDefinition getEntrySchema() {
        return wrapped.getEntrySchema();
    }

    @Override
    public String getType() {
        return wrapped.getType();
    }

    @Override
    public boolean isPassword() {
        return wrapped.isPassword();
    }

    @Override
    public boolean isRequired() {
        return wrapped.isRequired();
    }

    @Override
    public String getDefault() {
        return wrapped.getDefault();
    }

    @Override
    public boolean isDefinition() {
        return wrapped.isDefinition();
    }

    @Override
    public void checkIfCompatibleOrFail(PropertyDefinition propertyDefinition) throws IncompatiblePropertyDefinitionException {
        wrapped.checkIfCompatibleOrFail(propertyDefinition);
    }

}
