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
package ortus.boxlang.ai.mcp;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Several limits per server, turning the limit on and off, resetting counters and stats, and the per day history
 * with its retention, flush and client cap. Everything runs offline.
 */
public class mcpRateLimitHistoryTest extends BaseIntegrationTest {

	/**
	 * Shared BoxLang helpers: a fake clock, a request builder and a processor call
	 */
	private static final String HELPERS = """
	                                      import bxModules.bxai.models.mcp.MCPRequestProcessor;
	                                      import bxModules.bxai.models.mcp.ratelimit.RateLimitSet;
	                                      import bxModules.bxai.models.mcp.ratelimit.RateLimitHistory;
	                                      import bxModules.bxai.models.mcp.ratelimit.MemoryRateLimitStore;

	                                      clock = { now : 1700000000 };
	                                      /** A unique server name, the cache is shared between tests */
	                                      function uniq( p ) { return p & createUUID().left( 8 ); }
	                                      /** A request as the transport builds it */
	                                      function req( ip = "1.2.3.4", serverName = "rl" ) {
	                                      	return {
	                                      		serverName : serverName,
	                                      		method     : "POST",
	                                      		body       : '{"jsonrpc":"2.0","id":1,"method":"ping"}',
	                                      		headers    : {},
	                                      		acceptHeader: "application/json",
	                                      		params     : {},
	                                      		origin     : "",
	                                      		requestId  : createUUID(),
	                                      		metadata   : { remoteAddr : ip }
	                                      	};
	                                      }
	                                      /** Run a request through the HTTP transport */
	                                      function call( data ) {
	                                      	return MCPRequestProcessor::processWithTransport( MCPRequestProcessor::getHttpTransport(), data );
	                                      }
	                                      /** The numbers 1 to n as an array */
	                                      function nums( n ) {
	                                      	var a = [];
	                                      	for ( var i = 1; i <= n; i++ ) { a.append( i ); }
	                                      	return a;
	                                      }
	                                      /** The type of the error a function throws, "none" when it does not */
	                                      function errType( f ) {
	                                      	try {
	                                      		f();
	                                      		return "none";
	                                      	} catch ( any e ) {
	                                      		return e.type;
	                                      	}
	                                      }
	                                      /** A history on the fake clock */
	                                      function newHistory( name, retention = 7, max = 500 ) {
	                                      	return new RateLimitHistory( serverName: name, cacheName: "default", retentionDays: retention, flushSeconds: 30, maxClients: max, perMinute: 60, clock: () => clock.now );
	                                      }
	                                      """;

	/**
	 * Remove the servers a test created
	 */
	@AfterEach
	public void clearServers() {
		runtime.executeSource( "bxModules.bxai.models.mcp.MCPServer::clearAllInstances()", context );
	}

	/**
	 * Run a BoxLang script after the shared helpers
	 *
	 * @param script The BoxLang source
	 */
	private void run( String script ) {
		runtime.executeSource( HELPERS + script, context );
	}

	/**
	 * Read a script variable as an int
	 *
	 * @param name The variable name
	 *
	 * @return The value
	 */
	private int num( String name ) {
		return ( ( Number ) get( name ) ).intValue();
	}

	/**
	 * Read a script variable
	 *
	 * @param name The variable name
	 *
	 * @return The value
	 */
	private Object get( String name ) {
		return variables.get( Key.of( name ) );
	}

	// ------------------------------------------------------------------------------------------------
	// Several limits
	// ------------------------------------------------------------------------------------------------

	/**
	 * A client has to pass every limit, a refusal gives the request back to the others, and the wait is the longest
	 */
	@Test
	@DisplayName( "Two limits: every one has to pass, a refusal does not use up the other, the wait is the longest" )
	public void testTwoLimits() {
		run( """
		     set = new RateLimitSet( limits: [ { limit: 2, window: 60 }, { limit: 3, window: 3600 } ], store: new MemoryRateLimitStore(), clock: () => clock.now );
		     clock.now = 1700000000 - ( 1700000000 mod 3600 );
		     a = set.check( "c" ).allowed;
		     b = set.check( "c" ).allowed;
		     // Refused by the minute limit: the hour limit must not count it
		     c = set.check( "c" );
		     minuteRefused = !c.allowed;
		     minuteWindow = c.window;
		     // Half way into the next minute the first minute counts for half: the third request of the hour is fine
		     clock.now += 90;
		     d = set.check( "c" ).allowed;
		     // The hour limit is used up now, so the fourth request waits for the hour
		     e = set.check( "c" );
		     hourRefused = !e.allowed;
		     hourWindow = e.window;
		     longWait = e.retryAfter > 60;
		     limits = set.getLimits().map( l => l.window ).toList();
		     """ );
		assertThat( get( "a" ) ).isEqualTo( true );
		assertThat( get( "b" ) ).isEqualTo( true );
		assertThat( get( "minuteRefused" ) ).isEqualTo( true );
		assertThat( num( "minuteWindow" ) ).isEqualTo( 60 );
		assertThat( get( "d" ) ).isEqualTo( true );
		assertThat( get( "hourRefused" ) ).isEqualTo( true );
		assertThat( num( "hourWindow" ) ).isEqualTo( 3600 );
		assertThat( get( "longWait" ) ).isEqualTo( true );
		assertThat( get( "limits" ) ).isEqualTo( "60,3600" );
	}

	/**
	 * Two limits with the same window are an error, and a limit needs both numbers
	 */
	@Test
	@DisplayName( "Limits with the same window or missing numbers throw InvalidArgument" )
	public void testBadLimits() {
		run( """
		     same = errType( () => new RateLimitSet( limits: [ { limit: 1, window: 60 }, { limit: 5, window: 60 } ], store: new MemoryRateLimitStore() ) );
		     missing = errType( () => new RateLimitSet( limits: [ { limit: 1 } ], store: new MemoryRateLimitStore() ) );
		     empty = errType( () => new RateLimitSet( limits: [], store: new MemoryRateLimitStore() ) );
		     """ );
		assertThat( get( "same" ) ).isEqualTo( "InvalidArgument" );
		assertThat( get( "missing" ) ).isEqualTo( "InvalidArgument" );
		assertThat( get( "empty" ) ).isEqualTo( "InvalidArgument" );
	}

	/**
	 * Through the request processor, with the limits setting, the stats show every limit
	 */
	@Test
	@DisplayName( "withRateLimit( { limits } ) limits a server and getStats lists every limit" )
	public void testServerLimits() {
		run( """
		     name = uniq( "ml" );
		     srv = mcpServer( name ).withRateLimit( { limits: [ { limit: 2, window: 60 }, { limit: 100, window: 3600 } ], store: "memory" } );
		     codes = nums( 3 ).map( i => call( req( "1.1.1.1", name ) ).statusCode );
		     limitCount = srv.getStats().rateLimit.limits.len();
		     """ );
		var codes = ( ortus.boxlang.runtime.types.Array ) get( "codes" );
		assertThat( ( ( Number ) codes.get( 1 ) ).intValue() ).isEqualTo( 200 );
		assertThat( ( ( Number ) codes.get( 2 ) ).intValue() ).isEqualTo( 429 );
		assertThat( num( "limitCount" ) ).isEqualTo( 2 );
	}

	// ------------------------------------------------------------------------------------------------
	// On, off, reset
	// ------------------------------------------------------------------------------------------------

	/**
	 * Off keeps the configuration and checks nothing, on checks again
	 */
	@Test
	@DisplayName( "disableRateLimit() stops checking and keeps the setup, enableRateLimit() starts again" )
	public void testEnableDisable() {
		run( """
		     name = uniq( "oo" );
		     srv = mcpServer( name ).withRateLimit( { limit: 1, window: 60, store: "memory" } );
		     first = call( req( "2.2.2.2", name ) ).statusCode;
		     blocked = call( req( "2.2.2.2", name ) ).statusCode;
		     srv.disableRateLimit();
		     offFlags = [ srv.hasRateLimit(), srv.isRateLimitEnabled() ];
		     whileOff = nums( 5 ).map( i => call( req( "2.2.2.2", name ) ).statusCode ).filter( c => c == 200 ).len();
		     srv.enableRateLimit();
		     onFlags = [ srv.hasRateLimit(), srv.isRateLimitEnabled() ];
		     // The counter was kept: still blocked
		     again = call( req( "2.2.2.2", name ) ).statusCode;
		     enabledStat = srv.getStats().rateLimit.enabled;
		     // Remove it for good
		     srv.withRateLimit( false );
		     removed = [ srv.hasRateLimit(), srv.isRateLimitEnabled() ];
		     // enable with nothing configured uses the module defaults
		     fresh = mcpServer( uniq( "oo" ) );
		     fresh.enableRateLimit();
		     freshOn = fresh.isRateLimitEnabled();
		     """ );
		assertThat( num( "first" ) ).isEqualTo( 200 );
		assertThat( num( "blocked" ) ).isEqualTo( 429 );
		assertThat( num( "whileOff" ) ).isEqualTo( 5 );
		var off = ( ortus.boxlang.runtime.types.Array ) get( "offFlags" );
		assertThat( off.get( 0 ) ).isEqualTo( true );
		assertThat( off.get( 1 ) ).isEqualTo( false );
		var on = ( ortus.boxlang.runtime.types.Array ) get( "onFlags" );
		assertThat( on.get( 1 ) ).isEqualTo( true );
		assertThat( num( "again" ) ).isEqualTo( 429 );
		assertThat( get( "enabledStat" ) ).isEqualTo( true );
		var removed = ( ortus.boxlang.runtime.types.Array ) get( "removed" );
		assertThat( removed.get( 0 ) ).isEqualTo( false );
		assertThat( get( "freshOn" ) ).isEqualTo( true );
	}

	/**
	 * Reset gives every client its requests back, for the memory and the cache store
	 */
	@Test
	@DisplayName( "resetRateLimit() clears the counters of every client" )
	public void testResetCounters() {
		run( """
		     results = [];
		     for ( kind in [ "memory", "cache" ] ) {
		     	name = uniq( "rs" );
		     	srv = mcpServer( name ).withRateLimit( { limit: 1, window: 60, store: kind, cacheName: "default" } );
		     	call( req( "3.3.3.3", name ) );
		     	call( req( "4.4.4.4", name ) );
		     	blocked = call( req( "3.3.3.3", name ) ).statusCode == 429;
		     	cleared = srv.resetRateLimit();
		     	back = call( req( "3.3.3.3", name ) ).statusCode == 200 && call( req( "4.4.4.4", name ) ).statusCode == 200;
		     	results.append( blocked && cleared && back );
		     }
		     // A store that cannot clear says so
		     name = uniq( "rs" );
		     plain = mcpServer( name ).withRateLimit( { limit: 1, window: 60, store: new src.test.resources.FailingStore() } );
		     noClear = plain.resetRateLimit();
		     noRateLimit = mcpServer( uniq( "rs" ) ).resetRateLimit();
		     """ );
		var results = ( ortus.boxlang.runtime.types.Array ) get( "results" );
		assertThat( results.get( 0 ) ).isEqualTo( true );
		assertThat( results.get( 1 ) ).isEqualTo( true );
		assertThat( get( "noClear" ) ).isEqualTo( false );
		assertThat( get( "noRateLimit" ) ).isEqualTo( false );
	}

	// ------------------------------------------------------------------------------------------------
	// History
	// ------------------------------------------------------------------------------------------------

	/**
	 * The report has the days and the top consumers with requests, blocked, the busiest minute and the share of the limit
	 */
	@Test
	@DisplayName( "getRateLimitReport() gives days and top consumers" )
	public void testReport() {
		run( """
		     name = uniq( "rp" );
		     srv = mcpServer( name ).withRateLimit( { limit: 5, window: 60, store: "memory", retention: 0 } );
		     nums( 8 ).each( i => call( req( "5.5.5.5", name ) ) );
		     nums( 2 ).each( i => call( req( "6.6.6.6", name ) ) );
		     report = srv.getRateLimitReport( { days: 7, top: 10 } );
		     top = report.topConsumers[ 1 ];
		     topClient = top.client;
		     topRequests = top.requests;
		     topBlocked = top.blocked;
		     topPeak = top.peakPerMinute;
		     topPct = top.pctOfLimit;
		     second = report.topConsumers[ 2 ].client;
		     dayRequests = report.days[ 1 ].requests;
		     dayBlocked = report.days[ 1 ].blocked;
		     consumers = report.topConsumers.len();
		     noLimit = mcpServer( uniq( "rp" ) ).getRateLimitReport();
		     emptyDays = noLimit.days.len();
		     """ );
		assertThat( get( "topClient" ) ).isEqualTo( "5.5.5.5" );
		assertThat( num( "topRequests" ) ).isEqualTo( 8 );
		assertThat( num( "topBlocked" ) ).isEqualTo( 3 );
		assertThat( num( "topPeak" ) ).isEqualTo( 8 );
		// 8 in a minute against 5 per minute
		assertThat( num( "topPct" ) ).isEqualTo( 160 );
		assertThat( get( "second" ) ).isEqualTo( "6.6.6.6" );
		assertThat( num( "dayRequests" ) ).isEqualTo( 10 );
		assertThat( num( "dayBlocked" ) ).isEqualTo( 3 );
		assertThat( num( "consumers" ) ).isEqualTo( 2 );
		assertThat( num( "emptyDays" ) ).isEqualTo( 0 );
	}

	/**
	 * The history is written to the cache at most every flush time, and a node that restarts continues the day
	 */
	@Test
	@DisplayName( "History is flushed to the cache and a restarted node continues the day" )
	public void testFlushAndRestart() {
		run( """
		     name = uniq( "fl" );
		     h = newHistory( name );
		     h.record( "7.7.7.7", "ip", true );
		     h.record( "7.7.7.7", "ip", false );
		     // Inside the flush time: only the first request was written
		     clock.now += 10;
		     h.record( "7.7.7.7", "ip", true );
		     cacheKeys = cache( "default" ).getKeys().filter( k => k.startsWith( "bxai:mcp:ratelimit-history:" & name ) ).len();
		     h.flush();
		     // A new instance is a restarted node
		     h2 = newHistory( name );
		     h2.record( "7.7.7.7", "ip", true );
		     report = h2.report( 1, 10 );
		     requests = report.days[ 1 ].requests;
		     blocked = report.days[ 1 ].blocked;
		     cleared = false;
		     h2.clear();
		     afterClear = newHistory( name ).report( 1, 10 ).days.len();
		     """ );
		assertThat( num( "cacheKeys" ) ).isEqualTo( 1 );
		assertThat( num( "requests" ) ).isEqualTo( 4 );
		assertThat( num( "blocked" ) ).isEqualTo( 1 );
		assertThat( num( "afterClear" ) ).isEqualTo( 0 );
	}

	/**
	 * With retention 0 nothing goes to the cache, and a new day starts a new document
	 */
	@Test
	@DisplayName( "Retention 0 keeps history in memory only, and a new UTC day starts over" )
	public void testNoRetentionAndNewDay() {
		run( """
		     name = uniq( "nr" );
		     h = newHistory( name, 0 );
		     h.record( "8.8.8.8", "ip", true );
		     h.flush();
		     cacheKeys = cache( "default" ).getKeys().filter( k => k.startsWith( "bxai:mcp:ratelimit-history:" & name ) ).len();
		     clock.now += 86400;
		     h.record( "8.8.8.8", "ip", true );
		     h.record( "8.8.8.8", "ip", true );
		     // Only today is kept
		     days = h.report( 7, 10 ).days;
		     dayCount = days.len();
		     todayRequests = days[ 1 ].requests;
		     """ );
		assertThat( num( "cacheKeys" ) ).isEqualTo( 0 );
		assertThat( num( "dayCount" ) ).isEqualTo( 1 );
		assertThat( num( "todayRequests" ) ).isEqualTo( 2 );
	}

	/**
	 * Days before today come back from the cache, and a document of another node is merged in
	 */
	@Test
	@DisplayName( "The report merges earlier days and the documents of other nodes" )
	public void testMergeDaysAndNodes() {
		run( """
		     name = uniq( "mg" );
		     h = newHistory( name );
		     h.record( "9.9.9.9", "ip", true );
		     h.flush();
		     // Another node saved its day in the same cache
		     today = int( clock.now / 86400 );
		     cache( "default" ).set( "bxai:mcp:ratelimit-history:" & name & ":" & today & ":other-node", {
		     	server: name, day: today, node: "other-node", requests: 10, blocked: 4,
		     	clients: { "ip:9.9.9.9": { client: "9.9.9.9", type: "ip", requests: 10, blocked: 4, peak: 7, minute: 0, minuteCount: 0, lastBlockedAt: 5 } },
		     	other: { requests: 0, blocked: 0 }, updated: 1
		     }, 60, 0 );
		     // An earlier day of this node
		     yesterday = today - 1;
		     cache( "default" ).set( "bxai:mcp:ratelimit-history:" & name & ":" & yesterday & ":other-node", {
		     	server: name, day: yesterday, node: "other-node", requests: 6, blocked: 0,
		     	clients: { "ip:1.0.0.1": { client: "1.0.0.1", type: "ip", requests: 6, blocked: 0, peak: 2, minute: 0, minuteCount: 0, lastBlockedAt: 0 } },
		     	other: { requests: 0, blocked: 0 }, updated: 1
		     }, 60, 0 );
		     one = h.report( 1, 10 );
		     three = h.report( 3, 10 );
		     oneRequests = one.days[ 1 ].requests;
		     oneNodes = one.nodes;
		     topPeak = one.topConsumers[ 1 ].peakPerMinute;
		     topRequests = one.topConsumers[ 1 ].requests;
		     threeDays = three.days.len();
		     since = three.since;
		     cache( "default" ).getKeys().filter( k => k.startsWith( "bxai:mcp:ratelimit-history:" & name ) ).each( k => cache( "default" ).clear( k ) );
		     """ );
		assertThat( num( "oneRequests" ) ).isEqualTo( 11 );
		assertThat( num( "oneNodes" ) ).isEqualTo( 2 );
		assertThat( num( "topPeak" ) ).isEqualTo( 7 );
		assertThat( num( "topRequests" ) ).isEqualTo( 11 );
		assertThat( num( "threeDays" ) ).isEqualTo( 2 );
	}

	/**
	 * A day keeps at most maxClients, the totals stay exact, and the smallest go into other
	 */
	@Test
	@DisplayName( "A day is capped at maxClients and the totals stay exact" )
	public void testClientCap() {
		run( """
		     name = uniq( "cp" );
		     h = newHistory( name, 0, 50 );
		     // One big client, then a flood of 300 different ones
		     nums( 20 ).each( i => h.record( "big", "ip", false ) );
		     nums( 300 ).each( i => h.record( "ip-" & i, "ip", true ) );
		     report = h.report( 1, 200 );
		     listed = report.topConsumers.len();
		     total = report.days[ 1 ].requests;
		     blockedTotal = report.days[ 1 ].blocked;
		     bigKept = report.topConsumers[ 1 ].client;
		     """ );
		assertThat( num( "listed" ) ).isAtMost( 50 );
		assertThat( num( "total" ) ).isEqualTo( 320 );
		assertThat( num( "blockedTotal" ) ).isEqualTo( 20 );
		// The client with blocked requests is never the one folded away
		assertThat( get( "bigKept" ) ).isEqualTo( "big" );
	}

	/**
	 * Resetting the stats clears the numbers and the history of the server
	 */
	@Test
	@DisplayName( "resetRateLimitStats() clears counts and history" )
	public void testResetStats() {
		run( """
		     name = uniq( "rr" );
		     srv = mcpServer( name ).withRateLimit( { limit: 1, window: 60, store: "memory" } );
		     nums( 3 ).each( i => call( req( "1.2.3.4", name ) ) );
		     before = srv.getRateLimitReport().days.len();
		     srv.resetRateLimitStats();
		     after = srv.getRateLimitReport().days.len();
		     blockedAfter = srv.getStats().rateLimit.blocked;
		     """ );
		assertThat( num( "before" ) ).isEqualTo( 1 );
		assertThat( num( "after" ) ).isEqualTo( 0 );
		assertThat( num( "blockedAfter" ) ).isEqualTo( 0 );
	}

	// ------------------------------------------------------------------------------------------------
	// Settings
	// ------------------------------------------------------------------------------------------------

	/**
	 * The new settings have defaults, a server can override them, and bad values throw
	 */
	@Test
	@DisplayName( "retention, flush and client cap come from the module setting, can be overridden and are validated" )
	public void testSettings() {
		run( """
		     s = getModuleInfo( "bxai" ).settings.mcp.server.rateLimit;
		     defaults = [ s.retentionDays, s.historyFlushSeconds, s.historyMaxClients ];
		     srv = mcpServer( uniq( "st" ) ).withRateLimit( { limit: 5, store: "memory", retention: 30 } );
		     retention = srv.getStats().rateLimit.retentionDays;
		     badRetention = errType( () => mcpServer( "x" ).withRateLimit( { retention: 400 } ) );
		     badFlush = errType( () => mcpServer( "x" ).withRateLimit( { historyFlushSeconds: 1 } ) );
		     badClients = errType( () => mcpServer( "x" ).withRateLimit( { historyMaxClients: 10 } ) );
		     """ );
		var defaults = ( ortus.boxlang.runtime.types.Array ) get( "defaults" );
		assertThat( ( ( Number ) defaults.get( 0 ) ).intValue() ).isEqualTo( 7 );
		assertThat( ( ( Number ) defaults.get( 1 ) ).intValue() ).isEqualTo( 30 );
		assertThat( ( ( Number ) defaults.get( 2 ) ).intValue() ).isEqualTo( 500 );
		assertThat( num( "retention" ) ).isEqualTo( 30 );
		assertThat( get( "badRetention" ) ).isEqualTo( "InvalidArgument" );
		assertThat( get( "badFlush" ) ).isEqualTo( "InvalidArgument" );
		assertThat( get( "badClients" ) ).isEqualTo( "InvalidArgument" );
	}

}
