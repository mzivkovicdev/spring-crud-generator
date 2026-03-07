# Security Policy

## Supported versions

Security fixes are provided on a best-effort basis for the latest released version of this project.

If you are reporting a vulnerability, please first confirm that it still affects the most recent release.

## Reporting a vulnerability

If you discover a security vulnerability in Spring CRUD Generator, please do **not** open a public GitHub issue.

Instead, report it privately by email:

- **Email:** mzivkovic.dev@gmail.com

You can also use the contact information listed in the project README for responsible disclosure.

Please include as much detail as possible:

- affected version
- steps to reproduce
- proof of concept, if available
- impact assessment
- any suggested mitigation or fix

## What to expect

After receiving a report, I will review it on a best-effort basis and respond as soon as possible.

If the report is confirmed as a legitimate security issue, I will aim to:

- assess severity and impact
- prepare a fix or mitigation
- publish the fix in a future release
- credit the reporter, if they want to be acknowledged

## Scope

This policy covers vulnerabilities in:

- the Maven plugin itself
- generation logic
- templates and generated resources produced by this project
- bundled configuration or defaults that could cause insecure output

It does **not** cover:

- vulnerabilities introduced by user modifications after generation
- insecure deployment of generated applications
- third-party dependencies outside this project unless the issue is directly related to how this project uses them

## Disclosure policy

Please allow time for investigation and remediation before making any vulnerability public.

Coordinated disclosure is appreciated.