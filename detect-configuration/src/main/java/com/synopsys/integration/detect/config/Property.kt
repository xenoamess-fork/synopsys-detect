package com.synopsys.integration.detect.config

import java.lang.Exception

/**
 * This is the most basic property.
 * It has no type information and a value cannot be retrieved for it (without a subclass).
 * It is recommended things that only appear in the help but do not actually appear as 'keys' use this type - phone home pass-through for example.
**/
abstract class Property(val key: String) {
    var name: String? = null
    var from: String? = null
    var helpShort: String? = null
    var helpLong:String? = null
    var primaryGroup: Group? = null
    var additionalGroups: List<Group>? = null
    var category: Category = Category.Simple

    fun info(name:String, from: String): Property {
        this.name = name
        this.from = from
        return this
    }
    fun help(short:String, long: String? = null): Property  {
        this.helpShort = short
        this.helpLong = long
        return this
    }
    fun groups(primaryGroup: Group, vararg additionalGroups: Group): Property  {
        this.primaryGroup = primaryGroup
        this.additionalGroups = additionalGroups.toList()
        return this
    }
    fun category(category: Category): Property  {
        this.category = category
        return this
    }

    open fun isCaseSensitive(): Boolean = false
    open fun isOnlyExampleValues(): Boolean = false
    open fun listExampleValues(): List<String>? = emptyList()
    open fun describeDefault(): String? = null
}

abstract class TypedProperty<T>(key: String, val parser: ValueParser<T>) : Property(key)

abstract class OptionalProperty<T>(key: String, parser: ValueParser<T>) : TypedProperty<T>(key, parser) {
    // This is a property with a key and a value, but the value is optional.
}

abstract class RequiredProperty<T>(key: String, parser: ValueParser<T>, val default: T) : TypedProperty<T>(key, parser) {
    // This is a property with a key and with a default value, it will always have a value.
}

abstract class ValueParser<T> {
    @Throws(ValueParseException::class)
    abstract fun parse(value: String) : T
}

class ValueParseException (rawValue: String, typeName: String, additionalMessage:String = "", innerException: Exception? = null) : Exception("Unable to parse raw value '${rawValue}' and coerce it into type '${typeName}'. $additionalMessage", innerException)
