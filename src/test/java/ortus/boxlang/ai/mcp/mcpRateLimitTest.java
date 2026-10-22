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
 * Rate limiting of MCP servers: the limiter, the stores, and the server through the request processor.
 * Everything runs offline.
 */
public class mcpRateLimitTest extends BaseIntegrationTest {

	/**
	 * Shared BoxLang helpers: a fake clock limiter, a request builder and a processor call
	 */
	private static final String HELPERS = """
	                                      import bxModules.bxai.models.mcp.MCPRequestProcessor;
	                                      import bxModules.bxai.models.mcp.ratelimit.RateLimiter;
	                                      import bxModules.bxai.models.mcp.ratelimit.MemoryRateLimitStore;
	                                      import bxModules.bxai.models.mcp.ratelimit.CacheRateLimitStore;

	                                      clock = { now : 1200 };
	                                      function newLimiter( limit = 10, window = 60, store = new MemoryRateLimitStore() ) {
	                                      	return new RateLimiter( limit: limit, window: window, store: store, clock: () => clock.now );
	                                      }
	                                      function req( ip = "1.2.3.4", headers = {}, method = "POST", serverName = "rl", extra = {} ) {
	                                      	return {
	                                      		serverName : serverName,
	                                      		method     : method,
	                                      		body       : '{"jsonrpc":"2.0","id":1,"method":"ping"}',
	                                      		headers    : headers,
	                                      		acceptHeader: "application/json",
	                                      		params     : {},
	                                      		origin     : "",
	                                      		requestId  : createUUID(),
	                                      		metadata   : { remoteAddr : ip }
	                                      	}.append( extra );
	                                      }
	                                      function nums( n ) {
	                                      	var a = [];
	                                      	for ( var i = 1; i <= n; i++ ) { a.append( i ); }
	                                      	return a;
	                                      }
	                                      function errType( f ) {
	                                      	try {
	                                      		f();
	                                      		return "none";
	                                      	} catch ( any e ) {
	                                      		return e.type;
	                                      	}
	                                      }
	                                      function call( data ) {
	                                      	return MCPRequestProcessor::processWithTransport( MCPRequestProcessor::getHttpTransport(), data );
	                                      }
	                                      """;

	@AfterEach
	public void clearServers() {
		runtime.executeSource( "bxModules.bxai.models.mcp.MCPServer::clearAllInstances()", context );
	}

	private void run( String script ) {
		runtime.executeSource( HELPERS + script, context );
	}

	private int num( String name ) {
		return ( ( Number ) get( name ) ).intValue();
	}

	private Object get( String name ) {
		return variables.get( Key.of( name ) );
	}

	// ------------------------------------------------------------------------------------------------
	// The limiter
	// ------------------------------------------------------------------------------------------------

	@Test
	@DisplayName( "The limiter allows the limit, refuses the next request and does not count the refusal" )
	public void testLimiterBasics() {
		run( """
		     l = newLimiter( 3, 60 );
		     r = [];
		     for ( i = 1; i <= 5; i++ ) { r.append( l.check( "a" ) ); }
		     allowed = r.filter( x => x.allowed ).len();
		     remainingFirst = r[ 1 ].remaining;
		     firstBlocks = r.filter( x => !x.allowed && x.firstBlock ).len();
		     statusRemaining = l.status( "a" ).remaining;
		     """ );
		assertThat( num( "allowed" ) ).isEqualTo( 3 );
		assertThat( num( "remainingFirst" ) ).isEqualTo( 2 );
		// Two refusals, only the first is a "first block"
		assertThat( num( "firstBlocks" ) ).isEqualTo( 1 );
		// Refusals are not in the counter: still 3
		assertThat( num( "statusRemaining" ) ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "Retry-After is exact and the client is let in again then" )
	public void testLimiterRetryAfter() {
		run( """
		     l = newLimiter( 10, 60 );
		     for ( i = 1; i <= 10; i++ ) { l.check( "a" ); }
		     blocked = l.check( "a" );
		     retryAfter = blocked.retryAfter;
		     // One second before: still refused
		     clock.now = 1200 + retryAfter - 1;
		     stillBlocked = !l.check( "a" ).allowed;
		     // At retryAfter the next request fits
		     clock.now = 1200 + retryAfter;
		     nowAllowed = l.check( "a" ).allowed;
		     """ );
		assertThat( num( "retryAfter" ) ).isEqualTo( 66 );
		assertThat( get( "stillBlocked" ) ).isEqualTo( true );
		assertThat( get( "nowAllowed" ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "Clients do not share counters and reset gives a client its requests back" )
	public void testLimiterIsolationAndReset() {
		run( """
		     l = newLimiter( 2, 60 );
		     l.check( "a" ); l.check( "a" );
		     aBlocked = !l.check( "a" ).allowed;
		     bAllowed = l.check( "b" ).allowed;
		     l.reset( "a" );
		     aAgain = l.check( "a" ).allowed;
		     """ );
		assertThat( get( "aBlocked" ) ).isEqualTo( true );
		assertThat( get( "bAllowed" ) ).isEqualTo( true );
		assertThat( get( "aAgain" ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "The previous window counts less as time goes on and bad settings throw" )
	public void testLimiterSlidingAndValidation() {
		run( """
		     l = newLimiter( 10, 60 );
		     for ( i = 1; i <= 10; i++ ) { l.check( "a" ); }
		     // Half way into the next window the previous window counts for half: 5 + 1 <= 10
		     clock.now = 1260 + 30;
		     slid = l.check( "a" ).allowed;
		     badLimit = errType( () => new RateLimiter( limit: 0, window: 60, store: new MemoryRateLimitStore() ) ) == "InvalidArgument";
		     badWindow = errType( () => new RateLimiter( limit: 1, window: 0, store: new MemoryRateLimitStore() ) ) == "InvalidArgument";
		     hashA = l.keyHash( "ip:1.2.3.4" );
		     hashLen = hashA.len();
		     hashHidesKey = !hashA.findNoCase( "1.2.3.4" );
		     """ );
		assertThat( get( "slid" ) ).isEqualTo( true );
		assertThat( get( "badLimit" ) ).isEqualTo( true );
		assertThat( get( "badWindow" ) ).isEqualTo( true );
		assertThat( num( "hashLen" ) ).isEqualTo( 32 );
		assertThat( get( "hashHidesKey" ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "Many threads on one bucket never get more than the limit" )
	public void testLimiterConcurrency() {
		run( """
		     l = newLimiter( 25, 60 );
		     results = nums( 200 ).map( i => l.check( "hot" ).allowed, true, 16 );
		     allowedCount = results.filter( x => x ).len();
		     """ );
		assertThat( num( "allowedCount" ) ).isEqualTo( 25 );
	}

	// ------------------------------------------------------------------------------------------------
	// The stores
	// ------------------------------------------------------------------------------------------------

	@Test
	@DisplayName( "Memory and cache stores count, undo and remove" )
	public void testStores() {
		run( """
		     results = {};
		     for ( store in [ new MemoryRateLimitStore(), new CacheRateLimitStore( "default" ) ] ) {
		     	k = "bxai:test:#createUUID()#";
		     	store.increment( k, 120 );
		     	store.increment( k, 120 );
		     	two = store.get( k );
		     	store.increment( k, 120, -1 );
		     	one = store.get( k );
		     	store.increment( k, 120, -5 );
		     	floor = store.get( k );
		     	store.increment( k, 120 );
		     	store.remove( k );
		     	gone = store.get( k );
		     	results[ getMetadata( store ).name ] = [ two, one, floor, gone ];
		     }
		     values = results.valueArray();
		     """ );
		var values = ( ortus.boxlang.runtime.types.Array ) get( "values" );
		for ( Object row : values ) {
			var items = ( ortus.boxlang.runtime.types.Array ) row;
			assertThat( ( ( Number ) items.get( 0 ) ).intValue() ).isEqualTo( 2 );
			assertThat( ( ( Number ) items.get( 1 ) ).intValue() ).isEqualTo( 1 );
			// Never below zero
			assertThat( ( ( Number ) items.get( 2 ) ).intValue() ).isEqualTo( 0 );
			assertThat( ( ( Number ) items.get( 3 ) ).intValue() ).isEqualTo( 0 );
		}
	}

	// ------------------------------------------------------------------------------------------------
	// The server through the request processor
	// ------------------------------------------------------------------------------------------------

	@Test
	@DisplayName( "Blocked requests get 429, JSON-RPC -32002 and the rate limit headers" )
	public void testProcessorBlocks() {
		run( """
		     srv = mcpServer( "rl" ).withRateLimit( { limit: 2, window: 60, store: "memory" } );
		     first = call( req() );
		     call( req() );
		     blocked = call( req() );
		     otherIp = call( req( "9.9.9.9" ) );
		     otherStatus = otherIp.statusCode;
		     body = jsonDeserialize( blocked.content );
		     hasRateLimit = srv.hasRateLimit();
		     """ );
		assertThat( get( "hasRateLimit" ) ).isEqualTo( true );
		var first = ( ortus.boxlang.runtime.types.IStruct ) get( "first" );
		assertThat( ( ( Number ) first.get( Key.of( "statusCode" ) ) ).intValue() ).isEqualTo( 200 );
		var firstHeaders = ( ortus.boxlang.runtime.types.IStruct ) first.get( Key.of( "headers" ) );
		assertThat( ( ( Number ) firstHeaders.get( Key.of( "X-RateLimit-Limit" ) ) ).intValue() ).isEqualTo( 2 );
		assertThat( firstHeaders.containsKey( Key.of( "X-RateLimit-Remaining" ) ) ).isTrue();
		assertThat( firstHeaders.containsKey( Key.of( "X-Request-ID" ) ) ).isTrue();

		var blocked = ( ortus.boxlang.runtime.types.IStruct ) get( "blocked" );
		assertThat( ( ( Number ) blocked.get( Key.of( "statusCode" ) ) ).intValue() ).isEqualTo( 429 );
		var blockedHeaders = ( ortus.boxlang.runtime.types.IStruct ) blocked.get( Key.of( "headers" ) );
		assertThat( blockedHeaders.containsKey( Key.of( "Retry-After" ) ) ).isTrue();
		assertThat( ( ( Number ) blockedHeaders.get( Key.of( "X-RateLimit-Remaining" ) ) ).intValue() ).isEqualTo( 0 );
		assertThat( blockedHeaders.containsKey( Key.of( "X-Request-ID" ) ) ).isTrue();
		var error = ( ortus.boxlang.runtime.types.IStruct ) ( ( ortus.boxlang.runtime.types.IStruct ) get( "body" ) ).get( Key.of( "error" ) );
		assertThat( ( ( Number ) error.get( Key.of( "code" ) ) ).intValue() ).isEqualTo( -32002 );

		assertThat( num( "otherStatus" ) ).isEqualTo( 200 );
	}

	@Test
	@DisplayName( "A server without a rate limit is not limited, and false removes one" )
	public void testNoLimitAndOptOut() {
		run( """
		     mcpServer( "rl" );
		     codes = nums( 30 ).map( i => call( req() ).statusCode );
		     unlimited = codes.filter( c => c == 200 ).len();
		     srv = mcpServer( "rl2" ).withRateLimit( 1 ).withRateLimit( false );
		     off = !srv.hasRateLimit();
		     """ );
		assertThat( num( "unlimited" ) ).isEqualTo( 30 );
		assertThat( get( "off" ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "The limit runs before authentication and a CORS preflight is not counted" )
	public void testBeforeAuthAndPreflight() {
		run( """
		     srv = mcpServer( "rl" )
		     	.withCors( "*" )
		     	.withBasicAuth( "u", "p" )
		     	.withRateLimit( { limit: 2, window: 60, store: "memory" } );
		     // Failed logins count: the third is a 429, not a 401
		     codes = nums( 3 ).map( i => call( req() ).statusCode );
		     // Preflights for another client never use its requests
		     pre = nums( 5 ).map( i => call( req( "7.7.7.7", {}, "OPTIONS", "rl", { origin: "https://a.com" } ) ).statusCode );
		     okAfterPreflights = call( req( "7.7.7.7", { Authorization: "Basic " & toBase64( "u:p" ) } ) ).statusCode;
		     """ );
		var codes = ( ortus.boxlang.runtime.types.Array ) get( "codes" );
		assertThat( ( ( Number ) codes.get( 0 ) ).intValue() ).isEqualTo( 401 );
		assertThat( ( ( Number ) codes.get( 1 ) ).intValue() ).isEqualTo( 401 );
		assertThat( ( ( Number ) codes.get( 2 ) ).intValue() ).isEqualTo( 429 );
		assertThat( num( "okAfterPreflights" ) ).isEqualTo( 200 );
	}

	@Test
	@DisplayName( "The 429 keeps the CORS headers" )
	public void testBlockedKeepsCors() {
		run( """
		     mcpServer( "rl" ).withCors( "*" ).withRateLimit( { limit: 1, window: 60, store: "memory" } );
		     extra = { origin: "https://a.com" };
		     call( req( "1.1.1.1", {}, "POST", "rl", extra ) );
		     blocked = call( req( "1.1.1.1", {}, "POST", "rl", extra ) );
		     """ );
		var blocked = ( ortus.boxlang.runtime.types.IStruct ) get( "blocked" );
		assertThat( ( ( Number ) blocked.get( Key.of( "statusCode" ) ) ).intValue() ).isEqualTo( 429 );
		var headers = ( ortus.boxlang.runtime.types.IStruct ) blocked.get( Key.of( "headers" ) );
		assertThat( headers.containsKey( Key.of( "Access-Control-Allow-Origin" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Key apiKey: a verified key is its own bucket, fake keys fall back to the IP bucket" )
	public void testApiKeyKey() {
		run( """
		     srv = mcpServer( "rl" )
		     	.withApiKeyProvider( ( key, rd ) => key.startsWith( "good-" ) )
		     	.withRateLimit( { limit: 2, window: 60, store: "memory", key: "apiKey" } );
		     // Two different good keys from the same IP do not share a bucket
		     a = nums( 2 ).map( i => call( req( "5.5.5.5", { "X-API-Key": "good-a" } ) ).statusCode );
		     b = call( req( "5.5.5.5", { "X-API-Key": "good-b" } ) ).statusCode;
		     aBlocked = call( req( "5.5.5.5", { "X-API-Key": "good-a" } ) ).statusCode;
		     // Rotating fake keys all land in the IP bucket: 401, 401, then 429
		     fakes = nums( 4 ).map( i => call( req( "6.6.6.6", { "X-API-Key": "fake-#i#" } ) ).statusCode );
		     // Bearer works too
		     bearer = call( req( "8.8.8.8", { Authorization: "Bearer good-z" } ) ).statusCode;
		     // Without a provider the key is refused up front
		     noProvider = errType( () => mcpServer( "rl3" ).withRateLimit( { key: "apiKey" } ) ) == "InvalidArgument";
		     """ );
		var a = ( ortus.boxlang.runtime.types.Array ) get( "a" );
		assertThat( ( ( Number ) a.get( 1 ) ).intValue() ).isEqualTo( 200 );
		assertThat( num( "b" ) ).isEqualTo( 200 );
		assertThat( num( "aBlocked" ) ).isEqualTo( 429 );
		var fakes = ( ortus.boxlang.runtime.types.Array ) get( "fakes" );
		assertThat( ( ( Number ) fakes.get( 0 ) ).intValue() ).isEqualTo( 401 );
		assertThat( ( ( Number ) fakes.get( 1 ) ).intValue() ).isEqualTo( 401 );
		assertThat( ( ( Number ) fakes.get( 2 ) ).intValue() ).isEqualTo( 429 );
		assertThat( num( "bearer" ) ).isEqualTo( 200 );
		assertThat( get( "noProvider" ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "Key apiKey asks the provider once per request" )
	public void testApiKeyProviderCalledOnce() {
		run( """
		     calls = { n : 0 };
		     mcpServer( "rl" )
		     	.withApiKeyProvider( ( key, rd ) => { calls.n++; return true; } )
		     	.withRateLimit( { limit: 5, window: 60, store: "memory", key: "apiKey" } );
		     call( req( "5.5.5.5", { "X-API-Key": "k" } ) );
		     providerCalls = calls.n;
		     """ );
		assertThat( num( "providerCalls" ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Key global shares one bucket and a lambda key can group or exempt" )
	public void testGlobalAndLambdaKeys() {
		run( """
		     mcpServer( "g" ).withRateLimit( { limit: 2, window: 60, store: "memory", key: "global" } );
		     g = [ call( req( "1.1.1.1", {}, "POST", "g" ) ), call( req( "2.2.2.2", {}, "POST", "g" ) ), call( req( "3.3.3.3", {}, "POST", "g" ) ) ].map( r => r.statusCode );

		     // Lambda: limit by a header, an empty result is not limited
		     mcpServer( "c" ).withRateLimit( {
		     	limit: 1, window: 60, store: "memory",
		     	key: ( rd ) => rd.headers[ "X-Tenant" ] ?: ""
		     } );
		     t1 = call( req( "1.1.1.1", { "X-Tenant": "acme" }, "POST", "c" ) ).statusCode;
		     t1b = call( req( "2.2.2.2", { "X-Tenant": "acme" }, "POST", "c" ) ).statusCode;
		     t2 = call( req( "1.1.1.1", { "X-Tenant": "other" }, "POST", "c" ) ).statusCode;
		     exempt = nums( 5 ).map( i => call( req( "1.1.1.1", {}, "POST", "c" ) ).statusCode ).filter( s => s == 200 ).len();
		     """ );
		var g = ( ortus.boxlang.runtime.types.Array ) get( "g" );
		assertThat( ( ( Number ) g.get( 1 ) ).intValue() ).isEqualTo( 200 );
		assertThat( ( ( Number ) g.get( 2 ) ).intValue() ).isEqualTo( 429 );
		assertThat( num( "t1" ) ).isEqualTo( 200 );
		assertThat( num( "t1b" ) ).isEqualTo( 429 );
		assertThat( num( "t2" ) ).isEqualTo( 200 );
		assertThat( num( "exempt" ) ).isEqualTo( 5 );
	}

	@Test
	@DisplayName( "stdio has no IP: the ip key is one shared bucket" )
	public void testNoIp() {
		run( """
		     mcpServer( "rl" ).withRateLimit( { limit: 2, window: 60, store: "memory" } );
		     codes = nums( 3 ).map( i => call( req( "" ) ).statusCode );
		     """ );
		var codes = ( ortus.boxlang.runtime.types.Array ) get( "codes" );
		assertThat( ( ( Number ) codes.get( 2 ) ).intValue() ).isEqualTo( 429 );
	}

	@Test
	@DisplayName( "Custom store: a class path works, a failing store lets requests in or answers 503" )
	public void testCustomStoreAndFailOpen() {
		run( """
		     // A store class path
		     mcpServer( "rl" ).withRateLimit( { limit: 1, window: 60, store: "bxModules.bxai.models.mcp.ratelimit.MemoryRateLimitStore" } );
		     ok = call( req() ).statusCode;
		     blocked = call( req() ).statusCode;

		     // Failing stores: a struct is not a store, so use a class created on the fly
		     failing = new src.test.resources.FailingStore();
		     mcpServer( "open" ).withRateLimit( { limit: 1, window: 60, store: failing, failOpen: true } );
		     openCodes = nums( 3 ).map( i => call( req( "1.1.1.1", {}, "POST", "open" ) ).statusCode );
		     mcpServer( "closed" ).withRateLimit( { limit: 1, window: 60, store: failing, failOpen: false } );
		     closedCode = call( req( "1.1.1.1", {}, "POST", "closed" ) ).statusCode;
		     errors = mcpServer( "open" ).getStats().rateLimit.storeErrors;
		     """.replace( "failing = new src.test.resources.FailingStore();", "failing = new src.test.resources.FailingStore();" ) );
		assertThat( num( "ok" ) ).isEqualTo( 200 );
		assertThat( num( "blocked" ) ).isEqualTo( 429 );
		var open = ( ortus.boxlang.runtime.types.Array ) get( "openCodes" );
		assertThat( ( ( Number ) open.get( 2 ) ).intValue() ).isEqualTo( 200 );
		assertThat( num( "closedCode" ) ).isEqualTo( 503 );
		assertThat( num( "errors" ) ).isEqualTo( 3 );
	}

	@Test
	@DisplayName( "Stats count allowed and blocked, with the top blocked bounded" )
	public void testStats() {
		run( """
		     srv = mcpServer( "rl" ).withRateLimit( { limit: 1, window: 60, store: "memory" } );
		     call( req( "1.1.1.1" ) );
		     nums( 3 ).each( i => call( req( "1.1.1.1" ) ) );
		     // 40 different clients blocked once each
		     nums( 40 ).each( i => { call( req( "10.0.0.#i#" ) ); call( req( "10.0.0.#i#" ) ); } );
		     stats = srv.getStats();
		     """ );
		var	stats		= ( ortus.boxlang.runtime.types.IStruct ) get( "stats" );
		var	security	= ( ortus.boxlang.runtime.types.IStruct ) stats.get( Key.of( "security" ) );
		assertThat( ( ( Number ) security.get( Key.of( "rateLimitFailures" ) ) ).intValue() ).isEqualTo( 43 );
		var rate = ( ortus.boxlang.runtime.types.IStruct ) stats.get( Key.of( "rateLimit" ) );
		assertThat( ( ( Number ) rate.get( Key.of( "allowed" ) ) ).intValue() ).isEqualTo( 41 );
		assertThat( ( ( Number ) rate.get( Key.of( "blocked" ) ) ).intValue() ).isEqualTo( 43 );
		assertThat( ( ( ortus.boxlang.runtime.types.Array ) rate.get( Key.of( "topBlocked" ) ) ).size() ).isAtMost( 20 );
	}

	@Test
	@DisplayName( "A flood of clients writes few log lines and fires few events, the stats stay exact" )
	public void testFloodBudget() {
		run( """
		     hits = { n : 0 };
		     srv = mcpServer( "rl" ).withRateLimit( { limit: 1, window: 60, store: "memory" } );
		     BoxRegisterInterceptor( function( data ) { hits.n++; }, "onMCPRateLimitHit" );
		     nums( 500 ).each( i => { call( req( "10.1.#int( i / 250 )#.#i mod 250#" ) ); call( req( "10.1.#int( i / 250 )#.#i mod 250#" ) ); } );
		     events = hits.n;
		     blocked = srv.getStats().rateLimit.blocked;
		     """ );
		assertThat( num( "blocked" ) ).isEqualTo( 500 );
		// 500 clients blocked in one minute: at most 10 events (the budget), usually all in the same minute
		assertThat( ( ( Number ) get( "events" ) ).intValue() ).isAtMost( 20 );
		assertThat( ( ( Number ) get( "events" ) ).intValue() ).isAtLeast( 1 );
	}

	@Test
	@DisplayName( "Defaults come from the module setting and an explicit struct overrides them" )
	public void testModuleDefaults() {
		run( """
		     settings = getModuleInfo( "bxai" ).settings.mcp.server;
		     original = settings.rateLimit.duplicate();
		     try {
		     	settings.rateLimit.enabled = true;
		     	settings.rateLimit.limit = 3;
		     	settings.rateLimit.store = "memory";
		     	fromSetting = mcpServer( "s1" );
		     	settingLimit = fromSetting.getRateLimiter().getLimit();
		     	overridden = mcpServer( "s2" ).withRateLimit( { limit: 7 } ).getRateLimiter().getLimit();
		     	optOut = !mcpServer( "s3" ).withRateLimit( false ).hasRateLimit();
		     } finally {
		     	settings.rateLimit.append( original, true );
		     }
		     offByDefault = !mcpServer( "s4" ).hasRateLimit();
		     """ );
		assertThat( num( "settingLimit" ) ).isEqualTo( 3 );
		assertThat( num( "overridden" ) ).isEqualTo( 7 );
		assertThat( get( "optOut" ) ).isEqualTo( true );
		assertThat( get( "offByDefault" ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "Bad settings throw InvalidArgument" )
	public void testValidation() {
		run( """
		     function code( cfg ) {
		     	try { mcpServer( "v" ).withRateLimit( cfg ); return "none"; }
		     	catch ( any e ) { return e.type; }
		     }
		     badKey = code( { key: "nope" } );
		     badStore = code( { store: "nope" } );
		     badLimit = code( { limit: 0 } );
		     badType = code( "x" );
		     """ );
		assertThat( get( "badKey" ) ).isEqualTo( "InvalidArgument" );
		assertThat( get( "badStore" ) ).isEqualTo( "InvalidArgument" );
		assertThat( get( "badLimit" ) ).isEqualTo( "InvalidArgument" );
		assertThat( get( "badType" ) ).isEqualTo( "InvalidArgument" );
	}

	// ------------------------------------------------------------------------------------------------
	// Trusted proxies
	// ------------------------------------------------------------------------------------------------

	@Test
	@DisplayName( "Without trusted proxies the forwarded header is used, as before" )
	public void testProxiesDefault() {
		run( """
		     srv = mcpServer( "p" );
		     ip = srv.getClientIP( req( "10.0.0.1", { "x-forwarded-for": "203.0.113.5, 10.0.0.1" } ) );
		     star = mcpServer( "p2" ).withTrustedProxies( [ "*" ] ).getClientIP( req( "10.0.0.1", { "x-forwarded-for": "203.0.113.5, 10.0.0.1" } ) );
		     """ );
		assertThat( get( "ip" ) ).isEqualTo( "203.0.113.5" );
		assertThat( get( "star" ) ).isEqualTo( "203.0.113.5" );
	}

	@Test
	@DisplayName( "With trusted proxies a forged header from anyone else is ignored" )
	public void testProxiesStrict() {
		run( """
		     srv = mcpServer( "p" ).withTrustedProxies( [ "10.0.0.0/8", "192.168.1.1" ] );
		     // Direct client forging the header: its own address wins
		     forged = srv.getClientIP( req( "198.51.100.9", { "x-forwarded-for": "1.1.1.1" } ) );
		     // Through the proxy: the client
		     viaProxy = srv.getClientIP( req( "10.0.0.1", { "x-forwarded-for": "203.0.113.5" } ) );
		     // Chain walked from the right: the forged left value is skipped, trusted hops too
		     chain = srv.getClientIP( req( "10.0.0.1", { "x-forwarded-for": "1.1.1.1, 203.0.113.5, 10.2.2.2, 192.168.1.1" } ) );
		     // Trusted proxy but no header: its own address
		     noHeader = srv.getClientIP( req( "10.0.0.1" ) );
		     // The limit follows: a forged header does not make a new bucket
		     srv.withRateLimit( { limit: 1, window: 60, store: "memory" } );
		     c1 = call( req( "198.51.100.9", { "x-forwarded-for": "1.1.1.1" }, "POST", "p" ) ).statusCode;
		     c2 = call( req( "198.51.100.9", { "x-forwarded-for": "2.2.2.2" }, "POST", "p" ) ).statusCode;
		     """ );
		assertThat( get( "forged" ) ).isEqualTo( "198.51.100.9" );
		assertThat( get( "viaProxy" ) ).isEqualTo( "203.0.113.5" );
		assertThat( get( "chain" ) ).isEqualTo( "203.0.113.5" );
		assertThat( get( "noHeader" ) ).isEqualTo( "10.0.0.1" );
		assertThat( num( "c1" ) ).isEqualTo( 200 );
		assertThat( num( "c2" ) ).isEqualTo( 429 );
	}

	@Test
	@DisplayName( "Trusted proxies come from the module setting" )
	public void testProxiesFromSetting() {
		run( """
		     settings = getModuleInfo( "bxai" ).settings.mcp.server;
		     try {
		     	settings.trustedProxies = [ "10.0.0.0/8" ];
		     	proxies = mcpServer( "ps" ).getTrustedProxies();
		     } finally {
		     	settings.trustedProxies = [];
		     }
		     """ );
		var proxies = ( ortus.boxlang.runtime.types.Array ) get( "proxies" );
		assertThat( proxies.size() ).isEqualTo( 1 );
	}

}
