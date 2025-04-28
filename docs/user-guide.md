# Qubership ITF Lite User Guide

**ITF Lite** allows us to quickly create, execute and test results of REST and SOAP requests.

This provides QA engineer a possibility to perform functional testing directly in the ATP system with additional integrations and other convenient features such as:

- import/export request using cURL format
- integration with environment variables
- authentication settings
- request execution history
- pre/post scripts
- etc

### Core Entities

| Term                                                                                                                                                 | Decription                                                                                               |
|------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| Folder                                                                                                                                               | Serves as a container for requests and child folders. May contain authorization and permission settings. |
| Request                                                                                                                                              | Represents a request and underlying data.                                                                |
| HttpRequest                                                                                                                                          | Represents a HTTP request and underlying data.                                                           |
| RequestHeader                                                                                                                                        | Represents a HTTP request header. Acts as part of `HttpRequest`.                                         |
| RequestParam                                                                                                                                         | Represents a HTTP request parameter. Acts as part of `HttpRequest`.                                      |
| Cookie                                                                                                                                               | Represents a HTTP Cookie.                                                                                |
| HttpMethod                                                                                                                                           | Lists all HTTP methods supported by the system for executing.                                            |
| RequestAuthorization                                                                                                                                 | Represents a request authorization. The concrete implementation is extended by the corresponding class.  |
| BasicRequestAuthorization, BearerRequestAuthorization, InheritFromParentRequestAuthorization, OAuth1RequestAuthorization, OAuth2RequestAuthorization | Implementations of supported request authorization protocols or approaches.                              |
| RequestExecution                                                                                                                                     | Represents a request execution history data.                                                             |

## ITF Lite service
The `ITF Lite` service is responsible for interacting with and validating APIs through a convenient user interface. It enables developers and testers to construct, send, and analyze HTTP requests and responses in real-time. The service ensures a seamless workflow across several critical capabilities:

### **Construct API Requests:**
ITF Lite allows users to build and customize HTTP requests using various methods such as GET, POST, PUT, DELETE, PATCH etc. Users can define headers, query parameters, path variables, request bodies (JSON, XML, etc.), and authentication settings with ease. This feature provides complete flexibility for interacting with APIs under different scenarios.

### **Send Requests and View Responses:**
Users can send constructed requests to target APIs and instantly view detailed response information. This includes status codes, headers, response bodies, and timing details. The service supports a wide range of content types and enables users to validate responses against expected outcomes.

### **Save and Organize Requests:**
ITF Lite enables users to save requests into named collections or folders for reuse and better organization. This is especially useful for testing suites, regression scenarios, or collaboration between team members working on the same API set.

### **Environment Management:**
To simplify testing across different deployment environments (e.g., dev, QA, staging, prod), ITF Lite allows users to define and manage environment variables. These can be used in request URLs, headers, and bodies, providing dynamic substitution and reducing duplication.

### **Assertions and Tests:**
The service provides the ability to write JavaScript-based tests that run before the request executed and after a response is received. These tests can assert status codes, check values in the response body, validate headers, and perform other automated checks. Test results are presented clearly, making it easy to detect regressions or anomalies.

### **Request History and Replay:**
All sent requests are automatically recorded in the history log. Users can quickly view, re-send, or duplicate past requests, improving productivity during debugging or iterative development cycles.

### **Import and Export Collections:**
ITF Lite supports the import and export of request collections in standard formats (e.g., Postman Collection v2). This allows teams to share their test cases, migrate between tools, or maintain version control over test assets.

### **Authentication Support:**
ITF Lite includes built-in support for various authentication methods, including Basic Auth, Bearer Tokens, OAuth 1.0, OAuth 2.0, and more. Users can configure and persist auth settings per request or environment.

### **Context Variables Support:**
ITF Lite supports context variables—temporary values that are defined or updated at runtime. These can be populated from response data, scripts, or external sources and then reused in subsequent requests within the same testing session. This is especially useful for chaining requests and dynamic flows.