package ortus.boxlang.ai.loaders;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

public class DocumentTest extends BaseIntegrationTest {

	@DisplayName( "Can create a basic Document with content" )
	@Test
	public void testBasicDocumentCreation() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.Document;

				doc = new Document( content="This is a test document" );
				result = {
					content: doc.getContent(),
					hasContent: doc.hasContent(),
					length: doc.getContentLength()
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result.get( "content" ) ).isEqualTo( "This is a test document" );
		assertThat( result.get( "hasContent" ) ).isEqualTo( true );
		assertThat( result.get( "length" ) ).isEqualTo( 23 );
	}

	@DisplayName( "Can create a Document with custom ID and metadata" )
	@Test
	public void testDocumentWithMetadata() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.Document;

				doc = new Document(
					id="doc-123",
					content="Test content",
					metadata={ source: "test.txt", author: "John Doe" }
				);

				result = {
					id: doc.getId(),
					source: doc.getMeta( "source" ),
					author: doc.getMeta( "author" )
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result.get( "id" ) ).isEqualTo( "doc-123" );
		assertThat( result.get( "source" ) ).isEqualTo( "test.txt" );
		assertThat( result.get( "author" ) ).isEqualTo( "John Doe" );
	}

	@DisplayName( "Can get document preview with truncation" )
	@Test
	public void testDocumentPreview() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.Document;

				doc = new Document( content="This is a very long document that should be truncated when we create a preview of it." );
				result = {
					fullPreview: doc.preview( 100 ),
					shortPreview: doc.preview( 20 )
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct	result			= ( IStruct ) variables.get( "result" );
		String	shortPreview	= ( String ) result.get( "shortPreview" );
		assertThat( shortPreview ).contains( "..." );
		assertThat( shortPreview.length() ).isAtMost( 23 ); // 20 chars + "..."
	}

	@DisplayName( "Can estimate token count" )
	@Test
	public void testTokenCount() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.Document;

				doc = new Document( content="This is a test document with some content." );
				result = {
					tokenCount: doc.getTokenCount(),
					exceeds100: doc.exceedsTokenLimit( 100 ),
					exceeds5: doc.exceedsTokenLimit( 5 )
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result.get( "tokenCount" ) ).isInstanceOf( Number.class );
		assertThat( result.get( "exceeds100" ) ).isEqualTo( false );
		assertThat( result.get( "exceeds5" ) ).isEqualTo( true );
	}

	@DisplayName( "Can chunk a document into smaller pieces" )
	@Test
	public void testDocumentChunking() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.Document;

				longText = "This is sentence one. This is sentence two. This is sentence three. This is sentence four. ";
				longText &= "This is sentence five. This is sentence six. This is sentence seven. This is sentence eight.";

				doc = new Document(
					id="parent-doc",
					content=longText,
					metadata={ source: "test.txt" }
				);

				chunks = doc.chunk( chunkSize=50, overlap=10, strategy="characters" );

				result = {
					chunkCount: chunks.len(),
					firstChunk: chunks[1].getContent(),
					firstChunkMetadata: chunks[1].getMetadata(),
					hasParentId: chunks[1].getMeta( "parentId" ) == "parent-doc"
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result.get( "chunkCount" ) ).isInstanceOf( Number.class );
		assertThat( ( ( Number ) result.get( "chunkCount" ) ).intValue() ).isGreaterThan( 1 );
		assertThat( result.get( "firstChunk" ) ).isNotNull();
		assertThat( result.get( "hasParentId" ) ).isEqualTo( true );

		IStruct metadata = ( IStruct ) result.get( "firstChunkMetadata" );
		assertThat( metadata.get( "isChunk" ) ).isEqualTo( true );
		assertThat( metadata.get( "chunkIndex" ) ).isEqualTo( 1 );
	}

	@DisplayName( "Can validate document requirements" )
	@Test
	public void testDocumentValidation() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.Document;

				validDoc = new Document(
					content="This is valid content",
					metadata={ source: "test.txt", author: "John" }
				);

				invalidDoc = new Document( content="" );

				result = {
					validResult: validDoc.validate(
						minLength=10,
						maxLength=100,
						requiredMetadata=["source", "author"]
					),
					invalidResult: invalidDoc.validate( minLength=10 )
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct	result			= ( IStruct ) variables.get( "result" );
		IStruct	validResult		= ( IStruct ) result.get( "validResult" );
		IStruct	invalidResult	= ( IStruct ) result.get( "invalidResult" );

		assertThat( validResult.get( "valid" ) ).isEqualTo( true );
		assertThat( ( ( Array ) validResult.get( "errors" ) ).size() ).isEqualTo( 0 );

		assertThat( invalidResult.get( "valid" ) ).isEqualTo( false );
		assertThat( ( ( Array ) invalidResult.get( "errors" ) ).size() ).isGreaterThan( 0 );
	}

	@DisplayName( "Can hash and fingerprint documents" )
	@Test
	public void testDocumentHashing() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.Document;

				doc1 = new Document( content="Same content", metadata={ source: "test1.txt" } );
				doc2 = new Document( content="Same content", metadata={ source: "test2.txt" } );
				doc3 = new Document( content="Different content", metadata={ source: "test1.txt" } );

				result = {
					hash1: doc1.hash(),
					hash2: doc2.hash(),
					hash3: doc3.hash(),
					fingerprint1: doc1.fingerprint(),
					fingerprint2: doc2.fingerprint(),
					doc1EqualsDoc2: doc1.equals( doc2 ),
					doc1EqualsDoc3: doc1.equals( doc3 )
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );

		// Same content = same hash
		assertThat( result.get( "hash1" ) ).isEqualTo( result.get( "hash2" ) );
		// Different content = different hash
		assertThat( result.get( "hash1" ) ).isNotEqualTo( result.get( "hash3" ) );

		// Same content but different metadata = different fingerprints
		assertThat( result.get( "fingerprint1" ) ).isNotEqualTo( result.get( "fingerprint2" ) );

		// equals() checks content hash
		assertThat( result.get( "doc1EqualsDoc2" ) ).isEqualTo( true );
		assertThat( result.get( "doc1EqualsDoc3" ) ).isEqualTo( false );
	}

	@DisplayName( "Can manage document metadata" )
	@Test
	public void testMetadataManagement() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.Document;

				doc = new Document( content="Test", metadata={ source: "original.txt" } );

				// Set single meta value
				doc.setMeta( "author", "Jane Doe" );

				// Add multiple metadata values
				doc.addMetadata( { category: "test", tags: ["important", "review"] } );

				result = {
					author: doc.getMeta( "author" ),
					source: doc.getMeta( "source" ),
					category: doc.getMeta( "category" ),
					nonExistent: doc.getMeta( "nonExistent", "default-value" ),
					allMetadata: doc.getMetadata()
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result.get( "author" ) ).isEqualTo( "Jane Doe" );
		assertThat( result.get( "source" ) ).isEqualTo( "original.txt" );
		assertThat( result.get( "category" ) ).isEqualTo( "test" );
		assertThat( result.get( "nonExistent" ) ).isEqualTo( "default-value" );

		IStruct allMetadata = ( IStruct ) result.get( "allMetadata" );
		assertThat( allMetadata.size() ).isGreaterThan( 2 );
	}

	@DisplayName( "Can check for embeddings" )
	@Test
	public void testEmbeddings() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.Document;

				docWithoutEmbedding = new Document( content="Test" );
				docWithEmbedding = new Document(
					content="Test",
					embedding=[0.1, 0.2, 0.3, 0.4, 0.5]
				);

				result = {
					hasEmbedding1: docWithoutEmbedding.hasEmbedding(),
					hasEmbedding2: docWithEmbedding.hasEmbedding(),
					embeddingLength: docWithEmbedding.getEmbedding().len()
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result.get( "hasEmbedding1" ) ).isEqualTo( false );
		assertThat( result.get( "hasEmbedding2" ) ).isEqualTo( true );
		assertThat( result.get( "embeddingLength" ) ).isEqualTo( 5 );
	}

	@DisplayName( "Can serialize document to struct and JSON" )
	@Test
	public void testSerialization() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.Document;

				doc = new Document(
					id="test-123",
					content="Test content",
					metadata={ source: "test.txt" }
				);

				result = {
					asStruct: doc.toStruct(),
					asJson: doc.toJson(),
					asString: doc.toString()
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct	result		= ( IStruct ) variables.get( "result" );

		IStruct	asStruct	= ( IStruct ) result.get( "asStruct" );
		assertThat( asStruct.get( "id" ) ).isEqualTo( "test-123" );
		assertThat( asStruct.get( "content" ) ).isEqualTo( "Test content" );
		assertThat( asStruct.get( "hash" ) ).isNotNull();
		assertThat( asStruct.get( "fingerprint" ) ).isNotNull();

		String asJson = ( String ) result.get( "asJson" );
		assertThat( asJson ).contains( "test-123" );
		assertThat( asJson ).contains( "Test content" );

		String asString = ( String ) result.get( "asString" );
		assertThat( asString ).contains( "Document[" );
		assertThat( asString ).contains( "test-123" );
	}

	@DisplayName( "Can create document from struct and JSON" )
	@Test
	public void testDeserialization() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.Document;

				// From struct
				docStruct = {
					id: "doc-456",
					content: "Struct content",
					metadata: { source: "struct.txt" },
					embedding: [1, 2, 3]
				};
				docFromStruct = Document::fromStruct( docStruct );

				// From JSON
				docJson = '{"id":"doc-789","content":"JSON content","metadata":{"source":"json.txt"},"embedding":[4,5,6]}';
				docFromJson = Document::fromJson( docJson );

				result = {
					fromStructId: docFromStruct.getId(),
					fromStructContent: docFromStruct.getContent(),
					fromStructEmbedding: docFromStruct.getEmbedding().len(),
					fromJsonId: docFromJson.getId(),
					fromJsonContent: docFromJson.getContent()
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result.get( "fromStructId" ) ).isEqualTo( "doc-456" );
		assertThat( result.get( "fromStructContent" ) ).isEqualTo( "Struct content" );
		assertThat( result.get( "fromStructEmbedding" ) ).isEqualTo( 3 );
		assertThat( result.get( "fromJsonId" ) ).isEqualTo( "doc-789" );
		assertThat( result.get( "fromJsonContent" ) ).isEqualTo( "JSON content" );
	}

	@DisplayName( "Can clone a document" )
	@Test
	public void testDocumentCloning() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.Document;

				original = new Document(
					id="original-id",
					content="Original content",
					metadata={ source: "original.txt" }
				);

				cloned = original.clone();

				// Modify cloned metadata to ensure deep copy
				cloned.setMeta( "modified", true );

				result = {
					originalId: original.getId(),
					clonedId: cloned.getId(),
					sameContent: original.getContent() == cloned.getContent(),
					originalHasModified: original.getMeta( "modified", false ),
					clonedHasModified: cloned.getMeta( "modified", false )
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result.get( "originalId" ) ).isEqualTo( result.get( "clonedId" ) );
		assertThat( result.get( "sameContent" ) ).isEqualTo( true );
		// Cloned metadata should be independent
		assertThat( result.get( "originalHasModified" ) ).isEqualTo( false );
		assertThat( result.get( "clonedHasModified" ) ).isEqualTo( true );
	}

	@DisplayName( "Can merge multiple documents" )
	@Test
	public void testDocumentMerging() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.Document;

				doc1 = new Document( content="First document.", metadata={ source: "doc1.txt", type: "text" } );
				doc2 = new Document( content="Second document.", metadata={ source: "doc2.txt", author: "Jane" } );
				doc3 = new Document( content="Third document.", metadata={ source: "doc3.txt" } );

				// Merge single document
				merged1 = doc1.clone();
				merged1.merge( doc2 );

				// Merge array of documents
				merged2 = doc1.clone();
				merged2.merge( documents=[doc2, doc3], separator=" " );

				result = {
					merged1Content: merged1.getContent(),
					merged1SourceCount: merged1.getMetadata().keyArray().len(),
					merged2Content: merged2.getContent(),
					merged2HasAuthor: merged2.getMeta( "author", "" ) == "Jane"
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct	result			= ( IStruct ) variables.get( "result" );

		String	merged1Content	= ( String ) result.get( "merged1Content" );
		assertThat( merged1Content ).contains( "First document" );
		assertThat( merged1Content ).contains( "Second document" );

		String merged2Content = ( String ) result.get( "merged2Content" );
		assertThat( merged2Content ).contains( "First document" );
		assertThat( merged2Content ).contains( "Second document" );
		assertThat( merged2Content ).contains( "Third document" );

		assertThat( result.get( "merged2HasAuthor" ) ).isEqualTo( true );
	}

	@DisplayName( "Can use fluent chaining with metadata methods" )
	@Test
	public void testFluentChaining() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.Document;

				doc = new Document( content="Test" )
					.setMeta( "author", "John" )
					.setMeta( "category", "test" )
					.addMetadata( { tags: ["important"], version: 1 } );

				result = {
					author: doc.getMeta( "author" ),
					category: doc.getMeta( "category" ),
					tags: doc.getMeta( "tags" ),
					version: doc.getMeta( "version" )
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result.get( "author" ) ).isEqualTo( "John" );
		assertThat( result.get( "category" ) ).isEqualTo( "test" );
		assertThat( result.get( "version" ) ).isEqualTo( 1 );
	}

}
