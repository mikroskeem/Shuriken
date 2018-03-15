![Tryhard logo](https://nightsnack.cf/assets/shuriken/logo-w-background-256x256.png) 
# Shuriken

[![Build Status](https://travis-ci.org/mikroskeem/Shuriken.svg?branch=master)](https://travis-ci.org/mikroskeem/Shuriken)

The Java utilities collection.  
See wiki for code samples

## License
MIT

## Shuriken components

### Common
Package: `eu.mikroskeem.shuriken.common`  
Contains common utilities used in my projects (notnull-checks, SneakyThrow, InputStream to byte array etc.)

### Instrumentation
Package: `eu.mikroskeem.shuriken.instrumentation`  
Contains useful instrumentation related utilities, like:
- Class method/field/constructor & extending verification
- Method signature generator
- Class loader tools

### Reflect
Package: `eu.mikroskeem.shuriken.reflect`  
Awesome reflection library, which is more convenient to use than vanilla reflection. Retains type-safety (OOP like a boss lol) and uses Optional

### Injector
Package: `eu.mikroskeem.shuriken.injector`  
Simple `javax.inject`-based injector. Not fully compatible with it, but works fine

### Classloader
Package: `eu.mikroskeem.shuriken.classloader`  
Classloader which is able to load classes compressed with [Brotli](https://en.wikipedia.org/wiki/Brotli).  
See [this](https://git.mikroskeem.eu/mikroskeem/ShurikenMavenPlugin) for Maven plugin and more information.
