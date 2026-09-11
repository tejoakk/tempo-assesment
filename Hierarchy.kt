import kotlin.random.Random
import kotlin.test.*

// The task:
// 1. Read and understand the Hierarchy data structure described in this file.
// 2. Implement filter() function.
// 3. Implement more test cases.
//
// The task should take 30-90 minutes.
//
// When assessing the submission, we will pay attention to:
// - correctness, efficiency, and clarity of the code;
// - the test cases.

/**
 * A `Hierarchy` stores an arbitrary _forest_ (an ordered collection of ordered trees)
 * as an array of node IDs in the order of DFS traversal, combined with a parallel array of node depths.
 *
 * Parent-child relationships are identified by the position in the array and the associated depth.
 * Each tree root has depth 0, its children have depth 1 and follow it in the array, their children have depth 2 and follow them, etc.
 *
 * Example:
 * ```
 * nodeIds: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11
 * depths:  0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2
 * ```
 *
 * the forest can be visualized as follows:
 * ```
 * 1
 * - 2
 * - - 3
 * - - - 4
 * - 5
 * 6
 * - 7
 * 8
 * - 9
 * - 10
 * - - 11
 *```
 * 1 is a parent of 2 and 5, 2 is a parent of 3, etc. Note that depth is equal to the number of hyphens for each node.
 *
 * Invariants on the depths array:
 *  * Depth of the first element is 0.
 *  * If the depth of a node is `D`, the depth of the next node in the array can be:
 *      * `D + 1` if the next node is a child of this node;
 *      * `D` if the next node is a sibling of this node;
 *      * `d < D` - in this case the next node is not related to this node.
 */
interface Hierarchy {
  /** The number of nodes in the hierarchy. */
  val size: Int

  /**
   * Returns the unique ID of the node identified by the hierarchy index. The depth for this node will be `depth(index)`.
   * @param index must be non-negative and less than [size]
   * */
  fun nodeId(index: Int): Int

  /**
   * Returns the depth of the node identified by the hierarchy index. The unique ID for this node will be `nodeId(index)`.
   * @param index must be non-negative and less than [size]
   * */
  fun depth(index: Int): Int

  fun formatString(): String {
    return (0 until size).joinToString(
      separator = ", ",
      prefix = "[",
      postfix = "]"
    ) { i -> "${nodeId(i)}:${depth(i)}" }
  }
}

/**
 * A node is present in the filtered hierarchy iff its node ID passes the predicate and all of its ancestors pass it as well.
 */
fun Hierarchy.filter(nodeIdPredicate: (Int) -> Boolean): Hierarchy {
  val resultIds = ArrayList<Int>()
  val resultDepths = ArrayList<Int>()

  // For each depth we've descended into, keptByDepth remembers whether the node
  // we're currently under at that depth passed. So if we're looking at a node at
  // depth d, keptByDepth[d - 1] tells us whether its parent got kept.
  val keptByDepth = ArrayList<Boolean>()

  for (i in 0 until size) {
    val d = depth(i)

    // depth went back down (or stayed put), meaning we've left whatever subtree
    // we were in - anything we tracked for deeper levels is stale now
    while (keptByDepth.size > d) {
      keptByDepth.removeAt(keptByDepth.size - 1)
    }

    val parentKept = d == 0 || keptByDepth[d - 1]
    val kept = parentKept && nodeIdPredicate(nodeId(i))

    if (kept) {
      resultIds.add(nodeId(i))
      resultDepths.add(d)
    }

    check(keptByDepth.size == d)
    keptByDepth.add(kept)
  }

  return ArrayBasedHierarchy(resultIds.toIntArray(), resultDepths.toIntArray())
}

class ArrayBasedHierarchy(
  private val myNodeIds: IntArray,
  private val myDepths: IntArray,
) : Hierarchy {
  override val size: Int = myDepths.size

  override fun nodeId(index: Int): Int = myNodeIds[index]

  override fun depth(index: Int): Int = myDepths[index]
}

class ArrayBasedHierarchyTest {

  @Test
  fun size() {
    val hierarchy = ArrayBasedHierarchy(intArrayOf(10, 20, 30), intArrayOf(0, 1, 0))
    assertEquals(3, hierarchy.size)
  }

  @Test
  fun sizeEmpty() {
    val hierarchy = ArrayBasedHierarchy(IntArray(0), IntArray(0))
    assertEquals(0, hierarchy.size)
  }

  @Test
  fun nodeIdLooksUpByIndex() {
    val hierarchy = ArrayBasedHierarchy(intArrayOf(10, 20, 30), intArrayOf(0, 1, 0))
    assertEquals(10, hierarchy.nodeId(0))
    assertEquals(20, hierarchy.nodeId(1))
    assertEquals(30, hierarchy.nodeId(2))
  }

  @Test
  fun depthLooksUpByIndex() {
    val hierarchy = ArrayBasedHierarchy(intArrayOf(10, 20, 30), intArrayOf(0, 1, 0))
    assertEquals(0, hierarchy.depth(0))
    assertEquals(1, hierarchy.depth(1))
    assertEquals(0, hierarchy.depth(2))
  }

  @Test
  fun formatString() {
    val hierarchy = ArrayBasedHierarchy(intArrayOf(1, 2, 3), intArrayOf(0, 1, 2))
    assertEquals("[1:0, 2:1, 3:2]", hierarchy.formatString())
  }

  @Test
  fun formatStringEmpty() {
    val hierarchy = ArrayBasedHierarchy(IntArray(0), IntArray(0))
    assertEquals("[]", hierarchy.formatString())
  }
}

class FilterTest {

  private fun assertFilter(
    unfiltered: Hierarchy,
    predicate: (Int) -> Boolean,
    expected: Hierarchy,
  ) {
    val actual = unfiltered.filter(predicate)
    assertEquals(expected.formatString(), actual.formatString())
  }

  @Test
  fun testFilter() {
    val unfiltered: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
      intArrayOf(0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2))
    val filteredActual: Hierarchy = unfiltered.filter { nodeId -> nodeId % 3 != 0 }
    val filteredExpected: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(1, 2, 5, 8, 10, 11),
      intArrayOf(0, 1, 1, 0, 1, 2))
    assertEquals(filteredExpected.formatString(), filteredActual.formatString())
  }

  @Test
  fun empty() {
    val unfiltered: Hierarchy = ArrayBasedHierarchy(IntArray(0), IntArray(0))
    assertFilter(unfiltered, { true }, unfiltered)
  }

  @Test
  fun keepEverything() {
    val unfiltered: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
      intArrayOf(0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2))
    assertFilter(unfiltered, { true }, unfiltered)
  }

  @Test
  fun keepNothing() {
    val unfiltered: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
      intArrayOf(0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2))
    val expected: Hierarchy = ArrayBasedHierarchy(IntArray(0), IntArray(0))
    assertFilter(unfiltered, { false }, expected)
  }

  @Test
  fun droppingRootTakesTheWholeSubtreeWithIt() {
    // node 1 fails, so 2/3/4/5 all go too even though they'd pass on their own.
    // other roots shouldn't care.
    val unfiltered: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
      intArrayOf(0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2))
    val expected: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(6, 7, 8, 9, 10, 11),
      intArrayOf(0, 1, 0, 1, 1, 2))
    assertFilter(unfiltered, { nodeId -> nodeId != 1 }, expected)
  }

  @Test
  fun droppingAMiddleNodeOnlyTakesItsOwnDescendants() {
    // node 2 fails, taking 3 and 4 down with it. its sibling 5 is unaffected,
    // same for everything outside that subtree.
    val unfiltered: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
      intArrayOf(0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2))
    val expected: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(1, 5, 6, 7, 8, 9, 10, 11),
      intArrayOf(0, 1, 0, 1, 0, 1, 1, 2))
    assertFilter(unfiltered, { nodeId -> nodeId != 2 }, expected)
  }

  @Test
  fun singleNode() {
    val unfiltered: Hierarchy = ArrayBasedHierarchy(intArrayOf(42), intArrayOf(0))
    assertFilter(unfiltered, { true }, unfiltered)
    assertFilter(unfiltered, { false }, ArrayBasedHierarchy(IntArray(0), IntArray(0)))
  }

  @Test
  fun linearChain() {
    // each node is the sole child of the one before it. cutting one off
    // partway down should take everything below it with it.
    val unfiltered: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(1, 2, 3, 4, 5),
      intArrayOf(0, 1, 2, 3, 4))
    val expected: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(1, 2),
      intArrayOf(0, 1))
    assertFilter(unfiltered, { nodeId -> nodeId < 3 }, expected)
  }

  @Test
  fun allRootsExcludedLeavesNothing() {
    val unfiltered: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(1, 2, 6, 7),
      intArrayOf(0, 1, 0, 1))
    val expected: Hierarchy = ArrayBasedHierarchy(IntArray(0), IntArray(0))
    // both roots fail, so it doesn't matter what the predicate thinks of their kids
    assertFilter(unfiltered, { nodeId -> nodeId != 1 && nodeId != 6 }, expected)
  }

  @Test
  fun siblingAfterADeepSubtreeIsUnaffected() {
    // this is really about the ancestor stack: after walking all the way down
    // one branch, popping back up to a shallower sibling has to forget what
    // it knew about the deep branch, not carry it forward by mistake.
    val unfiltered: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(1, 2, 3, 4, 5),
      intArrayOf(0, 1, 2, 0, 1))
    val expected: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(1, 2, 3, 4),
      intArrayOf(0, 1, 2, 0))
    assertFilter(unfiltered, { nodeId -> nodeId != 5 }, expected)
  }

  @Test
  fun predicateIsNeverAskedAboutDroppedSubtrees() {
    // once a node's out, its kids are out regardless of what the predicate
    // says - so we shouldn't even bother calling it on them. worth locking
    // down in case the predicate is doing something non-trivial (a lookup,
    // a network call, whatever) rather than a pure check.
    val unfiltered: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
      intArrayOf(0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2))
    unfiltered.filter { nodeId ->
      if (nodeId == 4) fail("node 4 shouldn't have been checked, its parent (3) already failed")
      nodeId != 3
    }
  }

  @Test
  fun matchesANaiveTreeImplOnRandomForests() {
    // hand-picked cases above cover what I thought of. this throws a few
    // hundred random forests at both filter() and a slow-but-obviously-right
    // recursive version and checks they agree, as a wider net.
    val random = Random(seed = 42)
    repeat(300) {
      val size = random.nextInt(0, 25)
      val (nodeIds, depths) = randomForest(random, size)
      val unfiltered: Hierarchy = ArrayBasedHierarchy(nodeIds, depths)

      val keepDecision = nodeIds.toList().associateWith { random.nextInt(0, 3) != 0 }
      val predicate: (Int) -> Boolean = { id -> keepDecision.getValue(id) }

      assertFilter(unfiltered, predicate, naiveFilter(unfiltered, predicate))
    }
  }

  // builds a random forest that still respects the depth invariants from the class doc
  private fun randomForest(random: Random, size: Int): Pair<IntArray, IntArray> {
    val nodeIds = IntArray(size) { it + 1 }
    val depths = IntArray(size)
    var previousDepth = -1
    for (i in 0 until size) {
      // can start a new tree, go one deeper (a child), or stay level/go
      // shallower (a sibling somewhere up the chain) - never jump down more than one
      depths[i] = if (i == 0) 0 else random.nextInt(0, previousDepth + 2)
      previousDepth = depths[i]
    }
    return nodeIds to depths
  }

  // slow reference version: actually build the tree, prune failing nodes and
  // their subtrees, then flatten back to arrays. kept deliberately separate
  // from filter()'s bookkeeping so it's a real cross-check, not the same bug twice.
  private fun naiveFilter(hierarchy: Hierarchy, predicate: (Int) -> Boolean): Hierarchy {
    class Node(val id: Int, val children: MutableList<Node> = mutableListOf())

    var index = 0
    fun parseChildren(depth: Int): MutableList<Node> {
      val nodes = mutableListOf<Node>()
      while (index < hierarchy.size && hierarchy.depth(index) >= depth) {
        val node = Node(hierarchy.nodeId(index))
        index++
        node.children.addAll(parseChildren(depth + 1))
        nodes.add(node)
      }
      return nodes
    }
    val roots = parseChildren(depth = 0)

    fun filterNodes(nodes: List<Node>): List<Node> =
      nodes.filter { predicate(it.id) }.map { Node(it.id, filterNodes(it.children).toMutableList()) }
    val filteredRoots = filterNodes(roots)

    val resultIds = mutableListOf<Int>()
    val resultDepths = mutableListOf<Int>()
    fun flatten(nodes: List<Node>, depth: Int) {
      for (node in nodes) {
        resultIds.add(node.id)
        resultDepths.add(depth)
        flatten(node.children, depth + 1)
      }
    }
    flatten(filteredRoots, depth = 0)

    return ArrayBasedHierarchy(resultIds.toIntArray(), resultDepths.toIntArray())
  }
}
