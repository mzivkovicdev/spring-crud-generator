name: CI

on:
  push:
    branches: [ main, master ]
  pull_request:
    branches: [ '*' ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '${javaVersion}'
          cache: 'maven'

      - name: Build & run tests
        run: mvn -B verify

      - name: Upload Test Reports
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: test-reports
          path: '**/target/surefire-reports/'