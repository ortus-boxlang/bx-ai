package ai.tests;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.boxlang.box.BxEngine;
import ai.models.middleware.AiMiddlewareResult;

@TestMethodOrder(OrderAnnotation.class)
public class AiMiddlewareResultTest {

	@BeforeAll
	public static void setUp() {
		BxEngine.loadModule("bx-ai"); // Load your module
	}

	@Test
	@Order(1)
	@DisplayName("Test continue() factory")
	void testContinue() {
		AiMiddlewareResult result = AiMiddlewareResult.continue();
		assertEquals("continue", result.getType());
		assertTrue(result.isContinue());
		assertFalse(result.isTerminal());
	}

	@Test
	@Order(2)
	@DisplayName("Test cancel() factory")
	void testCancel() {
		AiMiddlewareResult result = AiMiddlewareResult.cancel("User cancelled");
		assertEquals("cancel", result.getType());
		assertEquals("User cancelled", result.getReason());
		assertTrue(result.isCancelled());
		assertTrue(result.isTerminal());
	}

	@Test
	@Order(3)
	@DisplayName("Test approve() factory")
	void testApprove() {
		AiMiddlewareResult result = AiMiddlewareResult.approve();
		assertEquals("approve", result.getType());
		assertTrue(result.isApproved());
	}

	@Test
	@Order(4)
	@DisplayName("Test reject() factory")
	void testReject() {
		AiMiddlewareResult result = AiMiddlewareResult.reject("Dangerous tool");
		assertEquals("reject", result.getType());
		assertEquals("Dangerous tool", result.getReason());
		assertTrue(result.isRejected());
		assertTrue(result.isTerminal());
	}

	@Test
	@Order(5)
	@DisplayName("Test edit() factory")
	void testEdit() {
		struct args = new struct();
		args.put("toolArgs", "{newArgs:true}");
		AiMiddlewareResult result = AiMiddlewareResult.edit(args);
		assertEquals("edit", result.getType());
		assertTrue(result.isEdit());
		assertEquals("{newArgs:true}", result.getData().get("toolArgs"));
	}

	@Test
	@Order(6)
	@DisplayName("Test suspend() factory")
	void testSuspend() {
		struct pending = new struct();
		pending.put("toolName", "deleteOrder");
		pending.put("toolArgs", "{id:42}");
		pending.put("toolCallId", "call_123");
		pending.put("question", "Delete order 42?");

		AiMiddlewareResult result = AiMiddlewareResult.suspend(pending);
		assertEquals("suspend", result.getType());
		assertTrue(result.isSuspended());
		assertTrue(result.isTerminal());
		assertEquals("deleteOrder", result.getData().get("toolName"));
	}

	@Test
	@Order(7)
	@DisplayName("Test isTerminal() logic")
	void testIsTerminal() {
		assertFalse(AiMiddlewareResult.continue().isTerminal());
		assertTrue(AiMiddlewareResult.cancel().isTerminal());
		assertTrue(AiMiddlewareResult.reject().isTerminal());
		assertTrue(AiMiddlewareResult.suspend(new struct()).isTerminal());
		assertFalse(AiMiddlewareResult.approve().isTerminal());
		assertFalse(AiMiddlewareResult.edit(new struct()).isTerminal());
	}
}