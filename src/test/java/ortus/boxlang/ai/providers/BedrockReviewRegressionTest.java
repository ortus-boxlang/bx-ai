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
package ortus.boxlang.ai.providers;

import static com.google.common.truth.Truth.assertThat;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

public class BedrockReviewRegressionTest extends BaseIntegrationTest {

	/** One ConverseStream replay frame: {"eventType":"<name>","bytes":"<base64 of the event JSON>"}. */
	private static String evt( String eventType, String json ) {
		return "{\"eventType\":\"" + eventType + "\",\"bytes\":\""
		    + Base64.getEncoder().encodeToString( json.getBytes( StandardCharsets.UTF_8 ) ) + "\"}";
	}

	@Test
	@DisplayName( "beforeToolCall argument edits survive both tool loops, in either slot, for sync and streaming alike" )
	public void toolArgumentEditsSurviveBothToolLoops() {
		// @formatter:off
		runtime.executeSource("""
			import bxModules.bxai.models.middleware.AiMiddlewareResult;
			provider = aiService("bedrock", {region:"us-east-1", bedrockApi:"converse", apiKey:"dummy"});
			evt = (kind,data) => jsonSerialize({eventType:kind,bytes:toBase64(charsetDecode(jsonSerialize(data),"UTF-8"))});
			results = [];
			for (streaming in [false,true]) {
				for (editMode in ["normalized","native","both","empty"]) {
					received = "";
					tool = aiTool("probe", "Probe", (string city = "DEFAULT") => { received = city; return city; });
					req = aiChatRequest(aiMessage().user("probe"), {model:"anthropic.claude-3-sonnet-20240229-v1:0", tools:[tool]}, {provider:"bedrock", providerOptions:{bedrockApi:"converse"}});
					turns = 0;
					req.addMiddleware({
						beforeToolCall: (ctx) => {
							if(editMode == "native" || editMode == "both") ctx.toolCall.input = {city:"NATIVE"};
							if(editMode == "normalized" || editMode == "both") ctx.toolArgs = {city:"SAFE"};
							if(editMode == "empty") ctx.toolArgs = {};
							return AiMiddlewareResult::continue();
						},
						wrapLLMCall: (ctx,handler) => {
							turns++;
							if(streaming) {
								if(turns > 1) return evt("messageStop",{stopReason:"end_turn"});
								return evt("contentBlockStart",{contentBlockIndex:0,start:{toolUse:{toolUseId:"one",name:"probe"}}})
									& evt("contentBlockDelta",{contentBlockIndex:0,delta:{toolUse:{input:'{"city":"ORIGINAL"}'}}})
									& evt("contentBlockStop",{contentBlockIndex:0}) & evt("messageStop",{stopReason:"tool_use"});
							}
							if(turns == 1) return {output:{message:{role:"assistant",content:[{toolUse:{toolUseId:"one",name:"probe",input:{city:"ORIGINAL"}}}]}},stopReason:"tool_use"};
							return {output:{message:{role:"assistant",content:[{text:"done"}]}},stopReason:"end_turn"};
						}
					});
					if(streaming) provider.chatStream(req,(chunk)=>{}); else provider.chat(req);
					results.append(received);
				}
			}
			actual = results.toList("|");
			""", context);
		// @formatter:on
		assertThat( variables.getAsString( Key.of( "actual" ) ) ).isEqualTo( "SAFE|NATIVE|SAFE|DEFAULT|SAFE|NATIVE|SAFE|DEFAULT" );
	}

	@Test
	@DisplayName( "A beforeLLMCall packet REPLACEMENT is what the streaming transport signs and sends" )
	public void streamingHonorsReplacementPacket() {
		// @formatter:off
		runtime.executeSource("""
			import bxModules.bxai.models.middleware.AiMiddlewareResult;
			provider = aiService("bedrock", {region:"us-east-1", apiKey:"dummy"});
			req = aiChatRequest(aiMessage().user("ORIGINAL"), {model:"anthropic.claude-3-sonnet-20240229-v1:0"}, {provider:"bedrock",providerOptions:{bedrockApi:"converse"}});
			seenPacket = {};
			req.addMiddleware({
				beforeLLMCall: (ctx) => { ctx.dataPacket = {messages:[{role:"user",content:[{text:"SAFE"}]}]}; return AiMiddlewareResult::continue(); },
				wrapLLMCall: (ctx,handler) => {
					seenPacket = ctx.dataPacket;
					return jsonSerialize({eventType:"messageStop",bytes:toBase64(charsetDecode('{"stopReason":"end_turn"}',"UTF-8"))});
				}
			});
			provider.chatStream(req,(chunk)=>{});
			actual = seenPacket.messages[1].content[1].text;
			""", context);
		// @formatter:on
		assertThat( variables.getAsString( Key.of( "actual" ) ) ).isEqualTo( "SAFE" );
	}

	@Test
	@DisplayName( "countTokens encodes the InvokeModel body as a base64 blob on the wire" )
	public void countTokensEncodesInvokeBodyOnWire() throws Exception {
		AtomicReference<String>	body	= new AtomicReference<>();
		HttpServer				server	= HttpServer.create( new InetSocketAddress( "127.0.0.1", 0 ), 0 );
		server.createContext( "/", exchange -> {
			body.set( new String( exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8 ) );
			byte[] response = "{\"inputTokens\":17}".getBytes( StandardCharsets.UTF_8 );
			exchange.getResponseHeaders().set( "Content-Type", "application/json" );
			exchange.sendResponseHeaders( 200, response.length );
			exchange.getResponseBody().write( response );
			exchange.close();
		} );
		server.start();
		try {
			variables.put( Key.of( "testEndpoint" ), "http://127.0.0.1:" + server.getAddress().getPort() );
			// @formatter:off
			runtime.executeSource("""
				provider = aiService("bedrock", {region:"us-east-1",baseURL:testEndpoint,bearerToken:"test-token"});
				req = aiChatRequest(aiMessage().user("Count café tokens"), {model:"anthropic.claude-3-sonnet-20240229-v1:0"}, {provider:"bedrock",providerOptions:{bedrockApi:"invoke"}});
				count = provider.countTokens(req);
				""", context);
			// @formatter:on
			assertThat( variables.getAsInteger( Key.of( "count" ) ) ).isEqualTo( 17 );
			variables.put( Key.of( "wireBody" ), body.get() );
			runtime.executeSource(
			    "decoded = jsonDeserialize(charsetEncode(toBinary(jsonDeserialize(wireBody).input.invokeModel.body), 'UTF-8')); actual = decoded.messages[1].content;",
			    context );
			assertThat( variables.getAsString( Key.of( "actual" ) ) ).isEqualTo( "Count café tokens" );
		} finally {
			server.stop( 0 );
		}
	}

	@Test
	@DisplayName( "A wrapLLMCall in-place edit of the streaming data packet reaches the wire" )
	public void streamWrapLLMCallInPlaceEditReachesWire() throws Exception {
		// The body is serialized INSIDE the transport closure, so an edit made by a wrapLLMCall
		// middleware that then calls handler() must show up in what the server actually receives.
		// Serializing before the hook froze the pre-edit JSON and silently dropped the change.
		AtomicReference<String>	body	= new AtomicReference<>();
		HttpServer				server	= HttpServer.create( new InetSocketAddress( "127.0.0.1", 0 ), 0 );
		server.createContext( "/", exchange -> {
			body.set( new String( exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8 ) );
			byte[] response = ( evt( "contentBlockDelta", "{\"contentBlockIndex\":0,\"delta\":{\"text\":\"wired\"}}" )
			    + evt( "messageStop", "{\"stopReason\":\"end_turn\"}" ) ).getBytes( StandardCharsets.UTF_8 );
			exchange.getResponseHeaders().set( "Content-Type", "application/json" );
			exchange.sendResponseHeaders( 200, response.length );
			exchange.getResponseBody().write( response );
			exchange.close();
		} );
		server.start();
		try {
			variables.put( Key.of( "testEndpoint" ), "http://127.0.0.1:" + server.getAddress().getPort() );
			// @formatter:off
			runtime.executeSource("""
				provider = aiService("bedrock", {region:"us-east-1",baseURL:testEndpoint,bearerToken:"test-token"});
				req = aiChatRequest(aiMessage().user("hi"), {model:"anthropic.claude-3-sonnet-20240229-v1:0"}, {provider:"bedrock",providerOptions:{bedrockApi:"converse"}});
				req.addMiddleware({
					wrapLLMCall: (ctx,handler) => {
						ctx.dataPacket.additionalModelRequestFields = { marker: "WRAPPED" };
						return handler();
					}
				});
				text = "";
				provider.chatStream(req,(chunk)=>{
					if( isArray(chunk.choices ?: "") && chunk.choices.len() ) {
						text &= (chunk.choices.first().delta.content ?: "");
					}
				});
				""", context);
			// @formatter:on
			assertThat( variables.getAsString( Key.of( "text" ) ) ).isEqualTo( "wired" );
			assertThat( body.get() ).contains( "WRAPPED" );
			variables.put( Key.of( "wireBody" ), body.get() );
			runtime.executeSource( "actual = jsonDeserialize(wireBody).additionalModelRequestFields.marker;", context );
			assertThat( variables.getAsString( Key.of( "actual" ) ) ).isEqualTo( "WRAPPED" );
		} finally {
			server.stop( 0 );
		}
	}

	@Test
	@DisplayName( "countTokens ignores a previous call's InvokeModel fallback stick and counts the Converse body" )
	public void countTokensIgnoresPreviousFallbackStick() throws Exception {
		AtomicReference<String>	body	= new AtomicReference<>();
		HttpServer				server	= HttpServer.create( new InetSocketAddress( "127.0.0.1", 0 ), 0 );
		server.createContext( "/", exchange -> {
			body.set( new String( exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8 ) );
			byte[] response = "{\"inputTokens\":42}".getBytes( StandardCharsets.UTF_8 );
			exchange.getResponseHeaders().set( "Content-Type", "application/json" );
			exchange.sendResponseHeaders( 200, response.length );
			exchange.getResponseBody().write( response );
			exchange.close();
		} );
		server.start();
		try {
			variables.put( Key.of( "testEndpoint" ), "http://127.0.0.1:" + server.getAddress().getPort() );
			// The chat() leg never touches the network: the middleware answers the Converse call
			// with the ValidationException that triggers the one-shot InvokeModel fallback, then
			// answers the invoke leg with a canned Claude body. That leaves "invoke" STUCK on the
			// request. countTokens() must resolve the API with honourRuntimeDecision=false, because
			// the next logical call clears the stick before it resolves — counting an InvokeModel
			// body here would report a number for a request that will never be sent.
			// @formatter:off
			runtime.executeSource("""
				provider = aiService("bedrock", {region:"us-east-1",baseURL:testEndpoint,bearerToken:"test-token"});
				req = aiChatRequest(aiMessage().user("count me"), {model:"anthropic.claude-3-sonnet-20240229-v1:0"}, {provider:"bedrock"});
				req.addMiddleware({
					wrapLLMCall: (ctx,handler) => {
						if( !( ctx.fallbackRetry ?: false ) ) {
							return { "error": {
								"message"   : "Bedrock request failed with status 400: ValidationException: The model does not support Converse.",
								"statusCode": 400
							} };
						}
						return { "content": [ { "type": "text", "text": "ok" } ], "stop_reason": "end_turn" };
					}
				});
				answer   = provider.chat(req);
				stuckApi = provider.resolveBedrockApi(req, "anthropic.claude-3-sonnet-20240229-v1:0");
				count    = provider.countTokens(req);
				""", context);
			// @formatter:on
			assertThat( variables.getAsString( Key.of( "answer" ) ) ).isEqualTo( "ok" );
			assertThat( variables.getAsString( Key.of( "stuckApi" ) ) ).isEqualTo( "invoke" );
			assertThat( variables.getAsInteger( Key.of( "count" ) ) ).isEqualTo( 42 );
			variables.put( Key.of( "wireBody" ), body.get() );
			runtime.executeSource(
			    "wire = jsonDeserialize(wireBody); usesConverse = wire.input.keyExists('converse'); usesInvoke = wire.input.keyExists('invokeModel');",
			    context );
			assertThat( variables.getAsBoolean( Key.of( "usesConverse" ) ) ).isTrue();
			assertThat( variables.getAsBoolean( Key.of( "usesInvoke" ) ) ).isFalse();
		} finally {
			server.stop( 0 );
		}
	}
}
