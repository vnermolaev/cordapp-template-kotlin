package com.vne

import java.util.ArrayDeque

class ConcatIterator<T>(iterator: Iterator<T>) : Iterator<T>, Iterable<T> {
    private val store = ArrayDeque<Iterator<T>>()

    init {
        if (iterator.hasNext())
            store.add(iterator)
    }

    override fun hasNext(): Boolean = when {
        store.isEmpty() -> false
        else -> store.first.hasNext()
    }

    override fun next(): T {
        val t = store.first.next()

        if (!store.first.hasNext())
            store.removeFirst()

        return t
    }

    operator fun plus(iterator: Iterator<T>): ConcatIterator<T> {
        if (iterator.hasNext())
            store.add(iterator)
        return this
    }

    override fun iterator(): Iterator<T> {
        return this;
    }
}

operator fun <T> Iterator<T>.plus(iterator: Iterator<T>): ConcatIterator<T> =
    when {
        this is ConcatIterator<T> -> this.plus(iterator)
        iterator is ConcatIterator<T> -> iterator.plus(this)
        else -> ConcatIterator(this).plus(iterator)
    }
