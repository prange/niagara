# Niagara-json - simple and safe json handling


Niagara-json is a lightweight and simple to use library for reading and writing json in Kotlin.
The philosopy behind the library is the principle of least surprise, which means it will
never throw an exception, all values are immutable and you can always rely on the types.

You also have to define converters to your domain types (classes) yourself, the take is that in the
long run it is safer to not let the arbitrary names of the properties of your classes
dictate the api surface of your application.

Lets check out `JsonParseExample.kt` from the `test/o.k.n.j.examples` directory.

```kotlin 
fun parse():JsonResult<JsonValue> {
    val input = """
        { 
            "name":"Ola Normann"
            , "stereotype":"brogrammer"
            , "level":3
            , "traits":["tattoo","beard","bun"]
        }
    """.trimIndent()

   val json:JsonResult<JsonValue> =
     JsonParser.parse(input)

    println(json)
    
    return json
}
```

The program parses a string into a JsonValue and prints it out before returning the result.
The return type of `JsonParser.parse` is `JsonResult<JsonValue>`. It wraps the 
actual json structure in a result object, since there are so many things that can go wrong
when parsing and reading json. `JsonResult` can be used to safely navigate, convert and read
from the wrapped object. Lets check it out.

```kotlin
fun readFields() {
    val json: JsonResult<JsonValue> = parse()
    
    val nameResult = //Should be a JsonSuccess wrapping the name
      json.field("name").asString()
    
    
    val failedTraits = //This is a failure, since the field "traits" is not a string but an array
      json.field("traits").asString()
    
    
    val notExistsField =  //This is also a failure, no field named "age" is present
      json.field("age").asInt()
}
```
But hey, we want the name, not the name wrapped in some result object. The thing is that
you cannot know if the operation you performed on the json actually succeeded. You have a pretty
good clue in this example since the input is crafted, but when the data comes from someone else
the field you expect might be missing or the json type is wrong. The `JsonResult` therefore
can be in two states: One success state which contains the expected value, or a failed state which
constains an error message explaining why the operation could not be done.


Since there shall be absolutely no surprises we have to make sure we cover our bases and prepare to
handle both the failed and the succeeded states:


```kotlin
fun readSafely() {
    val json: JsonResult<JsonValue> = parse()

    val nameResult = //Should be a JsonSuccess wrapping the name
      json.field("name").asString()

    //Get the value using a when expression
    val value =
      when (nameResult) {
          is JsonSuccess -> nameResult.value
          is JsonFail    -> nameResult.failures.toList().mkString(", ")
      }

    //Get the value using fold
    val foldValue =
      nameResult.fold(
        { failures -> failures.toList().mkString(", ") }, 
        { success -> success })
}
```
There are two options: Using a `when` expression or using the `fold` function. The `fold`
version is less verbose, and takes in two lambdas, one that handles the failure case and one
that handles the success. Bot lambdas must return the same type (int this example both return  `String`)