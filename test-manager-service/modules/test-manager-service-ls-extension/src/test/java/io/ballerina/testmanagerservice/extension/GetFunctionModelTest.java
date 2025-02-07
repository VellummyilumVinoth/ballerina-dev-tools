/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
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

package io.ballerina.testmanagerservice.extension;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.ballerina.testmanagerservice.extension.model.TestFunction;
import io.ballerina.testmanagerservice.extension.request.GetTestFunctionRequest;
import io.ballerina.testmanagerservice.extension.response.GetTestFunctionResponse;
import org.eclipse.lsp4j.TextEdit;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Tests for the getTestFunction service.
 *
 * @since 2.0.0
 */
public class GetFunctionModelTest extends AbstractLSTest {

    private static final Type TEXT_EDIT_LIST_TYPE = new TypeToken<Map<String, List<TextEdit>>>() {
    }.getType();

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        GetFunctionModelTest.TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath),
                GetFunctionModelTest.TestConfig.class);

        String sourcePath = sourceDir.resolve(testConfig.filePath()).toAbsolutePath().toString();
        GetTestFunctionRequest request = new GetTestFunctionRequest(testConfig.functionName(), sourcePath);

        JsonObject jsonMap = getResponse(request);
        GetTestFunctionResponse response = gson.fromJson(jsonMap, GetTestFunctionResponse.class);

        TestFunction actualServiceModel = response.function();
        boolean assertTrue = false;

        if (!assertTrue) {
            GetFunctionModelTest.TestConfig updatedConfig =
                    new GetFunctionModelTest.TestConfig(testConfig.description(), testConfig.filePath(),
                            testConfig.functionName(), response);
            updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Override
    protected String getResourceDir() {
        return "get_test_function";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return GetFunctionModelTest.class;
    }

    @Override
    protected String getApiName() {
        return "getTestFunction";
    }

    /**
     * Represents the test configuration for the source generator test.
     *
     * @param filePath    The path to the source file
     * @param description The description of the test
     * @param functionName The name of the function
     * @param response    The response of the test
     */
    private record TestConfig(String description, String filePath, String functionName,
                              GetTestFunctionResponse response) {
        public String description() {
            return description == null ? "" : description;
        }
    }
}
