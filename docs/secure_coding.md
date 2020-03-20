# Secure Coding and Security Code Reviews

## Mindset
Important to know WHEN one needs this information, rather than knowing all this information.

Be aware of cases when secure coding is important:

- when handling card numbers (e.g. gps connector, managed cards)
- when working in edge services and deviating from standard data escaping (e.g. displaying rendered markdown, 
  displaying rich content like emails)
- when working with external systems protocols and deviating (or coming up with) standard channel guidelines 
  (e.g. native SQL instead of using jooq / hibernate, SSO protocol)
- when working on authentication or authorisation (e.g. modifying entity authorisation / ownership rules, 
  introducing a new authentication mechanism like 2FA TOTP)
- when working with cryptography / encrypting data (do not use ECB, use IV for potentially repeating data, 
  security store keys)

## Introduction
Application security applies to all the layers of an application, typically:

- presentation tier (web applications, api gateway, session management),
- the business logic tier (application logic)
- the data tier (code that interacts with the data stores, db schema, migration scripts, etc).

Implementing secure code is only part of an Application Security Management process, which should include 
processes and procedures throughout the whole Application Life-Cycle, to manage the risks related to the 
Application(s) being developed. The [OWASP Cheat Sheets](https://cheatsheetseries.owasp.org/) offer tips
for the design and review of an applicationâ€™s security architecture. [OWASP - The Open Web Application 
Security Project](https://owasp.org/) offers a wealth of information related to Web Application security.

The security analysis of a software application can be achieved through a number of techniques, with the aim of identifying security flaws:

- automated scanning
- manual penetration testing, 
- static analysis, 
- manual code review

This document focuses of secure coding and manual code reviewing to counteract the vulnerabilities which a Web Application may be facing, auditing the source code for to verify that:

- the proper security controls are present
- they work as intended
- they have been invoked in all the right places

Secure Code Review should be integrated in the software development life-cycle:

| Phase | Security |
| ------ | --------
| Requirements definition | Application Security Requirements
| Architecture and Design	| Application Security Architecture and/or Threat Model<br>Identify Security Stakeholder Stories<br>Identify Security Controls<br>Identify Security Test Cases
| Development | Secure Coding Practices<br>Security Testing<br>Security Code Review<br>Security Test Cases
| Deployment | Penetration Testing<br>Secure Configuration Management<br>Secure Deployment
| Maintenance | Patching

In depth information can be found in the [OWASP Web Security Testing Guide](https://owasp.org/www-project-web-security-testing-guide/)

### Threat modelling
[Application Thread modelling](https://owasp.org/www-community/Application_Threat_Modeling) is a structured approach for analysing the security of an application. Enables the *identification, quantification, and addressing of the security risks* associated with an application - determines the correct controls and produce effective countermeasures within budget.

Helps to identify

- the different threat Agents & their motivation
- the attack surface, including any public and back-end interfaces
- possible attacks
- required security controls (both to stop likely attacks and to meet corporate policy)
- potential technical impacts

## Security Principles
- **Minimize Attack Surface Area** automatically reduces overall risk. E.g. remove unused features, offer feature to authenticated users only, centralise validation, etc.
- **Secure by default** e.g. enable password complexity/ageing - user can switch it off at his own risk
- **Defense in Depth** layered security mechanisms increase security of the system as a whole & can make severe vulnerabilities extraordinarily difficult to exploit
    - presentation tier checks and requests authentication (if required) and checks basic authorisation, e.g. through HATEOAS links (fine grained GUI authorisation, optional, DO NOT ASSUME THIS WAS DONE)
    - API gateway tier validates and sanitises all user input and checks for repeat submission and CSRF
    - presentation tier escapes data before rendering to prevent XSS, uses safe ways to add content to presentation, NEVER eval() or .innerHTML user input
    - business logic tier checks authorisation for a particular operation (coarse grained, per business function). This may include checking for a particular user group and / or checking rights for an operation
    - business logic tier checks ownership of data requested (as required; this is finer grained that a right to an operation, since a user may be permitted to update his own personal details but should not even read another user details)
- **Principle of Least Privilege** accounts/entities have the least amount of privilege required to perform their business processes
- **Positive Security Model**
    - use a white-list approach, define what is allowed and reject anything else 
    - By default, all operations should be performed by the authenticated subject such that authorisation checks and data access are checked against the proper subject. Some operations may require the use of a subject with elevated privileges. This subject should be given up as soon as the operation is finished to avoid opportunities for privilege escalation.
- **Fail securely**
- **Obscurity is not Security** including source code kept secret.
- **Favour simplicity** including code and security controls
- **Fix Security Issues Correctly** including developing a test, root cause analysis, impact analysis, other similar issues

## Security Controls
The following is a list of things to look out when writing or reviewing code.

### Authentication
> - Verify an identity through the use of credentials and secrets
> - Deny access to attackers who use various methods to attack the authentication system

All internal and external connections should go through an appropriate and adequate form of authentication. Make sure authentication cannot be bypassed. Development / debug back-doors should not be present in production code.

Review unauthenticated pages - Any page outside the scope of authentication should be reviewed to assess any possibility of security breach. Such pages should also protect against denial of service attacks.

Authentication credentials should be protected at rest and in transit

- Use SSL for form based authentications to protect from Replay attacks, man in the middle
- Use derivative information to limit damage in case of breach, e.g. hashing for passwords
- Passwords should be salted before hashing (birthday paradox)

Authentication credentials (or any other sensitive information) should only only accepted via the POST as GET can leak this information in logs and intermediary servers.

Protocols used should be resistant to brute force, dictionary, and replay attacks.

Strong password policies should be enforced -  complexity requirements (uppercase / lowercase / digits / special char), min length, change frequency, min change period, history.

Use multi-factor authentication for high value systems

In general, the most appropriate form of authentication should be used - password / one-time password (challenge-response) / certificates / two-factor / federated authentication (e.g. OAuth) etc.

Protect against brute force password attack by adding delay (e.g. hash 1000 times) and account lockout. Protect against timing (side-channel) attacks.

Account lockouts should not result in a denial of service attack - i.e. attacker locks out legitimate users.

User is re-authenticated for high value transactions. (E.g. with 2FA, request second factor)

Require reauthentication after a threashold of idle time.

Sanitize credentials - HTML, SQL and LDAP safe (letters, numbers, digits and few symbols)

Disable autocomplete for sensitive HTML form input field to prevent browser from remembering such sensitive data.

not use default accounts - e.g. in db connection. Do not offer default account in software.

If user names / keys are generated, do not make them predictable.

Password reset mechanisms are much weaker that passwords. Password resets should not reveal password hints and valid usernames. Question can be found in public records/social engineering or illegal to collect. If using email, use temporary one-time token valid for short period. Protect email link from phishing. Notify user of password change. There are other ways, e.g. send new passwd in sealed envelope, helpdesk identification, etc.

Offer logout and remove-account (disable) to user.

### Authorization

> - Ensure users can only perform allowed actions within their privilege level
> - Control access to protected resources using decisions based upon role or privilege level
> - Prevent privilege escalation attacks, tricking the system to perform operations not authorised to the authenticated subject

The Authorization mechanisms should work properly, fail securely, and prevent circumvention.

The principle of least privilege should be followed, granting subjects the minimum privileges needed to perform legitimate tasks - limits damage if attacker acquires access & separation of concerns.

Where applicable, RBAC (role-based access controls) should be used to restrict access to specific operations - simpler administration; otherwise, ACL (access control list) may be used.  

Authorization should be checked on every request.

Authorization routines and mechanisms should be centralized - no repetitive code.

Do not use or trust any client-side authentication or authorization tokens (in header, cookie, hidden fields, etc)

Do not rely on hiding / disabling elements at client side on authorization - direct calls could be crafted.

Protect any static content (e.g. reports / emails) - **Never** rely on security through obscurity e.g. obscure unauthorised URL.

Leverage inbuilt authorization frameworks where possible. If custom code is used, test thoroughly - requires special attention.

### Session Management

> - Secure convenience mechanism to avoid authentication for every request
> - Prevent common web attacks, such as replay, request forging and man-in-the-middle attacks

Session token must contain as little private information as possible.

If cookies are used, they must be defined, including their name, and why they are needed. All cookies must be configured to expire.

Applications should avoid or prevent common session-related web attacks, such as replay, request forging (including CSRF), and man-in-the-middle.

Avoid session fixation (attacker sets victim's SID to one he knows).

Avoid session sidejacking (SID sniffed from cookie especially in public Wi-Fi) - only supply session over HTTPS

Session ID must not be predictable & long enough - use strong cryptographic algorithms & large key space.

Session must have an idle time-out.

Clear all session state if any and remove / invalidate / overwrite any residual cookies on user logout.

Application must safely handle invalid Session IDs and session invalidations.

Session management must be thread safe.

For stateless authentication, HTTP only cookie should be used for payload signature, to prevent XSS attacks from stealing token (javascript cannot access value), and Authorisation header should be used to revent CSRF (forged form submission does not automatically send Authentication header).

### Data/Input Validation

> - Ensure that the application is robust against all forms of input data, whether obtained from the user, infrastructure, external entities or database systems
> - Protect against injection vulnerabilities such as interpreter injection, locale / Unicode attacks, file system attacks and buffer overflows.

**Golden Rule: All external input, no matter what it is, must be examined and validated.**

Data Validation mechanism must be present in all tiers. Each should validate before using/parsing input - e.g.

- web tier should validate for web-related issues (CSS, HTML/CSS injection, etc)
- persistence layer should validate for issues such as SQL injection
- directory lookups should check for LDAP injection

Integrity checks must be included wherever data passes from a trusted to a less trusted boundary, such as from the application to the user's browser in a hidden field, or transaction ID returned to a third party payment gateway.

Business rules validation must be performed apart from validation & integrity checks - e.g. integer overflows, boundaries permitted by business, date ranges, etc.

All input (that can be modified by a malicious user) such as HTTP headers/bodies, URL parameters, input fields, hidden fields, environment variables, cookies, etc must be properly validated.

Care must be taken with Canonicalization when sanitizing/encoding - equivalent forms of a name - e.g. '.' = ASCII 2E = Unicode C0 AE etc.

The data validation must occurs on the server side.

There should be no back-doors in the data validation model.

Output encoding must be used. E.g. HTML encoding.

No security decision should be based upon parameters that can be manipulated (e.g. URL parameters).

The data must be well formed and contains only known good chars if possible.

Data Validation Strategies (in order of preference)

- Accept known good (white-list) - e.g. only 8 to 10 digits for phone number
- Reject known bad (black-list) - reject any unexpected characters - slow and not secure (many reg-exps in CSS cheat sheet)
- Sanitize - eliminate or translate characters (such as to HTML entities or to remove quotes) to make the input "safe" - many exceptions to the rule & where is the input going to be used? HTML, Javascript, CSS, SQL, LDAP, shell command?

Data should be:

- Strongly typed at all times
- Length checked and fields length minimized
- Range checked if a numeric
- Unsigned unless required to be signed
- Syntax or grammar should be checked prior to first use or inspection - Whenever coding to a particular technology, the "special" characters should be determined and prevented from appearing in input, or properly escaped.

### Error Handling / Information Leakage

> - Handle all errors that can happen in the application in a structure and secure way
> - Fail securely without leaking information

All method/function calls that return a value must have proper error handling and return value checking.

Exceptions and error conditions should be properly handled in a structured manner - using *try { } catch { }* as opposed to returning an error code from a function.

Error messages should be scrubbed so that no sensitive information is revealed to the attacker.
Do not leak information in detailed message which can help an attacker - e.g. revealing internals.


The application must fail in a secure manner - privileges should be restored to the appropriate level in case of errors / exceptions.

Resources should be released even if an error occurs.

### Logging / Auditing

> - *Auditable*, all activities that affect user state or balances are formally tracked
> - *Traceable*, possible to determine where an activity occurred in all tiers of the application
> - *High integrity*, logs cannot be overwritten or tampered with by local or remote users

Auditing and logging of all administration activities must be enabled.

No sensitive information must be logged ever, including cookies, authentication credentials, card numbers, personal details depending on legislation, etc.

The payload being logged must be of a defined maximum length and the logging mechanism must enforce that length. Protect against DOS.

Actions being taken by the application on behalf of the client (particularly data manipulation / Create, Update, Delete (CUD) operations) must be audited.

Successful and unsuccessful authentication must be logged.

All application errors must be logged.

Logs must be protected. Integrity checked, access controlled, backed up (e.g. on write once / read many devices like CD), presence verified (e.g. using cron job), properly disposed of.

Log in such a way for admin to be able to trace an intruder action. They may be used as forensic evidence. They can also be fed into an Intrusion Detection System for attack detection.

### Cryptography

No sensitive data (credentials, card numbers, personal details) should be stored / transmitted in the clear, internally or externally.

The application must implement known good cryptographic methods, and well tested libraries (e.g. Java JCE)
- Standard encryption algorithms and correct key sizes are being used to protect eavesdropping.
- Hashed message authentication codes (HMACs) are being used to protect data integrity.
- SecureRandom is used for random number generation.

Keys should be stored in an secure manner

### Environment

The deployment environment should be protected e.g. limit access to protected folders through the browser. This is controlled through folder/file access rights and configuration in web.xml and Tomcat config files (.htaccess etc).
- Examine the file structure. Are any components that should not be directly accessible available to the user?

All memory and resources allocations must have the appropriate de-allocated.

The application should not be vulnerable to injection, e.g. SQL

Any calls to the underlying operating system or file open calls should take care of Injection and handle any possible errors.

All logical decisions should have a default clause. E.g. while switching on an enum, try all cases and default to an "Unhandled Case exception".

## Java Security Practices

- **Limit the accessibility of classes, interfaces, methods, and fields** 
    - Don't have public / protected / package-private fields or methods in a class unless really needed. 
    - Understand the accessibility rules of protected and package-private
- **Limit the extensibility of classes and methods**
    - Extensibility of classes should be enabled only if it is required, not just for the sake of being extensible. 
- **Hard Coding**
    - Don't hard code any passwords or encryption keys
- **Serialization/Deserialization**
    - Be aware of serialisation / deserialisation exploits, e.g. spring data binding attacks

## OWASP Top 10

Familiarity with the [latest top 10 list](https://github.com/OWASP/Top10/raw/master/2017/OWASP%20Top%2010-2017%20(en).pdf) is essential.

## PCI

PCI-DSS Requirement 6: Develop and maintain secure systems and applications

- 6.1 Establish a process to identify security vulnerabilities, using reputable outside sources for security vulnerability information, and assign a risk ranking (for example, as â€œhigh,â€ â€œmedium,â€ or â€œlowâ€) to newly discovered security vulnerabilities.
- 6.2 Ensure that all system components and software are protected from known vulnerabilities by installing applicable vendor-supplied security patches. Install critical security patches within one month of release.
- 6.3 Develop software applications in accordance with PCI DSS and based on industry best practices. Incorporate information security **throughout the software development life cycle**.
    - 6.3.1 **Removal** of custom application accounts, user IDs, and passwords before applications become active or are released to customers
    - 6.3.2 **Review** of custom code prior to release to production or customers in order to identify any potential coding vulnerability.
- 6.4 Follow **change control processes and procedures** for all changes to system components. The processes must include the following:
    - 6.4.1 Separate development/test and production environments
    - 6.4.2 Separation of duties between development/test and production environments
    - 6.4.3 Production data (live PANs) are not used for testing or development
    - 6.4.4 Removal of test data and accounts before production systems become active
    - 6.4.5 Change control procedures for the implementation of security patches and software modifications. Procedures must include the following:
        - 6.4.5.1 Documentation of impact. 6.4.5.1 Verify that documentation of impact is included in the change control documentation for each sampled change.
        - 6.4.5.2 Documented change approval by authorized parties.
        - 6.4.5.3 Functionality testing to verify that the change does not adversely impact the security of the system.
        - 6.4.5.4 Back-out procedures.
- 6.5 Develop applications based on **secure coding guidelines**. Prevent common coding vulnerabilities in software evelopment processes, to include the following:
    - Note: The vulnerabilities listed at 6.5.1 through 6.5.9 were current with industry best practices when this version of PCI DSS was published. However, as industry best practices for vulnerability management are updated (for example, the OWASP Guide, SANS CWE Top 25, CERT Secure Coding, etc.), the current best practices must be used for these requirements.
    - 6.5.1 Injection flaws, particularly SQL injection. Also consider OS Command Injection, LDAP and XPath injection flaws as well as other injection flaws.
    - 6.5.2 Buffer overflow
    - 6.5.3 Insecure cryptographic storage
    - 6.5.4 Insecure communications
    - 6.5.5 Improper error handling
    - 6.5.6 All "High" vulnerabilities identified in the vulnerability identification process (as defined in PCI DSS Requirement 6.2).
    - 6.5.7 Cross-site scripting (XSS)
    - 6.5.8 Improper Access Control (such as insecure direct object references, failure to restrict URL access, and directory traversal)
    - 6.5.9 Cross-site request forgery (CSRF)
    - 6.5.10 Broken authentication and session management (New in PCI 3)
- 6.6 For public-facing web applications, address new threats and vulnerabilities on an ongoing basis and ensure these applications are protected against known attacks by either of the following methods:
    - Reviewing public-facing web applications via manual or automated application vulnerability security assessment tools or methods, at least annually and after any changes.
    - Installing a web-application firewall in front of public-facing web applications
- 6.7 Ensure that security policies and operational procedures for developing and maintaining secure systems and applications are documented, in use, and known to all affected parties.

## References

https://owasp.org/

https://owasp.org/www-community/Application_Threat_Modeling

https://owasp.org/www-project-top-ten/

https://owasp.org/www-project-proactive-controls/

https://owasp.org/www-project-web-security-testing-guide/

https://cheatsheetseries.owasp.org/

https://www.pcisecuritystandards.org/documents/PCI_DSS_v3-2-1.pdf
