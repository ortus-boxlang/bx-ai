package ortus.boxlang.ai.loaders;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

@DisplayName( "TikaLoader Tests" )
public class TikaLoaderTest extends BaseIntegrationTest {

	@DisplayName( "TikaLoader can be created with file source" )
	@Test
	public void testTikaLoaderCreation() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.TikaLoader;
				loader = new TikaLoader( source: "/path/to/document.pdf" );
				result = {
					name: loader.getName(),
					source: loader.getSource()
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result.getAsString( Key.of( "name" ) ) ).isEqualTo( "TikaLoader" );
		assertThat( result.getAsString( Key.of( "source" ) ) ).isEqualTo( "/path/to/document.pdf" );
	}

	@DisplayName( "TikaLoader supports fluent configuration" )
	@Test
	public void testTikaLoaderFluentConfig() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.TikaLoader;
				loader = new TikaLoader( source: "/path/to/doc.pdf" )
					.maxLength( 10000 )
					.enableOCR( false )
					.password( "secret" );
				result = loader.getConfig();
		    """,
		    context
		);
		// @formatter:on

		IStruct config = ( IStruct ) variables.get( "result" );
		assertThat( config.getAsInteger( Key.of( "maxLength" ) ) ).isEqualTo( 10000 );
		assertThat( config.getAsBoolean( Key.of( "ocrEnabled" ) ) ).isFalse();
		assertThat( config.getAsString( Key.of( "passwordProtected" ) ) ).isEqualTo( "secret" );
	}

	@DisplayName( "TikaLoader can check supported extensions" )
	@Test
	public void testTikaLoaderSupportedExtensions() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.TikaLoader;
				result = {
					pdfSupported: TikaLoader::isSupported( "/path/file.pdf" ),
					docxSupported: TikaLoader::isSupported( "/path/file.docx" ),
					txtNotSupported: TikaLoader::isSupported( "/path/file.txt" ),
					extensions: TikaLoader::getSupportedExtensions()
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result.getAsBoolean( Key.of( "pdfSupported" ) ) ).isTrue();
		assertThat( result.getAsBoolean( Key.of( "docxSupported" ) ) ).isTrue();
		assertThat( result.getAsBoolean( Key.of( "txtNotSupported" ) ) ).isFalse();

		Array extensions = result.getAsArray( Key.of( "extensions" ) );
		assertThat( extensions ).isNotNull();
		assertThat( extensions.size() ).isGreaterThan( 5 );
	}

}
