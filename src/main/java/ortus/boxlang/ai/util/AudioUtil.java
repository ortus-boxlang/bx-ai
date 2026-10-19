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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Small audio helpers shared by the speech providers.
 */
public class AudioUtil {

	private static final Pattern RATE_PATTERN = Pattern.compile( "rate=(\\d+)" );

	private AudioUtil() {
	}

	/**
	 * Wrap raw signed little-endian PCM in a canonical 44-byte RIFF/WAVE header so it is playable as a .wav file.
	 *
	 * @param pcm           Raw PCM sample data
	 * @param sampleRate    Samples per second (e.g. 24000)
	 * @param channels      Channel count (1 = mono)
	 * @param bitsPerSample Bits per sample (16 for pcm_s16le)
	 *
	 * @return The complete WAV file bytes
	 */
	public static byte[] pcmToWav( byte[] pcm, int sampleRate, int channels, int bitsPerSample ) {
		int			byteRate	= sampleRate * channels * bitsPerSample / 8;
		int			blockAlign	= channels * bitsPerSample / 8;
		ByteBuffer	buffer		= ByteBuffer.allocate( 44 + pcm.length ).order( ByteOrder.LITTLE_ENDIAN );

		buffer.put( "RIFF".getBytes( StandardCharsets.US_ASCII ) );
		buffer.putInt( 36 + pcm.length );
		buffer.put( "WAVE".getBytes( StandardCharsets.US_ASCII ) );
		buffer.put( "fmt ".getBytes( StandardCharsets.US_ASCII ) );
		buffer.putInt( 16 );
		buffer.putShort( ( short ) 1 );
		buffer.putShort( ( short ) channels );
		buffer.putInt( sampleRate );
		buffer.putInt( byteRate );
		buffer.putShort( ( short ) blockAlign );
		buffer.putShort( ( short ) bitsPerSample );
		buffer.put( "data".getBytes( StandardCharsets.US_ASCII ) );
		buffer.putInt( pcm.length );
		buffer.put( pcm );

		return buffer.array();
	}

	/**
	 * Extract the sample rate from a mime type such as `audio/L16;codec=pcm;rate=24000`.
	 *
	 * @param mimeType The mime type string
	 * @param fallback Value returned when no rate is present
	 *
	 * @return The sample rate in Hz
	 */
	public static int sampleRateFromMime( String mimeType, int fallback ) {
		if ( mimeType == null ) {
			return fallback;
		}
		Matcher matcher = RATE_PATTERN.matcher( mimeType );
		return matcher.find() ? Integer.parseInt( matcher.group( 1 ) ) : fallback;
	}

}
