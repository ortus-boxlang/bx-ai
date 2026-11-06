---
description: Sign in to an MCP server that is protected with OAuth, for one account or for every person who talks to your agent
icon: key
---

# 🔑 MCP OAuth

Many MCP servers (mail, calendars, issue trackers) do not take a fixed key. They ask each person to **sign in** with OAuth, and then they accept an access token that expires and is renewed with a refresh token. BoxLang AI follows the [MCP authorization specification](https://modelcontextprotocol.io/specification/latest/basic/authorization) so that you do not have to.

You turn it on with `withOAuth()`. After that the client finds the authorization server, gets a client id, builds the sign in address, trades the code for tokens, sends the token on every request and refreshes it when it expires. Your code does two things: send the person to the address, and receive them back at a **callback route** that you own.

!!! note
    OAuth is for the HTTP transport. A stdio server is a program you launch and it gets its credentials from its environment (see [MCP Stdio Client](mcp-stdio-client.md)).

## 🚀 Quick Start

```javascript
client = MCP( "https://mail.example.com/mcp" ).withOAuth( {
    redirectUri : "https://app.example.com/oauth/callback"
} )

// 1. Start the sign in and send the person to the address
started = client.startAuthorization( "default" )
location( started.authorizationUrl )
```

At your callback route, when the authorization server sends the person back:

```javascript
// https://app.example.com/oauth/callback
signedIn = client.completeAuthorization( url.code, url.state, url.iss ?: "" )
```

From now on the client is signed in:

```javascript
tools    = client.listTools()
response = client.send( "inbox", {} )
```

The tokens were saved in a [store](#-token-stores). By default that is the BoxLang cache, in memory, so they are gone when the process stops. Use a `file` or `jdbc` store, or one of your own, to keep them.

## 🔄 How It Works

1. **Discovery.** The MCP server publishes *Protected Resource Metadata* ([RFC 9728](https://datatracker.ietf.org/doc/html/rfc9728)) that names its authorization server. The client reads it from the address in a `401` answer's `WWW-Authenticate` header or from the `/.well-known/oauth-protected-resource` addresses. It then reads the authorization server's metadata ([RFC 8414](https://datatracker.ietf.org/doc/html/rfc8414) or OpenID Connect discovery) and checks that the `issuer` in it is the one it asked for.
2. **A client id.** In this order: the `clientId` you gave, a Client ID Metadata Document when you gave a `clientMetadataUrl` and the server supports them, then Dynamic Client Registration. A registration is kept in the store, so the client registers once.
3. **The sign in.** `startAuthorization()` makes a PKCE secret (S256) and a `state`, saves them in the store for ten minutes, and returns the address of the authorization server's sign in page with the `resource` parameter ([RFC 8707](https://www.rfc-editor.org/rfc/rfc8707.html)) naming the MCP server.
4. **The callback.** `completeAuthorization()` finds the sign in by its `state` (a state can be used once), checks the `iss` the server sent back ([RFC 9207](https://datatracker.ietf.org/doc/html/rfc9207)), trades the code for tokens and saves them.
5. **Every request** carries `Authorization: Bearer <access token>`. A token that is within a minute of expiring is refreshed first, one refresh at a time per person. A `401` for a token that looked valid refreshes it and tries the call once more.

## ⚙️ Options

```javascript
MCP( url ).withOAuth( {
    redirectUri : "https://app.example.com/oauth/callback",
    store       : "cache",
    subject     : ( context ) => context.chatRequest.getUserId(),
    scopes      : [ "mail.read" ]
} )
```

| Option | Default | What it does |
|---|---|---|
| `redirectUri` | | The address of your callback route. Needed to sign in |
| `store` | `"cache"` | Where tokens live: `"cache"`, `"file"`, `"jdbc"`, a class path, or an `IMCPTokenStore` instance |
| `storeOptions` | `{}` | Options of the store: `{ cacheName }`, `{ path }`, `{ datasource, table }`, or the arguments of your class |
| `subject` | `"default"` | Whose tokens a call uses: a name, or a function `( context ) => string` |
| `scopes` | | Scopes to ask for. When left out: the scope the server asked for in its `401`, else everything it supports |
| `clientId`, `clientSecret` | | A client you registered with the authorization server yourself |
| `clientMetadataUrl` | | The HTTPS address of your Client ID Metadata Document |
| `clientName` | `"bx-ai"` | The name shown at sign in when the client registers itself |

## 👤 Whose Tokens

One client can serve many people. The `subject` option says whose tokens each call uses.

```javascript
client = MCP( "https://mail.example.com/mcp" ).withOAuth( {
    redirectUri : "https://app.example.com/oauth/callback",
    subject     : ( context ) => context.chatRequest.getUserId()
} )

client.startAuthorization( "alice" )        // Alice signs in
client.isAuthorized( "alice" )              // true once she has
client.isAuthorized( "bob" )                // false: Bob has not signed in
```

- `send( tool, args, context )` takes a `context` struct. The tools an agent builds from an MCP server (`MCPTool`) pass `{ chatRequest }`, so `context.chatRequest.getUserId()` is the person talking to the agent.
- A call with no context, such as `listTools()` when the agent starts, uses the subject `"default"`. For a server that needs a sign in to list its tools, sign in `"default"` first or use a fixed subject.
- With `withHandshake( true )` each person also gets an MCP session of their own.

## 🗄️ Token Stores

The client keeps three kinds of records in a store: tokens, the client registration and the sign ins waiting for their callback. Three stores ship:

| Store | Where | Options |
|---|---|---|
| `cache` (default) | A BoxLang cache. In memory with the default cache, durable with one backed by Redis or a file | `{ cacheName }` |
| `file` | One JSON file per record, owner-only permissions where the system has them | `{ path }` (required) |
| `jdbc` | A table, created when missing (`VARCHAR(8000)` records) | `{ datasource, table }` |

!!! warning
    The `file` and `jdbc` stores keep tokens as **plain text**, and the cache keeps them as it keeps anything. Protect the folder or table, or write a store that encrypts.

### Writing your own

A store is three methods. A store that encrypts seals the struct in `set()` and opens it in `get()`.

```javascript
class implements="bxModules.bxai.models.mcp.oauth.IMCPTokenStore" {

    function init( required any vault ) {
        variables.vault = arguments.vault
        return this
    }

    struct function get( required string key ) {
        var sealed = variables.vault.read( arguments.key )
        return isNull( sealed ) ? {} : jsonDeserialize( variables.vault.open( sealed ) )
    }

    void function set( required string key, required struct value, numeric ttlSeconds = 0 ) {
        variables.vault.write( arguments.key, variables.vault.seal( jsonSerialize( arguments.value ) ), arguments.ttlSeconds )
    }

    void function remove( required string key ) {
        variables.vault.delete( arguments.key )
    }

}
```

`get()` returns an empty struct when there is nothing (or the record has expired). `ttlSeconds` of `0` means never. Keys are opaque strings. Pass the class as `store: "my.Store"` with `storeOptions`, or an instance.

## 🤖 Agents

Put `oauth` in the server's config:

```javascript
agent = aiAgent(
    name       : "mail-agent",
    model      : aiModel( provider: "openai" ),
    mcpServers : [
        {
            url   : "https://mail.example.com/mcp",
            oauth : { redirectUri: "https://app.example.com/oauth/callback", subject: ( context ) => context.chatRequest.getUserId() }
        }
    ]
)
```

An agent lists the server's tools when it starts. If nobody has signed in yet the listing fails, the failure is logged, and the agent starts with no tools from that server. After signing in, hand the signed-in client to the agent again:

```javascript
client = MCP( url ).withOAuth( { ... } )
// ... sign in ...
agent.withMCPServer( client )
```

## ⚠️ When Someone Must Sign In

A call that cannot go through because the person has no usable tokens returns a failed `MCPResponse` (nothing is sent to the server) and fires `onMCPAuthorizationRequired`. Use the event to show a sign in:

```javascript
BoxRegisterInterceptor( ( event ) => {
    // event.subject, event.reason, event.baseURL, event.scope, event.client
    link = event.client.startAuthorization( event.subject ).authorizationUrl
}, "onMCPAuthorizationRequired" )
```

| `reason` | Means |
|---|---|
| `no_token` | There are no tokens, or they were refused when refreshed |
| `unauthorized` | The server answered `401` even for a fresh token |
| `insufficient_scope` | The server answered `403` and wants more scope. Read `event.scope`, add it to `scopes` and sign in again |

The methods you call yourself throw typed errors:

| Type | When |
|---|---|
| `MCPOAuthNotEnabled` | An OAuth method on a client without `withOAuth()` |
| `MCPOAuthMissingRedirectUri` | `startAuthorization()` without a `redirectUri` |
| `MCPOAuthDiscoveryFailed` | No resource metadata, no usable authorization server metadata, or no PKCE `S256` |
| `MCPOAuthNoClient` | No `clientId`, and the server has neither Client ID Metadata Documents nor registration |
| `MCPOAuthRegistrationFailed` | The authorization server refused to register the client |
| `MCPOAuthInvalidState` | The callback `state` is unknown, used or older than ten minutes |
| `MCPOAuthIssuerMismatch` | The `iss` of the callback is not the server the sign in started with |
| `MCPOAuthTokenError` | The authorization server refused the code |
| `MCPOAuthInsecureUrl` | An address that is not `https` (plain `http` is allowed to `localhost` only) |
| `MCPAuthorizationRequired`, `MCPOAuthRefreshFailed` | Inside a call: there are no usable tokens, or the authorization server could not be reached to refresh them. A call turns these into a failed response |

## 🔐 Security

- Every address in the flow must be `https`, except `localhost` and `127.0.0.1` for development.
- PKCE with `S256` is always used, and an authorization server that does not list it is refused.
- The `resource` parameter is sent on the authorization and the token request, so a token is for that MCP server.
- A `state` is used once and expires. The `iss` of the callback is compared before the code goes anywhere.
- The access token is sent only to the MCP server the client was built for. The refresh token goes only to the authorization server's token endpoint.
- Tokens are credentials: they are never in the `MCPResponse`, but they are in the store. See [Token Stores](#-token-stores).

## 📡 Events

| Event | When | Data |
|---|---|---|
| `onMCPAuthorizationRequired` | A person has to sign in or grant more scope | `client`, `baseURL`, `subject`, `reason`, `challenge`, `scope` |

The existing `onMCPClientRequest`, `onMCPClientResponse` and `onMCPClientError` fire as before.

## 🧯 Troubleshooting

- **"Could not find the Protected Resource Metadata"**: the server does not publish it. Servers that predate the authorization specification are not supported.
- **"does not say it supports PKCE with S256"**: the authorization server's metadata has no `code_challenge_methods_supported` with `S256`. MCP requires it.
- **"has no registration endpoint and no client id was given"**: register an OAuth client with the provider and pass `clientId` (and `clientSecret` if it has one).
- **A person signed in and has to sign in again after a restart**: the default cache store is in memory. Use `file`, `jdbc` or a durable cache.
- **The refresh fails every time**: the provider may issue refresh tokens only when you ask for an offline scope. Add it to `scopes`.
