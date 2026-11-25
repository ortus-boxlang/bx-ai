"""Exercise a fresh JVM or native MCP fixture without a model or external packages."""
import argparse
import http.client
import json
from uuid import uuid4


def post(packet):
    connection = http.client.HTTPConnection("127.0.0.1", args.port, timeout=10)
    try:
        connection.request("POST", "/mcp.bxm", json.dumps(packet), {
            "Content-Type": "application/json", "Accept": "application/json"
        })
        response = connection.getresponse()
        return response.status, response.read()
    finally:
        connection.close()


def call(method, params):
    rpc_id = str(uuid4())
    status, body = post({"jsonrpc": "2.0", "id": rpc_id, "method": method, "params": params})
    if status != 200:
        raise RuntimeError(f"{method}: HTTP {status}")
    reply = json.loads(body)
    assert reply["jsonrpc"] == "2.0" and reply["id"] == rpc_id, reply
    return reply["result"]


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=18964)
    parser.add_argument("--tools-only", action="store_true", help="test tool behavior, not notification compliance")
    args = parser.parse_args()
    initialized = call("initialize", {"protocolVersion": "2024-11-05", "capabilities": {},
                                      "clientInfo": {"name": "local-probe", "version": "1"}})
    assert initialized["serverInfo"]["name"] == "http-probe" and "tools" in initialized["capabilities"]
    status, body = post({"jsonrpc": "2.0", "method": "notifications/initialized"})
    tools = call("tools/list", {})["tools"]
    recorder = next(tool for tool in tools if tool["name"] == "record")
    assert "label" in recorder["inputSchema"]["required"]
    for label, expected in [("first", "first"), ("second", "first,second")]:
        result = call("tools/call", {"name": "record", "arguments": {"label": label}})
        assert result["content"] == [{"type": "text", "text": expected}], result
    print("PASS: initialize, discovery and closure history (first → first,second) on separate connections")
    # Check this last so the known notification gap cannot mask working tool behavior.
    if not args.tools_only and (status != 202 or body):
        raise SystemExit(f"Streamable HTTP notification: expected HTTP 202 and no body; got {status} {body!r}")
