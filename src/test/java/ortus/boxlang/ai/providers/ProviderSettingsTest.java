package ortus.boxlang.ai.providers;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Module settings under providers.<slug> (the provider name passed to aiService(), e.g. "openai-compatible")
 * must reach the service, as well as providers.<service name>. No network and no API keys: the URLs point
 * to a closed port and the chat packet is captured by a beforeLLMCall middleware that cancels the call.
 */
@DisplayName( "Provider Module Settings Tests" )
public class ProviderSettingsTest extends BaseIntegrationTest {

	// @formatter:off
	private static final String SETUP = """
		import bxModules.bxai.models.middleware.AiMiddlewareResult;
		settings = getModuleInfo( "bxai" ).settings;
		captured = {};
		capture = {
			beforeLLMCall : ( ctx ) => {
				captured.packet = ctx.dataPacket;
				return AiMiddlewareResult::cancel( "captured" );
			}
		};
	""";
	// @formatter:on

	@AfterEach
	public void resetSettings() {
		// @formatter:off
		runtime.executeSource(
		    """
		        settings = getModuleInfo( "bxai" ).settings;
		        settings.providers      = {};
		        settings.defaultParams  = {};
		        settings.profiles       = {};
		        settings.defaultProfile = "";
		    """,
		    context
		);
		// @formatter:on
	}

	@Test
	@DisplayName( "providers.openai-compatible.options.baseURL sets the chat URL" )
	public void testSlugOptionsBaseURL() {
		// @formatter:off
		runtime.executeSource( SETUP + """
			settings.providers = { "openai-compatible" : { options : { baseURL : "http://127.0.0.1:9/v1" } } };
			service = aiService( "openai-compatible" );
			chatURL = service.getChatURL();
			embeddingsURL = service.getEmbeddingsURL();
		""", context );
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "chatURL" ) ) ).isEqualTo( "http://127.0.0.1:9/v1/chat/completions" );
		assertThat( variables.getAsString( Key.of( "embeddingsURL" ) ) ).isEqualTo( "http://127.0.0.1:9/v1/embeddings" );
	}

	@Test
	@DisplayName( "providers.<service name> settings still apply (backwards compatible)" )
	public void testServiceNameOptionsStillApply() {
		// @formatter:off
		runtime.executeSource( SETUP + """
			settings.providers = { "OpenAICompatible" : { options : { baseURL : "http://127.0.0.1:9/legacy" } } };
			chatURL = aiService( "openai-compatible" ).getChatURL();
		""", context );
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "chatURL" ) ) ).startsWith( "http://127.0.0.1:9/legacy" );
	}

	@Test
	@DisplayName( "providers.<slug> wins over providers.<service name> when both exist" )
	public void testSlugWinsOverServiceName() {
		// @formatter:off
		runtime.executeSource( SETUP + """
			settings.providers = {
				"OpenAICompatible"  : { options : { baseURL : "http://127.0.0.1:9/legacy" } },
				"openai-compatible" : { options : { baseURL : "http://127.0.0.1:9/slug" } }
			};
			chatURL = aiService( "openai-compatible" ).getChatURL();
		""", context );
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "chatURL" ) ) ).startsWith( "http://127.0.0.1:9/slug" );
	}

	@Test
	@DisplayName( "providers.<slug>.params.model is applied to the service and the request" )
	public void testSlugParamsModel() {
		// @formatter:off
		runtime.executeSource( SETUP + """
			settings.providers = { "openai-compatible" : {
				params  : { model : "local-llama" },
				options : { baseURL : "http://127.0.0.1:9/v1" }
			} };
			serviceModel = aiService( "openai-compatible" ).getParams().model;
			aiChat( "hi", {}, { provider : "openai-compatible", middleware : capture } );
			packetModel = captured.packet.model;
		""", context );
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "serviceModel" ) ) ).isEqualTo( "local-llama" );
		assertThat( variables.getAsString( Key.of( "packetModel" ) ) ).isEqualTo( "local-llama" );
	}

	@Test
	@DisplayName( "Slug lookup works for other providers whose service name differs (docker)" )
	public void testDockerSlug() {
		// @formatter:off
		runtime.executeSource( SETUP + """
			settings.providers = { "docker" : { params : { model : "ai/docker-model" }, options : { baseURL : "http://127.0.0.1:9/engines/v1" } } };
			service = aiService( "docker" );
			dockerModel = service.getParams().model;
			dockerURL = service.getChatURL();
		""", context );
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "dockerModel" ) ) ).isEqualTo( "ai/docker-model" );
		assertThat( variables.getAsString( Key.of( "dockerURL" ) ) ).startsWith( "http://127.0.0.1:9/engines/v1" );
	}

	@Test
	@DisplayName( "Call options win over provider settings options" )
	public void testCallOptionsWin() {
		// @formatter:off
		runtime.executeSource( SETUP + """
			settings.providers = { "openai-compatible" : { options : { baseURL : "http://127.0.0.1:9/settings", timeout : 5 } } };
			service = aiService( "openai-compatible", { baseURL : "http://127.0.0.1:9/call" } );
			chatURL = service.getChatURL();
			timeout = service.getOptions().timeout;
		""", context );
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "chatURL" ) ) ).startsWith( "http://127.0.0.1:9/call" );
		// Settings still fill the gaps
		assertThat( variables.getAsInteger( Key.of( "timeout" ) ) ).isEqualTo( 5 );
	}

	@Test
	@DisplayName( "Params precedence: built-in < defaultParams < providers.<slug>.params < call params" )
	public void testParamsPrecedence() {
		// @formatter:off
		runtime.executeSource( SETUP + """
			settings.providers = { "openai-compatible" : { options : { baseURL : "http://127.0.0.1:9/v1" } } };

			// defaultParams beats the built-in default model of the service
			settings.defaultParams = { model : "default-model", temperature : 0.1 };
			aiChat( "hi", {}, { provider : "openai-compatible", middleware : capture } );
			defaultModel = captured.packet.model;

			// provider settings beat defaultParams
			settings.providers[ "openai-compatible" ].params = { model : "provider-model", temperature : 0.2 };
			aiChat( "hi", {}, { provider : "openai-compatible", middleware : capture } );
			providerModel = captured.packet.model;
			providerTemperature = captured.packet.temperature;

			// call params beat provider settings
			aiChat( "hi", { model : "call-model", temperature : 0.3 }, { provider : "openai-compatible", middleware : capture } );
			callModel = captured.packet.model;
			callTemperature = captured.packet.temperature;
		""", context );
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "defaultModel" ) ) ).isEqualTo( "default-model" );
		assertThat( variables.getAsString( Key.of( "providerModel" ) ) ).isEqualTo( "provider-model" );
		assertThat( variables.get( Key.of( "providerTemperature" ) ).toString() ).isEqualTo( "0.2" );
		assertThat( variables.getAsString( Key.of( "callModel" ) ) ).isEqualTo( "call-model" );
		assertThat( variables.get( Key.of( "callTemperature" ) ).toString() ).isEqualTo( "0.3" );
	}

	@Test
	@DisplayName( "A profile with provider and options but no model uses defaultParams.model, not the built-in default" )
	public void testProfileWithoutModelUsesDefaultParams() {
		// @formatter:off
		runtime.executeSource( SETUP + """
			settings.defaultParams = { model : "default-model" };
			settings.profiles = { local : { provider : "openai-compatible", options : { baseURL : "http://127.0.0.1:9/v1" } } };
			aiChat( "hi", {}, { profile : "local", middleware : capture } );
			profileModel = captured.packet.model;
		""", context );
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "profileModel" ) ) ).isEqualTo( "default-model" );
	}
}
