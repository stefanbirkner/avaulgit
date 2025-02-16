package com.github.stefanbirkner.avaulgit;

import java.util.*;
import java.util.regex.*;
import org.springframework.beans.factory.config.*;
import org.springframework.boot.origin.*;
import org.springframework.boot.origin.TextResourceOrigin.*;
import org.springframework.core.io.*;
import org.yaml.snakeyaml.*;
import org.yaml.snakeyaml.constructor.*;
import org.yaml.snakeyaml.nodes.*;
import org.yaml.snakeyaml.representer.*;
import org.yaml.snakeyaml.resolver.*;

/**
 * Merely a copy of org.springframework.boot.env.OriginTrackedYamlLoader with a
 * tiny change that adds support for Ansible Vault encrypted secrets.
 */
class OriginTrackedYamlLoaderWithAnsibleVaultSupport extends YamlProcessor {
    private final Resource resource;

    OriginTrackedYamlLoaderWithAnsibleVaultSupport(
        Resource resource
    ) {
        this.resource = resource;
        setResources(resource);
    }

    @Override
    protected Yaml createYaml() {
        var loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        loaderOptions.setMaxAliasesForCollections(Integer.MAX_VALUE);
        loaderOptions.setAllowRecursiveKeys(true);
        loaderOptions.setCodePointLimit(Integer.MAX_VALUE);
        return createYaml(loaderOptions);
    }

    private Yaml createYaml(
        LoaderOptions loaderOptions
    ) {
        var dumperOptions = new DumperOptions();
        return new Yaml(
            new OriginTrackingConstructor(loaderOptions),
            new Representer(dumperOptions),
            dumperOptions,
            loaderOptions,
            new NoTimestampResolver());
    }

    List<Map<String, Object>> load() {
        var result = new ArrayList<Map<String, Object>>();
        process((properties, map) -> result.add(getFlattenedMap(map)));
        return result;
    }

    /**
     * {@link ScalarNode} that replaces the key node in a {@link NodeTuple}.
     */
    private static class KeyScalarNode extends ScalarNode {

        KeyScalarNode(
            ScalarNode node
        ) {
            super(
                node.getTag(),
                node.getValue(),
                node.getStartMark(),
                node.getEndMark(),
                node.getScalarStyle());
        }

        static NodeTuple get(
            NodeTuple nodeTuple
        ) {
            var keyNode = nodeTuple.getKeyNode();
            var valueNode = nodeTuple.getValueNode();
            return new NodeTuple(KeyScalarNode.get(keyNode), valueNode);
        }

        private static Node get(
            Node node
        ) {
            if (node instanceof ScalarNode scalarNode)
                return new KeyScalarNode(scalarNode);
            else
                return node;
        }
    }

    /**
     * {@link Resolver} that limits {@link Tag#TIMESTAMP} tags.
     */
    private static final class NoTimestampResolver extends Resolver {

        @Override
        public void addImplicitResolver(
            Tag tag,
            Pattern regexp,
            String first,
            int limit
        ) {
            if (tag != Tag.TIMESTAMP)
                super.addImplicitResolver(tag, regexp, first, limit);
        }
    }

    /**
     * {@link Constructor} that tracks property origins. It is merely a copy of
     * org.springframework.boot.env.OriginTrackedYamlLoader.OriginTrackingConstructor
     * with a tiny in its constructor that adds support for Ansible Vault
     * encrypted secrets.
     */
    private class OriginTrackingConstructor extends SafeConstructor {

        OriginTrackingConstructor(
            LoaderOptions loadingConfig
        ) {
            super(loadingConfig);
            // This line is the only difference to Spring's original class
            this.yamlConstructors.put(new Tag("!vault"), new ConstructSecret());
        }

        @Override
        public Object getData(
        ) throws NoSuchElementException {
            var data = super.getData();
            if (data instanceof CharSequence charSequence && charSequence.isEmpty())
                return null;
            else
                return data;
        }

        @Override
        protected Object constructObject(
            Node node
        ) {
            if (node instanceof SequenceNode sequenceNode && sequenceNode.getValue().isEmpty())
                return constructTrackedObject(node, "");
            if (node instanceof ScalarNode && !(node instanceof KeyScalarNode))
                return constructTrackedObject(node, super.constructObject(node));
            if (node instanceof MappingNode mappingNode)
                replaceMappingNodeKeys(mappingNode);

            return super.constructObject(node);
        }

        private void replaceMappingNodeKeys(
            MappingNode node
        ) {
            var newValue = new ArrayList<NodeTuple>();
            node.getValue().stream()
                .map(KeyScalarNode::get)
                .forEach(newValue::add);
            node.setValue(newValue);
        }

        private Object constructTrackedObject(
            Node node,
            Object value
        ) {
            var origin = getOrigin(node);
            return OriginTrackedValue.of(getValue(value), origin);
        }

        private Object getValue(
            Object value
        ) {
            return (value != null) ? value : "";
        }

        private Origin getOrigin(
            Node node
        ) {
            var mark = node.getStartMark();
            var location = new Location(mark.getLine(), mark.getColumn());
            return new TextResourceOrigin(
                OriginTrackedYamlLoaderWithAnsibleVaultSupport.this.resource,
                location);
        }
    }
}
