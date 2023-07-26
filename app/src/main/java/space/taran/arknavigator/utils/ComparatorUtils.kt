package dev.arkbuilders.navigator.utils

// can be used with `toSortedMap` method which glues together values if they have equal keys
// this method mocks makes comparators to return 0 only if objects are referentially equal
fun <T> unequalCompareBy(comparator: Comparator<T>): Comparator<T> =
    Comparator { a, b ->
        if (a === b) {
            return@Comparator 0
        }

        val result = comparator.compare(a, b)
        if (result == 0) {
            compareValues(
                System.identityHashCode(a),
                System.identityHashCode(b)
            )
        } else result
    }
