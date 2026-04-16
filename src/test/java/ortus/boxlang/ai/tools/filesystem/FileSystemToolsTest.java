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
package ortus.boxlang.ai.tools.filesystem;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class FileSystemToolsTest extends BaseIntegrationTest {

	/**
	 * Base sandbox directory inside the project's build folder.
	 * Using a project-local path avoids OS-level symlinks (e.g. macOS /var → /private/var)
	 * that would cause guardPath canonical-path mismatches for not-yet-existing sub-paths.
	 */
	private static final Path	SANDBOX_BASE	= Path.of( "build/test-sandbox" ).toAbsolutePath();

	/** Temporary sandbox directory; created fresh before each test, deleted after. */
	private Path				tmpDir;

	/** BoxLang-escaped string form of tmpDir for use in inline source strings. */
	private String				bxTmpDir;

	@BeforeEach
	public void setupTmpDir() throws IOException {
		Files.createDirectories( SANDBOX_BASE );
		tmpDir		= Files.createTempDirectory( SANDBOX_BASE, "test-" );
		bxTmpDir	= tmpDir.toAbsolutePath().toString().replace( "\\", "/" );
	}

	@AfterEach
	public void cleanupTmpDir() throws IOException {
		// Walk and delete in reverse order (files before directories)
		if ( Files.exists( tmpDir ) ) {
			Files.walk( tmpDir )
			    .sorted( Comparator.reverseOrder() )
			    .forEach( p -> {
				    try {
					    Files.delete( p );
				    } catch ( IOException ignored ) {
				    }
			    } );
		}
	}

	// =========================================================================
	// Registration
	// =========================================================================

	@DisplayName( "scanClass() registers all FileSystemTools tool keys in the registry" )
	@Test
	public void testScanClassRegistersAllTools() {
		// @formatter:off
		runtime.executeSource(
			String.format(
				"""
					import bxModules.bxai.models.tools.filesystem.FileSystemTools;
					aiToolRegistry().scanClass(
					    new FileSystemTools( allowedPaths: [ "%s" ] ),
					    "fstest"
					)
					result = aiToolRegistry().has( "readFile@fstest" )
					    && aiToolRegistry().has( "readMultipleFiles@fstest" )
					    && aiToolRegistry().has( "writeFile@fstest" )
					    && aiToolRegistry().has( "appendFile@fstest" )
					    && aiToolRegistry().has( "editFile@fstest" )
					    && aiToolRegistry().has( "fileMetadata@fstest" )
					    && aiToolRegistry().has( "pathExists@fstest" )
					    && aiToolRegistry().has( "deleteFile@fstest" )
					    && aiToolRegistry().has( "moveFile@fstest" )
					    && aiToolRegistry().has( "copyFile@fstest" )
					    && aiToolRegistry().has( "searchFiles@fstest" )
					    && aiToolRegistry().has( "listAllowedDirectories@fstest" )
					    && aiToolRegistry().has( "listDirectory@fstest" )
					    && aiToolRegistry().has( "directoryTree@fstest" )
					    && aiToolRegistry().has( "createDirectory@fstest" )
					    && aiToolRegistry().has( "deleteDirectory@fstest" )
					    && aiToolRegistry().has( "zipFiles@fstest" )
					    && aiToolRegistry().has( "unzipFile@fstest" )
					    && aiToolRegistry().has( "checkZipFile@fstest" )
					// Cleanup
					aiToolRegistry().unregisterByModule( "fstest" )
				""",
				bxTmpDir
			),
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	// =========================================================================
	// Path guard / security
	// =========================================================================

	@DisplayName( "guardPath() blocks access outside allowedPaths" )
	@Test
	public void testGuardPathBlocksOutsidePaths() {
		// @formatter:off
		assertThrows( BoxRuntimeException.class, () ->
			runtime.executeSource(
				String.format(
					"""
						import bxModules.bxai.models.tools.filesystem.FileSystemTools;
						tools = new FileSystemTools( allowedPaths: [ "%s" ] );
						result = tools.readFile( "/etc/passwd" )
					""",
					bxTmpDir
				),
				context
			)
		);
		// @formatter:on
	}

	@DisplayName( "guardPath() allows access when allowedPaths is empty" )
	@Test
	public void testGuardPathAllowsWhenNoRestriction() throws IOException {
		Path testFile = tmpDir.resolve( "open.txt" );
		Files.writeString( testFile, "open access" );
		// @formatter:off
		runtime.executeSource(
			String.format(
				"""
					import bxModules.bxai.models.tools.filesystem.FileSystemTools;
					tools  = new FileSystemTools(); // no allowedPaths restriction
					result = tools.readFile( "%s" )
				""",
				testFile.toAbsolutePath().toString().replace( "\\", "/" )
			),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( result ) ).isEqualTo( "open access" );
	}

	// =========================================================================
	// readFile
	// =========================================================================

	@DisplayName( "readFile() returns the full text content of a file" )
	@Test
	public void testReadFile() throws IOException {
		Path testFile = tmpDir.resolve( "hello.txt" );
		Files.writeString( testFile, "Hello, BoxLang!" );
		// @formatter:off
		runtime.executeSource(
			String.format(
				"""
					import bxModules.bxai.models.tools.filesystem.FileSystemTools;
					tools  = new FileSystemTools( allowedPaths: [ "%s" ] );
					result = tools.readFile( "%s" )
				""",
				bxTmpDir,
				testFile.toAbsolutePath().toString().replace( "\\", "/" )
			),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( result ) ).isEqualTo( "Hello, BoxLang!" );
	}

	@DisplayName( "readFile() throws NotFound for a missing file" )
	@Test
	public void testReadFileMissingThrows() {
		// @formatter:off
		assertThrows( BoxRuntimeException.class, () ->
			runtime.executeSource(
				String.format(
					"""
						import bxModules.bxai.models.tools.filesystem.FileSystemTools;
						tools  = new FileSystemTools( allowedPaths: [ "%s" ] );
						result = tools.readFile( "%s/no-such-file.txt" )
					""",
					bxTmpDir,
					bxTmpDir
				),
				context
			)
		);
		// @formatter:on
	}

	// =========================================================================
	// readMultipleFiles
	// =========================================================================

	@DisplayName( "readMultipleFiles() returns a struct mapping path to content" )
	@Test
	public void testReadMultipleFiles() throws IOException {
		Path	fileA	= tmpDir.resolve( "a.txt" );
		Path	fileB	= tmpDir.resolve( "b.txt" );
		Files.writeString( fileA, "alpha" );
		Files.writeString( fileB, "beta" );
		// @formatter:off
		runtime.executeSource(
			String.format(
				"""
					import bxModules.bxai.models.tools.filesystem.FileSystemTools;
					tools   = new FileSystemTools( allowedPaths: [ "%s" ] );
					parsed  = tools.readMultipleFiles( jsonSerialize( [ "%s", "%s" ] ) )
					resultA = parsed[ "%s" ]
					resultB = parsed[ "%s" ]
					result  = ( resultA == "alpha" && resultB == "beta" )
				""",
				bxTmpDir,
				fileA.toAbsolutePath().toString().replace( "\\", "/" ),
				fileB.toAbsolutePath().toString().replace( "\\", "/" ),
				fileA.toAbsolutePath().toString().replace( "\\", "/" ),
				fileB.toAbsolutePath().toString().replace( "\\", "/" )
			),
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "readMultipleFiles() maps inaccessible paths to an error string" )
	@Test
	public void testReadMultipleFilesErrorEntry() throws IOException {
		Path fileA = tmpDir.resolve( "good.txt" );
		Files.writeString( fileA, "ok" );
		// @formatter:off
		runtime.executeSource(
			String.format(
				"""
					import bxModules.bxai.models.tools.filesystem.FileSystemTools;
					tools      = new FileSystemTools( allowedPaths: [ "%s" ] );
					parsed     = tools.readMultipleFiles( jsonSerialize( [ "%s", "/etc/passwd" ] ) )
					errorEntry = parsed[ "/etc/passwd" ]
					result     = errorEntry.startsWith( "ERROR:" )
				""",
				bxTmpDir,
				fileA.toAbsolutePath().toString().replace( "\\", "/" )
			),
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	// =========================================================================
	// writeFile / appendFile
	// =========================================================================

	@DisplayName( "writeFile() creates a new file with the given content" )
	@Test
	public void testWriteFile() {
		String targetPath = bxTmpDir + "/write-test.txt";
		// @formatter:off
		runtime.executeSource(
			String.format(
				"""
					import bxModules.bxai.models.tools.filesystem.FileSystemTools;
					tools  = new FileSystemTools( allowedPaths: [ "%s" ] );
					result = tools.writeFile( "%s", "written content" )
				""",
				bxTmpDir,
				targetPath
			),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( result ) ).startsWith( "OK:" );
		assertThat( tmpDir.resolve( "write-test.txt" ).toFile().exists() ).isTrue();
	}

	@DisplayName( "appendFile() appends content to an existing file" )
	@Test
	public void testAppendFile() throws IOException {
		Path testFile = tmpDir.resolve( "append-test.txt" );
		Files.writeString( testFile, "line1\n" );
		// @formatter:off
		runtime.executeSource(
			String.format(
				"""
					import bxModules.bxai.models.tools.filesystem.FileSystemTools;
					tools  = new FileSystemTools( allowedPaths: [ "%s" ] );
					result = tools.appendFile( "%s", "line2" )
				""",
				bxTmpDir,
				testFile.toAbsolutePath().toString().replace( "\\", "/" )
			),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( result ) ).startsWith( "OK:" );
		String content = Files.readString( testFile );
		assertThat( content ).contains( "line1" );
		assertThat( content ).contains( "line2" );
	}

	// =========================================================================
	// editFile
	// =========================================================================

	@DisplayName( "editFile() replaces all occurrences of oldText with newText" )
	@Test
	public void testEditFile() throws IOException {
		Path testFile = tmpDir.resolve( "edit-test.txt" );
		Files.writeString( testFile, "foo bar foo baz foo" );
		// @formatter:off
		runtime.executeSource(
			String.format(
				"""
					import bxModules.bxai.models.tools.filesystem.FileSystemTools;
					tools  = new FileSystemTools( allowedPaths: [ "%s" ] );
					result = tools.editFile( "%s", "foo", "qux" )
				""",
				bxTmpDir,
				testFile.toAbsolutePath().toString().replace( "\\", "/" )
			),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( result ) ).contains( "3" ); // 3 replacements
		assertThat( Files.readString( testFile ) ).isEqualTo( "qux bar qux baz qux" );
	}

	// =========================================================================
	// fileMetadata / pathExists
	// =========================================================================

	@DisplayName( "fileMetadata() returns a struct with size and mimeType fields" )
	@Test
	public void testFileMetadata() throws IOException {
		Path testFile = tmpDir.resolve( "meta.txt" );
		Files.writeString( testFile, "some content" );
		// @formatter:off
		runtime.executeSource(
			String.format(
				"""
					import bxModules.bxai.models.tools.filesystem.FileSystemTools;
					tools  = new FileSystemTools( allowedPaths: [ "%s" ] );
					meta   = tools.fileMetadata( "%s" )
					result = ( structKeyExists( meta, "size" ) && structKeyExists( meta, "mimeType" ) )
				""",
				bxTmpDir,
				testFile.toAbsolutePath().toString().replace( "\\", "/" )
			),
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "pathExists() returns true for an existing file" )
	@Test
	public void testPathExistsTrue() throws IOException {
		Path testFile = tmpDir.resolve( "exists.txt" );
		Files.writeString( testFile, "x" );
		// @formatter:off
		runtime.executeSource(
			String.format(
				"""
					import bxModules.bxai.models.tools.filesystem.FileSystemTools;
					tools  = new FileSystemTools( allowedPaths: [ "%s" ] );
					result = tools.pathExists( "%s" )
				""",
				bxTmpDir,
				testFile.toAbsolutePath().toString().replace( "\\", "/" )
			),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( result ) ).isEqualTo( "true" );
	}

	@DisplayName( "pathExists() returns false for a missing path" )
	@Test
	public void testPathExistsFalse() {
		// @formatter:off
		runtime.executeSource(
			String.format(
				"""
					import bxModules.bxai.models.tools.filesystem.FileSystemTools;
					tools  = new FileSystemTools( allowedPaths: [ "%s" ] );
					result = tools.pathExists( "%s/does-not-exist.txt" )
				""",
				bxTmpDir,
				bxTmpDir
			),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( result ) ).isEqualTo( "false" );
	}

	// =========================================================================
	// deleteFile / moveFile / copyFile
	// =========================================================================

	@DisplayName( "deleteFile() removes the file and returns a confirmation" )
	@Test
	public void testDeleteFile() throws IOException {
		Path testFile = tmpDir.resolve( "to-delete.txt" );
		Files.writeString( testFile, "bye" );
		// @formatter:off
		runtime.executeSource(
			String.format(
				"""
					import bxModules.bxai.models.tools.filesystem.FileSystemTools;
					tools  = new FileSystemTools( allowedPaths: [ "%s" ] );
					result = tools.deleteFile( "%s" )
				""",
				bxTmpDir,
				testFile.toAbsolutePath().toString().replace( "\\", "/" )
			),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( result ) ).startsWith( "OK:" );
		assertThat( testFile.toFile().exists() ).isFalse();
	}

	@DisplayName( "moveFile() moves the file to the destination" )
	@Test
	public void testMoveFile() throws IOException {
		Path	source	= tmpDir.resolve( "source.txt" );
		Path	dest	= tmpDir.resolve( "moved.txt" );
		Files.writeString( source, "move me" );
		// @formatter:off
		runtime.executeSource(
			String.format(
				"""
					import bxModules.bxai.models.tools.filesystem.FileSystemTools;
					tools  = new FileSystemTools( allowedPaths: [ "%s" ] );
					result = tools.moveFile( "%s", "%s" )
				""",
				bxTmpDir,
				source.toAbsolutePath().toString().replace( "\\", "/" ),
				dest.toAbsolutePath().toString().replace( "\\", "/" )
			),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( result ) ).startsWith( "OK:" );
		assertThat( source.toFile().exists() ).isFalse();
		assertThat( dest.toFile().exists() ).isTrue();
	}

	@DisplayName( "copyFile() copies the file to the destination, leaving the source intact" )
	@Test
	public void testCopyFile() throws IOException {
		Path	source	= tmpDir.resolve( "original.txt" );
		Path	dest	= tmpDir.resolve( "copy.txt" );
		Files.writeString( source, "copy me" );
		// @formatter:off
		runtime.executeSource(
			String.format(
				"""
					import bxModules.bxai.models.tools.filesystem.FileSystemTools;
					tools  = new FileSystemTools( allowedPaths: [ "%s" ] );
					result = tools.copyFile( "%s", "%s" )
				""",
				bxTmpDir,
				source.toAbsolutePath().toString().replace( "\\", "/" ),
				dest.toAbsolutePath().toString().replace( "\\", "/" )
			),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( result ) ).startsWith( "OK:" );
		assertThat( source.toFile().exists() ).isTrue();
		assertThat( Files.readString( dest ) ).isEqualTo( "copy me" );
	}

	// =========================================================================
	// searchFiles
	// =========================================================================

	@DisplayName( "searchFiles() returns matches with file, line, and content fields" )
	@Test
	public void testSearchFiles() throws IOException {
		Path	fileA	= tmpDir.resolve( "search-a.txt" );
		Path	fileB	= tmpDir.resolve( "search-b.txt" );
		Files.writeString( fileA, "the quick brown fox\njumps over the lazy dog\n" );
		Files.writeString( fileB, "no match here\n" );
		// @formatter:off
		runtime.executeSource(
			String.format(
				"""
					import bxModules.bxai.models.tools.filesystem.FileSystemTools;
					tools   = new FileSystemTools( allowedPaths: [ "%s" ] );
					matches = tools.searchFiles( "%s", "fox" )
					result  = ( matches.len() == 1 && matches[1].content.findNoCase( "fox" ) > 0 )
				""",
				bxTmpDir,
				bxTmpDir
			),
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	// =========================================================================
	// listAllowedDirectories
	// =========================================================================

	@DisplayName( "listAllowedDirectories() returns the configured allowed paths" )
	@Test
	public void testListAllowedDirectories() {
		// @formatter:off
		runtime.executeSource(
			String.format(
				"""
					import bxModules.bxai.models.tools.filesystem.FileSystemTools;
					tools  = new FileSystemTools( allowedPaths: [ "%s" ] );
					dirs   = tools.listAllowedDirectories()
					result = ( dirs.len() == 1 )
				""",
				bxTmpDir
			),
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "listAllowedDirectories() returns an empty array when no restrictions set" )
	@Test
	public void testListAllowedDirectoriesEmpty() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.filesystem.FileSystemTools;
				tools  = new FileSystemTools();
				dirs   = tools.listAllowedDirectories()
				result = ( dirs.len() == 0 )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	// =========================================================================
	// listDirectory / directoryTree
	// =========================================================================

	@DisplayName( "listDirectory() returns an array with name and type fields" )
	@Test
	public void testListDirectory() throws IOException {
		Files.writeString( tmpDir.resolve( "f1.txt" ), "a" );
		Files.writeString( tmpDir.resolve( "f2.txt" ), "b" );
		Files.createDirectory( tmpDir.resolve( "subdir" ) );
		// @formatter:off
		runtime.executeSource(
			String.format(
				"""
					import bxModules.bxai.models.tools.filesystem.FileSystemTools;
					tools   = new FileSystemTools( allowedPaths: [ "%s" ] );
					entries = tools.listDirectory( "%s" )
					result  = ( entries.len() == 3 )
				""",
				bxTmpDir,
				bxTmpDir
			),
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "directoryTree() returns a nested struct tree with children" )
	@Test
	public void testDirectoryTree() throws IOException {
		Path subDir = Files.createDirectory( tmpDir.resolve( "sub" ) );
		Files.writeString( subDir.resolve( "child.txt" ), "child" );
		// @formatter:off
		runtime.executeSource(
			String.format(
				"""
					import bxModules.bxai.models.tools.filesystem.FileSystemTools;
					tools  = new FileSystemTools( allowedPaths: [ "%s" ] );
					tree   = tools.directoryTree( "%s" )
					result = ( tree.type == "dir" && tree.children.len() > 0 )
				""",
				bxTmpDir,
				bxTmpDir
			),
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	// =========================================================================
	// createDirectory / deleteDirectory
	// =========================================================================

	@DisplayName( "createDirectory() creates the directory and its parents" )
	@Test
	public void testCreateDirectory() {
		String newDir = bxTmpDir + "/nested/deep/dir";
		// @formatter:off
		runtime.executeSource(
			String.format(
				"""
					import bxModules.bxai.models.tools.filesystem.FileSystemTools;
					tools  = new FileSystemTools( allowedPaths: [ "%s" ] );
					result = tools.createDirectory( "%s" )
				""",
				bxTmpDir,
				newDir
			),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( result ) ).startsWith( "OK:" );
		assertThat( tmpDir.resolve( "nested/deep/dir" ).toFile().isDirectory() ).isTrue();
	}

	@DisplayName( "deleteDirectory() removes the directory tree and returns a confirmation" )
	@Test
	public void testDeleteDirectory() throws IOException {
		Path subDir = Files.createDirectory( tmpDir.resolve( "to-remove" ) );
		Files.writeString( subDir.resolve( "junk.txt" ), "junk" );
		// @formatter:off
		runtime.executeSource(
			String.format(
				"""
					import bxModules.bxai.models.tools.filesystem.FileSystemTools;
					tools  = new FileSystemTools( allowedPaths: [ "%s" ] );
					result = tools.deleteDirectory( "%s" )
				""",
				bxTmpDir,
				subDir.toAbsolutePath().toString().replace( "\\", "/" )
			),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( result ) ).startsWith( "OK:" );
		assertThat( subDir.toFile().exists() ).isFalse();
	}

	// =========================================================================
	// zipFiles / unzipFile / checkZipFile
	// =========================================================================

	@DisplayName( "zipFiles() creates a zip archive from a directory" )
	@Test
	public void testZipFiles() throws IOException {
		Path srcDir = Files.createDirectory( tmpDir.resolve( "to-zip" ) );
		Files.writeString( srcDir.resolve( "doc.txt" ), "zipped content" );
		String zipPath = bxTmpDir + "/archive.zip";
		// @formatter:off
		runtime.executeSource(
			String.format(
				"""
					import bxModules.bxai.models.tools.filesystem.FileSystemTools;
					tools  = new FileSystemTools( allowedPaths: [ "%s" ] );
					result = tools.zipFiles( "%s", "%s" )
				""",
				bxTmpDir,
				srcDir.toAbsolutePath().toString().replace( "\\", "/" ),
				zipPath
			),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( result ) ).startsWith( "OK:" );
		assertThat( tmpDir.resolve( "archive.zip" ).toFile().exists() ).isTrue();
	}

	@DisplayName( "checkZipFile() returns true for a valid zip file" )
	@Test
	public void testCheckZipFileTrue() throws IOException {
		// Create a real zip using the tool first, then verify
		Path srcDir = Files.createDirectory( tmpDir.resolve( "zip-check-src" ) );
		Files.writeString( srcDir.resolve( "file.txt" ), "content" );
		String zipPath = bxTmpDir + "/check-archive.zip";
		// @formatter:off
		runtime.executeSource(
			String.format(
				"""
					import bxModules.bxai.models.tools.filesystem.FileSystemTools;
					tools   = new FileSystemTools( allowedPaths: [ "%s" ] );
					tools.zipFiles( "%s", "%s" )
					result = tools.checkZipFile( "%s" )
				""",
				bxTmpDir,
				srcDir.toAbsolutePath().toString().replace( "\\", "/" ),
				zipPath,
				zipPath
			),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( result ) ).isEqualTo( "true" );
	}

	@DisplayName( "checkZipFile() returns false for a plain text file" )
	@Test
	public void testCheckZipFileFalse() throws IOException {
		Path textFile = tmpDir.resolve( "not-a-zip.txt" );
		Files.writeString( textFile, "just text" );
		// @formatter:off
		runtime.executeSource(
			String.format(
				"""
					import bxModules.bxai.models.tools.filesystem.FileSystemTools;
					tools  = new FileSystemTools( allowedPaths: [ "%s" ] );
					result = tools.checkZipFile( "%s" )
				""",
				bxTmpDir,
				textFile.toAbsolutePath().toString().replace( "\\", "/" )
			),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( result ) ).isEqualTo( "false" );
	}

	@DisplayName( "unzipFile() extracts a zip archive to the destination directory" )
	@Test
	public void testUnzipFile() throws IOException {
		// Zip something first
		Path srcDir = Files.createDirectory( tmpDir.resolve( "unzip-src" ) );
		Files.writeString( srcDir.resolve( "inner.txt" ), "extracted" );
		String	zipPath		= bxTmpDir + "/unzip-archive.zip";
		String	extractPath	= bxTmpDir + "/unzip-out";
		// @formatter:off
		runtime.executeSource(
			String.format(
				"""
					import bxModules.bxai.models.tools.filesystem.FileSystemTools;
					tools = new FileSystemTools( allowedPaths: [ "%s" ] );
					tools.zipFiles( "%s", "%s" )
					result = tools.unzipFile( "%s", "%s" )
				""",
				bxTmpDir,
				srcDir.toAbsolutePath().toString().replace( "\\", "/" ),
				zipPath,
				zipPath,
				extractPath
			),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( result ) ).startsWith( "OK:" );
		assertThat( tmpDir.resolve( "unzip-out" ).toFile().isDirectory() ).isTrue();
	}

}
