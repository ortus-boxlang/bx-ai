package ortus.boxlang.ai.providers;

import static com.google.common.truth.Truth.assertThat;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

public class BedrockReviewRegressionTest extends BaseIntegrationTest {

	@Test
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
		assertThat(variables.getAsString(Key.of("actual"))).isEqualTo("SAFE|NATIVE|SAFE|DEFAULT|SAFE|NATIVE|SAFE|DEFAULT");
	}

	@Test
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
		assertThat(variables.getAsString(Key.of("actual"))).isEqualTo("SAFE");
	}

	@Test
	public void countTokensEncodesInvokeBodyOnWire() throws Exception {
		AtomicReference<String> body = new AtomicReference<>();
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			byte[] response = "{\"inputTokens\":17}".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, response.length);
			exchange.getResponseBody().write(response);
			exchange.close();
		});
		server.start();
		try {
			variables.put(Key.of("testEndpoint"), "http://127.0.0.1:" + server.getAddress().getPort());
			// @formatter:off
			runtime.executeSource("""
				provider = aiService("bedrock", {region:"us-east-1",baseURL:testEndpoint,bearerToken:"test-token"});
				req = aiChatRequest(aiMessage().user("Count café tokens"), {model:"anthropic.claude-3-sonnet-20240229-v1:0"}, {provider:"bedrock",providerOptions:{bedrockApi:"invoke"}});
				count = provider.countTokens(req);
				""", context);
			// @formatter:on
			assertThat(variables.getAsInteger(Key.of("count"))).isEqualTo(17);
			variables.put(Key.of("wireBody"), body.get());
			runtime.executeSource("decoded = jsonDeserialize(charsetEncode(toBinary(jsonDeserialize(wireBody).input.invokeModel.body), 'UTF-8')); actual = decoded.messages[1].content;", context);
			assertThat(variables.getAsString(Key.of("actual"))).isEqualTo("Count café tokens");
		} finally {
			server.stop(0);
		}
	}
}
