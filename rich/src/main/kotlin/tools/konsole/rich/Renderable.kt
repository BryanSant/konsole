package tools.konsole.rich

/**
 * Anything that can be rendered to a stream of [Segment]s.
 *
 * Implementations must be **idempotent**: the rendering pipeline may call [render]
 * twice (once for measurement, once for output) and expects identical results.
 *
 * For cheaper measurement, also implement [Measurable].
 */
public fun interface Renderable {
    public fun render(console: Console, options: RenderOptions): Sequence<Segment>
}

/** A [Renderable] that knows its own min/max width without rendering. */
public interface Measurable : Renderable {
    public fun measure(console: Console, options: RenderOptions): Measurement
}

/**
 * Adapter from an arbitrary value type to a [Renderable].
 *
 * Register on a [Console] via [Console.registerAdapter] to make [Console.print] accept the type
 * directly without an explicit `toRenderable(...)` call.
 */
public fun interface ToRenderable<in T> {
    public fun toRenderable(value: T): Renderable
}
