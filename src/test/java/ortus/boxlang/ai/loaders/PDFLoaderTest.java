package ortus.boxlang.ai.loaders;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

@DisplayName( "PDFLoader Tests" )
public class PDFLoaderTest extends BaseIntegrationTest {

	private static File	testPdfFile;
	private static File	multiPagePdfFile;
	private static File	pdfWithMetadata;

	@BeforeAll
	public static void createTestPDFs() throws IOException {
		// Create test directory
		File testDir = new File( "build/test-pdfs" );
		testDir.mkdirs();

		// Create simple PDF
		testPdfFile = new File( testDir, "test-simple.pdf" );
		createSimplePDF( testPdfFile, "Hello from BoxLang PDF Loader!\nThis is a test PDF file." );

		// Create multi-page PDF
		multiPagePdfFile = new File( testDir, "test-multipage.pdf" );
		createMultiPagePDF( multiPagePdfFile );

		// Create PDF with metadata
		pdfWithMetadata = new File( testDir, "test-metadata.pdf" );
		createPDFWithMetadata( pdfWithMetadata );
	}

	@AfterAll
	public static void cleanup() {
		// Clean up test files
		if ( testPdfFile != null && testPdfFile.exists() ) {
			testPdfFile.delete();
		}
		if ( multiPagePdfFile != null && multiPagePdfFile.exists() ) {
			multiPagePdfFile.delete();
		}
		if ( pdfWithMetadata != null && pdfWithMetadata.exists() ) {
			pdfWithMetadata.delete();
		}
	}

	private static void createSimplePDF( File file, String content ) throws IOException {
		try ( PDDocument document = new PDDocument() ) {
			PDPage page = new PDPage( PDRectangle.A4 );
			document.addPage( page );

			try ( PDPageContentStream contentStream = new PDPageContentStream( document, page ) ) {
				contentStream.beginText();
				contentStream.setFont( new PDType1Font( Standard14Fonts.FontName.HELVETICA ), 12 );
				contentStream.newLineAtOffset( 50, 750 );

				String[] lines = content.split( "\\n" );
				for ( String line : lines ) {
					contentStream.showText( line );
					contentStream.newLineAtOffset( 0, -15 );
				}

				contentStream.endText();
			}

			document.save( file );
		}
	}

	private static void createMultiPagePDF( File file ) throws IOException {
		try ( PDDocument document = new PDDocument() ) {
			// Page 1
			PDPage page1 = new PDPage( PDRectangle.A4 );
			document.addPage( page1 );
			try ( PDPageContentStream contentStream = new PDPageContentStream( document, page1 ) ) {
				contentStream.beginText();
				contentStream.setFont( new PDType1Font( Standard14Fonts.FontName.HELVETICA ), 12 );
				contentStream.newLineAtOffset( 50, 750 );
				contentStream.showText( "Page 1 Content" );
				contentStream.endText();
			}

			// Page 2
			PDPage page2 = new PDPage( PDRectangle.A4 );
			document.addPage( page2 );
			try ( PDPageContentStream contentStream = new PDPageContentStream( document, page2 ) ) {
				contentStream.beginText();
				contentStream.setFont( new PDType1Font( Standard14Fonts.FontName.HELVETICA ), 12 );
				contentStream.newLineAtOffset( 50, 750 );
				contentStream.showText( "Page 2 Content" );
				contentStream.endText();
			}

			// Page 3
			PDPage page3 = new PDPage( PDRectangle.A4 );
			document.addPage( page3 );
			try ( PDPageContentStream contentStream = new PDPageContentStream( document, page3 ) ) {
				contentStream.beginText();
				contentStream.setFont( new PDType1Font( Standard14Fonts.FontName.HELVETICA ), 12 );
				contentStream.newLineAtOffset( 50, 750 );
				contentStream.showText( "Page 3 Content" );
				contentStream.endText();
			}

			document.save( file );
		}
	}

	private static void createPDFWithMetadata( File file ) throws IOException {
		try ( PDDocument document = new PDDocument() ) {
			PDPage page = new PDPage( PDRectangle.A4 );
			document.addPage( page );

			// Set metadata
			var info = document.getDocumentInformation();
			info.setTitle( "Test PDF Title" );
			info.setAuthor( "BoxLang AI Module" );
			info.setSubject( "Testing PDF Loader" );
			info.setKeywords( "test, pdf, boxlang" );
			info.setCreator( "PDFLoaderTest" );

			try ( PDPageContentStream contentStream = new PDPageContentStream( document, page ) ) {
				contentStream.beginText();
				contentStream.setFont( new PDType1Font( Standard14Fonts.FontName.HELVETICA ), 12 );
				contentStream.newLineAtOffset( 50, 750 );
				contentStream.showText( "PDF with metadata" );
				contentStream.endText();
			}

			document.save( file );
		}
	}

	@DisplayName( "PDFLoader can load a simple PDF" )
	@Test
	public void testPDFLoaderBasic() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.PDFLoader;

				loader = new PDFLoader( source: "%s" );
				rawDocs = loader.load();

				result = {
					count: rawDocs.len(),
					hasContent: rawDocs[1].getContent().len() > 0,
					content: rawDocs[1].getContent()
				};
		    """.formatted( testPdfFile.getAbsolutePath() ),
		    context
		);
		// @formatter:on

		IStruct	result	= ( IStruct ) variables.get( "result" );
		int		count	= result.getAsInteger( Key.of( "count" ) );

		assertThat( count ).isEqualTo( 1 );
		assertThat( result.getAsBoolean( Key.of( "hasContent" ) ) ).isTrue();

		String content = result.getAsString( Key.of( "content" ) );
		assertThat( content ).contains( "Hello from BoxLang PDF Loader" );
		assertThat( content ).contains( "This is a test PDF file" );
	}

	@DisplayName( "PDFLoader extracts metadata from PDF" )
	@Test
	public void testPDFLoaderMetadata() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.PDFLoader;

				loader = new PDFLoader( source: "%s" );
				rawDocs = loader.load();
				metadata = rawDocs[1].getMetadata();

				result = {
					hasTitle: metadata.keyExists( "title" ),
					hasAuthor: metadata.keyExists( "author" ),
					hasPageCount: metadata.keyExists( "pageCount" ),
					title: metadata.title ?: "",
					author: metadata.author ?: "",
					pageCount: metadata.pageCount ?: 0
				};
		    """.formatted( pdfWithMetadata.getAbsolutePath() ),
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );

		assertThat( result.getAsBoolean( Key.of( "hasTitle" ) ) ).isTrue();
		assertThat( result.getAsBoolean( Key.of( "hasAuthor" ) ) ).isTrue();
		assertThat( result.getAsBoolean( Key.of( "hasPageCount" ) ) ).isTrue();

		assertThat( result.getAsString( Key.of( "title" ) ) ).isEqualTo( "Test PDF Title" );
		assertThat( result.getAsString( Key.of( "author" ) ) ).isEqualTo( "BoxLang AI Module" );
		assertThat( result.getAsInteger( Key.of( "pageCount" ) ) ).isEqualTo( 1 );
	}

	@DisplayName( "PDFLoader can extract specific page range" )
	@Test
	public void testPDFLoaderPageRange() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.PDFLoader;

				// Extract only page 2
				loader = new PDFLoader( source: "%s" )
					.pageRange( 2, 2 );

				rawDocs = loader.load();
				content = rawDocs[1].getContent();

				result = {
					hasPage2: content.contains( "Page 2 Content" ),
					hasPage1: content.contains( "Page 1 Content" ),
					hasPage3: content.contains( "Page 3 Content" )
				};
		    """.formatted( multiPagePdfFile.getAbsolutePath() ),
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );

		assertThat( result.getAsBoolean( Key.of( "hasPage2" ) ) ).isTrue();
		assertThat( result.getAsBoolean( Key.of( "hasPage1" ) ) ).isFalse();
		assertThat( result.getAsBoolean( Key.of( "hasPage3" ) ) ).isFalse();
	}

	@DisplayName( "PDFLoader handles multi-page PDFs" )
	@Test
	public void testPDFLoaderMultiPage() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.PDFLoader;

				loader = new PDFLoader( source: "%s" );
				rawDocs = loader.load();

				result = {
					count: rawDocs.len(),
					content: rawDocs[1].getContent(),
					pageCount: rawDocs[1].getMetadata().pageCount ?: 0
				};
		    """.formatted( multiPagePdfFile.getAbsolutePath() ),
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );

		assertThat( result.getAsInteger( Key.of( "count" ) ) ).isEqualTo( 1 );
		assertThat( result.getAsInteger( Key.of( "pageCount" ) ) ).isEqualTo( 3 );

		String content = result.getAsString( Key.of( "content" ) );
		assertThat( content ).contains( "Page 1 Content" );
		assertThat( content ).contains( "Page 2 Content" );
		assertThat( content ).contains( "Page 3 Content" );
	}

	@DisplayName( "PDFLoader handles missing file" )
	@Test
	public void testPDFLoaderFileNotFound() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.PDFLoader;

				try {
					loader = new PDFLoader( source: "/nonexistent/file.pdf" );
					rawDocs = loader.load();
					result = false;
				} catch( any e ) {
					result = e.type == "PDFLoader.FileNotFound";
				}
		    """,
		    context
		);
		// @formatter:on

		boolean threwCorrectError = variables.getAsBoolean( result );
		assertThat( threwCorrectError ).isTrue();
	}

	@DisplayName( "PDFLoader handles no source" )
	@Test
	public void testPDFLoaderNoSource() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.PDFLoader;

				try {
					loader = new PDFLoader();
					rawDocs = loader.load();
					result = false;
				} catch( any e ) {
					result = e.type == "PDFLoader.NoSource";
				}
		    """,
		    context
		);
		// @formatter:on

		boolean threwCorrectError = variables.getAsBoolean( result );
		assertThat( threwCorrectError ).isTrue();
	}

	@DisplayName( "PDFLoader can disable metadata extraction" )
	@Test
	public void testPDFLoaderNoMetadata() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.PDFLoader;

				loader = new PDFLoader(
					source: "%s",
					config: { includeMetadata: false }
				);
				rawDocs = loader.load();
				metadata = rawDocs[1].getMetadata();

				result = {
					hasTitle: metadata.keyExists( "title" ),
					hasAuthor: metadata.keyExists( "author" )
				};
		    """.formatted( pdfWithMetadata.getAbsolutePath() ),
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );

		// When metadata is disabled, PDF-specific fields should not be present
		assertThat( result.getAsBoolean( Key.of( "hasTitle" ) ) ).isFalse();
		assertThat( result.getAsBoolean( Key.of( "hasAuthor" ) ) ).isFalse();
	}

	@DisplayName( "PDFLoader fluent API works" )
	@Test
	public void testPDFLoaderFluentAPI() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.PDFLoader;

				loader = new PDFLoader( source: "%s" )
					.sortByPosition( true )
					.addMoreFormatting( false )
					.suppressDuplicates( true );

				rawDocs = loader.load();

				result = rawDocs.len();
		    """.formatted( testPdfFile.getAbsolutePath() ),
		    context
		);
		// @formatter:on

		int count = variables.getAsInteger( result );
		assertThat( count ).isEqualTo( 1 );
	}

}
