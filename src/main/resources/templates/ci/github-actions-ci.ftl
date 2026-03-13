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

      - name: Cache Maven repository
        uses: actions/cache@v4
        with:
          path: |
            ~/.m2/repository
          key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
          restore-keys: |
            ${{ runner.os }}-maven-

      - name: Build & run tests
        run: mvn -B verify

      - name: Upload Test Reports
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: test-reports
          path: '**/target/surefire-reports/'