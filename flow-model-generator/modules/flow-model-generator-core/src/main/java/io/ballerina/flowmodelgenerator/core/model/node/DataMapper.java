/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */


package io.ballerina.flowmodelgenerator.core.model.node;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.flowmodelgenerator.core.CommonUtils;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.projects.Document;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.TextEdit;

import java.util.List;
import java.util.Optional;

/**
 * Represents the properties of a data mapper node in the flow model.
 *
 * @since 1.4.0
 */
public class DataMapper extends NodeBuilder {

    public static final String LABEL = "Data Mapper";
    public static final String DESCRIPTION = "Map data from multiple variables to a record type";

    public static final String FUNCTION_NAME_KEY = "functionName";
    public static final String FUNCTION_NAME_LABEL = "Data mapper name";
    public static final String FUNCTION_NAME_DOC = "Name of the data mapper function";

    public static final String INPUTS_KEY = "inputs";
    public static final String INPUTS_LABEL = "Inputs";
    public static final String INPUTS_DOC = "Input variables of the data mapper function";

    public static final String OUTPUT_KEY = "output";
    public static final String OUTPUT_LABEL = "Output";
    public static final String OUTPUT_DOC = "Output of the data mapper function";

    @Override
    public void setConcreteConstData() {
        metadata().label(LABEL).description(DESCRIPTION);
        codedata().node(FlowNode.Kind.DATA_MAPPER);
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        properties()
                .defaultVariable()
                .defaultCustom(FUNCTION_NAME_KEY, FUNCTION_NAME_LABEL, FUNCTION_NAME_DOC, Property.ValueType.IDENTIFIER,
                        null, "transform");

        // Obtain the visible variables to the cursor position
        WorkspaceManager workspaceManager = context.workspaceManager();
        SemanticModel semanticModel;
        Document document;
        try {
            workspaceManager.loadProject(context.filePath());
            semanticModel = workspaceManager.semanticModel(context.filePath()).orElseThrow();
            document = workspaceManager.document(context.filePath()).orElseThrow();
        } catch (WorkspaceDocumentException | EventSyncException e) {
            throw new RuntimeException(e);
        }

        // TODO: Convert the following from 2n to n
        // Get the visible variables to the cursor position
        List<String> visibleVariables = semanticModel.visibleSymbols(document, context.position()).stream()
                .filter(symbol -> symbol.kind() == SymbolKind.VARIABLE)
                .flatMap(symbol -> getVariableSignature(semanticModel, (VariableSymbol) symbol).stream())
                .toList();
        properties().defaultCustom(INPUTS_KEY, INPUTS_LABEL, INPUTS_DOC, Property.ValueType.SET, visibleVariables, "");

        // Get the visible record types to the cursor position
        List<String> visibleRecordTypes = semanticModel.visibleSymbols(document, context.position()).stream()
                .filter(symbol -> symbol.kind() == SymbolKind.TYPE_DEFINITION)
                .flatMap(symbol -> getRecordTypeSignature((TypeDefinitionSymbol) symbol).stream())
                .toList();
        properties().defaultCustom(OUTPUT_KEY, OUTPUT_LABEL, OUTPUT_DOC, Property.ValueType.SET, visibleRecordTypes,
                "");
    }

    private static Optional<String> getVariableSignature(SemanticModel semanticModel, VariableSymbol symbol) {
        Optional<String> name = symbol.getName();
        String typeSignature = CommonUtils.getTypeSignature(semanticModel, symbol.typeDescriptor(), false);
        return name.map(s -> typeSignature + " " + s);
    }

    private static Optional<String> getRecordTypeSignature(TypeDefinitionSymbol symbol) {
        if (symbol.typeDescriptor().typeKind() != TypeDescKind.RECORD) {
            return Optional.empty();
        }
        Optional<String> moduleName = symbol.getModule().flatMap(Symbol::getName);

        // TODO: Make this more scalable
        if (moduleName.isPresent() && moduleName.get().equals("lang.annotations")) {
            return Optional.empty();
        }
        return symbol.getName();
    }

    @Override
    public List<TextEdit> toSource(SourceBuilder sourceBuilder) {
        return null;
    }
}
