# SCSL Version 1.0: Service Contract Specification Language

## Abstract

SCSL is a language for the definition of (micro)service contracts.

## Terminology and Conformance Language

Normative text describes one or both of the following kinds of elements:

* Vital elements of the specification
* Elements that contain the conformance language key words as defined by [IETF RFC 2119](https://www.ietf.org/rfc/rfc2119.txt) "Key words for use in RFCs to Indicate Requirement Levels"

Informative text is potentially helpful to the user, but dispensable. Informative text can be changed, added, or deleted editorially without negatively affecting the implementation of the specification. Informative text does not contain conformance keywords.

All text in this document is, by default, normative.

The key words "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", and "OPTIONAL" in this document are to be interpreted as described in [IETF RFC 2119](https://www.ietf.org/rfc/rfc2119.txt) "Key words for use in RFCs to Indicate Requirement Levels".

## Definitions and Terminology

### General

In this specification, **API definition** means an API using this specification.

**SCSL Specification** refers to this document.

**REST** is used in the context of an API implemented using some or all of the principles of REST (Representational State Transfer), which was introduced and first defined in 2000 in Chapter 5, [REST](http://www.ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm), of the doctoral dissertation *"Architectural Styles and the Design of Network-based Software Architecture"* by Roy Fielding.

A **resource** is the conceptual mapping to an entity or set of entities.

A trailing question mark, for example **description?**, indicates an optional property, a trailing asterisk, for example **methods\***, indicates a repeatable property (0 or more), and trailing asterisk, for example **string+**, indicates a repeatable property (1 or more) 

### Markdown

Throughout this specification, **Markdown** means [GitHub-Flavored Markdown](https://help.github.com/articles/github-flavored-markdown/).

## Table of Content

<!-- TOC -->

- [Introduction](#introduction)
- [Markup Language](#markup-language)
- [The Root of the Document](#the-root-of-the-document)
- [Protobuf Schema](#protobuf-schema)
    - [Validations](#validations)
    - [Documentation](#documentation)
- [Resources and Nested Resources](#resources-and-nested-resources)
- [Methods](#methods)
    - [Responses](#responses)
- [Security](#security)
- [Modularization](#modularization)
	- [Includes](#includes)

<!-- /TOC -->

## Introduction

This specification describes the Service Contract Specification language (SCSL). SCSL is a human- and machine-readable language for the definition of a RESTful application programming interface (API). SCSL is designed to improve the specification of the API by providing a format that the API provider and API consumers can use as a mutual contract. SCSL can, for example, facilitate providing user documentation and source code stubs for client and server implementations. Such provisions streamline and enhance the definition and development of interoperable applications that utilize RESTful APIs.

SCSL is inspired, and in many ways follows, the RAML (http://raml.org/) specification. However, SCSL is not aimed at an HTTP based implementation, but rather, promotes the use of a lighter weight, binary protocol based on Google's protobuf (https://developers.google.com/protocol-buffers/).

This document is organized as follows:

* **Basic Information**. How to describe core aspects of the API, such as its name, title, location (or URI), and defaults and how to include supporting documentation for the API.
* **Resources**. How to specify API resources and nested resources, as well as URI parameters in any URI templates.
* **Methods**. How to specify the methods on API resources and their request headers, query parameters, and request bodies.
* **Responses**. The specification of API responses, including status codes, media types, response headers, and response bodies.
* **Includes**. How an API definition can consist of externalized definition documents.

## Markup Language

This specification uses [YAML 1.2](http://www.yaml.org/spec/1.2/spec.html) as its underlying format. YAML is a human-readable data format that aligns well with the design goals of this specification. As in YAML, all nodes such as keys, values, and tags, are case-sensitive.

SCSL API definitions are YAML 1.2-compliant documents that begin with a REQUIRED YAML-comment line that indicates the SCSL version, as follows:

```yaml
#%SCSL 1.0
title: My API
```

The first line of a SCSL API definition document MUST begin with the text _#%SCSL_ followed by a single space followed by the text _1.0_ and nothing else before the end of the line. SCSL fragment documents begin similarly with the SCSL version comment and a [fragment identifier](#fragments), but are not in themselves SCSL API definition documents.

To facilitate the automated processing of SCSL documents, SCSL imposes the following restrictions and requirements in addition to the core YAML 1.2 specification:

* The first line of a SCSL file consists of a YAML comment that specifies the SCSL version. Therefore, SCSL processors cannot completely ignore all YAML comments.

## The Root of the Document

The root section of the SCSL document describes the basic information about an API, such as its name and version. 

Nodes in a SCSL-documented API definition MAY appear in any order.

The following table enumerates the possible nodes at the root of a SCSL document:

| Name         | Description 
|:----         |:-----------
| title?       | A short, plain-text label for the API. Its value is a string and default to *name*.
| description? | A substantial, human-friendly description of the API. Its value is a string and MAY be formatted using [markdown](#markdown).
| name         | The name of this service. Must uniquely identify a single service in the cluster of services. Its value is a string comprised of lowercase letters a-z, numbers and underscores. It must begin with a letter and cannot have whitespace.
| version?     | The major version of the API, for example 1. Its value is a number and the default version is 0.
| spi?         | Indicates whether this is an API or an SPI. Its value is a boolean and the default is false.
| schema       | The relative path to the protobuf schema file.
| basePackage  | The base package of this API for code generation and matching with protobuf messages. Messages defined in this API are matched against this package using protobuf rules as described in [https://developers.google.com/protocol-buffers/docs/proto3#packages-and-name-resolution].
| context      | The protobuf message used as the context. The context represents the information known at runtime and is equivalent to headers when working with HTTP.
| constants?   | A map of constants. Keys are strings comprised of lowercase letters a-z, numbers and underscores, must begin with a letter and cannot have whitespace. Values are strings.
| security?    | The security attribute.
| method*      | The root resource [methods](#methods).
| resource*    | Nested resources.

## Protobuf Schema

The API schema is defined using Google protobuf schema syntax, including validation definitions.

For opaque objects, it is recommended to use `bytes` instead of `google.protobuf.Any` to avoid tying the API to specific messages. `Any` serialises the fully qualified message name with the payload, making refactoring the message name or package backward incompatible. For gateways, opaque messages should be avoided and specific endpoints provided with specific messages.

The schema points back to the scsl file using the **scsl.location** custom option, importable from the scsl.proto definitions. This is such that processing of scsl can be executed as a protoc plugin.

### Validations

Validations are added by importing the valid.proto definitions. Field validations are defined using the **valid.field**, **valid.values** and **valid.keys** custom options, while message validations are added using the **valid.message** custom option. 
 
Note than, as map key types, protobuf permits integer types, strings and booleans as keys. However, scsl does not allow booleans as key type as this scenario is better served with two fields (one for true and one for false). 
In addition, protobuf does not permit enums as key types, so scsl allows specifying this using **valid.enumeration**, in which case the key is treated as an enum key in the specified enumeration rather than a string type.
 
| Field Validation | Parameters                               | Applicability        | Description
|:---------------- |:----------                               |:-------------        |:-----------
| required         | -                                        | all types (not keys) | Requires the field to have a non-default value. For numbers the default is 0, for boolean it is false, for string, bytes, lists and maps it is empty and for messages it is the default instance.
| has_text         | -                                        | string               | Requires the string to have text, not just whitespace.
| requires         | string+                                  | all types            | Indicates that this field depends on other fields. The parameter specifies the names of the required fields
| size             | int or _; int or _                       | string, list, map    | Specifies the inclusive size bounds. One of the bounds can be omitted by specifying _, e.g. size(2, _)
| range            | number, string or _, number, string or _ | number               | Specifies the inclusive range bounds. one of the bounds can be omitted by specifying _, e.g. range(2, _). Another number field may be used as the upper or lower bounds, e.g. range(from, _)
| exc_range        | number, string or _, number, string or _ | number               | Specifies the exclusive range bounds. one of the bounds can be omitted by specifying _, e.g. exc_range(2, _). Another number field may be used as the upper or lower bounds, e.g. exc_range(from, _)
| in               | number+, enum_key+ or string+            | number, enum, string | Specifies the allowed values. Numbers for number fields, enum constants for enum fields, quotes string enclosed in single quotes (') for string fields.
| not_in           | number+, enum_key+ or string+            | number, enum, string | Specifies the disallowed values. Numbers for number fields, enum constants for enum fields, quotes string enclosed in single quotes (') for string fields. 
| regex            | string                                   | string               | Requires the string to match the given regular expression

| Message Validation | Parameters   | Description
|:------------------ |:----------   |:-----------
| exactly            | int; string+ | Requires exactly n (first parameter) of the given field names.
| at_least           | int; string+ | Requires at least n (first parameter) of the given field names.
| at_most            | int; string+ | Requires at most n (first parameter) of the given field names.
| all_or_none        | string+      | Requires either all or none of the given field names.

The following is an example proto file using validations

```
syntax = "proto3";

package some.test;

import "scsl.proto";
import "valid.proto";

option optimize_for = SPEED;
option (scsl.location) = "example.scsl";
        
message TestMessage {

  enum Props {
    A = 0;
    B = 1;
    C = 2;
  }

  string name = 1 [(valid.field) = "size(5, 10)"];
  int32 count = 2 [(valid.field) = "range(2, 100)"];
  map<string, string> properties = 3 [(valid.enumeration) = "Props", (valid.values) = "required size(_, 10)"];
    
  option (valid.message) = "at_least(1, name, count)";
}
```

### Documentation

The Protobuf schema definitions can also be documented, so that clients of the API can view these descriptions.

Descriptions are added by importing the `description.proto` definitions. Descriptions at the following levels are supported:
  - Message level descriptions, using the `description.message` option
  - Field level descriptions, using the `description.field` option
  - Enum message level descriptions, using the `description.enumeration` option
  - Enum value level descriptions, using the `description.value` option

The following is an example proto file using descriptions

```
syntax = "proto3";

package some.test;

import "scsl.proto";
import "description.proto";

option optimize_for = SPEED;
option (scsl.location) = "example.scsl";
        
message TestMessage {

  option (description.message) = "A message used for testing purposes";

  enum Props {
    option (description.enumeration) = "Enum describing the possible properties that can be used";
    A = 0 [(description.value) = "Property A"];
    B = 1 [(description.value) = "Property B"];
    C = 2 [(description.value) = "Property C"];
  }

  string name = 1 [(description.field) = "The name of something"];
  repeated Props supported_properties = 2 [(description.field) = "The supported properties of something"];
}
```

## Resources and Nested Resources

The API itself, as well as every node whose key begins with a slash, is a resource node.

The API itself is the top-level resource. The path to this top level resource is an empty string key (""). A resource defined as a child node of another resource is called a nested resource. A nested resource is identified by its relative path, which MUST begin with a slash ("/").

Nested resources may be either a static path or a parametrised path. A static path is a string comprised of lowercase letters a-z, numbers and underscores. It must begin with a letter and cannot have whitespace. A parametrised path must be defined as a string following the same rules as a static path, enclosed in curly brackets {} and must define the parameter type (see next section). A nested resource can itself have a child (nested) resource, creating a multiply-nested resource.

This example shows an API definition with one nested resource, /items that has a nested resource, /{id} of type int64; and the nested resource, /{id}, has two nested resources, /keys, /notes.

```
#%SCSL 1.0
name: example
schema: example.proto
basePackage: some.example
context: ExampleContext
/items:
  /{id}:
    parameter: int64
    /keys:
    /notes:
```

The value of a resource node is a map whose key-value pairs are described in the following table.

| Name         | Description
|:----         |:-----------
| parameter?   | Required for parametrised resources. Specifies the protobuf scalar type from [https://developers.google.com/protocol-buffers/docs/proto3#scalar].
| description? | A substantial, human-friendly description of a resource. Its value is a string and MAY be formatted using [markdown](#markdown).
| security?    | The security attribute.
| method*      | The root resource [methods](#methods)
| resource*    | Nested resources

## Methods

Methods aree declared inline, as a map where the key represents the name of the method and its value is a method declaration. Method names are strings comprised of lowercase letters a-z, numbers and underscores. It must begin with a letter and cannot have whitespace. 

A resource can define any number of methods. 2 methods have special meaning and semantics; **get** indicates a type of lookup with **NO SIDE EFFECTS** and has the same semantics as HTTP get; **watch** defines an observable collection of events, is only applicable to unparametrised paths and has no HTTP equivalent; any other method has the same semantics as HTTP post and can have side effects.

| Name         | Description
|:----         |:-----------
| description? | A longer, human-friendly description of the method in the context of the resource. Its value is a string and MAY be formatted using [markdown](#markdown).
| security?    | The security attribute.
| request?     | The protobuf message required as input. Not applicable to watch
| responses?   | The protobuf messages expected as responses to a request.

### Responses

The OPTIONAL **responses** node of a method on a resource describes the possible responses to invoking that method on that resource. 

| Name      | Description
|:----      |:-----------
| success?  | The protobuf message expected for successful (200, 201) responses.
| conflict? | The protobuf messages expected as responses to a request.

the above success status corresponds to HTTP 2xx statuses (specifically, 200 and 201) and the conflict status corresponds to the HTTP 409 status code. All other status codes assumess a standard error schema.  

The following example illustrates some possible responses:

```yaml
#%SCSL 1.0
name: example
schema: example.proto
basePackage: some.example
context: ExampleContext
/items:
  /{id}:
    parameter: int64
    get:
      responses:
        success: Item
    create:
      request: ItemCreate
      responses:
        success: Item
        conflict: ItemConflict
```

## Security

A security tag can be specified at the resource and method level. The security tag is applied to all sub resources and methods unless overridden.
 
TODO more detail on what this means (this may be removed)

## Modularization

SCSL provides a mechanism to help modularize an API specification by composing the API from included fragments.

### Includes

SCSL processors MUST support the OPTIONAL **!include** tag, which specifies the inclusion of external files into the API specification. Being a YAML tag, the exclamation point ("!") prefix is required. In an API specification, the !include tag is located only in a node value position. The !include tag MUST be the value of a node, which assigns the contents of the file named by the !include tag to the value of the node.

In the following example, the definition if the /nested subresource is retrieved from a file called nested.scsl and used as the value of the node.

```yaml
#%SCSL 1.0
name: example
schema: example.proto
basePackage: some.example
context: ExampleContext
/nested: !include nested.scsl
```

The !include tag accepts a single argument, the location of the content to be included, that MUST be specified explicitly. The value of the argument MUST be a path as described in the following table:

| Argument      | Description | Examples
|:-------       |:----------- |:--------
| absolute path | A path that begins with a single slash ("/") and is interpreted relative to the root SCSL file location. | /resources/nested.scsl
| relative path | A path that neither begins with a single slash ("/") nor constitutes a URL, and is interpreted relative to the location of the included file. | ../resources/other.scsl

To simplify the API definition, and because the parsing context of the included file is not shared between the file and its parent, an included file SHALL NOT use a YAML reference to an anchor in a separate file. Likewise, a reference made from a parent file SHALL NOT reference an anchor defined in an included file.

The !include tag argument must be static: namely, it MUST NOT contain any parameters.

A file to be included MUST begin with a SCSL fragment identifier line, which consists of the text _#%SCSL_ followed left-to-right by a single space, the text 1.0, a single space, and the text _Fragment_:

When SCSL fragments are included, SCSL parsers MUST NOT only read the content, but must also parse it and add the content to the declaring structure as if the content were declared inline. SCSL parsers MUST parse the content of the file as SCSL content and append the parsed structures to the SCSL document node.
