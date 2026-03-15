package org.example.core.wrappers.spoon

import spoon.reflect.code.*
import spoon.reflect.declaration.CtMethod
import spoon.reflect.declaration.CtParameter
import spoon.reflect.reference.CtParameterReference

/**
 * Analyzes parameter flow in expressions to detect when a URL depends on method parameters.
 * 
 * This enables resolving URLs through call sites when the URL is built using parameters:
 * ```java
 * void makeRequest(String endpoint) {
 *     restTemplate.post(baseUrl + endpoint, ...);  // endpoint is parameter
 * }
 * 
 * // Call site:
 * makeRequest("/api/users");  // Can resolve endpoint = "/api/users"
 * ```
 */
class ParameterFlowAnalyzer {
    
    /**
     * Result of parameter dependency analysis
     */
    data class ParameterDependency(
        val parameterIndex: Int,
        val parameterName: String,
        val isFullExpression: Boolean  // true if entire expression is just the parameter
    )
    
    /**
     * Find which method parameters are used in an expression.
     * 
     * @param expression The expression to analyze (e.g., URL argument)
     * @param method The method containing this expression
     * @return List of parameter dependencies found
     */
    fun findParameterDependencies(
        expression: CtExpression<*>,
        method: CtMethod<*>
    ): List<ParameterDependency> {
        val parameters = method.parameters
        if (parameters.isEmpty()) return emptyList()
        
        val dependencies = mutableListOf<ParameterDependency>()
        
        // Check if expression directly is a parameter reference
        if (expression is CtVariableRead<*>) {
            val ref = expression.variable
            if (ref is CtParameterReference<*>) {
                val paramIndex = findParameterIndex(ref.simpleName, parameters)
                if (paramIndex >= 0) {
                    dependencies.add(ParameterDependency(paramIndex, ref.simpleName, true))
                    return dependencies
                }
            }
        }
        
        // Recursively find parameter references in the expression
        findParameterReferencesRecursive(expression, parameters, dependencies)
        
        return dependencies.distinctBy { it.parameterIndex }
    }
    
    private fun findParameterReferencesRecursive(
        element: spoon.reflect.declaration.CtElement,
        parameters: List<CtParameter<*>>,
        result: MutableList<ParameterDependency>
    ) {
        when (element) {
            is CtVariableRead<*> -> {
                val ref = element.variable
                if (ref is CtParameterReference<*>) {
                    val paramIndex = findParameterIndex(ref.simpleName, parameters)
                    if (paramIndex >= 0) {
                        result.add(ParameterDependency(paramIndex, ref.simpleName, false))
                    }
                } else {
                    // Could be a local variable - check if its name matches a parameter
                    val varName = ref.simpleName
                    val paramIndex = findParameterIndex(varName, parameters)
                    if (paramIndex >= 0) {
                        result.add(ParameterDependency(paramIndex, varName, false))
                    }
                }
            }
        }
        
        for (child in element.directChildren) {
            findParameterReferencesRecursive(child, parameters, result)
        }
    }
    
    private fun findParameterIndex(name: String, parameters: List<CtParameter<*>>): Int {
        return parameters.indexOfFirst { it.simpleName == name }
    }
    
    /**
     * Check if a local variable's value comes from a method parameter.
     * Traces simple assignments like: String url = baseUrl + endpoint;
     * 
     * Returns only direct parameter references (not parameters used as method targets).
     * 
     * @param varName The variable name to trace
     * @param method The method to search in
     * @return The parameter dependency if found, null otherwise
     */
    fun traceVariableToParameter(
        varName: String,
        method: CtMethod<*>
    ): List<ParameterDependency>? {
        val body = method.body ?: return null
        
        // Find assignment to this variable
        val assignment = findVariableAssignment(varName, body) ?: return null
        
        // Find the direct parameter index (for path-like usage)
        val pathParamIndex = findPathParameterIndex(assignment, method)
        if (pathParamIndex >= 0) {
            val paramName = method.parameters[pathParamIndex].simpleName
            return listOf(ParameterDependency(pathParamIndex, paramName, false))
        }
        
        return null
    }
    
    private fun findVariableAssignment(varName: String, block: CtBlock<*>): CtExpression<*>? {
        for (statement in block.statements) {
            when (statement) {
                is CtLocalVariable<*> -> {
                    if (statement.simpleName == varName) {
                        return statement.defaultExpression
                    }
                }
                is CtAssignment<*, *> -> {
                    val assigned = statement.assigned
                    if (assigned is CtVariableWrite<*> && assigned.variable.simpleName == varName) {
                        return statement.assignment
                    }
                }
            }
        }
        return null
    }
    
    /**
     * Extract the "path" portion from an expression that concatenates base URL + endpoint.
     * 
     * For expression like: config.getUrl() + endpoint
     * Returns: the part that represents the endpoint (parameter reference)
     * 
     * @param expression The full URL expression
     * @param method The containing method
     * @return The parameter index that contains the path, or -1 if not found
     */
    fun findPathParameterIndex(
        expression: CtExpression<*>,
        method: CtMethod<*>
    ): Int {
        // For binary operations like "baseUrl + endpoint"
        if (expression is CtBinaryOperator<*>) {
            // Check right operand first (usually the endpoint in "base + endpoint")
            val rightIndex = findDirectParameterIndex(expression.rightHandOperand, method)
            if (rightIndex >= 0) {
                return rightIndex
            }
            
            // Check left operand
            val leftIndex = findDirectParameterIndex(expression.leftHandOperand, method)
            if (leftIndex >= 0) {
                return leftIndex
            }
        }
        
        // Direct parameter reference
        return findDirectParameterIndex(expression, method)
    }
    
    /**
     * Find parameter index for direct parameter references only.
     * Excludes parameters used as method call targets (e.g., config.getUrl())
     * 
     * @return Parameter index or -1 if not a direct parameter reference
     */
    private fun findDirectParameterIndex(expression: CtExpression<*>, method: CtMethod<*>): Int {
        // Only match direct variable reads, not method invocations
        if (expression is CtVariableRead<*>) {
            val varName = expression.variable.simpleName
            val paramIndex = method.parameters.indexOfFirst { it.simpleName == varName }
            if (paramIndex >= 0) {
                // Verify it's a String-like parameter (endpoint paths are strings)
                val paramType = method.parameters[paramIndex].type?.simpleName
                if (paramType in listOf("String", "Object", null)) {
                    return paramIndex
                }
            }
        }
        
        return -1
    }
}
