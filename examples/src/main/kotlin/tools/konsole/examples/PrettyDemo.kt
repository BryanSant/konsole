package tools.konsole.examples

import tools.konsole.rich.Console
import tools.konsole.rich.inspect.Inspect
import tools.konsole.rich.pretty.Pretty

/**
 * Pretty-printing arbitrary Kotlin data structures and inspecting objects.
 *
 *   ./gradlew :examples:runExample -Pexample=PrettyDemo
 *
 * - `Pretty(value)` walks collections, primitives, and data classes,
 *   colorising via the `repr.*` theme entries.
 * - `Inspect(value)` renders a Panel listing the object's class, fields,
 *   and (optionally) methods — mirrors rich's `inspect()`.
 */

public data class Address(val street: String, val city: String, val zip: String)

public data class Person(
    val name: String,
    val age: Int,
    val nicknames: List<String>,
    val address: Address,
    val tags: Map<String, Any>,
)

public fun main() {
    val console = Console.system()

    val alice = Person(
        name = "Alice",
        age = 32,
        nicknames = listOf("Al", "Allie", "A"),
        address = Address(street = "221B Baker St", city = "London", zip = "NW1 6XE"),
        tags = mapOf("admin" to true, "score" to 99.5, "uuid" to "550e8400-e29b-41d4-a716-446655440000"),
    )

    console.print("[bold]Pretty:[/]")
    console.print(Pretty(alice))
    console.print()

    console.print("[bold]Pretty with maxDepth=1:[/]")
    console.print(Pretty(alice, maxDepth = 1))
    console.print()

    console.print("[bold]Pretty over a mixed collection:[/]")
    val mixed = listOf("hello", 42, 3.14, true, null, listOf(1, 2, 3), mapOf("k" to "v"))
    console.print(Pretty(mixed))
    console.print()

    console.print("[bold]Inspect(person, methods=false):[/]")
    console.print(Inspect(alice, methods = false))
}
