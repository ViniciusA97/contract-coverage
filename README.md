# SpoonWrapper Test Cases

This project contains unit tests to validate the functionality of the `SpoonWrapper` class, which is responsible for analyzing Java source code and extracting API endpoint information from `RestTemplate.exchange` method calls. Below is a detailed description of the test cases and what each one verifies.

## Test Cases

### 1. **Test Case: `test3/HttpClient3.java` and `test3/Main.java`**
- **Description**: This test case verifies that the `SpoonWrapper` can correctly extract the endpoint when the `RestTemplate.exchange` method is called directly with hardcoded arguments.
- **Expected Result**: The endpoint extracted should be:
    - **Path**: `/test-3`
    - **Method**: `POST`

### 2. **Test Case: `test5/Main.java`**
- **Description**: This test case ensures that the `SpoonWrapper` can handle cases where the URL is constructed using a constant and concatenation. The `RestTemplate.exchange` method is called with a dynamically constructed URL.
- **Expected Result**: The endpoint extracted should be:
    - **Path**: `/test-5`
    - **Method**: `POST`

### 3. **Test Case: `test6/HttpClient6.java`**
- **Description**: This test case validates that the `SpoonWrapper` can extract endpoint information when the `RestTemplate.exchange` method is called within a private method, and the arguments are defined as local variables.
- **Expected Result**: The endpoint extracted should be:
    - **Path**: `/test-6`
    - **Method**: `POST`

### 4. **Test Case: `test7/HttpClient7.java`**
- **Description**: This test case verifies that the `SpoonWrapper` can extract endpoints when the arguments of the `RestTemplate.exchange` method are passed as parameters to another method.
- **Expected Result**: The endpoint extracted should be:
    - **Path**: `/test-7`
    - **Method**: `GET`

### 5. **Test Case: `test8/HttpClient8.java`**
- **Description**: This test case ensures that the `SpoonWrapper` can handle cases where the URL and method are defined as constants in a separate class.
- **Expected Result**: The endpoint extracted should be:
    - **Path**: `/test-8`
    - **Method**: `PUT`

### 6. **Test Case: `test9/HttpClient9.java`**
- **Description**: This test case validates that the `SpoonWrapper` can process calls to the `RestTemplate.exchange` method where the arguments are obtained from helper methods.
- **Expected Result**: The endpoint extracted should be:
    - **Path**: `/test-9`
    - **Method**: `DELETE`

### 7. **Test Case: `test10/HttpClient10.java`**
- **Description**: This test case ensures that the `SpoonWrapper` can handle multiple calls to the `RestTemplate.exchange` method within the same method.
- **Expected Result**: The endpoints extracted should be:
    - **Path**: `/test-10-a`
    - **Method**: `PATCH`
    - **Path**: `/test-10-b`
    - **Method**: `POST`

## Summary

Each test case is designed to cover a different scenario:
- **Hardcoded arguments** (`test3`).
- **Dynamic URL construction** (`test5`).
- **Local variable usage in private methods** (`test6`).
- **Arguments passed as method parameters** (`test7`).
- **Constants in separate classes** (`test8`).
- **Helper methods for arguments** (`test9`).
- **Multiple calls in the same method** (`test10`).

These tests ensure that the `SpoonWrapper` can handle various ways of defining and passing arguments to the `RestTemplate.exchange` method.