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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

@Disabled( "Run manually when Docker Model Runner is available" )
public class DockerModelRunnerServiceTest extends BaseIntegrationTest {

	private static final String	DOCKER_BASE_URL	= "http://localhost:12434";
	private static final String	DOCKER_MODEL	= "ai/gemma3:latest";

	@BeforeEach
	public void beforeEach() {
		moduleRecord.settings.put( "provider", "docker" );
		// Clear any apiKey from previous tests (e.g., Bedrock uses struct-based apiKey)
		moduleRecord.settings.put( "apiKey", "" );
	}

	/**
	 * Check if Docker Model Runner is available and has the required model
	 */
	private boolean isDockerModelAvailable() {
		try {
			URL					url			= URI.create( DOCKER_BASE_URL + "/v1/models" ).toURL();
			HttpURLConnection	connection	= ( HttpURLConnection ) url.openConnection();
			connection.setRequestMethod( "GET" );
			connection.setConnectTimeout( 2000 );
			connection.setReadTimeout( 2000 );

			if ( connection.getResponseCode() != 200 ) {
				connection.disconnect();
				return false;
			}

			// Read response and check for model
			BufferedReader	reader		= new BufferedReader( new InputStreamReader( connection.getInputStream() ) );
			StringBuilder	response	= new StringBuilder();
			String			line;
			while ( ( line = reader.readLine() ) != null ) {
				response.append( line );
			}
			reader.close();
			connection.disconnect();

			return response.toString().contains( DOCKER_MODEL );
		} catch ( Exception e ) {
			return false;
		}
	}

	@Test
	@DisplayName( "Can instantiate Docker Model Runner service via aiService BIF" )
	public void testInstantiateDocker() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = aiService(
					"docker",
					{
						baseURL: "http://localhost:12434"
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
	@DisplayName( "Docker service can be configured with model" )
	public void testConfiguration() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = aiService(
					"docker",
					{
						baseURL: "http://my-docker-host:8080",
						model: "test-model"
					}
				)

				configuredModel = service.getParams().model
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "configuredModel" ) ) ).isEqualTo( "test-model" );
	}

	@Test
	@DisplayName( "Docker Model Runner can make real API call with ai/gemma3 model" )
	public void testRealDockerModelCall() {
		if ( !isDockerModelAvailable() ) {
			System.out.println( "Skipping testRealDockerModelCall - Docker Model Runner not available or model " + DOCKER_MODEL + " not found" );
			return;
		}

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				response = aiChat(
					aiMessage().user( "Say 'Docker test successful' and nothing else" ),
					{
						model: "%s",
						max_tokens: 50
					},
					{
						provider: "docker",
						baseURL: "%s",
						returnFormat: "single"
					}
				)

				hasContent = !isNull( response ) && len( response ) > 0
			""".formatted( DOCKER_MODEL, DOCKER_BASE_URL ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasContent" ) ) ).isEqualTo( true );
	}

}
