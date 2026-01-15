/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.ai.providers;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

public class DockerModelRunnerServiceTest extends BaseIntegrationTest {

	@Test
	@DisplayName( "Can instantiate Docker Model Runner service via aiService BIF" )
	public void testInstantiateDocker() {
		// @formatter:off
		runtime.executeSource(
			"""
				service = aiService(
					"docker",
					{
						baseURL: "http://localhost:11435"
					}
				)
				serviceName = service.getName()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "serviceName" ) ) ).isEqualTo( "DockerModelRunner" );
	}

	@Test
	@DisplayName( "Docker service can be configured" )
	public void testConfiguration() {
		// @formatter:off
		runtime.executeSource(
			"""
				service = aiService(
					"docker",
					{
						baseURL: "http://my-docker-host:8080",
						model: "test-model"
					}
				)
				
				hasService = !isNull( service )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasService" ) ) ).isEqualTo( true );
	}

	@Test
	@EnabledIfSystemProperty( named = "docker.test", matches = "true" )
	@DisplayName( "Docker service can make a simple chat request" )
	public void testSimpleChatRequest() {
		// @formatter:off
		runtime.executeSource(
			"""
				response = aiChat(
					"docker",
					{
						model: "qwen2.5:0.5b-instruct",
						useHostURL: true,
						max_tokens: 50
					},
					aiMessage().user( "Say 'Docker test successful' and nothing else" )
				)
				
				hasContent = !isNull( response.content )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasContent" ) ) ).isEqualTo( true );
	}
}
