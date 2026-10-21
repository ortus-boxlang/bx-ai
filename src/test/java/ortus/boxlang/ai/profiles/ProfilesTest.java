package ortus.boxlang.ai.profiles;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

@DisplayName( "Configuration Profiles Tests" )
public class ProfilesTest extends BaseIntegrationTest {

	/**
	 * Env var (read as a system property) that holds the API key of the "keyed" profile
	 */
	private static final String	KEY_ENV		= "BXAI_PROFILE_TEST_KEY";
	private static final String	KEY_VALUE	= "sk-profile-secret-value";

	/**
	 * Imports and the settings every test starts from. Settings are a shared reference, so they are reset after each test.
	 */
	// @formatter:off
	private static final String SETUP = """
		import bxModules.bxai.models.providers.MockService;
		import bxModules.bxai.models.util.ProfileResolver;
		settings = getModuleInfo( "bxai" ).settings;
		settings.profiles = {
			fast    : { provider: "mock", model: "fast-model", params: { temperature: 0 } },
			planner : { provider: "mock", model: "planner-model", params: { temperature: 0.7, max_tokens: 4096 } },
			keyed   : { provider: "mock", apiKeyEnv: "%s" }
		};
		MockService::clearRecorded();
	""".formatted( KEY_ENV );
	// @formatter:on

	@BeforeEach
	public void setKey() {
		System.setProperty( KEY_ENV, KEY_VALUE );
	}

	@AfterEach
	public void resetSettings() {
		System.clearProperty( KEY_ENV );
		// @formatter:off
		runtime.executeSource(
		    """
		        settings = getModuleInfo( "bxai" ).settings;
		        settings.profiles       = {};
		        settings.defaultProfile = "";
		        settings.providers      = {};
		        settings.embeddings     = { defaultProvider: "", defaultModel: "", defaultApiKey: "" };
		    """,
		    context
		);
		// @formatter:on
	}

	private void run( String source ) {
		runtime.executeSource( SETUP + source, context );
	}

	@DisplayName( "No profiles configured: only the default profile exists and calls behave as before" )
	@Test
	public void testBackwardsCompatible() {
		runtime.executeSource(
		    """
		        profiles = aiProfiles();
		        count = profiles.len();
		        onlyDefault = profiles.first().name == "default" && profiles.first().isDefault;
		        reply = aiChat( "Hello", {}, { provider: "mock" } );
		    """,
		    context
		);

		assertThat( variables.getAsNumber( Key.of( "count" ) ).intValue() ).isEqualTo( 1 );
		assertThat( variables.getAsBoolean( Key.of( "onlyDefault" ) ) ).isTrue();
		assertThat( variables.getAsString( Key.of( "reply" ) ) ).isEqualTo( "Mock response" );
	}

	@DisplayName( "aiChat options.profile selects the provider, model and params" )
	@Test
	public void testAiChatProfile() {
		run( """
		         aiChat( "Hello", {}, { profile: "fast" } );
		         sent = MockService::getRecorded().last();
		         model = sent.model;
		         temperature = sent.params.temperature;
		     """ );

		assertThat( variables.getAsString( Key.of( "model" ) ) ).isEqualTo( "fast-model" );
		assertThat( variables.getAsNumber( Key.of( "temperature" ) ).intValue() ).isEqualTo( 0 );
	}

	@DisplayName( "Call params win over the profile, the profile wins over provider params" )
	@Test
	public void testLayering() {
		run( """
		         settings.providers = { mock: { params: { temperature: 0.1, top_p: 0.5 } } };

		         aiChat( "Hello", {}, { profile: "fast" } );
		         fromProfile = MockService::getRecorded().last().params;

		         aiChat( "Hello", { temperature: 0.9 }, { profile: "fast" } );
		         fromCall = MockService::getRecorded().last().params;

		         profileTemperature = fromProfile.temperature;
		         providerTopP = fromProfile.top_p;
		         callTemperature = fromCall.temperature;
		     """ );

		assertThat( variables.getAsNumber( Key.of( "profileTemperature" ) ).intValue() ).isEqualTo( 0 );
		assertThat( variables.getAsNumber( Key.of( "providerTopP" ) ).doubleValue() ).isEqualTo( 0.5 );
		assertThat( variables.getAsNumber( Key.of( "callTemperature" ) ).doubleValue() ).isEqualTo( 0.9 );
	}

	@DisplayName( "defaultProfile is used when a call names no provider and no profile" )
	@Test
	public void testDefaultProfile() {
		run( """
		         settings.defaultProfile = "planner";
		         aiChat( "Hello" );
		         model = MockService::getRecorded().last().model;
		         describedDefault = aiProfiles().filter( p => p.isDefault ).first().name;
		     """ );

		assertThat( variables.getAsString( Key.of( "model" ) ) ).isEqualTo( "planner-model" );
		assertThat( variables.getAsString( Key.of( "describedDefault" ) ) ).isEqualTo( "planner" );
	}

	@DisplayName( "aiModel( profile ) builds the model from the profile" )
	@Test
	public void testAiModelProfile() {
		run( """
		         model = aiModel( profile: "fast" );
		         config = model.getConfig();
		         profile = config.profile;
		         provider = config.provider;
		         model.run( "Hello" );
		         sent = MockService::getRecorded().last().model;
		     """ );

		assertThat( variables.getAsString( Key.of( "profile" ) ) ).isEqualTo( "fast" );
		assertThat( variables.getAsString( Key.of( "provider" ) ) ).isEqualTo( "mock" );
		assertThat( variables.getAsString( Key.of( "sent" ) ) ).isEqualTo( "fast-model" );
	}

	@DisplayName( "aiAgent( profile ) uses the profile, options.profile switches one run, withProfile switches the agent, memory survives" )
	@Test
	public void testAgentProfiles() {
		run( """
		         agent = aiAgent( name: "switcher", profile: "fast" );
		         startProfile = agent.getProfile();

		         agent.run( "one" );
		         first = MockService::getRecorded().last().model;

		         // One run on another profile, the agent keeps its own
		         agent.run( "two", {}, { profile: "planner" } );
		         perRun = MockService::getRecorded().last().model;
		         profileAfterRun = agent.getProfile();
		         agent.run( "three" );
		         backToOwn = MockService::getRecorded().last().model;

		         // Persistent switch
		         agent.withProfile( "planner" );
		         agent.run( "four" );
		         switched = MockService::getRecorded().last().model;
		         switchedProfile = agent.getProfile();

		         // The last request carried the whole conversation, so memory survived every switch
		         lastMessages = MockService::getRecorded().last().messages;
		         sawFirstQuestion = lastMessages.filter( m => ( m.content ?: "" ) == "one" ).len() == 1;
		     """ );

		assertThat( variables.getAsString( Key.of( "startProfile" ) ) ).isEqualTo( "fast" );
		assertThat( variables.getAsString( Key.of( "first" ) ) ).isEqualTo( "fast-model" );
		assertThat( variables.getAsString( Key.of( "perRun" ) ) ).isEqualTo( "planner-model" );
		assertThat( variables.getAsString( Key.of( "profileAfterRun" ) ) ).isEqualTo( "fast" );
		assertThat( variables.getAsString( Key.of( "backToOwn" ) ) ).isEqualTo( "fast-model" );
		assertThat( variables.getAsString( Key.of( "switched" ) ) ).isEqualTo( "planner-model" );
		assertThat( variables.getAsString( Key.of( "switchedProfile" ) ) ).isEqualTo( "planner" );
		assertThat( variables.getAsBoolean( Key.of( "sawFirstQuestion" ) ) ).isTrue();
	}

	@DisplayName( "aiAgent rejects a model together with a profile" )
	@Test
	public void testAgentModelAndProfile() {
		assertThrows( Exception.class, () -> run( """
		                                              aiAgent( model: aiModel( "mock" ), profile: "fast" );
		                                          """ ) );
	}

	@DisplayName( "An unknown profile throws ProfileNotFound" )
	@Test
	public void testUnknownProfile() {
		Exception thrown = assertThrows( Exception.class, () -> run( """
		                                                                 aiChat( "Hello", {}, { profile: "nope" } );
		                                                             """ ) );
		assertThat( thrown.getMessage() ).contains( "Profile [nope]" );
	}

	@DisplayName( "Profile names that match a provider, are reserved or point at nothing are rejected" )
	@Test
	public void testValidation() {
		String[] invalid = {
		    "{ profiles: { openai: { model: \"x\" } } }",
		    "{ profiles: { \"default\": { model: \"x\" } } }",
		    "{ profiles: { \"bad name\": { model: \"x\" } } }",
		    "{ profiles: { fast: { params: \"not a struct\" } } }",
		    "{ profiles: { fast: { model: \"x\" } }, defaultProfile: \"missing\" }",
		    "{ profiles: { fast: { model: \"x\" } }, providers: { fast: {} } }"
		};
		for ( String config : invalid ) {
			Exception thrown = assertThrows( Exception.class, () -> run( "ProfileResolver::validate( " + config + " );" ), config );
			assertThat( thrown.getMessage() ).isNotEmpty();
		}

		// A good configuration passes
		run( """
		         ProfileResolver::validate( { profiles: { fast: { provider: "openai", model: "x", params: {}, apiKeyEnv: "X" } }, defaultProfile: "fast" } );
		         ok = true;
		     """ );
		assertThat( variables.getAsBoolean( Key.of( "ok" ) ) ).isTrue();
	}

	@DisplayName( "The key comes from the env var, is used for the profile's provider only, and is never listed" )
	@Test
	public void testApiKeySecurity() {
		run( """
		         // The profile resolves its key from the env var name
		         ownKey = aiService( profile: "keyed" ).getOptions().apiKey;

		         // Another provider never receives it
		         otherKey = aiService( "ollama", {}, "keyed" ).getOptions().apiKey ?: "";

		         // An explicit key wins
		         explicitKey = aiService( profile: "keyed", options: { apiKey: "explicit" } ).getOptions().apiKey;

		         // Nothing that describes profiles or providers holds the key
		         listing = jsonSerialize( aiProfiles() ) & jsonSerialize( aiProfile( "keyed" ) ) & jsonSerialize( aiProviders() ) & jsonSerialize( aiModel( profile: "keyed" ).getConfig() );
		         keyed = aiProfile( "keyed" );
		         hasKey = keyed.hasApiKey;
		         envName = keyed.apiKeyEnv;
		     """ );

		assertThat( variables.getAsString( Key.of( "ownKey" ) ) ).isEqualTo( KEY_VALUE );
		assertThat( variables.getAsString( Key.of( "otherKey" ) ) ).isNotEqualTo( KEY_VALUE );
		assertThat( variables.getAsString( Key.of( "explicitKey" ) ) ).isEqualTo( "explicit" );
		assertThat( variables.getAsString( Key.of( "listing" ) ) ).doesNotContain( KEY_VALUE );
		assertThat( variables.getAsBoolean( Key.of( "hasKey" ) ) ).isTrue();
		assertThat( variables.getAsString( Key.of( "envName" ) ) ).isEqualTo( KEY_ENV );
	}

	@DisplayName( "Secret looking params and options are masked when a profile is described" )
	@Test
	public void testMasking() {
		run( """
		         settings.profiles.masked = { provider: "mock", options: { token: "shh-token", timeout: 5 } };
		         described = aiProfile( "masked" );
		         token = described.options.token;
		         timeout = described.options.timeout;
		     """ );

		assertThat( variables.getAsString( Key.of( "token" ) ) ).isEqualTo( "****" );
		assertThat( variables.getAsNumber( Key.of( "timeout" ) ).intValue() ).isEqualTo( 5 );
	}

	@DisplayName( "aiEmbed uses the embeddings defaults and profiles to pick the provider" )
	@Test
	public void testEmbedDefaults() {
		// Claude has no embeddings, so the error names the provider that was picked and nothing is sent over the network
		run( """
		         settings.profiles.noEmbeddings = { provider: "claude" };

		         settings.embeddings.defaultProvider = "claude";
		         fromDefaults = "";
		         try {
		             aiEmbed( "Hello" );
		         } catch( any e ) {
		             fromDefaults = e.message;
		         }

		         settings.embeddings.defaultProvider = "";
		         fromProfile = "";
		         try {
		             aiEmbed( "Hello", {}, { profile: "noEmbeddings" } );
		         } catch( any e ) {
		             fromProfile = e.message;
		         }
		     """ );

		assertThat( variables.getAsString( Key.of( "fromDefaults" ) ) ).contains( "[Claude]" );
		assertThat( variables.getAsString( Key.of( "fromProfile" ) ) ).contains( "[Claude]" );
	}

	@DisplayName( "aiProviders lists the providers without keys" )
	@Test
	public void testProviders() {
		run( """
		         providers = aiProviders();
		         names = providers.map( p => p.name );
		         hasMock = names.contains( "mock" );
		         hasOpenAI = names.contains( "openai" );
		     """ );

		assertThat( variables.getAsBoolean( Key.of( "hasMock" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasOpenAI" ) ) ).isTrue();
	}

}
