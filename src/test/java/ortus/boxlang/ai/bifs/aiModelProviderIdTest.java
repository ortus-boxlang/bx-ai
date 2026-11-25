package ortus.boxlang.ai.bifs;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * AiModel.getConfig().provider is the service display name, getConfig().providerId is the id aiService() takes
 */
@DisplayName( "AiModel provider id Tests" )
public class aiModelProviderIdTest extends BaseIntegrationTest {

	@DisplayName( "openai-compatible and docker report the id aiService() accepts" )
	@Test
	public void testIdsDifferFromDisplayNames() {
		// @formatter:off
		runtime.executeSource(
			"""
				compatible = aiModel( "openai-compatible", { apiKey: "test" } ).getConfig()
				docker     = aiModel( "docker", { apiKey: "test" } ).getConfig()
				compatibleService = aiService( compatible.providerId, { apiKey: "test" } ).getName()
				dockerService     = aiService( docker.providerId, { apiKey: "test" } ).getName()
			""",
			context
		);
		// @formatter:on

		var	compatible	= variables.getAsStruct( Key.of( "compatible" ) );
		var	docker		= variables.getAsStruct( Key.of( "docker" ) );
		assertThat( compatible.get( Key.of( "providerId" ) ) ).isEqualTo( "openai-compatible" );
		assertThat( docker.get( Key.of( "providerId" ) ) ).isEqualTo( "docker" );
		// The display name stays as it was, nothing existing breaks
		assertThat( compatible.get( Key.of( "provider" ) ) ).isEqualTo( "OpenAICompatible" );
		assertThat( docker.get( Key.of( "provider" ) ) ).isEqualTo( "DockerModelRunner" );
		assertThat( variables.getAsString( Key.of( "compatibleService" ) ) ).isEqualTo( "OpenAICompatible" );
		assertThat( variables.getAsString( Key.of( "dockerService" ) ) ).isEqualTo( "DockerModelRunner" );
	}

	@DisplayName( "A service built without aiService() still reports its id" )
	@Test
	public void testIdWithoutAiService() {
		// @formatter:off
		runtime.executeSource(
			"""
				compatibleId = new bxModules.bxai.models.providers.OpenAICompatibleService().getProviderId()
				dockerId     = new bxModules.bxai.models.providers.DockerModelRunnerService().getProviderId()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "compatibleId" ) ) ).isEqualTo( "openai-compatible" );
		assertThat( variables.getAsString( Key.of( "dockerId" ) ) ).isEqualTo( "docker" );
	}

	@DisplayName( "Every registered provider round trips: aiService( aiModel( id ).getConfig().providerId )" )
	@Test
	public void testEveryProviderRoundTrips() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.util.ProfileResolver;
				failures = []
				checked  = []
				for( id in ProfileResolver::providerNames() ){
					try {
						providerId = aiModel( id, { apiKey: "test" } ).getConfig().providerId
						again      = aiService( providerId, { apiKey: "test" } )
						if( providerId != id ){
							failures.append( "#id# reported #providerId#" )
						}
						checked.append( id )
					} catch( any e ){
						failures.append( "#id#: #e.message#" )
					}
				}
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "failures" ) ).toString() ).isEqualTo( "[]" );
		assertThat( ( ( ortus.boxlang.runtime.types.Array ) variables.get( Key.of( "checked" ) ) ).size() ).isAtLeast( 20 );
	}
}
