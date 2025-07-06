package com.ra4king.circuitsim.gui

import com.ra4king.circuitsim.gui.LinkWires.Wire
import com.ra4king.circuitsim.gui.PathFinding.LocationPreference.*
import java.util.*

/**
 * @author Roi Atalla
 */
object PathFinding {
    /**
     * Computes the best path between the source point and destination point.
     * @param sx source x
     * @param sy source y
     * @param dx destination x
     * @param dy destination y
     * @param callback a callback which indicates
     * whether a wire can be placed at a given point and orientation
     * @return the set of wires that form the best path between the two points
     */
	@JvmStatic
	fun bestPath(sx: Int, sy: Int, dx: Int, dy: Int, callback: ValidWireLocation): MutableSet<Wire>? {
        if (dx < 0 || dy < 0) return mutableSetOf()

        val source = Point(sx, sy)
        val destination = Point(dx, dy)
        
        // Map of points to their corresponding parent point.
        // 
        // Also acts as the visited set.
        // If a key exists in this map, it is considered "visited".
        val parents = HashMap<Point, Point?>()
        val frontier = PriorityQueue<Node>()

        frontier.add(Node(Cost(0, 0, 0), source, null, null))
        var iterations = 0
        while (!frontier.isEmpty()) {
            if (Thread.currentThread().isInterrupted) return mutableSetOf()

            iterations++
            if (iterations == 5000) {
                System.err.println("Path finding taking too long, bail...")
                return mutableSetOf()
            }

            val current = frontier.poll()
            // Skip if already visited
            if (parents.containsKey(current.point)) continue
            parents[current.point] = current.parent

            if (destination == current.point) 
                return constructPath(parents, current.point)

            // Add neighbors
            for (direction in Direction.entries) {
                // Filter opposite direction (cause wires can't move backwards)
                // and out-of-bounds neighbors.
                if (direction.opposite == current.directionEntered) continue
                val neighbor = direction.move(current.point)
                if (neighbor.x < 0 || neighbor.y < 0) continue

                val preference = callback.isValidWireLocation(neighbor.x, neighbor.y, direction.isHorizontal)

                val additionalLength = when (preference) {
                    PREFER -> 0
                    VALID -> 1
                    INVALID -> continue
                }

                var additionalTurns = 0
                if (current.directionEntered != null && direction != current.directionEntered) {
                    additionalTurns = if (neighbor.x == destination.x || neighbor.y == destination.y) 1 else 2
                }

                val totalCost = Cost(
                    current.cost.length + additionalLength,
                    current.cost.turns + additionalTurns,
                    neighbor.manhattanDistance(destination)
                )
                frontier.add(Node(totalCost, neighbor, current.point, direction))
            }
        }

        System.err.println("No possible paths found...")
        return null
    }

    private fun constructPath(cameFrom: MutableMap<Point, Point?>, current: Point): MutableSet<Wire> {
        var current = current
        val totalPath = HashSet<Wire>()

        var lastEndpoint = current
        var next = cameFrom[current]
        while (next != null) {
            if (!(lastEndpoint.x == current.x && current.x == next.x) &&
                !(lastEndpoint.y == current.y && current.y == next.y)
            ) {
                val len = (current.x - lastEndpoint.x) + (current.y - lastEndpoint.y)
                totalPath.add(Wire(null, lastEndpoint.x, lastEndpoint.y, len, lastEndpoint.y == current.y))
                lastEndpoint = current
            }

            current = next
            next = cameFrom[current]
        }

        val len = (current.x - lastEndpoint.x) + (current.y - lastEndpoint.y)
        if (len != 0) {
            totalPath.add(Wire(null, lastEndpoint.x, lastEndpoint.y, len, lastEndpoint.y == current.y))
        }

        return totalPath
    }

    /**
     * Value that is returned in a [PathFinding.ValidWireLocation] callback.
     */
    enum class LocationPreference {
        /**
         * Under no circumstances may this point be part of the wire path.
         */
        INVALID,

        /**
         * This point may be part of the wire path.
         */
        VALID,

        /**
         * This point should be part of the wire path.
         */
        PREFER
    }

    /**
     * Callback for generating whether a given point should be allowed to be part of a path.
     */
    fun interface ValidWireLocation {
        fun isValidWireLocation(x: Int, y: Int, horizontal: Boolean): LocationPreference
    }

    private enum class Direction(private val deltaX: Int, private val deltaY: Int) {
        RIGHT(1, 0), LEFT(-1, 0), DOWN(0, 1), UP(0, -1);

        val opposite: Direction
            get() = when (this) {
                RIGHT -> LEFT
                LEFT -> RIGHT
                DOWN -> UP
                UP -> DOWN
            }
        val isHorizontal: Boolean
            get() = this == LEFT || this == RIGHT

        fun move(point: Point) = Point(point.x + deltaX, point.y + deltaY)
    }

    class Point internal constructor(val x: Int, val y: Int) {
        
        fun manhattanDistance(other: Point): Int {
            val dx = this.x - other.x
            val dy = this.y - other.y
            return Math.absExact(dx) + Math.absExact(dy)
        }

        override fun hashCode() = Objects.hash(x, y)

        override fun equals(other: Any?) = other is Point && this.x == other.x && this.y == other.y

        override fun toString() = "Point(x = $x, y = $y)"
    }

    private class Cost(val length: Int, val turns: Int, private val heuristicCost: Int) : Comparable<Cost> {
            
        val score get() = this.turns * 5 + this.length + this.heuristicCost

        override fun compareTo(other: Cost) = this.score.compareTo(other.score)

        override fun hashCode() = Objects.hash(length, turns, heuristicCost)

        override fun equals(other: Any?) = other is Cost && this.length == other.length && 
                this.turns == other.turns && this.heuristicCost == other.heuristicCost

        override fun toString() = "Cost(length = $length, turns = $turns, heuristicCost = $heuristicCost)"
    }

    private class Node(
        val cost: Cost,
        val point: Point,
        val parent: Point?,
        val directionEntered: Direction?
    ) : Comparable<Node> {
        
        // Comparisons are only based on cost.
        // The rest is extra data that tags along.
        override fun compareTo(other: Node) = this.cost.compareTo(other.cost)

        override fun equals(other: Any?) = other is Node && this.cost == other.cost

        override fun hashCode() = Objects.hash(cost)
    }
}
