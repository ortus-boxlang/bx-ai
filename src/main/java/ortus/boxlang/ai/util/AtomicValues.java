package ortus.boxlang.ai.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Factory for the counters and references held by MCP statistics.
 */
public final class AtomicValues {

	private AtomicValues() {
	}

	/**
	 * Create an independent signed 32-bit counter.
	 *
	 * @param initial Initial counter value
	 *
	 * @return A new atomic counter
	 */
	public static AtomicInteger integer( int initial ) {
		return new AtomicInteger( initial );
	}

	/**
	 * Store the supplied value by identity, without serializing it.
	 *
	 * @param initial Initial reference value, possibly null
	 *
	 * @return A new atomic reference
	 */
	public static AtomicReference<Object> reference( Object initial ) {
		return new AtomicReference<>( initial );
	}
}
