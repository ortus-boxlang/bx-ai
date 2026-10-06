/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.ai.util;

import static com.google.common.truth.Truth.assertThat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class AudioUtilTest {

	@DisplayName( "pcmToWav writes a canonical RIFF/WAVE header followed by the PCM data" )
	@Test
	public void testPcmToWav() {
		byte[]		pcm		= new byte[] { 1, 2, 3, 4, 5, 6 };
		byte[]		wav		= AudioUtil.pcmToWav( pcm, 24000, 1, 16 );
		ByteBuffer	buffer	= ByteBuffer.wrap( wav ).order( ByteOrder.LITTLE_ENDIAN );

		assertThat( wav.length ).isEqualTo( 44 + pcm.length );
		assertThat( new String( wav, 0, 4, StandardCharsets.US_ASCII ) ).isEqualTo( "RIFF" );
		assertThat( buffer.getInt( 4 ) ).isEqualTo( 36 + pcm.length );
		assertThat( new String( wav, 8, 4, StandardCharsets.US_ASCII ) ).isEqualTo( "WAVE" );
		assertThat( new String( wav, 12, 4, StandardCharsets.US_ASCII ) ).isEqualTo( "fmt " );
		assertThat( buffer.getShort( 20 ) ).isEqualTo( ( short ) 1 );
		assertThat( buffer.getShort( 22 ) ).isEqualTo( ( short ) 1 );
		assertThat( buffer.getInt( 24 ) ).isEqualTo( 24000 );
		assertThat( buffer.getInt( 28 ) ).isEqualTo( 48000 );
		assertThat( buffer.getShort( 32 ) ).isEqualTo( ( short ) 2 );
		assertThat( buffer.getShort( 34 ) ).isEqualTo( ( short ) 16 );
		assertThat( new String( wav, 36, 4, StandardCharsets.US_ASCII ) ).isEqualTo( "data" );
		assertThat( buffer.getInt( 40 ) ).isEqualTo( pcm.length );
		assertThat( wav[ 44 ] ).isEqualTo( ( byte ) 1 );
		assertThat( wav[ 49 ] ).isEqualTo( ( byte ) 6 );
	}

	@DisplayName( "sampleRateFromMime reads the rate or falls back" )
	@Test
	public void testSampleRateFromMime() {
		assertThat( AudioUtil.sampleRateFromMime( "audio/L16;codec=pcm;rate=24000", 16000 ) ).isEqualTo( 24000 );
		assertThat( AudioUtil.sampleRateFromMime( "audio/wav", 16000 ) ).isEqualTo( 16000 );
		assertThat( AudioUtil.sampleRateFromMime( null, 16000 ) ).isEqualTo( 16000 );
	}
}
