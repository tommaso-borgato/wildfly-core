/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.elytron;

import static org.jboss.as.controller.PersistentResourceXMLDescription.decorator;
import static org.jboss.as.controller.parsing.ParseUtils.requireAttributes;
import static org.wildfly.extension.elytron.ElytronDescriptionConstants.CUSTOM_PERMISSION_MAPPER;
import static org.wildfly.extension.elytron.ElytronDescriptionConstants.CUSTOM_PRINCIPAL_DECODER;
import static org.wildfly.extension.elytron.ElytronDescriptionConstants.CUSTOM_PRINCIPAL_TRANSFORMER;
import static org.wildfly.extension.elytron.ElytronDescriptionConstants.CUSTOM_REALM_MAPPER;
import static org.wildfly.extension.elytron.ElytronDescriptionConstants.CUSTOM_ROLE_DECODER;
import static org.wildfly.extension.elytron.ElytronDescriptionConstants.CUSTOM_ROLE_MAPPER;
import static org.wildfly.extension.elytron.ElytronDescriptionConstants.FROM;
import static org.wildfly.extension.elytron.ElytronDescriptionConstants.PRINCIPAL_DECODER;
import static org.wildfly.extension.elytron.ElytronDescriptionConstants.PRINCIPAL_TRANSFORMER;
import static org.wildfly.extension.elytron.ElytronDescriptionConstants.REALM_MAPPING;
import static org.wildfly.extension.elytron.ElytronDescriptionConstants.ROLE_MAPPER;
import static org.wildfly.extension.elytron.ElytronDescriptionConstants.TO;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshallers;
import org.jboss.as.controller.AttributeParsers;
import org.jboss.as.controller.MapAttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * XML handling for the <mappers /> element.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author Tomaz Cerar
 */
class MapperParser {
    enum Version {
        VERSION_1_0,
        VERSION_1_1,
        CURRENT // permission-sets in permission-mappings and constant-permission-mappers
    }

    private final Version version;

    private PersistentResourceXMLDescription simpleMapperParser = PersistentResourceXMLDescription.builder(PermissionMapperDefinitions.getSimplePermissionMapper().getPathElement())
            .addAttribute(PermissionMapperDefinitions.MAPPING_MODE)
            .addAttribute(PermissionMapperDefinitions.PERMISSION_MAPPINGS)
            .build();

    private PersistentResourceXMLDescription simpleMapperParser_1_0 = PersistentResourceXMLDescription.builder(PermissionMapperDefinitions.getSimplePermissionMapper().getPathElement())
            .addAttribute(PermissionMapperDefinitions.MAPPING_MODE)
            .addAttribute(PermissionMapperDefinitions.PERMISSION_MAPPINGS_1_0)
            .build();

    private PersistentResourceXMLDescription simpleMapperParser_1_1 = PersistentResourceXMLDescription.builder(PermissionMapperDefinitions.getSimplePermissionMapper().getPathElement())
            .addAttribute(PermissionMapperDefinitions.MAPPING_MODE)
            .addAttribute(PermissionMapperDefinitions.PERMISSION_MAPPINGS_1_1)
            .build();

    private PersistentResourceXMLDescription logicalPermissionMapper = PersistentResourceXMLDescription.builder(PermissionMapperDefinitions.getLogicalPermissionMapper().getPathElement())
            .addAttribute(PermissionMapperDefinitions.LOGICAL_OPERATION)
            .addAttribute(PermissionMapperDefinitions.LEFT)
            .addAttribute(PermissionMapperDefinitions.RIGHT)
            .build();
    private PersistentResourceXMLDescription constantPermissionMapper = PersistentResourceXMLDescription.builder(PermissionMapperDefinitions.getConstantPermissionMapper().getPathElement())
            .addAttribute(PermissionMapperDefinitions.PERMISSIONS)
            .addAttribute(PermissionMapperDefinitions.PERMISSION_SETS)
            .build();

    private PersistentResourceXMLDescription constantPermissionMapper_1_0 = PersistentResourceXMLDescription.builder(PermissionMapperDefinitions.getConstantPermissionMapper().getPathElement())
            .addAttribute(PermissionMapperDefinitions.PERMISSIONS)
            .build();

    private PersistentResourceXMLDescription aggregatePrincipalDecoderParser = PersistentResourceXMLDescription.builder(PrincipalDecoderDefinitions.getAggregatePrincipalDecoderDefinition().getPathElement())
            .addAttribute(PrincipalDecoderDefinitions.getAggregatePrincipalDecoderDefinition().getReferencesAttribute(), new AttributeParsers.NamedStringListParser(PRINCIPAL_DECODER), new AttributeMarshallers.NamedStringListMarshaller(PRINCIPAL_DECODER))
            .build();

    private PersistentResourceXMLDescription concatenatingPrincipalDecoderParser = PersistentResourceXMLDescription.builder(PrincipalDecoderDefinitions.getConcatenatingPrincipalDecoder().getPathElement())
            .addAttribute(PrincipalDecoderDefinitions.JOINER)
            .addAttribute(PrincipalDecoderDefinitions.PRINCIPAL_DECODERS)
            .build();

    private PersistentResourceXMLDescription constantPrincipalDecoderParser = PersistentResourceXMLDescription.builder(PrincipalDecoderDefinitions.getConstantPrincipalDecoder().getPathElement())
            .addAttribute(PrincipalDecoderDefinitions.CONSTANT)
            .build();

    private PersistentResourceXMLDescription x500AttributePrincipalDecoderParser = PersistentResourceXMLDescription.builder(PrincipalDecoderDefinitions.getX500AttributePrincipalDecoder().getPathElement())
            .addAttribute(PrincipalDecoderDefinitions.OID)
            .addAttribute(PrincipalDecoderDefinitions.ATTRIBUTE_NAME)
            .addAttribute(PrincipalDecoderDefinitions.JOINER)
            .addAttribute(PrincipalDecoderDefinitions.START_SEGMENT)
            .addAttribute(PrincipalDecoderDefinitions.MAXIMUM_SEGMENTS)
            .addAttribute(PrincipalDecoderDefinitions.REVERSE)
            .addAttribute(PrincipalDecoderDefinitions.CONVERT)
            .addAttribute(PrincipalDecoderDefinitions.REQUIRED_OIDS)
            .addAttribute(PrincipalDecoderDefinitions.REQUIRED_ATTRIBUTES)
            .build();

    private PersistentResourceXMLDescription aggregatePrincipalTransformerParser = PersistentResourceXMLDescription.builder(PrincipalTransformerDefinitions.getAggregatePrincipalTransformerDefinition().getPathElement())
            .addAttribute(PrincipalTransformerDefinitions.getAggregatePrincipalTransformerDefinition().getReferencesAttribute(), new AttributeParsers.NamedStringListParser(PRINCIPAL_TRANSFORMER), new AttributeMarshallers.NamedStringListMarshaller(PRINCIPAL_TRANSFORMER))
            .build();

    private PersistentResourceXMLDescription chainedPrincipalTransformersParser = PersistentResourceXMLDescription.builder(PrincipalTransformerDefinitions.getChainedPrincipalTransformerDefinition().getPathElement())
            .addAttribute(PrincipalTransformerDefinitions.getChainedPrincipalTransformerDefinition().getReferencesAttribute(), new AttributeParsers.NamedStringListParser(PRINCIPAL_TRANSFORMER), new AttributeMarshallers.NamedStringListMarshaller(PRINCIPAL_TRANSFORMER))
            .build();
    private PersistentResourceXMLDescription constantPrincipalTransformersParser = PersistentResourceXMLDescription.builder(PrincipalTransformerDefinitions.getConstantPrincipalTransformerDefinition().getPathElement())
            .addAttribute(PrincipalTransformerDefinitions.CONSTANT)
            .build();
    private PersistentResourceXMLDescription regexPrincipalTransformerParser = PersistentResourceXMLDescription.builder(PrincipalTransformerDefinitions.getRegexPrincipalTransformerDefinition().getPathElement())
            .addAttribute(RegexAttributeDefinitions.PATTERN)
            .addAttribute(PrincipalTransformerDefinitions.REPLACEMENT)
            .addAttribute(PrincipalTransformerDefinitions.REPLACE_ALL)
            .build();
    private PersistentResourceXMLDescription regexValidatingTransformerParser = PersistentResourceXMLDescription.builder(PrincipalTransformerDefinitions.getRegexValidatingPrincipalTransformerDefinition().getPathElement())
            .addAttribute(RegexAttributeDefinitions.PATTERN)
            .addAttribute(PrincipalTransformerDefinitions.MATCH)
            .build();

    private PersistentResourceXMLDescription constantRealmMapperParser = PersistentResourceXMLDescription.builder(RealmMapperDefinitions.getConstantRealmMapper().getPathElement())
            .addAttribute(RealmMapperDefinitions.REALM_NAME)
            .build();

    private PersistentResourceXMLDescription simpleRegexRealmMapperParser = PersistentResourceXMLDescription.builder(RealmMapperDefinitions.getSimpleRegexRealmMapperDefinition().getPathElement())
            .addAttribute(RegexAttributeDefinitions.PATTERN_CAPTURE_GROUP)
            .addAttribute(RealmMapperDefinitions.DELEGATE_REALM_MAPPER)
            .build();

    private PersistentResourceXMLDescription mappedRegexRealmMapperParser = PersistentResourceXMLDescription.builder(RealmMapperDefinitions.getMappedRegexRealmMapper().getPathElement())
            .addAttribute(RegexAttributeDefinitions.PATTERN_CAPTURE_GROUP)
            .addAttribute(RealmMapperDefinitions.DELEGATE_REALM_MAPPER)
            .addAttribute(RealmMapperDefinitions.REALM_REALM_MAP, new AttributeParsers.PropertiesParser(null, REALM_MAPPING, false) {
                public void parseSingleElement(MapAttributeDefinition attribute, XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
                    final String[] array = requireAttributes(reader, FROM, TO);
                    attribute.parseAndAddParameterElement(array[0], array[1], operation, reader);
                    ParseUtils.requireNoContent(reader);
                }

            }, new AttributeMarshallers.MapAttributeMarshaller(null, null, false) {
                @Override
                public void marshallSingleElement(AttributeDefinition attribute, ModelNode property, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
                    writer.writeEmptyElement(REALM_MAPPING);
                    writer.writeAttribute(FROM, property.asProperty().getName());
                    writer.writeAttribute(TO, property.asProperty().getValue().asString());
                }
            })
            .build();

    private PersistentResourceXMLDescription simpleRoleDecoderParser = PersistentResourceXMLDescription.builder(RoleDecoderDefinitions.getSimpleRoleDecoderDefinition().getPathElement())
            .addAttribute(RoleDecoderDefinitions.ATTRIBUTE)
            .build();

    private PersistentResourceXMLDescription addPrefixRoleMapperParser = PersistentResourceXMLDescription.builder(RoleMapperDefinitions.getAddPrefixRoleMapperDefinition().getPathElement())
            .addAttribute(RoleMapperDefinitions.PREFIX)
            .build();
    private PersistentResourceXMLDescription addSuffixRoleMapperParser = PersistentResourceXMLDescription.builder(RoleMapperDefinitions.getAddSuffixRoleMapperDefinition().getPathElement())
            .addAttribute(RoleMapperDefinitions.SUFFIX)
            .build();

    private PersistentResourceXMLDescription aggregateRoleMapperParser = PersistentResourceXMLDescription.builder(RoleMapperDefinitions.getAggregateRoleMapperDefinition().getPathElement())
            .addAttribute(RoleMapperDefinitions.getAggregateRoleMapperDefinition().getReferencesAttribute(), new AttributeParsers.NamedStringListParser(ROLE_MAPPER), new AttributeMarshallers.NamedStringListMarshaller(ROLE_MAPPER))
            .build();
    private PersistentResourceXMLDescription constantRoleMapperParser = PersistentResourceXMLDescription.builder(RoleMapperDefinitions.getConstantRoleMapperDefinition().getPathElement())
            .addAttribute(RoleMapperDefinitions.ROLES)
            .build();

    private PersistentResourceXMLDescription logicalRoleMapperParser = PersistentResourceXMLDescription.builder(RoleMapperDefinitions.getLogicalRoleMapperDefinition().getPathElement())
            .addAttribute(RoleMapperDefinitions.LOGICAL_OPERATION)
            .addAttribute(RoleMapperDefinitions.LEFT)
            .addAttribute(RoleMapperDefinitions.RIGHT)
            .build();

    MapperParser(Version version) {
        this.version = version;
    }

    MapperParser() {
        this.version = Version.CURRENT;
    }

    //1.0 version of parser is different at simple mapperParser

    private PersistentResourceXMLDescription getSimpleMapperParser() {
        if (version.equals(Version.VERSION_1_0)) {
            return simpleMapperParser_1_0;
        } else if (version.equals(Version.VERSION_1_1)) {
            return simpleMapperParser_1_1;
        }
        return simpleMapperParser;
    }

    private PersistentResourceXMLDescription getConstantPermissionMapperParser() {
        if (version.equals(Version.VERSION_1_0) || version.equals(Version.VERSION_1_1)) {
            return constantPermissionMapper_1_0;
        } else {
            return constantPermissionMapper;
        }
    }


    static PersistentResourceXMLDescription getCustomComponentParser(String componentType) {
        return PersistentResourceXMLDescription.builder(PathElement.pathElement(componentType))
                .setUseElementsForGroups(false)
                .addAttribute(ClassLoadingAttributeDefinitions.MODULE)
                .addAttribute(ClassLoadingAttributeDefinitions.CLASS_NAME)
                .addAttribute(CustomComponentDefinition.CONFIGURATION)
                .build();
    }

    public PersistentResourceXMLDescription getParser() {
        return decorator(ElytronDescriptionConstants.MAPPERS)
                .addChild(getCustomComponentParser(CUSTOM_PERMISSION_MAPPER))
                .addChild(logicalPermissionMapper)
                .addChild(getSimpleMapperParser())
                .addChild(getConstantPermissionMapperParser())
                .addChild(aggregatePrincipalDecoderParser)
                .addChild(concatenatingPrincipalDecoderParser)
                .addChild(constantPrincipalDecoderParser)
                .addChild(getCustomComponentParser(CUSTOM_PRINCIPAL_DECODER))
                .addChild(x500AttributePrincipalDecoderParser)
                .addChild(aggregatePrincipalTransformerParser)
                .addChild(chainedPrincipalTransformersParser)
                .addChild(constantPrincipalTransformersParser)
                .addChild(getCustomComponentParser(CUSTOM_PRINCIPAL_TRANSFORMER))
                .addChild(regexPrincipalTransformerParser)
                .addChild(regexValidatingTransformerParser)
                .addChild(constantRealmMapperParser)
                .addChild(getCustomComponentParser(CUSTOM_REALM_MAPPER))
                .addChild(simpleRegexRealmMapperParser)
                .addChild(mappedRegexRealmMapperParser)
                .addChild(getCustomComponentParser(CUSTOM_ROLE_DECODER))
                .addChild(simpleRoleDecoderParser)
                .addChild(addPrefixRoleMapperParser)
                .addChild(addSuffixRoleMapperParser)
                .addChild(aggregateRoleMapperParser)
                .addChild(constantRoleMapperParser)
                .addChild(getCustomComponentParser(CUSTOM_ROLE_MAPPER))
                .addChild(logicalRoleMapperParser)
                .build();
    }
}
