package ortus.boxlang.ai.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Platform boundary for the atomic values held by MCP statistics. */
public final class AtomicValues {

	private AtomicValues() {
	}

	/** Create an independent signed 32-bit counter. */
	public static AtomicInteger integer( int initial ) {
		return new AtomicInteger( initial );
	}

	/** Preserve the actual BoxLang value, not a serialized representation. */
	public static AtomicReference<Object> reference( Object initial ) {
		return new AtomicReference<>( initial );
	}
}
