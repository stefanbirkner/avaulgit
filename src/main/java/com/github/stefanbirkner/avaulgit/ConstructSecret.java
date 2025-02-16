package com.github.stefanbirkner.avaulgit;

import org.yaml.snakeyaml.constructor.*;
import org.yaml.snakeyaml.nodes.*;

/**
 * Creates a {@link Secret} from a {@code ScalarNode}.
 */
class ConstructSecret extends AbstractConstruct {
    @Override
    public Object construct(Node node) {
        if (node instanceof ScalarNode scalarNode)
            return new Secret(scalarNode.getValue());
        else
            throw new IllegalArgumentException(
                "The node's type is " + node.getClass().getName()
                    + ", but a Secret can only be created from "
                    + ScalarNode.class.getName());
    }
}
